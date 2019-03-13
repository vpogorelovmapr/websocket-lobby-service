package tv.weplay.ws.lobby.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import tv.weplay.ws.lobby.model.dto.LobbyStatus;
import tv.weplay.ws.lobby.model.dto.Settings;

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

    private MatchEntity match;

    private LocalDateTime lobbyStartDatetime;

    private List<LobbyMapEntity> lobbyMap;
}
