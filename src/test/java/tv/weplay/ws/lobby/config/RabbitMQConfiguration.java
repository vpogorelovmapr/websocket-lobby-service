package tv.weplay.ws.lobby.config;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
import tv.weplay.ws.lobby.config.properties.RabbitmqProperties;

import java.util.Map;

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
    public Queue tournamentIncomingQueue() {
        return new Queue(rabbitmqProperties.getIncomingTournamentsQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue tournamentCancelQueue() {
        return new Queue(rabbitmqProperties.getTournamentsCancelQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue incomingUIQueue() {
        return new Queue(rabbitmqProperties.getIncomingUiQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue outcomingTournamentQueue() {
        return new Queue(rabbitmqProperties.getOutcomingTournamentsQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue outcomingUIQueue() {
        return new Queue(rabbitmqProperties.getOutcomingUiQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public Queue privateQueue() {
        return new Queue(rabbitmqProperties.getOutcomingPrivateQueueName(), true, false, false, dlxArgs());
    }

    @Bean
    public TopicExchange outcomingTournamentExchange() {
        return new TopicExchange(rabbitmqProperties.getOutcomingTournamentsQueueName());
    }

    @Bean
    public TopicExchange outcomingUIExchange() {
        return new TopicExchange(rabbitmqProperties.getOutcomingUiQueueName());
    }

    @Bean
    public TopicExchange privateExchange() {
        return new TopicExchange(rabbitmqProperties.getOutcomingPrivateQueueName());
    }

    @Bean
    public Binding outcomingTournamentBinding(Queue outcomingTournamentQueue, TopicExchange outcomingTournamentExchange) {
        return BindingBuilder.bind(outcomingTournamentQueue).to(outcomingTournamentExchange).with(rabbitmqProperties.getDeadLetterQueueName());
    }

    @Bean
    public Binding outcomingUIBinding(Queue outcomingUIQueue, TopicExchange outcomingUIExchange) {
        return BindingBuilder.bind(outcomingUIQueue).to(outcomingUIExchange).with("#");
    }

    @Bean
    public Binding privateBinding(Queue privateQueue, TopicExchange privateExchange) {
        return BindingBuilder.bind(privateQueue).to(privateExchange).with("#");
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
