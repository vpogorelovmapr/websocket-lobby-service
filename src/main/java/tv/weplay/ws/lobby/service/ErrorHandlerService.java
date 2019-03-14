package tv.weplay.ws.lobby.service;

import java.util.Optional;
import tv.weplay.ws.lobby.model.error.ErrorType;

public interface ErrorHandlerService {

    void sendErrorMessage(String exchange, String routingKey, ErrorType type,
            Optional<String> optionalInfo);

}
