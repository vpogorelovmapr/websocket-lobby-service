package tv.weplay.ws.lobby.mapper;

import tv.weplay.ws.lobby.model.dto.MatchMember;
import tv.weplay.ws.lobby.model.entity.MatchMemberEntity;

import java.util.List;

public interface MatchMemberMapper {

    MatchMember toDTO(MatchMemberEntity entity);

    MatchMemberEntity toEntity(MatchMember dto);

    List<MatchMember> toDTOs(List<MatchMemberEntity> entities);

    List<MatchMemberEntity> toEntities(List<MatchMember> dtos);
}
