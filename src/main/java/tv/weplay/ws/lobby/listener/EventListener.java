package tv.weplay.ws.lobby.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.service.LobbyService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;
    private final JsonApiConverter converter;

    @RabbitListener(queues = "#{rabbitmqProperties.incomingTournamentsQueueName}")
    public void handleLobbyCreationEvent(String rawEvent) throws Exception {
        log.info("Raw event received: {}", rawEvent);
        Event event = objectMapper.readValue(rawEvent, Event.class);
        handleLobbyCreatedEvent(event);
    }

    @RabbitListener(queues = "#{rabbitmqProperties.incomingUiQueueName}")
    public void handleUIEvent(String rawEvent, @Header("user_id") Long userId) throws Exception {
        log.info("Raw event received: {}", rawEvent);
        Event event = objectMapper.readValue(rawEvent, Event.class);
        handleUIEvent(userId, event);
    }

    private void handleLobbyCreatedEvent(Event event) {
        try {
            if (event.getEventMetaData().getType().equals(EventTypes.LOBBY_CREATED)) {
                Lobby lobby = converter.readDocument(event.getEventData().toString(), Lobby.class)
                        .get();
                lobbyService.create(lobby);
            }
        } catch (Exception e) {
            log.info("Invalid lobby event", e);
        }
    }

    private void handleUIEvent(@Header("user_id") Long userId, Event event) {
        if (event.getEventMetaData().getType().equals(EventTypes.MEMBER_EVENT)) {
            MatchMember member = converter
                    .readDocument(event.getEventData().toString(), MatchMember.class).get();
            log.info("Member: {}", member);
            lobbyService.updateMemberStatus(member.getLobby().getId(), member.getId());
        } else if (event.getEventMetaData().getType().equals(EventTypes.VOTE_EVENT)) {
            LobbyMap map = converter.readDocument(event.getEventData().toString(), LobbyMap.class)
                    .get();
            lobbyService.voteCardByUser(map.getLobby().getId(), map, userId);
        }
    }

}
