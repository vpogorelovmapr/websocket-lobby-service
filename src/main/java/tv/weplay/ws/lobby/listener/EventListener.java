package tv.weplay.ws.lobby.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.config.properties.RabbitmqQueues;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.Event;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;
    private final JsonApiConverter converter;
    private final RabbitmqQueues rabbitmqQueues;
    private final RabbitMQEventSenderService rabbitMQService;

    @Scheduled(fixedDelay = 3000)
    public void schedule() {
        String text = "{\"data\":{\"id\":83,\"type\":\"Lobby\",\"attributes\":{\"status\":\"UPCOMING\",\"duration\":120,\"settings\":{\"vote_pool\":[3,2,1],\"vote_time\":15,\"start_date\":null,\"vote_format\":\"best1\",\"vote_veto_logic\":\"drop1-drop2-pick\"}},\"relationships\":{\"match\":{\"data\":{\"id\":134,\"type\":\"Match\"}},\"maps\":{\"data\":[{\"id\":101,\"type\":\"LobbyMap\"},{\"id\":102,\"type\":\"LobbyMap\"},{\"id\":103,\"type\":\"LobbyMap\"}]}}},\"included\":{\"Match\":{\"134\":{\"id\":134,\"type\":\"Match\",\"attributes\":{\"start_datetime\":null,\"end_datetime\":null,\"score1\":0,\"score2\":0,\"status\":\"UPCOMING\"},\"relationships\":{\"player1\":{\"data\":{\"id\":3,\"type\":\"TournamentMember\"}},\"player2\":{\"data\":{\"id\":2,\"type\":\"TournamentMember\"}},\"winner\":{\"data\":null},\"node\":{\"data\":{\"id\":175,\"type\":\"Node\"}},\"lobby\":{\"data\":{\"id\":83,\"type\":\"Lobby\"}},\"members\":{\"data\":[{\"id\":101,\"type\":\"MatchMember\"},{\"id\":102,\"type\":\"MatchMember\"}]}}}},\"LobbyMap\":{\"101\":{\"id\":101,\"type\":\"LobbyMap\",\"attributes\":{\"vote\":\"DROP\",\"status\":\"NONE\"},\"relationships\":{\"map\":{\"data\":null},\"member\":{\"data\":{\"id\":288,\"type\":\"Member\"}},\"lobby\":{\"data\":{\"id\":83,\"type\":\"Lobby\"}}}},\"102\":{\"id\":102,\"type\":\"LobbyMap\",\"attributes\":{\"vote\":\"DROP\",\"status\":\"NONE\"},\"relationships\":{\"map\":{\"data\":null},\"member\":{\"data\":{\"id\":93,\"type\":\"Member\"}},\"lobby\":{\"data\":{\"id\":83,\"type\":\"Lobby\"}}}},\"103\":{\"id\":103,\"type\":\"LobbyMap\",\"attributes\":{\"vote\":\"PICK\",\"status\":\"NONE\"},\"relationships\":{\"map\":{\"data\":null},\"member\":{\"data\":null},\"lobby\":{\"data\":{\"id\":83,\"type\":\"Lobby\"}}}}},\"MatchMember\":{\"101\":{\"id\":101,\"type\":\"MatchMember\",\"attributes\":{\"status\":\"OFFLINE\",\"participation_type\":\"HOME\"},\"relationships\":{\"tournament_member\":{\"data\":{\"id\":3,\"type\":\"TournamentMember\"}},\"match\":{\"data\":{\"id\":134,\"type\":\"Match\"}}}},\"102\":{\"id\":102,\"type\":\"MatchMember\",\"attributes\":{\"status\":\"OFFLINE\",\"participation_type\":\"AWAY\"},\"relationships\":{\"tournament_member\":{\"data\":{\"id\":2,\"type\":\"TournamentMember\"}},\"match\":{\"data\":{\"id\":134,\"type\":\"Match\"}}}}}}}";
        Lobby lobby = converter.readDocument(text.getBytes(), Lobby.class).get();
        publishToUIChannel(lobby);
    }

//    @RabbitListener(queues = "#{rabbitmqQueues.incomingTournamentsEvents}")
    public void handleEvent(byte[] rawEvent) throws Exception {
        log.info("Raw event received: {}", new String(rawEvent));
        Event event = objectMapper.readValue(rawEvent, Event.class);
        Lobby lobby = converter.readDocument(event.getEventData().toString().getBytes(), Lobby.class).get();
        lobbyService.update(lobby);
    }

    @SneakyThrows
    private void publishToUIChannel(Lobby created) {
        byte[] data = converter.writeObject(created);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents());
    }
}
