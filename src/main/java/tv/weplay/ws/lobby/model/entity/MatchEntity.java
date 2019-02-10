package tv.weplay.ws.lobby.model.entity;

import lombok.*;
import tv.weplay.ws.lobby.model.dto.Node;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchEntity {

    private Long id;

    private Long score1;

    private Long score2;

    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    private String status;

    private MatchMemberEntity player1;

    private MatchMemberEntity player2;

    private Node node;

    private List<MatchMemberEntity> members;

}
