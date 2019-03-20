package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDateTimes {

    @JsonProperty("create_datetime")
    private LocalDateTime createDateTime;

}
