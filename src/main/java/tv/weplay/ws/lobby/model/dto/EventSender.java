package tv.weplay.ws.lobby.model.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSender {

    private String service;

    private String version;
}
