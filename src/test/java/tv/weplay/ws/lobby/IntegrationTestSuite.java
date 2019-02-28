package tv.weplay.ws.lobby;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tv.weplay.ws.lobby.service.LobbyServiceTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        LobbyServiceTest.class
})
public class IntegrationTestSuite {

}
