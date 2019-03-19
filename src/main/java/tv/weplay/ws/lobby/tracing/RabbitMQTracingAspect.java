package tv.weplay.ws.lobby.tracing;

import static brave.Span.Kind.CLIENT;

import brave.Span;
import brave.Tracer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class RabbitMQTracingAspect {

    private static final String SERVICE_NAME = "rabbitmq";

    private final Tracer tracer;

    @Pointcut(
            "execution (* org.springframework.amqp.rabbit.core.RabbitTemplate.convertAndSend(String, String, Object, org.springframework.amqp.core.MessagePostProcessor)) "
                    + "&& args(exchange, routeKey, event, messagePostProcessor)")
    public void convertAndSend(String exchange, String routeKey, Object event,
            MessagePostProcessor messagePostProcessor) {
    }

    @Around("convertAndSend(exchange, routeKey, event, messagePostProcessor)")
    public Object addSenderSpan(ProceedingJoinPoint joinPoint, String exchange, String routeKey,
            Object event, MessagePostProcessor messagePostProcessor) throws Throwable {

        Object result;
        String methodName = joinPoint.getSignature().getName();
        Span span = tracer.nextSpan().name(methodName);
        span.remoteServiceName(SERVICE_NAME);
        span.kind(CLIENT);

        try {
            span.start();
            span.tag("method", methodName);
            span.tag("exchange", exchange);
            span.tag("routeKey", routeKey);
            span.tag("payload", event.toString());

            result = joinPoint.proceed();

        } finally {
            span.finish();
        }
        return result;
    }
}
