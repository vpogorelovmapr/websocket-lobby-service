package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Type("Match")
public class Match {

    @Id(LongIdHandler.class)
    private Long id;

    private Long score1;

    private Long score2;

    @JsonProperty("start_datetime")
    private LocalDateTime startDatetime;

    @JsonProperty("end_datetime")
    private LocalDateTime endDatetime;

    private String status;

    @Relationship("player1")
    private MatchMember player1;

    @Relationship("player2")
    private MatchMember player2;

    @Relationship("node")
    private Node node;

    @Relationship("members")
    private List<MatchMember> members;

}
