package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.config.properties.RabbitmqQueues;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStartJob extends QuartzJobBean {

    public static final String LOBBY_ID = "lobbyId";

    private final LobbyService lobbyService;
    private final RabbitMQEventSenderService rabbitMQService;
    private final JsonApiConverter converter;
    private final RabbitmqQueues rabbitmqQueues;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        Long lobbyId = jobDataMap.getLong(LOBBY_ID);
        Lobby lobby = lobbyService.findById(lobbyId);

        LobbyStatus status = allMatchMemberPresent(lobby) ? LobbyStatus.ONGOING : LobbyStatus.CANCELED;
        lobby.setStatus(status);
        lobbyService.update(lobby);
        publishEventToRabbitMQ(lobby);
    }

    private boolean allMatchMemberPresent(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .map(MatchMember::getStatus)
                .noneMatch(status -> status.equals(MemberStatus.OFFLINE));
    }

    @SneakyThrows
    private void publishEventToRabbitMQ(Lobby lobby) {
        Lobby event = buildLobbyEvent(lobby);
        byte[] data = converter.writeObject(event);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents(), EventTypes.MATCH_STATUS_EVENT);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingTournamentsEvents(),
                EventTypes.MATCH_STATUS_EVENT);
    }

    private Lobby buildLobbyEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .duration(lobby.getDuration())
                .status(lobby.getStatus())
                .build();
    }
}
