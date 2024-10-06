package config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.payment.alarm.service.EmailService;
import org.payment.db.user.UserEntity;
import org.payment.db.user.UserRdbRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class RetryUserBatchConfig {

    private final UserRdbRepository userRdbRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    // TaskExecutor 생성 (병렬 처리)
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(50);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("retry-batch-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public RepeatOperations customRepeatOperations() {
        TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
        repeatTemplate.setTaskExecutor(taskExecutor());
        return repeatTemplate;
    }

    // 1. skippedEmails 재처리 Job
    @Bean
    public Job retrySkippedEmailsJob(JobRepository jobRepository, Step retrySkippedEmailsStep) {
        return new JobBuilder("retrySkippedEmailsJob", jobRepository)
                .start(retrySkippedEmailsStep)
                .build();
    }

    // 2. failedUpdates 재처리 Job
    @Bean
    public Job retryFailedUpdatesJob(JobRepository jobRepository, Step retryFailedUpdatesStep) {
        return new JobBuilder("retryFailedUpdatesJob", jobRepository)
                .start(retryFailedUpdatesStep)
                .build();
    }

    // 1. Redis에서 skippedEmails 읽어서 다시 이메일 발송하는 Step 구성
    @Bean
    public Step retrySkippedEmailsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("retrySkippedEmailsStep", jobRepository)
                .<String, String>chunk(100, transactionManager) // 한 번에 100개씩 처리
                .reader(skippedEmailsReader())  // Redis에서 skippedEmails 읽기
                .processor(skippedEmailsProcessor()) // 이메일 재발송 처리
                .writer(skippedEmailsWriter()) // 이메일 발송 결과 처리
                .stepOperations(customRepeatOperations())  // 병렬 처리 적용
                .build();
    }

    // 2. Redis에서 failedUpdates 읽어서 다시 DB 업데이트하는 Step 구성
    @Bean
    public Step retryFailedUpdatesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("retryFailedUpdatesStep", jobRepository)
                .<String, UserEntity>chunk(100, transactionManager) // 한 번에 100개씩 처리
                .reader(failedUpdatesReader())  // Redis에서 failedUpdates 읽기
                .processor(failedUpdateProcessor()) // 읽은 항목 처리
                .writer(userWriter())  // 다시 DB에 저장
                .stepOperations(customRepeatOperations())  // 병렬 처리 적용
                .build();
    }

    // Redis에서 skippedEmails 읽어오는 ItemReader
    @Bean
    public ItemReader<String> skippedEmailsReader() {
        return new RedisItemReader(redisTemplate, "skippedEmails");
    }

    // Redis에서 실패한 이메일에 대해 다시 발송하는 ItemProcessor
    @Bean
    public ItemProcessor<String, String> skippedEmailsProcessor() {
        return email -> {
            try {
                emailService.sendPasswordChangeEmail(email);  // 이메일 재발송
                log.debug("재발송 성공: " + email);
            } catch (Exception ex) {
                log.error("재발송 실패: " + email);
                redisTemplate.opsForList().leftPush("failedEmailRetry", email);  // 재발송 실패 시 다시 Redis에 저장
            }
            return email;
        };
    }

    // skippedEmails Writer
    @Bean
    public ItemWriter<String> skippedEmailsWriter() {
        return emails -> log.debug("처리된 이메일 수: " + emails.size());
    }

    // Redis에서 failedUpdates 읽어오는 ItemReader
    @Bean
    public ItemReader<String> failedUpdatesReader() {
        return new RedisItemReader(redisTemplate, "failedUpdates");
    }

    // Redis에서 가져온 데이터를 처리하는 ItemProcessor
    @Bean
    public ItemProcessor<String, UserEntity> failedUpdateProcessor() {
        return userId -> userRdbRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));
    }

    // DB에 업데이트하는 ItemWriter (메일 발송 성공 여부에 따라)
    @Bean
    public ItemWriter<UserEntity> userWriter() {
        return users -> {
            for (UserEntity user : users) {
                try {
                    user.setIsPwModifySednYn(true);  // 업데이트 처리
                    userRdbRepository.save(user);
                } catch (Exception e) {
                    redisTemplate.opsForList().leftPush("failedUpdates", user.getUserId());  // 실패 시 다시 저장
                    log.error("DB 업데이트 실패로 Redis에 저장: " + user.getUserId());
                }
            }
        };
    }

    // RedisItemReader 구현 (Redis에서 실패 항목을 읽어옴)
    public static class RedisItemReader implements ItemReader<String> {
        private final RedisTemplate<String, String> redisTemplate;
        private final String key;

        public RedisItemReader(RedisTemplate<String, String> redisTemplate, String key) {
            this.redisTemplate = redisTemplate;
            this.key = key;
        }

        @Override
        public String read() {
            return redisTemplate.opsForList().rightPop(key);  // Redis에서 항목 하나를 꺼냄
        }
    }
}
