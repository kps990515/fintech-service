package config;

import exception.DBUpdateFailedException;
import exception.EmailRetryableException;
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
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class UserBatchConfig {

    private final UserRdbRepository userRdbRepository;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    // Retry 및 Skip 처리 적용 (최대 3회 재시도 후 실패한 항목은 Redis에 저장)
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);  // 최대 3회 재시도
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @Bean
    // 메인쓰레드와 분리된 새로운 쓰레드 풀 생성해서 병렬처리
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);  // 최소 10개의 스레드 유지
        taskExecutor.setMaxPoolSize(50);   // 최대 50개의 스레드 허용
        taskExecutor.setQueueCapacity(100); // 대기 큐 용량
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setThreadNamePrefix("batch-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    // TaskExecutorRepeatTemplate:  Spring Batch에서 병렬로 처리하기 위해 사용하는 템플릿
    public RepeatOperations customRepeatOperations() {
        TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
        repeatTemplate.setTaskExecutor(taskExecutor());  // TaskExecutor 적용
        return repeatTemplate;
    }

    @Bean
    public Job sendPasswordChangeEmailJob(JobRepository jobRepository, Step sendPasswordChangeEmailStep) {
        return new JobBuilder("sendPasswordChangeEmailJob", jobRepository)
                .start(sendPasswordChangeEmailStep)
                .build();
    }

    @Bean
    public Step sendPasswordChangeEmailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("sendPasswordChangeEmailStep", jobRepository)
                .<UserEntity, UserEntity>chunk(100, transactionManager)  // Chunk 단위로 100개씩 처리
                .reader(userReader())  // Chunk 기반 페이징 처리
                .processor(userProcessor())  // 이메일 전송 로직 적용
                .writer(userWriter())
                .faultTolerant()
                .retryLimit(3)  // 3회 재시도
                .retry(Exception.class)
                .skipPolicy(skipPolicy())  // 실패한 경우 스킵 후 Redis에 저장
                .startLimit(3)  // 스텝 자체의 최대 실행 횟수 제한
                .stepOperations(customRepeatOperations())  // 병렬 처리 적용
                .build();
    }

    @Bean
    public ItemReader<UserEntity> userReader() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return new RepositoryItemReaderBuilder<UserEntity>()
                .repository(userRdbRepository)
                .methodName("findUsersToNotify")
                .arguments(Collections.singletonList(oneMonthAgo))
                .build();
    }

    @Bean
    public ItemProcessor<UserEntity, UserEntity> userProcessor() {
        return user -> {
            try {
                emailService.sendPasswordChangeEmail(user.getEmail());  // 이메일 발송
            } catch (Exception e) {
                // 이메일 발송 실패 시 예외를 던져 Retry를 유도
                throw new EmailRetryableException(user, e);
            }
            return user;
        };
    }

    @Bean
    public ItemWriter<UserEntity> userWriter() {
        return users -> {
            for (UserEntity user : users) {
                try {
                    user.setIsPwModifySednYn(true);  // 이메일 발송 성공 후 필드 업데이트
                    userRdbRepository.save(user);    // 데이터베이스에 저장
                } catch (Exception e) {
                    // DB 업데이트 실패 시 커스텀 예외를 던짐
                    throw new DBUpdateFailedException(user.getUserId(), e);
                }
            }
        };
    }

    @Bean
    public SkipPolicy skipPolicy() {
        return (throwable, skipCount) -> {
            if (skipCount >= 3) {  // 3회 재시도 후에도 실패한 경우
                if (throwable instanceof EmailRetryableException) {
                    // 이메일 발송 실패 시 Redis에 저장
                    UserEntity failedUser = ((EmailRetryableException) throwable).getUser();
                    redisTemplate.opsForList().leftPush("skippedEmails", failedUser.getEmail());
                    log.error("이메일 발송 실패로 Redis에 저장: " + failedUser.getEmail());
                } else if (throwable instanceof DBUpdateFailedException) {
                    // DB 업데이트 실패 시 Redis에 저장
                    String failedUserId = ((DBUpdateFailedException) throwable).getUser();
                    redisTemplate.opsForList().leftPush("failedUpdates", failedUserId);
                    log.error("DB 업데이트 실패로 Redis에 저장: " + failedUserId);
                }
                return true;  // 스킵을 허용
            }
            return false;  // 아직 재시도가 남아 있으면 스킵하지 않음
        };
    }
}
