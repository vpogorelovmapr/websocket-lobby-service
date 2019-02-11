package tv.weplay.ws.lobby.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tv.weplay.ws.lobby.service.SchedulerService;

import java.time.ZonedDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    public static final String MATCH_START_GROUP = "match-start-group";
    public static final String VOTE_GROUP = "vote-group";
    public static final String VOTE_PREFIX = "vote_";

    private final Scheduler scheduler;

    public void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map,
                         Class<? extends Job> jobClass) {
        schedule(identity, jobGroup, startAt, map, null, jobClass);
    }

    @SneakyThrows
    public void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map, Integer interval,
                         Class<? extends Job> jobClass) {
        JobDetail jobDetail = buildJobDetail(identity, jobGroup, map, jobClass);
        Trigger trigger = buildJobTrigger(jobDetail, startAt, interval);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    @SneakyThrows
    public void unschedule(String identity, String group) {
        scheduler.deleteJob(new JobKey(identity, group));
        scheduler.unscheduleJob(new TriggerKey(identity, group));
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt, Integer interval) {
        ScheduleBuilder<?> scheduleBuilder = interval != null ? SimpleScheduleBuilder.repeatSecondlyForever(interval) :
                SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow();
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), jobDetail.getKey().getGroup())
                .withDescription(jobDetail.getDescription())
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(scheduleBuilder)
                .build();
    }

    private JobDetail buildJobDetail(String identity, String jobGroup, JobDataMap jobDataMap,
                                     Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(identity, jobGroup)
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

}
