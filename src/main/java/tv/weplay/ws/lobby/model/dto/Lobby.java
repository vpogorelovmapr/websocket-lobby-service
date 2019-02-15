package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @JsonProperty("start_datetime")
    private LocalDateTime startDatetime;

    private Long duration;

    private Settings settings;

    @Relationship("match")
    private Match match;

    @Relationship("maps")
    private List<LobbyMap> lobbyMap;
}
