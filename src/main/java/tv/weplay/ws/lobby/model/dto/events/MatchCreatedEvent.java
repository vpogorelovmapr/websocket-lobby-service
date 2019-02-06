package tv.weplay.ws.lobby.model.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.*;

@Data
@Type("MatchCreatedEvent")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchCreatedEvent {

    @Id(LongIdHandler.class)
    private Long id;

    @JsonProperty("lobby_id")
    private Long lobbyId;

    private Long duration;
}
