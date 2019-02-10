package tv.weplay.ws.lobby.model.entity;

import lombok.*;
import tv.weplay.ws.lobby.model.dto.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LobbyMapEntity {

    private Long id;

    private String vote;

    private LobbyMapStatus status;

    private VoteItem voteItem;

    private Member member;
}
