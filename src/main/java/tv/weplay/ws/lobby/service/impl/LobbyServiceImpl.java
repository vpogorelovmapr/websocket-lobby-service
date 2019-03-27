package tv.weplay.ws.lobby.service.impl;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static tv.weplay.ws.lobby.model.dto.TournamentMemberRole.*;
import static tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService.DEFAULT_EXCHANGE;
import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.config.properties.RabbitmqProperties;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.mapper.LobbyMapper;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;
import tv.weplay.ws.lobby.model.error.ErrorType;
import tv.weplay.ws.lobby.repository.LobbyRepository;
import tv.weplay.ws.lobby.scheduled.MatchStartJob;
import tv.weplay.ws.lobby.scheduled.VoteJob;
import tv.weplay.ws.lobby.service.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    private static final String LOBBY_ID = "lobbyId";

    private final LobbyMapper lobbyMapper;
    private final LobbyRepository lobbyRepository;
    private final EventSenderService eventSenderService;
    private final RabbitmqProperties rmqProperties;
    private final JsonApiConverter converter;
    private final SchedulerService schedulerService;
    private final ErrorHandlerService errorHandlerService;

    @Override
    public Lobby create(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        entity.setLobbyStartDatetime(LocalDateTime.now());
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby created = lobbyMapper.toDTO(createdEntity);
        log.info("Created lobby {}", created);

        Lobby event = buildLobbyCreatedEvent(created);
        String routingKey = buildInvitesRoutingKey(lobby);
        publishInvitesToRMQ(event, routingKey);

        scheduleLobbyStartJob(created);

        return created;
    }

    @Override
    public Lobby update(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        LobbyEntity updatedEntity = lobbyRepository.save(entity);
        Lobby updated = lobbyMapper.toDTO(updatedEntity);
        log.info("Updated lobby {}", updated);
        return updated;
    }

    @Override
    public void delete(Long id) {
        log.info("Delete lobby with id {}", id);
        lobbyRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        log.info("Delete all lobbies");
        lobbyRepository.deleteAll();
    }

    @Override
    public Lobby findById(Long id) {
        return lobbyMapper.toDTO(lobbyRepository.findById(id).orElse(null));
    }

    @Override
    public List<Lobby> findAll() {
        return lobbyMapper.toDTOs(lobbyRepository.findAll());
    }

    @Override
    public void startOrCancelLobby(Long lobbyId) {
        if (allMatchMemberPresent(lobbyId)) {
            start(lobbyId);
        } else {
            cancel(lobbyId);
        }
    }

    @Override
    public void start(Long lobbyId) {
        log.info("Start voting. Lobby id: {} ", lobbyId);
        Lobby lobby = findById(lobbyId);
        lobby.setStatus(LobbyStatus.ONGOING);
        update(lobby);

        Lobby event = buildMatchStartEvent(lobby);
        publishEventToRMQ(event, lobby.getId().toString(), EventTypes.LOBBY_STARTED);

        if (isVotingCompleted(lobby)) {
            lobby.setStatus(LobbyStatus.ENDED);
            Lobby endEvent = buildChangeLobbyStatusEvent(lobby);
            publishEventToRMQ(endEvent, lobby.getId().toString(), EventTypes.LOBBY_ENDED);

            log.info("Lobby[{}] state before deletion: {}", lobby.getId(), lobby);
            delete(lobbyId);
        } else {
            scheduleVoteJob(lobbyId, lobby.getSettings().getVoteTime());
        }
    }

    @Override
    public void cancel(Long lobbyId) {
        log.info("Switching to cancel lobby sate. Lobby id: {} ", lobbyId);
        Lobby lobby = findById(lobbyId);
        if (Objects.isNull(lobby)) {
            log.error("Lobby [{}] doesn't exist", lobbyId);
            return;
        }
        if (lobby.getStatus().equals(LobbyStatus.UPCOMING)) {
            log.info("Removing job {}", LOBBY_PREFIX + lobbyId);
            schedulerService.unschedule(LOBBY_PREFIX + lobbyId, MATCH_START_GROUP);
        }
        if (lobby.getStatus().equals(LobbyStatus.ONGOING)) {
            log.info("Removing job {}", VOTE_PREFIX + lobbyId);
            schedulerService.unschedule(VOTE_PREFIX + lobbyId, VOTE_GROUP);
        }
        lobby.setStatus(LobbyStatus.CANCELED);
        update(lobby);

        Lobby event = buildChangeLobbyStatusEvent(lobby);
        publishEventToRMQ(event, lobby.getId().toString(), EventTypes.LOBBY_CANCELED);

        String userInformation = getUsersInformation(lobby);
        log.info("Lobby[{}] state was canceled. USer information: {}", userInformation);
        sendErrorNotification(lobby.getId(), ErrorType.LOBBY_CANCELED, Optional.of(userInformation));
        log.info("Lobby[{}] state before deletion: {}", lobby.getId(), lobby);
        delete(lobbyId);
    }

    @Override
    public void updateMemberStatus(Long lobbyId, Long memberId) {
        log.info("Updating member with id {} for lobby {}", memberId, lobbyId);
        Lobby lobby = findById(lobbyId);
        log.info("Lobby found: {}", lobby);
        if (isNull(lobby)) {
            sendErrorNotification(lobbyId, ErrorType.LOBBY_NOT_EXIST, Optional.empty());
            return;
        }
        Optional<MatchMember> matchMember = lobby.getMatch().getMembers().stream()
                .filter(member -> member.getId().equals(memberId))
                .findAny();
        matchMember.ifPresent(member -> {
            member.setStatus(MemberStatus.ONLINE);
            update(lobby);
            MatchMember event = buildMatchMemberEvent(member, lobbyId);
            publishEventToRMQ(event, lobby.getId().toString(), EventTypes.MEMBER);
        });
    }

    @Override
    public void voteRandomCard(Long lobbyId, LobbyMapStatus type) {
        Lobby lobby = findById(lobbyId);
        Long cardId = getRandomCardId(lobby);
        voteCardByServer(lobby.getId(), cardId, type);
    }

    @Override
    public void voteCardByServer(Long lobbyId, Long cardId, LobbyMapStatus status) {
        Lobby lobby = findById(lobbyId);
        Optional<LobbyMap> lobbyMap = getNextLobbyMap(lobby);
        lobbyMap.ifPresent(map -> {
            map.setVoteItem(new VoteItem(cardId));
            map.setStatus(status);
            update(lobby);

            LobbyMap event = buildLobbyMapEvent(map, lobbyId);
            publishEventToRMQ(event, lobby.getId().toString(), EventTypes.VOTE);
            voteRandomCardIfLastVote(lobby);
        });
    }

    @Override
    public void voteCardByUser(Long lobbyId, LobbyMap lobbyMap, Long userId) {
        Lobby lobby = findById(lobbyId);
        Optional<LobbyMap> nextLobbyMap = getNextLobbyMap(lobby);
        nextLobbyMap.ifPresent(map -> {
            if (!isValidVoteRequest(lobbyMap, lobby, map, userId)) {
                return;
            }
            map.setVoteItem(lobbyMap.getVoteItem());
            map.setStatus(LobbyMapStatus.USER_PICK);
            Lobby updated = update(lobby);
            if (!isLastVote(updated)) {
                rescheduleVoteJob(lobbyId, updated);
            }

            LobbyMap event = buildLobbyMapEvent(map, lobbyId);
            publishEventToRMQ(event, lobby.getId().toString(), EventTypes.VOTE);

            voteRandomCardIfLastVote(lobby);
        });
    }

    private void rescheduleVoteJob(Long lobbyId, Lobby lobby) {
        schedulerService.unschedule(VOTE_PREFIX + lobbyId, VOTE_GROUP);
        scheduleVoteJob(lobbyId, lobby.getSettings().getVoteTime());
    }

    private boolean isVotingCompleted(Lobby lobby) {
        return lobby.getLobbyMap().stream()
                .map(LobbyMap::getVoteItem)
                .noneMatch(Objects::isNull);
    }

    private boolean isLastVote(Lobby lobby) {
        return lobby.getLobbyMap().stream()
                .map(LobbyMap::getVoteItem)
                .filter(Objects::isNull)
                .count() == 1;
    }

    private void voteRandomCardIfLastVote(Lobby lobby) {
        if (isLastVote(lobby)) {
            lobby.setStatus(LobbyStatus.ENDED);
            voteRandomCard(lobby.getId(), LobbyMapStatus.SERVER_PICK);
            schedulerService.unschedule(VOTE_PREFIX + lobby.getId(), VOTE_GROUP);
            Lobby event = buildChangeLobbyStatusEvent(lobby);
            publishEventToRMQ(event, lobby.getId().toString(), EventTypes.LOBBY_ENDED);
            log.info("Lobby[{}] state before deletion: {}", lobby.getId(), lobby);
            delete(lobby.getId());
        }
    }

    @SneakyThrows
    private void publishInvitesToRMQ(Object event, String routingKey) {
        byte[] data = converter.writeObject(event);
        log.info("Publishing event to rabbitMQ [{}]", new String(data));
        eventSenderService
                .prepareAndSendEvent(rmqProperties.getOutcomingPrivateQueueName(), data,
                routingKey, EventTypes.LOBBY_CREATED);
        eventSenderService.prepareAndSendEvent(DEFAULT_EXCHANGE, data,
                rmqProperties.getOutcomingTournamentsQueueName(),
                EventTypes.LOBBY_CREATED);
    }

    @SneakyThrows
    private void publishEventToRMQ(Object event, String routingKey, String type) {
        byte[] data = converter.writeObject(event);
        log.info("Publishing event to rabbitMQ [{}]", new String(data));
        eventSenderService.prepareAndSendEvent(rmqProperties.getOutcomingUiQueueName(), data,
                routingKey, type);
        eventSenderService.prepareAndSendEvent(DEFAULT_EXCHANGE, data,
                rmqProperties.getOutcomingTournamentsQueueName(), type);
    }

    private String buildInvitesRoutingKey(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .map(MatchMember::getTournamentMember)
                .map(TournamentMember::getMember)
                .map(Member::getId)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private MatchMember buildMatchMemberEvent(MatchMember member, Long lobbyId) {
        return MatchMember.builder()
                .id(member.getId())
                .status(member.getStatus())
                .lobby(Lobby.builder().id(lobbyId).build())
                .build();
    }

    private LobbyMap buildLobbyMapEvent(LobbyMap map, Long lobbyId) {
        return LobbyMap.builder()
                .id(map.getId())
                .lobby(Lobby.builder().id(lobbyId).build())
                .status(map.getStatus())
                .voteItem(map.getVoteItem())
                .updatedDatetime(LocalDateTime.now())
                .build();
    }

    private Lobby buildLobbyCreatedEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .startDatetime(lobby.getStartDatetime())
                .status(lobby.getStatus())
                .build();
    }

    private Lobby buildChangeLobbyStatusEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .status(lobby.getStatus())
                .build();
    }

    private Lobby buildMatchStartEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .status(lobby.getStatus())
                .build();
    }

    private void scheduleLobbyStartJob(Lobby lobby) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobby.getId());
        schedulerService.schedule(LOBBY_PREFIX + lobby.getId(), MATCH_START_GROUP,
                ZonedDateTime.now().plusSeconds(lobby.getDuration()), dataMap, MatchStartJob.class);
    }

    private void scheduleVoteJob(Long lobbyId, Integer interval) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobbyId);
        schedulerService.schedule(VOTE_PREFIX + lobbyId, VOTE_GROUP,
                ZonedDateTime.now().plusSeconds(interval), dataMap,
                interval, VoteJob.class);
    }

    private Long getRandomCardId(Lobby lobby) {
        List<Long> votePool = getFreeCardIds(lobby);
        int index = (int) (Math.random() % (votePool.size() - 1));
        return votePool.get(index);
    }

    private List<Long> getFreeCardIds(Lobby lobby) {
        List<Long> pickedCardIds = getPickedCardIds(lobby);
        List<Long> votePool = new ArrayList<>(lobby.getSettings().getVotePool());
        votePool.removeAll(pickedCardIds);
        return votePool;
    }

    private List<Long> getPickedCardIds(Lobby lobby) {
        return lobby.getLobbyMap().stream()
                .map(LobbyMap::getVoteItem)
                .filter(Objects::nonNull)
                .map(VoteItem::getId)
                .collect(Collectors.toList());
    }

    private Optional<LobbyMap> getNextLobbyMap(Lobby lobby) {
        return lobby.getLobbyMap().stream()
                .filter(map -> isNull(map.getVoteItem()))
                .findFirst();
    }

    private boolean allMatchMemberPresent(Long lobbyId) {
        Lobby lobby = findById(lobbyId);
        if (!allCaptainsPresent(lobby)) {
            log.info("Captains are not online. Lobby: [{}]", lobby.getId());
            return false;
        }
        Map<ParticipationType, Long> expectedCoreMemberCount = calculateCoreMemberCount(lobby);
        Map<ParticipationType, Long> actualCoreMemberCount = calculateAuxiliaryMemberCount(lobby);

        return expectedCoreMemberCount.entrySet().stream()
                .allMatch(entry -> {
                    if (isNull(actualCoreMemberCount.get(entry.getKey()))) {
                        log.info("Team: [{}]. Core members are not present", entry.getKey());
                        return false;
                    }
                    log.info("Team: [{}]. Expected core number: {}. Actual: {}", entry.getKey(),
                            entry.getValue(), actualCoreMemberCount.get(entry.getKey()));
                    return entry.getValue() <= actualCoreMemberCount.get(entry.getKey());
                });
    }

    private Map<ParticipationType, Long> calculateCoreMemberCount(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .filter(member -> member.getTournamentMember().getRole().equals(CORE))
                .collect(groupingBy(MatchMember::getParticipationType, counting()));
    }

    private Map<ParticipationType, Long> calculateAuxiliaryMemberCount(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .filter(member -> member.getStatus().equals(MemberStatus.ONLINE))
                .filter(member -> member.getTournamentMember().getRole().equals(CORE) ||
                        member.getTournamentMember().getRole().equals(STAND_IN))
                .collect(groupingBy(MatchMember::getParticipationType, counting()));
    }

    private boolean allCaptainsPresent(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .filter(member -> member.getTournamentMember().getRole().equals(CAPTAIN))
                .map(MatchMember::getStatus)
                .noneMatch(status -> status.equals(MemberStatus.OFFLINE));
    }

    private boolean isValidVoteRequest(LobbyMap lobbyMap, Lobby lobby, LobbyMap map, Long userId) {
        if (!lobbyMap.getId().equals(map.getId())) {
            log.info("Invalid lobby map id {}", lobbyMap.getId());
            sendErrorNotification(lobby.getId(), ErrorType.INVALID_LOBBY_MAP_ID, Optional.empty());
            return false;
        }
        Member member = map.getMember();
        if (member == null || !member.getId().equals(userId)) {
            log.info("Invalid user id {}", userId);
            sendErrorNotification(lobby.getId(), ErrorType.INVALID_USER_ID, Optional.empty());
            return false;

        }
        List<Long> freeCardIds = getFreeCardIds(lobby);
        if (!freeCardIds.contains(lobbyMap.getVoteItem().getId())) {
            log.info("Invalid card id {}", lobbyMap.getVoteItem().getId());
            sendErrorNotification(lobby.getId(), ErrorType.INVALID_CARD_ID, Optional.empty());
            return false;
        }
        return true;
    }

    private String getUsersInformation(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .map(member -> String.format("MatchMember id=%s, UMS id=%s, Status=%s",
                        member.getId(), member.getTournamentMember().getMember().getId(),
                        member.getStatus()) )
                .collect(Collectors.joining(";"));
    }

    private void sendErrorNotification(Long lobbyId, ErrorType errorType,
            Optional<String> optionalInfo) {
        errorHandlerService.sendErrorMessage(rmqProperties.getOutcomingUiQueueName(),
                lobbyId.toString(), errorType, optionalInfo);
    }

}
