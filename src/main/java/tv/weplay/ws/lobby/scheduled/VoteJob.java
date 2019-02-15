package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import tv.weplay.ws.lobby.model.dto.LobbyMapType;
import tv.weplay.ws.lobby.service.LobbyService;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteJob extends QuartzJobBean {

    private static final String LOBBY_ID = "lobbyId";

    private final LobbyService lobbyService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        log.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        Long lobbyId = jobDataMap.getLong(LOBBY_ID);

        lobbyService.voteRandomCard(lobbyId, LobbyMapType.SERVER_PICK_TIMEOUT);

    }
}
