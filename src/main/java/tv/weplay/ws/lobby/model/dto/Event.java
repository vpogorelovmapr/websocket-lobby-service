package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @JsonProperty("meta")
    private EventMetaData eventMetaData;

    @JsonProperty("data")
    private JsonNode eventData;
}
