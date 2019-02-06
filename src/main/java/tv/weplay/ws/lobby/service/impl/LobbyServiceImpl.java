package tv.weplay.ws.lobby.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.config.properties.RabbitmqQueues;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.mapper.LobbyMapper;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.dto.LobbyStatus;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;
import tv.weplay.ws.lobby.repository.LobbyRepository;
import tv.weplay.ws.lobby.service.LobbyService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyServiceImpl implements LobbyService {

    private final LobbyMapper lobbyMapper;
    private final LobbyRepository lobbyRepository;
    private final RabbitMQEventSenderService rabbitMQService;
    private final RabbitmqQueues rabbitmqQueues;
    private final JsonApiConverter apiConverter;

    @Override
    public Lobby update(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        LobbyEntity createdEntity = lobbyRepository.save(entity);
        Lobby created = lobbyMapper.toDTO(createdEntity);
        log.info("Updated lobby {}", lobby);
        publishEventToChannel(created);
        return created;
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
    private void publishEventToChannel(Lobby created) {
        Lobby event = buildLobby(created.getDuration(), LobbyStatus.ONGOING);
        byte[] data = apiConverter.writeObject(event);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents(), EventTypes.MATCH_STATUS_EVENT);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingTournamentsEvents(),
                EventTypes.MATCH_STATUS_EVENT);
    }

    private Lobby buildLobby(Long duration, LobbyStatus status) {
        return Lobby.builder()
                .duration(duration)
                .status(status)
                .build();
    }
}
