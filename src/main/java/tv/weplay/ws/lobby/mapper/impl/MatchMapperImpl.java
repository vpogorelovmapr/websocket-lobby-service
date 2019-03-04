package tv.weplay.ws.lobby.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.mapper.MatchMapper;
import tv.weplay.ws.lobby.mapper.MatchMemberMapper;
import tv.weplay.ws.lobby.model.dto.Match;
import tv.weplay.ws.lobby.model.entity.MatchEntity;

@Component
@RequiredArgsConstructor
public class MatchMapperImpl implements MatchMapper {

    private final MatchMemberMapper matchMemberMapper;

    @Override
    public Match toDTO(MatchEntity entity) {
        if (entity == null) {
            return null;
        }
        return Match.builder()
                .id(entity.getId())
                .startDatetime(entity.getStartDatetime())
                .endDatetime(entity.getEndDatetime())
                .score2(entity.getScore2())
                .score1(entity.getScore1())
                .node(entity.getNode())
                .status(entity.getStatus())
                .player1(entity.getPlayer1())
                .player2(entity.getPlayer2())
                .members(matchMemberMapper.toDTOs(entity.getMembers()))
                .build();
    }

    @Override
    public MatchEntity toEntity(Match dto) {
        if (dto == null) {
            return null;
        }
        return MatchEntity.builder()
                .id(dto.getId())
                .startDatetime(dto.getStartDatetime())
                .endDatetime(dto.getEndDatetime())
                .score2(dto.getScore2())
                .score1(dto.getScore1())
                .node(dto.getNode())
                .status(dto.getStatus())
                .player1(dto.getPlayer1())
                .player2(dto.getPlayer2())
                .members(matchMemberMapper.toEntities(dto.getMembers()))
                .build();
    }
}
