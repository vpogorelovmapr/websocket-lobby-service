package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.model.dto.Lobby;
import tv.weplay.ws.lobby.service.LobbyService;

import static tv.weplay.ws.lobby.scheduled.SchedulerHelper.VOTE_GROUP;
import static tv.weplay.ws.lobby.scheduled.SchedulerHelper.VOTE_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteJob extends QuartzJobBean {

    public static final String LOBBY_ID = "lobbyId";

    private final LobbyService lobbyService;
    private final SchedulerHelper schedulerHelper;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        Long lobbyId = jobDataMap.getLong(LOBBY_ID);
        Lobby lobby = lobbyService.findById(lobbyId);

        lobbyService.voteRandomCard(lobbyId);

        if (lobbyService.isLastVote(lobby)) {
            lobbyService.voteRandomCard(lobbyId);
            schedulerHelper.unschedule(VOTE_PREFIX + lobbyId, VOTE_GROUP);
        }
    }
}
