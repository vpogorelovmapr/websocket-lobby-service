package tv.weplay.ws.lobby.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.DeserializationFeature;
import com.github.jasminb.jsonapi.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.*;

@Configuration
public class JsonApiConverterConfiguration {

    @Bean
    public JsonApiConverter jsonApiConverter(ObjectMapper mapper) {
        JsonApiConverter converter = new JsonApiConverter(mapper, Lobby.class, LobbyMap.class, TournamentMember.class);
        converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
        converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);
        return converter;
    }
}
