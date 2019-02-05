package tv.weplay.ws.lobby.model.dto;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.*;

@Data
@Type("Tournament")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    @Id(LongIdHandler.class)
    private Long id;
}
