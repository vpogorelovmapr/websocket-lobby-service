package tv.weplay.ws.lobby.mapper.impl;

import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.mapper.LobbyMapper;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LobbyMapperImpl implements LobbyMapper {
    @Override
    public Lobby toDTO(LobbyEntity lobbyEntity) {
        if (lobbyEntity == null) {
            return null;
        }
        return Lobby.builder()
                .id(lobbyEntity.getId())
                .duration(lobbyEntity.getDuration())
                .settings(lobbyEntity.getSettings())
                .status(lobbyEntity.getStatus())
                .match(lobbyEntity.getMatch())
                .lobbyMap(lobbyEntity.getLobbyMap())
                .lobbyStartDatetime(lobbyEntity.getLobbyStartDatetime())
                .build();
    }

    @Override
    public LobbyEntity toEntity(Lobby lobbyDTO) {
        if (lobbyDTO == null) {
            return null;
        }
        return LobbyEntity.builder()
                .id(lobbyDTO.getId())
                .duration(lobbyDTO.getDuration())
                .settings(lobbyDTO.getSettings())
                .status(lobbyDTO.getStatus())
                .match(lobbyDTO.getMatch())
                .lobbyMap(lobbyDTO.getLobbyMap())
                .lobbyStartDatetime(lobbyDTO.getLobbyStartDatetime())
                .build();
    }

    @Override
    public List<Lobby> toDTOs(List<LobbyEntity> lobbies) {
        if (lobbies == null) {
            return null;
        }
        return lobbies.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
