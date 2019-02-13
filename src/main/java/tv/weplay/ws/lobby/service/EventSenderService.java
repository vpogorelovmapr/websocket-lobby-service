package tv.weplay.ws.lobby.service;


public interface EventSenderService {

  void prepareAndSendEvent(String exchange, String data, String queueName, String type);

  void prepareAndSendEvent(String exchange, byte[] data, String queueName, String type);
}
