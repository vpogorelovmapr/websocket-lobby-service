package tv.weplay.ws.lobby;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tv.weplay.ws.lobby.config.properties.RabbitmqProperties;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.SchedulerService;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class AbstractEnd2EndTestBase {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    protected RabbitmqProperties rabbitmqProperties;

    @Autowired
    protected LobbyService lobbyService;

    @Autowired
    private RabbitAdmin admin;

    @After
    public void after() {
        lobbyService.deleteAll();
        admin.purgeQueue(rabbitmqProperties.getOutcomingUiQueueName(), true);
        admin.purgeQueue(rabbitmqProperties.getOutcomingTournamentsQueueName(), true);
        admin.purgeQueue(rabbitmqProperties.getOutcomingPrivateQueueName(), true);
        admin.purgeQueue(rabbitmqProperties.getDeadLetterQueueName(), true);
        schedulerService.clear();
    }

}
