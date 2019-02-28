package tv.weplay.ws.lobby.config;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
import tv.weplay.ws.lobby.config.properties.RabbitmqProperties;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfiguration {

    private final RabbitmqProperties rabbitmqProperties;

    @Bean
    public Map<String, Object> dlxArgs() {
        return ImmutableMap.of(
                "x-dead-letter-exchange", rabbitmqProperties.getDeadLetterExchangeName(),
                "x-dead-letter-routing-key", rabbitmqProperties.getDeadLetterQueueName());
    }

    @Bean
    public Queue frontendQueue() {
        return new Queue(rabbitmqProperties.getIncomingUiQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(rabbitmqProperties.getDeadLetterQueueName());
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(rabbitmqProperties.getDeadLetterExchangeName());
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(rabbitmqProperties.getDeadLetterQueueName());
    }
}
