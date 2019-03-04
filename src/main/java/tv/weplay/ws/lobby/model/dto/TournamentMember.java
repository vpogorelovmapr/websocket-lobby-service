package tv.weplay.ws.lobby.model.dto;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

@Data
@Type("TournamentMember")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TournamentMember {
    @Id(LongIdHandler.class)
    private Long id;

    private TournamentMemberRole role;

    private String status;

    @Relationship("member")
    private Member member;

}
