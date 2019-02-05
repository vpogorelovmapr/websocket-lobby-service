package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings {

    @JsonProperty("vote_pool")
    private String[] votePool;

    @JsonProperty("vote_time")
    private Long voteTime;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("vote_format")
    private String voteFormat;

    @JsonProperty("vote_veto_logic")
    private String voteVetoLogic;
}
