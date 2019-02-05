package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @JsonProperty("meta")
    private EventMetaData eventMetaData;

    @JsonProperty("data")
    private Object eventData;
}
