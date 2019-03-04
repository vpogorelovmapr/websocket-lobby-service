package tv.weplay.ws.lobby.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.mapper.MatchMemberMapper;
import tv.weplay.ws.lobby.model.dto.MatchMember;
import tv.weplay.ws.lobby.model.entity.MatchMemberEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MatchMemberMapperImpl implements MatchMemberMapper {

    @Override
    public MatchMember toDTO(MatchMemberEntity entity) {
        if (entity == null) {
            return null;
        }
        return MatchMember.builder()
                .id(entity.getId())
                .tournamentMember(entity.getTournamentMember())
                .participationType(entity.getParticipationType())
                .status(entity.getStatus())
                .tournament(entity.getTournament())
                .build();
    }

    @Override
    public MatchMemberEntity toEntity(MatchMember dto) {
        if (dto == null) {
            return null;
        }
        return MatchMemberEntity.builder()
                .id(dto.getId())
                .tournamentMember(dto.getTournamentMember())
                .participationType(dto.getParticipationType())
                .status(dto.getStatus())
                .tournament(dto.getTournament())
                .build();
    }

    @Override
    public List<MatchMember> toDTOs(List<MatchMemberEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchMemberEntity> toEntities(List<MatchMember> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
