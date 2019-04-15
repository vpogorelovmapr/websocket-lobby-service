package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

@Data
@ToString(exclude = "lobby")
@Type("MatchMember")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchMember {

    @Id(LongIdHandler.class)
    private Long id;

    private MemberStatus status;

    @JsonProperty("participation_type")
    private ParticipationType participationType;

    @Relationship("tournament")
    private Tournament tournament;

    @Relationship("tournament_member")
    private TournamentMember tournamentMember;

    @Relationship("lobby")
    private Lobby lobby;
}
