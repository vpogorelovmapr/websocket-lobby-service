package tv.weplay.ws.lobby.config;

import com.github.jasminb.jsonapi.DeserializationFeature;
import com.github.jasminb.jsonapi.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.dto.events.MatchCreatedEvent;

@Configuration
public class JsonApiConverterConfiguration {

    @Bean
    public JsonApiConverter jsonApiConverter() {
        JsonApiConverter converter = new JsonApiConverter(Lobby.class, MatchCreatedEvent.class);
        converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
        converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);
        return converter;
    }
}
