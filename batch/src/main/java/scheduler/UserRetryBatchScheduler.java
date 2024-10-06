package scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class UserRetryBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job retrySkippedEmailsJob;
    private final Job retryFailedUpdatesJob;

    // 스케줄러로 주기적으로 skippedEmails와 failedUpdates 재처리 작업 실행
    @Scheduled(cron = "0 0 * * * ?")  // 매 시간마다 재처리 실행
    public void retrySkippedEmails(JobLauncher jobLauncher, Job retrySkippedEmailsJob) {
        try {
            jobLauncher.run(retrySkippedEmailsJob, new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            System.err.println("skippedEmails 재처리 작업 실행 중 오류 발생: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 * * * ?")  // 매 시간 30분마다 DB 업데이트 재처리 실행
    public void retryFailedUpdates(JobLauncher jobLauncher, Job retryFailedUpdatesJob) {
        try {
            jobLauncher.run(retryFailedUpdatesJob, new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            System.err.println("failedUpdates 재처리 작업 실행 중 오류 발생: " + e.getMessage());
        }
    }
}