package tv.weplay.ws.lobby;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tv.weplay.ws.lobby.service.LobbyServiceTest;
import tv.weplay.ws.lobby.service.RabbitMQEventsTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        LobbyServiceTest.class,
        RabbitMQEventsTest.class
})
public class IntegrationTestSuite {

}
