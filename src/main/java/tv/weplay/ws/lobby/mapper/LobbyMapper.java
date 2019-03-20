package tv.weplay.ws.lobby.mapper;

import java.util.List;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

public interface LobbyMapper {

    Lobby toDTO(LobbyEntity entity);

    LobbyEntity toEntity(Lobby dto);

    List<Lobby> toDTOs(List<LobbyEntity> entities);
}
