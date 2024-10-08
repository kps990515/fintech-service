### [환경 구성]
### 1. EC2 인스턴스
    - 클라우드 환경에서 안정적이고 확장 가능한 배포를 위해 AWS EC2 인스턴스를 선택하였습니다. 이를 통해 Jenkins와 Docker 컨테이너를 호스팅할 수 있었습니다.
### 2. Docker
    - 애플리케이션과 그 의존성을 하나의 컨테이너에 담아, 환경에 구애받지 않고 일관된 실행 환경을 제공하기 위해 Docker를 사용했습니다. 이를 통해 개발 환경, 테스트 환경, 프로덕션 환경 모두에서 동일한 설정으로 애플리케이션을 실행할 수 있었습니다
### 3. Jenkins(DID방식)
    - CI/CD 파이프라인의 중심에 Jenkins를 배치하여, 코드 변경 시 자동으로 빌드, 테스트, 배포까지 수행하도록 설정했습니다. 이를 통해 코드가 커밋될 때마다 자동으로 CI/CD 프로세스가 실행되어 빠른 피드백을 받을 수 있었습니다.  
<br>

### [CI/CD 파이프라인 구성]
### 1. 코드 연동 (GitHub)
    - GitHub와 Jenkins를 Webhook으로 연동하여, 개발자가 GitHub에 코드를 푸시하면 Jenkins가 이를 감지하고 자동으로 빌드 프로세스를 시작하도록 설정했습니다.
### 2. 빌드 및 테스트
    - Jenkins는 Docker를 활용해 Spring Boot 애플리케이션을 빌드하고, 단위 테스트 및 통합 테스트를 실행하여 코드 품질을 검증합니다.
### 3. Docker 이미지 생성 및 배포
    - 테스트가 성공적으로 완료되면, Jenkins는 Docker 이미지를 생성하고 Docker Hub에 푸시합니다. 이후, EC2 인스턴스에서 해당 이미지를 가져와 컨테이너를 재배포합니다.
### 4. 데이터베이스 연동 (MySQL)
    - MySQL은 별도의 Docker 컨테이너로 실행되며, Spring Boot 애플리케이션이 이를 사용하도록 설정하였습니다. 이를 통해 데이터베이스 환경을 손쉽게 관리하고, 필요 시 데이터베이스의 컨테이너를 독립적으로 관리할 수 있었습니다.
<br>

### [서비스 설명]
### 1. [토스페이먼츠 연동](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/6.%20Service/1.%20TossPayment/README.md)
### 2. [Object Mapper](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/6.%20Service/README.md)
<br>

### [환경구성]

### 1. [멀티모듈](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/1.%20MultiModule/README.md)
       fintech-service / api / db

### 2. [멀티프로필](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/2.%20MultiProfile/README.md)
       local / dev / prod

### 3. [Docker](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/3.%20Docker/README.md)

### 4. [Https](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/4.%20Https/README.md)

### 5. [CI/CD](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/5.%20CICD/README.md)

### 6. [AWS](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/7.%20AWS/README.md)
       EC2, VPC, RDS 

### 7. [Jenkins](https://github.com/kps990515/fintech-service/blob/master/description/Project%20Setting/README.md)

<br>

### [회사기술스택]

### [회사 Gitflow](https://github.com/kps990515/fintech-service/blob/master/description/etc/Company%20GitFlow/README.md)
### [로그적재/확인 flow](https://github.com/kps990515/fintech-service/tree/master/description/etc/Company%20LogFlow)
### [CI/CD](https://github.com/kps990515/fintech-service/blob/master/description/etc/Company%20CICD/README.md)