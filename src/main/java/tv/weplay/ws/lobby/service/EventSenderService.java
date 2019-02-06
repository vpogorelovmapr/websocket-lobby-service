package tv.weplay.ws.lobby.service;


public interface EventSenderService {

  void prepareAndSendEvent(String data, String queueName, String type);

  void prepareAndSendEvent(byte[] data, String queueName, String type);
}
