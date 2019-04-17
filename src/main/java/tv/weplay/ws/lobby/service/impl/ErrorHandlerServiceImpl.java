package tv.weplay.ws.lobby.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.error.ErrorType;
import tv.weplay.ws.lobby.model.error.ErrorTypeMapping;
import tv.weplay.ws.lobby.service.ErrorHandlerService;
import tv.weplay.ws.lobby.service.EventSenderService;
import tv.weplay.ws.lobby.model.error.Error;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ErrorHandlerServiceImpl implements ErrorHandlerService {

    private final Map<ErrorType, ErrorTypeMapping> errorCodeMappings;
    private final JsonApiConverter converter;
    private final EventSenderService eventSenderService;

    @Override
    public void sendErrorMessage(String exchange, String routingKey, ErrorType type,
            Optional<String> optionalInfo) {
        byte[] data = converter.writeObject(buildErrorContent(type, optionalInfo));
        log.info("Event to be sent: [{}]", new String(data));
        eventSenderService.prepareAndSendEvent(exchange, data, routingKey, EventTypes.ERROR);
    }

    private Error buildErrorContent(ErrorType type, Optional<String> optionalInfo) {
        ErrorTypeMapping errorMapping = errorCodeMappings.get(type);
        return Error.builder()
                .code(errorMapping.getCode())
                .description(optionalInfo.orElse(errorMapping.getDescription()))
                .dateTime(LocalDateTime.now())
                .build();
    }

}
