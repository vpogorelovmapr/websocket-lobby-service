package tv.weplay.ws.lobby.service;

import java.time.ZonedDateTime;
import org.quartz.Job;
import org.quartz.JobDataMap;

public interface SchedulerService {

    void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map,
            Class<? extends Job> jobClass);

    void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map,
            Integer interval,
            Class<? extends Job> jobClass);

    void unschedule(String identity, String group);

}
