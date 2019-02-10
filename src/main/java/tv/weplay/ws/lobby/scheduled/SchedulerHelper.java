package tv.weplay.ws.lobby.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class SchedulerHelper {

    public static final String MATCH_START_GROUP = "match-start-group";

    private final Scheduler scheduler;

    public void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map,
                         Class<? extends Job> jobClass) {
        schedule(identity, jobGroup, startAt, map, null, jobClass);
    }

    @SneakyThrows
    public void schedule(String identity, String jobGroup, ZonedDateTime startAt, JobDataMap map, String cronExpression,
                         Class<? extends Job> jobClass) {
        JobDetail jobDetail = buildJobDetail(identity, jobGroup, map, jobClass);
        Trigger trigger = buildJobTrigger(jobDetail, startAt, cronExpression);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt, String cronExpression) {
        ScheduleBuilder<?> scheduleBuilder = cronExpression != null ? CronScheduleBuilder.cronSchedule(cronExpression) :
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