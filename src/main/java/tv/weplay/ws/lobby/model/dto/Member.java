package tv.weplay.ws.lobby.model.dto;

import com.github.jasminb.jsonapi.LongIdHandler;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.*;

@Data
@Type("Member")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id(LongIdHandler.class)
    private Long id;
}
