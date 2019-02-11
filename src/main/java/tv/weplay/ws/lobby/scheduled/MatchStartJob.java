package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.common.EventTypes;
import tv.weplay.ws.lobby.config.properties.RabbitmqQueues;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.*;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.SchedulerService;
import tv.weplay.ws.lobby.service.impl.RabbitMQEventSenderService;

import java.time.ZonedDateTime;

import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.VOTE_GROUP;
import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.VOTE_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStartJob extends QuartzJobBean {

    public static final String LOBBY_ID = "lobbyId";

    private final LobbyService lobbyService;
    private final RabbitMQEventSenderService rabbitMQService;
    private final JsonApiConverter converter;
    private final RabbitmqQueues rabbitmqQueues;
    private final SchedulerService schedulerService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        Long lobbyId = jobDataMap.getLong(LOBBY_ID);
        Lobby lobby = lobbyService.findById(lobbyId);

        LobbyStatus status = allMatchMemberPresent(lobby) ? LobbyStatus.ONGOING : LobbyStatus.CANCELED;
        String type = status.equals(LobbyStatus.ONGOING) ? EventTypes.MATCH_STARTED_EVENT : EventTypes.MATCH_CANCELED_EVENT;
        lobby.setStatus(status);
        lobbyService.update(lobby);

        publishEventToRabbitMQ(lobby, type);

        if (status.equals(LobbyStatus.ONGOING)) {
            scheduleVoteJob(lobbyId);
        }
    }

    private boolean allMatchMemberPresent(Lobby lobby) {
        return lobby.getMatch().getMembers().stream()
                .map(MatchMember::getStatus)
                .noneMatch(status -> status.equals(MemberStatus.OFFLINE));
    }

    @SneakyThrows
    private void publishEventToRabbitMQ(Lobby lobby, String type) {
        Lobby event = buildLobbyEvent(lobby);
        byte[] data = converter.writeObject(event);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingUiEvents(), type);
        rabbitMQService.prepareAndSendEvent(data, rabbitmqQueues.getOutcomingTournamentsEvents(), type);
    }

    private Lobby buildLobbyEvent(Lobby lobby) {
        return Lobby.builder()
                .id(lobby.getId())
                .status(lobby.getStatus())
                .build();
    }

    private void scheduleVoteJob(Long lobbyId) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(LOBBY_ID, lobbyId);
        schedulerService.schedule(VOTE_PREFIX + lobbyId, VOTE_GROUP, ZonedDateTime.now().plusSeconds(15), dataMap, 15,
                VoteJob.class);
    }
}
