package tv.weplay.ws.lobby.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;
import tv.weplay.ws.lobby.model.dto.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchMemberEntity {

    @Id(LongIdHandler.class)
    private Long id;

    private MemberStatus status;

    @JsonProperty("participation_type")
    private String participationType;

    @Relationship("tournament")
    private Tournament tournament;

    @Relationship("member")
    private Member member;
}
