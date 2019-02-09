package tv.weplay.ws.lobby.service;

import tv.weplay.ws.lobby.model.dto.Lobby;

import java.util.List;

public interface LobbyService {

    Lobby create(Lobby lobby);

    Lobby update(Lobby lobby);

    void delete(Long id);

    Lobby findById(Long id);

    List<Lobby> findAll();
}
