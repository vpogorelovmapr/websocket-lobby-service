package tv.weplay.ws.lobby.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pro.javatar.security.jwt.TokenVerifier;
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

    @RabbitListener(queues = "#{rabbitmqQueues.incomingTournamentsEvents}")
    public void handleLobbyCreationEvent(byte[] rawEvent) throws Exception {
        log.info("Raw event received: {}", new String(rawEvent));
        Event event = objectMapper.readValue(rawEvent, Event.class);
        Lobby lobby = converter.readDocument(event.getEventData().toString(), Lobby.class).get();
        lobbyService.create(lobby);
    }

    @RabbitListener(queues = "#{rabbitmqQueues.incomingUiEvents}")
    public void handleUIEvent(String rawEvent, @Header("user_id") Long userId) throws Exception {
        log.info("Raw event received: {}", rawEvent);
        Event event = objectMapper.readValue(rawEvent, Event.class);

        if (event.getEventMetaData().getType().equals(EventTypes.MEMBER_EVENT)) {
            MatchMember member = converter.readDocument(event.getEventData().toString(), MatchMember.class).get();
            lobbyService.updateMemberStatus(member.getLobby().getId(), member.getId());
        } else if (event.getEventMetaData().getType().equals(EventTypes.VOTE_EVENT)) {
//            Long userId = getUserId(authhorization.toString());
//            Long userId = Long.parseLong(userIdHeader);
            LobbyMap map = converter.readDocument(event.getEventData().toString(), LobbyMap.class).get();
            lobbyService.voteCardByUser(map.getLobby().getId(), map, userId);
        }
    }

    @SneakyThrows
    private Long getUserId(String authorization) {
        String token = authorization.split(" ")[1];
        TokenVerifier verifier = TokenVerifier.create(token);
        return verifier.getToken().getUserId();
    }
}
