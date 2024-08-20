## CI/CD

### 절차
1. **코드 푸시 및 트리거**
   - Bitbucket 푸시하면 Webhook을 통해 CI/CD 파이프라인 트리거  
     <br>

2. **Tekton PipieLine 실행**
   -  yaml파일 설정되로 Tekton 파이프라인 트리거  
      <br>

3. **Docker 이미지 생성**
   - Tekton Pipeline으로 애플리케이션 코드로 Docker이미지 생성
     <br>


4. **Nexus 이미지 업로드**
   - 생성된 Docker 이미지 Nexus에 저장/관리
     <br>


5. **ArgoCd를 통한 배포**
   - CI과정이 완료되면 ArgoCD로 CD과정 실행
     <br>


6. **Blue-Green 배포 및 Istio 헬스 체크**
   - 배포 전략 : Blue-Green
   - Istio가 HealthCheck를 통해 신규 Pod 상태 확인
   - HealthCheck 통과시 기존 Pod의 연결을 종료(잔여 TX는 처리)
   - 신규 Pod로 트래픽 전환
