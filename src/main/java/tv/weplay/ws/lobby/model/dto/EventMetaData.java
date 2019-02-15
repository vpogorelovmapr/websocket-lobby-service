package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetaData {

    private String type;

    private EventSender sender;

    @JsonProperty("datetimes")
    private EventDateTimes eventDateTimes;

    private EventProtocol protocol;

}
