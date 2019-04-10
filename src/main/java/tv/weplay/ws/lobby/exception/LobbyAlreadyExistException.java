package tv.weplay.ws.lobby.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LobbyAlreadyExistException extends RuntimeException {

    public LobbyAlreadyExistException(String message) {
        super(message);
    }

}
