package tv.weplay.ws.lobby.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.mapper.*;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LobbyMapperImpl implements LobbyMapper {

    private final LobbyMapMapper lobbyMapMapper;
    private final MatchMapper matchMapper;

    @Override
    public Lobby toDTO(LobbyEntity entity) {
        if (entity == null) {
            return null;
        }
        return Lobby.builder()
                .id(entity.getId())
                .duration(entity.getDuration())
                .settings(entity.getSettings())
                .status(entity.getStatus())
                .match(matchMapper.toDTO(entity.getMatch()))
                .lobbyMap(lobbyMapMapper.toDTOs(entity.getLobbyMap()))
                .startDatetime(entity.getLobbyStartDatetime())
                .build();
    }

    @Override
    public LobbyEntity toEntity(Lobby dto) {
        if (dto == null) {
            return null;
        }
        return LobbyEntity.builder()
                .id(dto.getId())
                .duration(dto.getDuration())
                .settings(dto.getSettings())
                .status(dto.getStatus())
                .match(matchMapper.toEntity(dto.getMatch()))
                .lobbyMap(lobbyMapMapper.toEntities(dto.getLobbyMap()))
                .lobbyStartDatetime(dto.getStartDatetime())
                .build();
    }

    @Override
    public List<Lobby> toDTOs(List<LobbyEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
