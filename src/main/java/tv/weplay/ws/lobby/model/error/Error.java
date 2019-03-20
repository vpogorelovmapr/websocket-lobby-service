package tv.weplay.ws.lobby.model.error;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Type("Error")
public class Error {

    @Id(LongIdHandler.class)
    private Long id;
    private String description;
    private LocalDateTime dateTime;
}
