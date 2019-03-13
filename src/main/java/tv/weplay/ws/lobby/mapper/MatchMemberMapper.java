package tv.weplay.ws.lobby.mapper;

import java.util.List;
import tv.weplay.ws.lobby.model.dto.MatchMember;
import tv.weplay.ws.lobby.model.entity.MatchMemberEntity;

public interface MatchMemberMapper {

    MatchMember toDTO(MatchMemberEntity entity);

    MatchMemberEntity toEntity(MatchMember dto);

    List<MatchMember> toDTOs(List<MatchMemberEntity> entities);

    List<MatchMemberEntity> toEntities(List<MatchMember> dtos);
}
