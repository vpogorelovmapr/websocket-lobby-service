package tv.weplay.ws.lobby.model.dto;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

import java.util.List;

@Data
@Type("Lobby")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Lobby {

    @Id(LongIdHandler.class)
    private Long id;

    private LobbyStatus status;

    private Long duration;

    private Settings settings;

    @Relationship("match")
    private Match match;

    @Relationship("maps")
    private List<LobbyMap> lobbyMap;
}
