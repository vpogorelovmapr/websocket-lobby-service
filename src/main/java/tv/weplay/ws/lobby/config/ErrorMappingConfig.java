package tv.weplay.ws.lobby.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tv.weplay.ws.lobby.model.error.ErrorType;
import tv.weplay.ws.lobby.model.error.ErrorTypeMapping;

@Configuration
@RequiredArgsConstructor
public class ErrorMappingConfig {

    private InputStream globalErrorCodes = getClass().getClassLoader().getResourceAsStream("error-codes.json");

    private final ObjectMapper objectMapper;

    @Bean
    public Map<ErrorType, ErrorTypeMapping> errorCodeMappings() throws IOException {
        TypeReference typeReference = new TypeReference<Map<ErrorType, ErrorTypeMapping>>() {};
        return objectMapper.readValue(globalErrorCodes, typeReference);
    }
}
