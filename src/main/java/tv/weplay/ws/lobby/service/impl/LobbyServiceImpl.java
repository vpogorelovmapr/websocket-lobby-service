package tv.weplay.ws.lobby.service.impl;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static tv.weplay.ws.lobby.model.dto.TournamentMemberRole.CAPTAIN;
import static tv.weplay.ws.lobby.model.dto.TournamentMemberRole.CORE;
import static tv.weplay.ws.lobby.model.dto.TournamentMemberRole.STAND_IN;
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
import tv.weplay.ws.lobby.repository.LobbyRepository;
import tv.weplay.ws.lobby.scheduled.MatchStartJob;
import tv.weplay.ws.lobby.scheduled.VoteJob;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.SchedulerService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    private static final String LOBBY_ID = "lobbyId";

    private final LobbyMapper lobbyMapper;
    private final LobbyRepository lobbyRepository;
    private final RabbitMQEventSenderService rabbitMQService;
    private final RabbitmqProperties rabbitmqProperties;
    private final JsonApiConverter converter;
    private final SchedulerService schedulerService;

    @Override
    public Lobby create(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        entity.setLobbyStartDatetime(LocalDateTime.now());
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby created = lobbyMapper.toDTO(createdEntity);
        log.info("Created lobby {}", created);
        Lobby event = buildLobbyCreatedEvent(created);
        String routingKey = buildInvitesRoutingKey(lobby);
        publishInvitesToRabbitMQ(event, routingKey, EventTypes.MATCH_CREATED_EVENT);
        scheduleStartMatchJob(created);
        return created;
    }

    @Override
    public Lobby update(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby updated = lobbyMapper.toDTO(createdEntity);
        log.info("Updated lobby {}", updated);
        return updated;
    }

    @Override
    public void delete(Long id) {
        log.info("Delete lobby with id {}", id);
        lobbyRepository.deleteById(id);
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
    public void startVoting(Long lobbyId) {
        log.info("Start match for lobby with id {} ", lobbyId);
        Lobby lobby = findById(lobbyId);
        LobbyStatus status = allMatchMemberPresent(lobby) ? LobbyStatus.ONGOING : LobbyStatus.CANCELED;
        String type = status.equals(LobbyStatus.ONGOING) ? EventTypes.MATCH_STARTED_EVENT :
                EventTypes.MATCH_CANCELED_EVENT;
        lobby.setStatus(status);
        update(lobby);

        Lobby event = status.equals(LobbyStatus.ONGOING) ? buildMatchStartEvent(lobby) : buildChangeLobbyStatusEvent(lobby);
        publishEventToRabbitMQ(event, lobby.getId().toString(), type);

        if (status.equals(LobbyStatus.ONGOING)) {
            if (isVotingCompleted(lobby)) {
                lobby.setStatus(LobbyStatus.ENDED);
                Lobby endEvent = buildChangeLobbyStatusEvent(lobby);
                publishEventToRabbitMQ(endEvent, lobby.getId().toString(), EventTypes.MATCH_ENDED_EVENT);
            } else {
                scheduleVoteJob(lobbyId, lobby.getSettings().getVoteTime());
            }
        }
    }

    @Override
    public void updateMemberStatus(Long lobbyId, Long memberId) {
        log.info("Updating member with id {} for lobby {}", memberId, lobbyId);
        Lobby lobby = findById(lobbyId);
        log.info("Lobby found: {}", lobby);
        Optional<MatchMember> matchMember = lobby.getMatch().getMembers().stream()
                .filter(member -> member.getId().equals(memberId))
                .findAny();
        matchMember.ifPresent(member -> {
            member.setStatus(MemberStatus.ONLINE);
            update(lobby);
            MatchMember event = buildMatchMemberEvent(member, lobbyId);
            publishEventToRabbitMQ(event, lobby.getId().toString(), EventTypes.MEMBER_EVENT);
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
            publishEventToRabbitMQ(event, lobby.getId().toString(), EventTypes.VOTE_EVENT);
            voteRandomCardIfLastVote(lobby);
        });
    }

    @Override
    public void voteCardByUser(Long lobbyId, LobbyMap lobbyMap, Long userId) {
        Lobby lobby = findById(lobbyId);
        Optional<LobbyMap> nextLobbyMap = getNextLobbyMap(lobby);
        nextLobbyMap.ifPresent(map -> {
            //TODO: Send error message to rabbitmq
            if (!isValidVoteRequest(lobbyMap, lobby, map, userId)) return;
            map.setVoteItem(lobbyMap.getVoteItem());
            map.setStatus(LobbyMapStatus.USER_PICK);
            Lobby updated = update(lobby);
            if (!isLastVote(updated)) {
                rescheduleVoteJob(lobbyId, updated);
            }
            LobbyMap event = buildLobbyMapEvent(map, lobbyId);
            publishEventToRabbitMQ(event, lobby.getId().toString(), EventTypes.VOTE_EVENT);
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
            publishEventToRabbitMQ(event, lobby.getId().toString(), EventTypes.MATCH_ENDED_EVENT);
        }
    }

    @SneakyThrows
    private void publishInvitesToRabbitMQ(Object event, String routingKey, String type) {
        byte[] data = converter.writeObject(event);
        log.info("Publishing event to rabbitMQ [{}]", new String(data));
        rabbitMQService.prepareAndSendEvent(rabbitmqProperties.getOutcomingPrivateQueueName(), data,
                routingKey, type);
        rabbitMQService.prepareAndSendEvent(DEFAULT_EXCHANGE, data,
                rabbitmqProperties.getOutcomingTournamentsQueueName(), type);
    }

    @SneakyThrows
    private void publishEventToRabbitMQ(Object event, String routingKey, String type) {
        byte[] data = converter.writeObject(event);
        log.info("Publishing event to rabbitMQ [{}]", new String(data));
        rabbitMQService.prepareAndSendEvent(rabbitmqProperties.getOutcomingUiQueueName(), data, routingKey, type);
        rabbitMQService.prepareAndSendEvent(DEFAULT_EXCHANGE, data,
                rabbitmqProperties.getOutcomingTournamentsQueueName(), type);
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

    private void scheduleStartMatchJob(Lobby lobby) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobby.getId());
        schedulerService.schedule(UUID.randomUUID().toString(), MATCH_START_GROUP,
                ZonedDateTime.now().plusSeconds(lobby.getDuration()), dataMap, MatchStartJob.class);
    }

    private void scheduleVoteJob(Long lobbyId, Integer interval) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobbyId);
        schedulerService.schedule(VOTE_PREFIX + lobbyId, VOTE_GROUP, ZonedDateTime.now().plusSeconds(interval), dataMap,
                interval, VoteJob.class);
    }

    private Long getRandomCardId(Lobby lobby) {
        List<Long> pickedCardIds = getPickedCardIds(lobby);
        List<Long> votePool = new ArrayList<>(lobby.getSettings().getVotePool());
        votePool.removeAll(pickedCardIds);
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
                .filter(map -> Objects.isNull(map.getVoteItem()))
                .findFirst();
    }

    private boolean allMatchMemberPresent(Lobby lobby) {
        if (!allCaptainsPresent(lobby)) {
            log.info("Captains are not online. Lobby: [{}]", lobby.getId());
            return false;
        }
        Map<String, Long> expectedCoreMemberCount = calculateCoreMemberCount(lobby);
        Map<String, Long> actualCoreMemberCount = calculateAuxiliaryMemberCount(lobby);

        return expectedCoreMemberCount.entrySet().stream()
                .allMatch(entry -> {
                    if (actualCoreMemberCount.get(entry.getKey()) == null) {
                        log.info("Team: [{}]. Core members are not present", entry.getKey());
                        return false;
                    }
                    log.info("Team: [{}]. Expected core number: {}. Actual: {}", entry.getKey(), entry.getValue(), actualCoreMemberCount.get(entry.getKey()));
                    return entry.getValue() <= actualCoreMemberCount.get(entry.getKey());
                });
    }

    private Map<String, Long> calculateCoreMemberCount(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .filter(member -> member.getTournamentMember().getRole().equals(CORE))
                .collect(groupingBy(MatchMember::getParticipationType, counting()));
    }

    private Map<String, Long> calculateAuxiliaryMemberCount(Lobby lobby) {
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
        //We cannot throw exception here because ack message will not be sent to rabbitmq and message not read
        if (!lobbyMap.getId().equals(map.getId())) {
            log.info("Invalid lobby map id {}", lobbyMap.getId());
            return false;
        }
        Member member = map.getMember();
        if (member == null || !member.getId().equals(userId)) {
            log.info("Invalid user id {}", userId);
            return false;

        }
        List<Long> freeCardIds = getFreeCardIds(lobby);
        if (!freeCardIds.contains(lobbyMap.getVoteItem().getId())) {
            log.info("Invalid card id {}", lobbyMap.getVoteItem().getId());
            return false;
        }
        return true;
    }

}
