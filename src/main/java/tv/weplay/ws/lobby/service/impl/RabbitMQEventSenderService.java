package tv.weplay.ws.lobby.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.service.EventSenderService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQEventSenderService implements EventSenderService {

  private static final String DEFAULT_EXCHANGE = "";

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${info.app.version}")
  private String applicationVersion;

  @Value("${info.app.messageProtocolVersion}")
  private String messageProtocolVersion;

  @Override
  public void prepareAndSendEvent(String data, String queueName, String type) {
    String payload = buildRabbitMQEvent(data, type);
    log.info("Event to be sent: [{}]", payload);
    rabbitTemplate.convertAndSend(DEFAULT_EXCHANGE, queueName, payload);
    log.info("Message that has been successfully sent: [{}]", payload);
  }

  @Override
  public void prepareAndSendEvent(byte[] data, String queueName, String type) {
    prepareAndSendEvent(new String(data), queueName, type);
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
