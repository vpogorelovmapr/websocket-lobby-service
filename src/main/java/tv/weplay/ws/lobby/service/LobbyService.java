package tv.weplay.ws.lobby.service;

import java.util.List;
import tv.weplay.ws.lobby.model.dto.*;

public interface LobbyService {

    Lobby create(Lobby lobby);

    Lobby update(Lobby lobby);

    void delete(Long id);

    void deleteAll();

    Lobby findById(Long id);

    List<Lobby> findAll();

    void startVoting(Long lobbyId);

    void updateMemberStatus(Long lobbyId, Long memberId);

    void voteRandomCard(Long id, LobbyMapStatus type);

    void voteCardByServer(Long lobbyId, Long cardId, LobbyMapStatus type);

    void voteCardByUser(Long lobbyId, LobbyMap map, Long userId);
}
