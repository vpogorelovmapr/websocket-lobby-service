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
import tv.weplay.ws.lobby.model.dto.events.MatchCreatedEvent;
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

    @SneakyThrows
    @Scheduled(fixedDelay = 3000)
    public void schedule() {
        String text = "\"data\":{\"type\":\"Lobby\",\"id\":\"1\",\"attributes\":{\"duration\":\"120\"}}}";
        byte[] data = converter.writeObject(text.getBytes());
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents(), "MatchStatusEvent");
    }

//    @RabbitListener(queues = "#{rabbitmqQueues.incomingTournamentsEvents}")
    public void handleEvent(byte[] rawEvent) throws Exception {
        log.info("Raw event received: {}", new String(rawEvent));
        Event event = objectMapper.readValue(rawEvent, Event.class);
        Lobby lobby = converter.readDocument(event.getEventData().toString().getBytes(), Lobby.class).get();
        lobbyService.update(lobby);
    }
}
