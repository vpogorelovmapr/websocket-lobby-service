package tv.weplay.ws.lobby.model.entity;

import lombok.*;
import tv.weplay.ws.lobby.model.dto.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchMemberEntity {

    private Long id;

    private MemberStatus status;

    private ParticipationType participationType;

    private Tournament tournament;

    private TournamentMember tournamentMember;
}
