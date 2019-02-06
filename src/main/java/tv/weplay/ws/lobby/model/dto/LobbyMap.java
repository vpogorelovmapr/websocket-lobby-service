package tv.weplay.ws.lobby.model.dto;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

@Data
@Type("LobbyMap")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LobbyMap {

    @Id(LongIdHandler.class)
    private Long id;

    private String vote;

    private LobbyMapStatus status;

    @Relationship("member")
    private Member member;
}
