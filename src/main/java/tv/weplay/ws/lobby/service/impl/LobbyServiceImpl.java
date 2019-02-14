package tv.weplay.ws.lobby.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.config.properties.RabbitmqQueues;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.mapper.LobbyMapper;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;
import tv.weplay.ws.lobby.repository.LobbyRepository;
import tv.weplay.ws.lobby.scheduled.MatchStartJob;
import tv.weplay.ws.lobby.scheduled.VoteJob;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.SchedulerService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService.DEFAULT_EXCHANGE;
import static tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService.UI_EXCHANGE;
import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    private static final String LOBBY_ID = "lobbyId";

    private final LobbyMapper lobbyMapper;
    private final LobbyRepository lobbyRepository;
    private final RabbitMQEventSenderService rabbitMQService;
    private final RabbitmqQueues rabbitmqQueues;
    private final JsonApiConverter converter;
    private final SchedulerService schedulerService;

    @Override
    public Lobby create(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        entity.setLobbyStartDatetime(LocalDateTime.now());
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby created = lobbyMapper.toDTO(createdEntity);
        log.info("Created lobby {}", created);
        Lobby event = buildLobbyCreatedEvent(lobby);
        publishEventToRabbitMQ(event, lobby.getId(), EventTypes.MATCH_CREATED_EVENT );
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
    public void startMatch(Long lobbyId) {
        log.info("Start match for lobby with id {} ", lobbyId);
        Lobby lobby = findById(lobbyId);
        LobbyStatus status = allMatchMemberPresent(lobby) ? LobbyStatus.ONGOING : LobbyStatus.CANCELED;
        String type = status.equals(LobbyStatus.ONGOING) ? EventTypes.MATCH_STARTED_EVENT :
                EventTypes.MATCH_CANCELED_EVENT;
        lobby.setStatus(status);
        update(lobby);

        Lobby event = status.equals(LobbyStatus.ONGOING) ? buildMatchStartEvent(lobby) : buildChangeLobbyStatusEvent(lobby);
        publishEventToRabbitMQ(event, lobby.getId(), type);

        if (status.equals(LobbyStatus.ONGOING)) {
            scheduleVoteJob(lobbyId, lobby.getSettings().getVoteTime());
        }
    }

    @Override
    public void updateMemberStatus(Long lobbyId, Long memberId) {
        log.info("Updating member with id {} for lobby {}", memberId, lobbyId);
        Lobby lobby = findById(lobbyId);
        Optional<MatchMember> matchMember = lobby.getMatch().getMembers().stream()
                .filter(member -> member.getId().equals(memberId))
                .findAny();
        matchMember.ifPresent(member -> {
            member.setStatus(MemberStatus.ONLINE);
            update(lobby);
            MatchMember event = buildMatchMemberEvent(member, lobbyId);
            publishEventToRabbitMQ(event, lobby.getId(), EventTypes.MEMBER_EVENT);
        });
    }

    @Override
    public void voteRandomCard(Long lobbyId, LobbyMapType type) {
        Lobby lobby = findById(lobbyId);
        Long cardId = getRandomCardId(lobby);
        voteCardByServer(lobby.getId(), cardId, type);
    }

    @Override
    public void voteCardByServer(Long lobbyId, Long cardId, LobbyMapType type) {
        Lobby lobby = findById(lobbyId);
        Optional<LobbyMap> lobbyMap = getNextLobbyMap(lobby);
        lobbyMap.ifPresent(map -> {
            map.setVoteItem(new VoteItem(cardId));
            map.setStatus(type);
            update(lobby);
            LobbyMap event = buildLobbyMapEvent(map, lobbyId);
            publishEventToRabbitMQ(event, lobby.getId(), EventTypes.VOTE_EVENT);
            voteRandomCardIfLastVote(lobby);
        });
    }

    @Override
    public void voteCardByUser(Long lobbyId, LobbyMap lobbyMap, Long userId) {
        Lobby lobby = findById(lobbyId);
        Optional<LobbyMap> nextLobbyMap = getNextLobbyMap(lobby);
        nextLobbyMap.ifPresent(map -> {
            if (!isValidVoteRequest(lobbyMap, lobby, map, userId)) return;
            map.setVoteItem(lobbyMap.getVoteItem());
            map.setStatus(LobbyMapType.USER_PICK);
            Lobby updated = update(lobby);
            if (!isLastVote(updated)) {
                rescheduleVoteJob(lobbyId, updated);
            }
            LobbyMap event = buildLobbyMapEvent(map, lobbyId);
            publishEventToRabbitMQ(event, lobby.getId(), EventTypes.VOTE_EVENT);
            voteRandomCardIfLastVote(lobby);
        });
    }

    private void rescheduleVoteJob(Long lobbyId, Lobby lobby) {
        schedulerService.unschedule(VOTE_PREFIX + lobbyId, VOTE_GROUP);
        scheduleVoteJob(lobbyId, lobby.getSettings().getVoteTime());
    }

    @Override
    public boolean isLastVote(Lobby lobby) {
        return lobby.getLobbyMap().stream()
                .map(LobbyMap::getVoteItem)
                .filter(Objects::isNull)
                .count() == 1;
    }

    private void voteRandomCardIfLastVote(Lobby lobby) {
        if (isLastVote(lobby)) {
            lobby.setStatus(LobbyStatus.ENDED);
            voteRandomCard(lobby.getId(), LobbyMapType.SERVER_PICK);
            schedulerService.unschedule(VOTE_PREFIX + lobby.getId(), VOTE_GROUP);
            Lobby event = buildChangeLobbyStatusEvent(lobby);
            publishEventToRabbitMQ(event, lobby.getId(), EventTypes.MATCH_ENDED_EVENT);
        }
    }

    @SneakyThrows
    private void publishEventToRabbitMQ(Object event, Long lobbyId, String type) {
        byte[] data = converter.writeObject(event);
        log.info("Publishing event to rabbitMQ [{}]", new String(data));
        rabbitMQService.prepareAndSendEvent(UI_EXCHANGE, data, rabbitmqQueues.getOutcomingUiEvents(), type);
        rabbitMQService.prepareAndSendEvent(DEFAULT_EXCHANGE, data, rabbitmqQueues.getOutcomingTournamentsEvents(), type);
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
                .voteItem(map.getVoteItem())
                .build();
    }

    private Lobby buildLobbyCreatedEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .lobbyStartDatetime(lobby.getLobbyStartDatetime())
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
        return lobby.getMatch().getMembers().stream()
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
