package tv.weplay.ws.lobby.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.service.EventSenderService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQEventSenderService implements EventSenderService {

    public static final String DEFAULT_EXCHANGE = "";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${info.app.version}")
    private String applicationVersion;

    @Value("${info.app.messageProtocolVersion}")
    private String messageProtocolVersion;

    @Override
    public void prepareAndSendEvent(String exchange, String data, String routeKey, String type) {
        String payload = buildRabbitMQEvent(data, type);
        log.trace("Event to be sent: [{}]", payload);
        rabbitTemplate.convertAndSend(exchange, routeKey, payload);
        log.trace("Message that has been successfully sent: [{}]", payload);
    }

    @Override
    public void prepareAndSendEvent(String exchange, byte[] data, String routeKey, String type) {
        prepareAndSendEvent(exchange, new String(data), routeKey, type);
    }

    @SneakyThrows
    private String buildRabbitMQEvent(String body, String type) {
        return objectMapper.writeValueAsString(new Event(buildEventMetaData(type),
                objectMapper.readTree(body)));
    }

    private EventMetaData buildEventMetaData(String type) {
        return EventMetaData.builder()
                .type(type)
                .sender(new EventSender(applicationName, applicationVersion))
                .eventDateTimes(new EventDateTimes(LocalDateTime.now()))
                .protocol(new EventProtocol(messageProtocolVersion))
                .build();
    }

}
