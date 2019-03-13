package tv.weplay.ws.lobby.mapper.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.mapper.LobbyMapMapper;
import tv.weplay.ws.lobby.model.dto.LobbyMap;
import tv.weplay.ws.lobby.model.entity.LobbyMapEntity;

@Component
public class LobbyMapMapperImpl implements LobbyMapMapper {

    @Override
    public LobbyMap toDTO(LobbyMapEntity entity) {
        if (entity == null) {
            return null;
        }
        return LobbyMap.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .vote(entity.getVote())
                .voteItem(entity.getVoteItem())
                .member(entity.getMember())
                .build();
    }

    @Override
    public LobbyMapEntity toEntity(LobbyMap dto) {
        if (dto == null) {
            return null;
        }
        return LobbyMapEntity.builder()
                .id(dto.getId())
                .status(dto.getStatus())
                .vote(dto.getVote())
                .voteItem(dto.getVoteItem())
                .member(dto.getMember())
                .build();
    }

    @Override
    public List<LobbyMap> toDTOs(List<LobbyMapEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LobbyMapEntity> toEntities(List<LobbyMap> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
