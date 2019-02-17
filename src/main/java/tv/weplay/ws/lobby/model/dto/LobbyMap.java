package tv.weplay.ws.lobby.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@ToString(exclude = "lobby")
@Type("LobbyMap")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LobbyMap {

    @Id(LongIdHandler.class)
    private Long id;

    private String vote;

    private LobbyMapType status;

    @Relationship("map")
    private VoteItem voteItem;

    @Relationship("member")
    private Member member;

    @Relationship("lobby")
    private Lobby lobby;

    @JsonProperty("update_datetime")
    private LocalDateTime updatedDatetime;
}
