package tv.weplay.ws.lobby.config.properties;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitmqProperties {

    private String incomingTournamentsEvents;

    private String outcomingTournamentsEvents;

    private String incomingUiEvents;

    private String outcomingUiEvents;

    private String deadLetterExchangeName;

    private String deadLetterQueueName;
}
