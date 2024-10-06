## 비동기 멀티스레드

### 비동기 : 하나의 작업을 요청하고 완료되기 전에 다른 작업을 요청할수 있는 방식
### 멀티스레드 : 여러개 작업을 처리할 수 있도록 여러개 쓰레드 사용하는 방식

### @Async와 CompletableFuture
- @Async
  - 메서드 자체를 비동기로 처리
  - 결과 반환을 기대하지 않는 비동기 작업에 적합
  - 반환이 필요하면 CompletableFuture결합해서 사용
  - 내부적으로 새로운 쓰레드 사용
- CompletableFuture
  - 작업결과를 비동기적으로 처리, 체이닝을 통해 여러 비동기 작업을 연결가능
  - 비동기 작업결과 처리 : thenApply(), thenAccept(), thenCompose()등

| **특징**                       | **`@Async`**                                        | **`CompletableFuture`**                               |
|---------------------------------|----------------------------------------------------|------------------------------------------------------|
| **소속**                        | 스프링 프레임워크                                   | 자바 표준 라이브러리                                   |
| **비동기 실행 방식**            | 메서드 자체를 비동기로 실행                         | 비동기 작업의 결과를 처리하고 체이닝 가능              |
| **결과 처리**                  | 일반적으로 결과를 반환하지 않음                     | 비동기 작업의 결과를 반환 및 후속 작업 체이닝 가능     |
| **비동기 활성화 필요 여부**     | `@EnableAsync`를 통해 활성화 필요                   | 자바 표준으로 별도의 활성화 필요 없음                  |
| **실행 환경**                  | 스프링의 쓰레드 풀을 사용                            | `ForkJoinPool` 또는 직접 정의한 쓰레드 풀 사용 가능    |
| **주요 용도**                   | 단순 비동기 작업 실행                               | 비동기 작업 간의 복잡한 체이닝 및 결과 처리            |

```java
@Service
public class AsyncCompletableService {

    @Async
    public CompletableFuture<String> asyncMethodWithResult() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);  // 2초 대기
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Async task result!";
        });
    }
}
```

### 자바에서 사용하는 방법
- @Async
```java
@Service
public class AsyncService {

    @Async
    public void asyncMethod() {
        try {
            Thread.sleep(2000);  // 2초 동안 대기
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Async method is running!");
    }
}
```
```java
@RestController
public class AsyncController {

    @Autowired
    private AsyncService asyncService;

    @GetMapping("/execute-async")
    public String executeAsync() {
        System.out.println("Request received. Executing async method...");
        asyncService.asyncMethod();  // 비동기 메서드 호출
        System.out.println("Request processed.");
        return "Async request is being processed!";
    }
}
```

1. Thread클래스
```java
public class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread is running!");
    }

    public static void main(String[] args) {
        MyThread thread = new MyThread();
        thread.start();  // 새로운 쓰레드에서 실행
        System.out.println("Main thread is running");
    }
} 
```
2. Runnable 인터페이스
- 스레드가 시작되면, run() 메서드 내의 코드가 실행
- Thread 클래스 : Thread생성 및 제어
- Runnable : Thread가 실행할 작업 정의
```java
// Runnable 인터페이스를 구현한 클래스
public class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Thread is running: " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        // Runnable 객체 생성
        MyTask task = new MyTask();
        // Thread 생성, Runnable 전달
        Thread thread = new Thread(task);
        // 스레드 시작
        thread.start();
    }
}
```

3. ExecutorService
- 쓰레드 풀을 생성하고 비동기로 작업처리
- submit()을 통해 작업을 쓰레드 풀에 전달하고 비동기로 실행
- shutdown()을 통해 작업 완료 후 쓰레드풀 종료
```java
public class ExecutorExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable task1 = () -> {
            System.out.println("Task 1 is running");
        };

        Runnable task2 = () -> {
            System.out.println("Task 2 is running");
        };

        executor.submit(task1);
        executor.submit(task2);

        executor.shutdown();  // 모든 작업이 완료된 후 종료
    }
} 
```
```java
public class DatabaseDataProcessor implements Runnable {
    private List<String> dataBatch;

    public DatabaseDataProcessor(List<String> dataBatch) {
        this.dataBatch = dataBatch;
    }

    @Override
    public void run() {
        // 병렬 데이터 처리 로직
        for (String data : dataBatch) {
            System.out.println("Processing data: " + data);
            // 예시: 데이터 저장, 변환, API 호출 등의 작업
            try {
                Thread.sleep(500); // 각 데이터 처리에 500ms 소요되는 것으로 가정
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Batch processing complete.");
    }

    public static void main(String[] args) {
        // 예시: 데이터베이스에서 가져온 데이터를 3개의 배치로 나누어 병렬 처리
        List<String> dataBatch1 = List.of("Data 1", "Data 2", "Data 3");
        List<String> dataBatch2 = List.of("Data 4", "Data 5", "Data 6");
        List<String> dataBatch3 = List.of("Data 7", "Data 8", "Data 9");

        // 스레드 풀 생성 (고정된 스레드 수로 병렬 처리)
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 각 배치 데이터를 처리할 스레드 등록
        executor.submit(new DatabaseDataProcessor(dataBatch1));
        executor.submit(new DatabaseDataProcessor(dataBatch2));
        executor.submit(new DatabaseDataProcessor(dataBatch3));

        // 작업 완료 후 스레드 풀 종료
        executor.shutdown();
    }
}
```

4. CompletableFuture
- supplyAsync()는 비동기적으로 작업을 수행한 후 결과를 반환
- thenApply() : 비동기 작업 완료 후 결과를 변환하는데 사용
- thenAccept() : 비동기 작업은 수행하지만 결과는 반환하지 않음
- thenCompose() : 비동기 작업이 완료된 후 또 다른 비동기 작업을 연결
- future.get()은 결과를 기다리고 반환하는 블로킹 메서드입니다.
```java
public class CompletableFutureExample {
    public static void main(String[] args) {
        CompletableFuture.runAsync(() -> {
            System.out.println("Asynchronous task is running!");
        });

        System.out.println("Main thread is running");
    }
} 
```
```java
@RestController
public class ApiController {

    @Autowired
    private ApiService apiService;

    @GetMapping("/async-api")
    public CompletableFuture<String> callAsyncApi() {
        String apiUrl = "https://jsonplaceholder.typicode.com/posts/1";
        return apiService.fetchDataFromApi(apiUrl);
    }
}
```
- thenApply()
```java
@Service
public class ApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Async
    public CompletableFuture<String> fetchDataFromApi(String apiUrl) {
        // 비동기적으로 API 호출 처리
        return CompletableFuture.supplyAsync(() -> {
            String response = restTemplate.getForObject(apiUrl, String.class);
            return "API Data: " + response;
        }).thenApply(result -> {
            System.out.println("Result: " + result);  // 비동기 작업 완료 후 결과 처리
            return result;
        });
    }
}
```

- thenAccept()
```java
CompletableFuture<byte[]> downloadFuture = CompletableFuture.supplyAsync(() -> {
    // 파일 다운로드
    return new byte[1024];  // 예시 데이터
});

// 결과를 받아 파일로 저장 (결과 반환 없음)
downloadFuture.thenAccept(fileData -> {
    System.out.println("Saving file with size: " + fileData.length);
});
```

- thenCompose()
```java
CompletableFuture<String> authenticateUser = CompletableFuture.supplyAsync(() -> {
    // 사용자 인증
    return "UserToken";
});

// 사용자 인증 후 데이터를 가져오는 비동기 작업 연결
CompletableFuture<String> fetchData = authenticateUser.thenCompose(token -> {
    return CompletableFuture.supplyAsync(() -> {
        // 토큰으로 API 호출
        return "UserData for token: " + token;
    });
});

fetchData.thenAccept(data -> System.out.println(data));
```

5. kafka에서의 사용법
```java
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    @Async  // 비동기적으로 처리
    public void consume(String message) {
        System.out.println("Consuming message: " + message);
        // 여기서 여러 메시지를 병렬로 처리할 수 있음
    }
}
```

6. Redis에서의 사용법
```java
@Service
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Async  // 비동기적으로 메시지 전송
    public void publishMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
        System.out.println("Published message to channel: " + channel);
    }
}
```