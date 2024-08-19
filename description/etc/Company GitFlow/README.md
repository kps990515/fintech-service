## 회사 Git 전략 : 기능 단위의 수시배포에 적합

### 브랜치
1. Release 
    - 개발의 시작점
    - 개발자는 Release브랜치에서 feature브랜치를 생성
    - 운영 배포 시 개별 Feature브랜치를 Release에 병합
2. develop, stage
    - 테스트용 브랜치
3. Master
    - 기록용 브랜치, 히스토리 관리

### 배포 순서
1. Release에서 개발요건별로 Feature 브랜치 생성
2. 개발완료되면 develop, stage머지 후 테스트
3. 테스트 완료되면 Release 머지(운영반영)

### 특징
1. Feature 브랜치의 개별 배포 : 특정기능(feature)개발되는 즉시, 다른 기능과 독립적으로 유연하게 배포 가능
2. 배포의 유연성 : Feature단위로 독립적 테스트, 운영 배포 가능
3. 빠른 문제 대응 : 배포된 기능 문제 발생 시 해당 feature브랜치만 수정해서 바로 배포 가능

