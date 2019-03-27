package tv.weplay.ws.lobby.service;

import java.time.ZonedDateTime;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;

public interface SchedulerService {
    @NewSpan
    void schedule(@SpanTag("identity") String identity, @SpanTag("jobGroup") String jobGroup, @SpanTag("startAt") ZonedDateTime startAt, JobDataMap map,
            Class<? extends Job> jobClass);
    @NewSpan
    void schedule(@SpanTag("identity") String identity, @SpanTag("jobGroup") String jobGroup, @SpanTag("startAt") ZonedDateTime startAt, JobDataMap map,
            @SpanTag("interval") Integer interval,
            Class<? extends Job> jobClass);
    @NewSpan
    void unschedule(@SpanTag("identity") String identity, @SpanTag("group") String group);

    void clear();
}
