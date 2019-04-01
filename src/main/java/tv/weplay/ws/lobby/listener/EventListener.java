package tv.weplay.ws.lobby.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
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

    @NewSpan
    @RabbitListener(queues = "#{rabbitmqProperties.incomingTournamentsQueueName}")
    public void handleLobbyCreationEvent(@SpanTag("event") String rawEvent) throws Exception {
        log.info("Raw event received: {}", rawEvent);
        Event event = objectMapper.readValue(rawEvent, Event.class);
        handleLobbyCreatedEvent(event);
    }

    @NewSpan
    @RabbitListener(queues = "#{rabbitmqProperties.incomingUiQueueName}")
    public void handleUIEvent(@SpanTag("event") String rawEvent, @SpanTag("user_id") @Header("user_id") Long userId) throws Exception {
        log.info("Raw event received: {}", rawEvent);
        Event event = objectMapper.readValue(rawEvent, Event.class);
        handleUIEvent(userId, event);
    }

    private void handleLobbyCreatedEvent(Event event) {
        if (event.getEventMetaData().getType().equals(EventTypes.LOBBY_CREATE_REQUEST)) {
            Lobby lobby = converter.readObject(event.getEventData().toString(), Lobby.class);
            lobbyService.create(lobby);
        } else if (event.getEventMetaData().getType().equals(EventTypes.LOBBY_CANCELED)) {
            Lobby lobby = converter.readObject(event.getEventData().toString(), Lobby.class);
            lobbyService.cancel(lobby.getId(), false);
        }
    }

    private void handleUIEvent(@Header("user_id") Long userId, Event event) {
        if (event.getEventMetaData().getType().equals(EventTypes.MEMBER)) {
            MatchMember member = converter
                    .readObject(event.getEventData().toString(), MatchMember.class);
            lobbyService.updateMemberStatus(member.getLobby().getId(), member);
        } else if (event.getEventMetaData().getType().equals(EventTypes.VOTE)) {
            LobbyMap map = converter.readObject(event.getEventData().toString(), LobbyMap.class);
            lobbyService.voteCardByUser(map.getLobby().getId(), map, userId);
        }
    }

}
