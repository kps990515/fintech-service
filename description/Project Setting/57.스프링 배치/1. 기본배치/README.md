## 스프링 배치

### 동작원리
- Job: 배치 작업의 전체 흐름을 정의한 객체로, 여러 Step으로 구성
- Step: Job을 구성하는 최소 실행 단위로, 하나의 Job은 여러 Step으로 구성
- JobLauncher: Job을 실행하는 역할을 하며, Job과 JobParameters를 받아서 배치 작업을 시작
- JobRepository: 배치 메타데이터(실행 상태, 실행 이력, Step의 상태 등)를 저장하고 관리하는 역할
- ItemReader: 데이터를 읽는 역할
- ItemProcessor: 데이터를 가공하는 역할
- ItemWriter: 처리된 데이터를 저장하는 역할

### 주요기능
- Chunk : 스프링 배치는 대량의 데이터를 작은 청크(chunk)로 나누어 처리
- 트랜잭션 관리: 각 청크가 하나의 트랜잭션 단위로 처리되며, 청크 단위로 롤백가능
- Retry/Skip: 처리 중 오류가 발생했을 때 자동으로 재시도를 하거나, 특정 조건에 맞춰 오류를 스킵 가능
- 병렬 처리
- Restart: 배치 작업이 실패했을 경우, 실패한 지점에서부터 재시작할 수 있는 기능을 제공

### 코드
1. RetryTemplate : 재시도 정책
- 특정 함수에서 발생하는 예외/실패에 대해 재시도 수행 설정
- 재시도 횟수, 간격, 예외유형에 따른 다른 retry등 세밀한 설정 가능
```java
// Retry 및 Skip 처리 적용 (최대 3회 재시도 후 실패한 항목은 Redis에 저장)
@Bean
public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(3);  // 최대 3회 재시도
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
}
```

2. ThreadPoolTaskExecutor (병렬 처리용 스레드 풀 설정)
- CorePoolSize: 최소 유지되는 스레드 수
- Queue(기본값 Integer.MaxValue): CorePoolSize 스레드가 다 차면 큐에 저장 -> 큐가 꽉차면 MaxPoolSize로 넘어감
- MaxPoolSize : Queue가 꽉 찬 경우 추가적인 요청이 들어오면 Max Pool Size까지 스레드를 확장하여 처리
- setRejectedExecutionHandler : maxPoolsize도달, 대기큐 용량 초과시 처리하는 방법
  - CallerRunsPolicy: 작업을 거부하는 대신, 메인 스레드에서 직접 실행
  - AbortPolicy (기본값): 작업을 거부하고, RejectedExecutionException을 던져 예외 발생
  - DiscardPolicy: 작업을 그냥 무시하고 버림
  - DiscardOldestPolicy: 가장 오래된 대기 중인 작업을 버리고, 새로운 작업을 대기 큐에 추가
```java
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
```

3. TaskExecutorRepeatTemplate (병렬 처리 템플릿 설정)
```java
@Bean
// TaskExecutorRepeatTemplate:  Spring Batch에서 병렬로 처리하기 위해 사용하는 템플릿
public RepeatOperations customRepeatOperations() {
    TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
    repeatTemplate.setTaskExecutor(taskExecutor());  // TaskExecutor 적용
    return repeatTemplate;
} 
```

4. Job, Step
```java
@Bean
public Job sendPasswordChangeEmailJob(JobRepository jobRepository, Step sendPasswordChangeEmailStep) {
    return new JobBuilder("sendPasswordChangeEmailJob", jobRepository)
            .start(sendPasswordChangeEmailStep)
            .build();
} 
```
```java
@Bean
public Step sendPasswordChangeEmailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("sendPasswordChangeEmailStep", jobRepository)
            .<UserEntity, UserEntity>chunk(100, transactionManager)  // Chunk 단위로 100개씩 처리
            .reader(userReader())  // Chunk 기반 페이징 처리
            .processor(userProcessor())  // 이메일 전송 로직 적용
            .writer(userWriter())
            .faultTolerant()
            .retryLimit(3)  // reader,processor,writer 각각 3회 재시도
            .retry(Exception.class) // 모든 예외에 대해 재시도
            .skipPolicy(skipPolicy())  // 실패한 경우 스킵 후 Redis에 저장
            .startLimit(3)  // 스텝 자체의 최대 실행 횟수 제한
            .stepOperations(customRepeatOperations())  // 병렬 처리 적용
            .build();
}
```

5. ItemReader
```java
@Bean
public ItemReader<UserEntity> userReader() {
    LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
    return new RepositoryItemReaderBuilder<UserEntity>()
            .repository(userRdbRepository)
            .methodName("findUsersToNotify")
            .arguments(Collections.singletonList(oneMonthAgo))
            .build();
}
```

7. ItemProcessor
```java
@Bean
public ItemProcessor<UserEntity, UserEntity> userProcessor() {
    return user -> {
        try {
            // RetryTemplate을 사용해 이메일 발송 재시도 처리
            retryTemplate().execute(context -> {
                emailService.sendPasswordChangeEmail(user.getEmail());  // 이메일 발송
                return null;
            });
        } catch (Exception e) {
            // 이메일 발송 실패 시 예외를 던져 Retry를 유도
            throw new EmailRetryableException(user, e);
        }
        return user;
    };
}
```

8. ItemWriter
- chunk기반으로 트랜잭션실행되서 문제생기면 롤백
```java
@Bean
public ItemWriter<UserEntity> userWriter() {
    return users -> {
        for (UserEntity user : users) {
            try {
                // RetryTemplate을 사용해 DB 업데이트 재시도 처리
                retryTemplate().execute(context -> {
                    user.setIsPwModifySednYn(true);  // 이메일 발송 성공 후 필드 업데이트
                    userRdbRepository.save(user);    // 데이터베이스에 저장
                    return null;
                });
            } catch (Exception e) {
                // DB 업데이트 실패 시 커스텀 예외를 던짐
                throw new DBUpdateFailedException(user.getUserId(), e);
            }
        }
    };
}
```

9. Retry 및 Skip
```java
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
```

10. Exception
```java
@Getter
public class DBUpdateFailedException extends RuntimeException {
    private final String user;

    public DBUpdateFailedException(String user, Throwable cause) {
        super(cause);
        this.user = user;
    }
}
```
```java
@Getter
public class EmailRetryableException extends RuntimeException {
    private final UserEntity user;

    public EmailRetryableException(UserEntity user, Throwable cause) {
        super(cause);
        this.user = user;
    }
}
```

11. Scheduler
```java
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
```



