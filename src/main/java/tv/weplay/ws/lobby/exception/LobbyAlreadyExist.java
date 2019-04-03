package tv.weplay.ws.lobby.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LobbyAlreadyExist extends RuntimeException {

    public LobbyAlreadyExist(String message) {
        super(message);
    }

}
