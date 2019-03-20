package tv.weplay.ws.lobby.mapper;

import java.util.List;
import tv.weplay.ws.lobby.model.dto.LobbyMap;
import tv.weplay.ws.lobby.model.entity.LobbyMapEntity;

public interface LobbyMapMapper {

    LobbyMap toDTO(LobbyMapEntity entity);

    LobbyMapEntity toEntity(LobbyMap dto);

    List<LobbyMap> toDTOs(List<LobbyMapEntity> entities);

    List<LobbyMapEntity> toEntities(List<LobbyMap> entities);
}
