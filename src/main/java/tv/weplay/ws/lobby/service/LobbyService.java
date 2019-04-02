package tv.weplay.ws.lobby.service;

import java.util.List;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import tv.weplay.ws.lobby.model.dto.*;

public interface LobbyService {

    @NewSpan
    Lobby create(@SpanTag("lobby") Lobby lobby);
    @NewSpan
    Lobby update(@SpanTag("lobby") Lobby lobby);
    @NewSpan
    void delete(@SpanTag("id") Long id);

    void deleteAll();

    Lobby findById(Long id);

    List<Lobby> findAll();
    @NewSpan
    void startOrCancelLobby(@SpanTag("lobbyId") Long lobbyId);
    @NewSpan
    void cancel(@SpanTag("lobbyId") Long lobbyId, boolean shouldNotifyTM);
    @NewSpan
    void start(@SpanTag("lobbyId") Long lobbyId);
    @NewSpan
    void updateMemberStatus(@SpanTag("lobbyId") Long lobbyId, @SpanTag("memberId") Long memberId);
    @NewSpan
    void voteRandomCard(@SpanTag("id") Long id, @SpanTag("lobbyMapStatus") LobbyMapStatus type);
    @NewSpan
    void voteCardByServer(@SpanTag("lobbyId") Long lobbyId, @SpanTag("cardId") Long cardId, @SpanTag("lobbyMapStatus") LobbyMapStatus type);
    @NewSpan
    void voteCardByUser(@SpanTag("lobbyId") Long lobbyId, @SpanTag("lobbyMap") LobbyMap map, @SpanTag("userId") Long userId);
}
