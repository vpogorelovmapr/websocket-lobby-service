package tv.weplay.ws.lobby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WebsocketLobbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketLobbyApplication.class, args);
    }

}

