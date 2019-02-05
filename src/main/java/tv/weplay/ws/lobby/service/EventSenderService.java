package tv.weplay.ws.lobby.service;


public interface EventSenderService {

  void prepareAndSendEvent(String data, String queueNam);

  void prepareAndSendEvent(byte[] data, String queueNam);
}
