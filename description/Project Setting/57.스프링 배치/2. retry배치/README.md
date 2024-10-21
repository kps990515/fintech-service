## 스프링 Retry배치
- Redis에 저장된 실패된 애들 재 처리
- RetryTemplate, TaskExecutor는 동일

```java
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
```

1. skippedEmails 재처리 Job
```java
@Bean
public Job sendPasswordChangeEmailJob(JobRepository jobRepository, Step sendPasswordChangeEmailStep) {
    return new JobBuilder("sendPasswordChangeEmailJob", jobRepository)
            .start(sendPasswordChangeEmailStep)
            .build();
} 
```

2. failedUpdates 재처리 Job
```java
@Bean
public Job retryFailedUpdatesJob(JobRepository jobRepository, Step retryFailedUpdatesStep) {
    return new JobBuilder("retryFailedUpdatesJob", jobRepository)
            .start(retryFailedUpdatesStep)
            .build();
}
```

3. skippedEmails 읽어서 다시 이메일 발송하는 Step
```java
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
```

4. failedUpdates 읽어서 다시 DB 업데이트하는 Step
```java
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
```

5. failed Email처리
- ItemReader
```java
@Bean
public ItemReader<String> skippedEmailsReader() {
    return new RedisItemReader(redisTemplate, "skippedEmails");
}
```
- ItemProcessor
```java
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
```
- ItemWriter
```java
@Bean
public ItemWriter<String> skippedEmailsWriter() {
    return emails -> log.debug("처리된 이메일 수: " + emails.size());
} 
```

6. Failed DB처리
- ItemReader
```java
public ItemReader<String> failedUpdatesReader() {
        return new RedisItemReader(redisTemplate, "failedUpdates");
}
```
- ItemProcessor
```java
@Bean
public ItemProcessor<String, UserEntity> failedUpdateProcessor() {
    return userId -> userRdbRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));
}
```
- ItemWriter
```java
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
```

7. Scheduler
```java
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
```



