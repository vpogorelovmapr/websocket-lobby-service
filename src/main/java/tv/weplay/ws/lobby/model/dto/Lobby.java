package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

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

    @JsonProperty("start_vote_datetime")
    private LocalDateTime startVoteDatetime;

    private Long duration;

    private Settings settings;

    @Relationship("match")
    private Match match;

    @Relationship("maps")
    private List<LobbyMap> lobbyMap;
}
