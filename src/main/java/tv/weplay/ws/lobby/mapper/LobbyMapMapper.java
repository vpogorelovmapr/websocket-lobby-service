package tv.weplay.ws.lobby.mapper;

import tv.weplay.ws.lobby.model.dto.LobbyMap;
import tv.weplay.ws.lobby.model.entity.LobbyMapEntity;

import java.util.List;

public interface LobbyMapMapper {

    LobbyMap toDTO(LobbyMapEntity entity);

    LobbyMapEntity toEntity(LobbyMap dto);

    List<LobbyMap> toDTOs(List<LobbyMapEntity> entities);

    List<LobbyMapEntity> toEntities(List<LobbyMap> entities);
}
