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
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.dto.LobbyStatus;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;
import tv.weplay.ws.lobby.repository.LobbyRepository;
import tv.weplay.ws.lobby.scheduled.MatchStartJob;
import tv.weplay.ws.lobby.scheduled.SchedulerHelper;
import tv.weplay.ws.lobby.service.LobbyService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static tv.weplay.ws.lobby.scheduled.MatchStartJob.LOBBY_ID;
import static tv.weplay.ws.lobby.scheduled.SchedulerHelper.MATCH_START_GROUP;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    private final LobbyMapper lobbyMapper;
    private final LobbyRepository lobbyRepository;
    private final RabbitMQEventSenderService rabbitMQService;
    private final RabbitmqQueues rabbitmqQueues;
    private final JsonApiConverter apiConverter;
    private final SchedulerHelper schedulerHelper;

    @Override
    public Lobby create(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        entity.setStatus(LobbyStatus.ONGOING);
        entity.setLobbyStartDatetime(LocalDateTime.now());
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby created = lobbyMapper.toDTO(createdEntity);
        log.info("Created lobby {}", created);
        publishEventToRabbitMQ(created);
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

    @SneakyThrows
    private void publishEventToRabbitMQ(Lobby lobby) {
        log.info("Publishing event to rabbitMQ {}", lobby);
        Lobby event = buildLobbyEvent(lobby);
        byte[] data = apiConverter.writeObject(event);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents(), EventTypes.MATCH_STATUS_EVENT);
    }

    private Lobby buildLobbyEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .lobbyStartDatetime(lobby.getLobbyStartDatetime())
                .status(lobby.getStatus())
                .build();
    }

    private void scheduleStartMatchJob(Lobby lobby) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobby.getId());
        schedulerHelper.schedule(UUID.randomUUID().toString(), MATCH_START_GROUP, ZonedDateTime.now().plusSeconds(30),
                dataMap, MatchStartJob.class);
    }
}
