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
    void cancelVoting(@SpanTag("lobbyId") Long lobbyId);

    void startVoting(Long lobbyId);

    void updateMemberStatus(Long lobbyId, Long memberId);

    void voteRandomCard(Long id, LobbyMapStatus type);

    void voteCardByServer(Long lobbyId, Long cardId, LobbyMapStatus type);

    void voteCardByUser(Long lobbyId, LobbyMap map, Long userId);
}
