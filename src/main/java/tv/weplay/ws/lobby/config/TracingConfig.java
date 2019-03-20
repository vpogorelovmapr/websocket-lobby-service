package tv.weplay.ws.lobby.config;

import brave.handler.FinishedSpanHandler;
import brave.handler.MutableSpan;
import brave.propagation.TraceContext;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.zipkin2.ZipkinProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ZipkinProperties.class) // need if you want to disable zipkin without disabling sleuth
public class TracingConfig {

    private static Set<String> excludedSpans = new HashSet<String>() {
        {
            add("publish");
            add("next-message");
        }
    };

    @Bean
    FinishedSpanHandler excludeSpans() {
        return new FinishedSpanHandler() {
            @Override
            public boolean handle(TraceContext traceContext, MutableSpan span) {
                String spanName = span.name();
                if (StringUtils.isBlank(spanName) || excludedSpans.contains(spanName)) {
                    return false;
                }
                return true;
            }
        };
    }
}
