package tv.weplay.ws.lobby.service;


import java.util.Map;

public interface EventSenderService {

    void prepareAndSendEvent(String exchange, String data, String queueName, String type,
            Map<String, ?> headers);

    void prepareAndSendEvent(String exchange, byte[] data, String queueName, String type,
            Map<String, ?> headers);

    void prepareAndSendEvent(String exchange, byte[] data, String queueName, String type);

    String receiveAndConvert(String queueName, Long timeout);
}
