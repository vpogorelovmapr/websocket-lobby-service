package tv.weplay.ws.lobby.repository;

import org.springframework.data.repository.CrudRepository;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

import java.util.List;

public interface LobbyRepository extends CrudRepository<LobbyEntity, Long> {
    List<LobbyEntity> findAll();
}
