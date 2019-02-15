package tv.weplay.ws.lobby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@EnableRedisRepositories
public class WebsocketLobbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketLobbyApplication.class, args);
    }

}

