package tv.weplay.ws.lobby.service;

import tv.weplay.ws.lobby.model.dto.*;

import java.util.List;

public interface LobbyService {

    Lobby create(Lobby lobby);

    Lobby update(Lobby lobby);

    void delete(Long id);

    Lobby findById(Long id);

    List<Lobby> findAll();

    void startVoting(Long lobbyId);

    void updateMemberStatus(Long lobbyId, Long memberId);

    void voteRandomCard(Long id, LobbyMapStatus type);

    void voteCardByServer(Long lobbyId, Long cardId, LobbyMapStatus type);

    void voteCardByUser(Long lobbyId, LobbyMap map, Long userId);
}
