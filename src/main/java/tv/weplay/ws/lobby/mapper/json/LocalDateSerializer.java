package tv.weplay.ws.lobby.mapper.json;

import static java.time.format.DateTimeFormatter.ofPattern;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer extends JsonSerializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.format(FORMATTER));
    }
}
