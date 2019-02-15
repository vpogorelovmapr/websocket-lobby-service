package tv.weplay.ws.lobby.mapper;

import tv.weplay.ws.lobby.model.dto.Match;
import tv.weplay.ws.lobby.model.entity.MatchEntity;

public interface MatchMapper {

    Match toDTO(MatchEntity entity);

    MatchEntity toEntity(Match dto);

}
