package tv.weplay.ws.lobby.model.error;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorTypeMapping {

    private Long code;
    private String description;
}
