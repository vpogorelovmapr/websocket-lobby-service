package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.model.dto.LobbyMapType;
import tv.weplay.ws.lobby.service.LobbyService;
import tv.weplay.ws.lobby.service.SchedulerService;

import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.VOTE_GROUP;
import static tv.weplay.ws.lobby.service.impl.SchedulerServiceImpl.VOTE_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteJob extends QuartzJobBean {

    private static final String LOBBY_ID = "lobbyId";

    private final LobbyService lobbyService;
    private final SchedulerService schedulerService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        Long lobbyId = jobDataMap.getLong(LOBBY_ID);
        Lobby lobby = lobbyService.findById(lobbyId);

        lobbyService.voteRandomCard(lobbyId, LobbyMapType.SERVER_PICK_TIMEOUT);

        if (lobbyService.isLastVote(lobby)) {
            lobbyService.voteRandomCard(lobbyId, LobbyMapType.SERVER_PICK);
            schedulerService.unschedule(VOTE_PREFIX + lobbyId, VOTE_GROUP);

            //TODO: Send END event
        }
    }
}
