package scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class UserBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job sendPasswordChangeEmailJob;

    @Scheduled(cron = "0 0 9 * * ?")  // 매일 아침 9시에 실행
    public void runPasswordChangeEmailJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(sendPasswordChangeEmailJob, jobParameters);
        } catch (Exception e) {
            // 오류 처리 로직
        }
    }
}