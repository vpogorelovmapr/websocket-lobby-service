package tv.weplay.ws.lobby.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import tv.weplay.ws.lobby.model.entity.LobbyEntity;

public interface LobbyRepository extends CrudRepository<LobbyEntity, Long> {

    List<LobbyEntity> findAll();
}
