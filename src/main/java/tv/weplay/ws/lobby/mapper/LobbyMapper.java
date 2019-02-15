package tv.weplay.ws.lobby.mapper;

import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

import java.util.List;

public interface LobbyMapper {

    Lobby toDTO(LobbyEntity entity);

    LobbyEntity toEntity(Lobby dto);

    List<Lobby> toDTOs(List<LobbyEntity> entities);
}
