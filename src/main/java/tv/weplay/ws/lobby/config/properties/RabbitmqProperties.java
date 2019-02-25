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

    private String incomingTournamentsQueueName;

    private String outcomingTournamentsQueueName;

    private String incomingUiQueueName;

    private String outcomingUiQueueName;

    private String deadLetterExchangeName;

    private String deadLetterQueueName;
}
