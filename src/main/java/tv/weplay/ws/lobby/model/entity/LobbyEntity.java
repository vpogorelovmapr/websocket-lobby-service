package tv.weplay.ws.lobby.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import tv.weplay.ws.lobby.model.dto.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("lobbies")
public class LobbyEntity {

    @Id
    private Long id;

    private LobbyStatus status;

    private Long duration;

    private Settings settings;

    private Match match;

    private List<LobbyMap> lobbyMap;
}
