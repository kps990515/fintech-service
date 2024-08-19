## ECR
- Amazon Elastic Container Registry
- Docker 컨테이너 이미지 저장소

1. IAM 권한설정
- AmazonEC2ContainerRegistryPowerUser

2. Access키 만들기

3. Elastic Container Service -> Amazon Elastic Container Registry
- 프라이빗 리포지토리 만들어주기

4. jenkins plugin설치
- aws credential
- pipeline: aws

5. Jenkins-Server:  aws cli  설치
- docker exec -it jenkins_prod /bin/bash
- curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install

6. jenkins credential
- AWS credential선택
- 2번에 있던 액세스, 비밀액세스키 입력

### Docker Jenkins컨테이너를 통해 Fintech-Service 컨테이너 빌드 후 ECR에 업로드
```shell
pipeline {
    agent any
    environment {
       ECR_REPO = "724772081580.dkr.ecr.ap-northeast-2.amazonaws.com" // Amazon Elastic Container Registry URI
       AWS_CREDENTIALS="aws-login"
       ECR_NAME = "fintech-service" // ecr 이름
       REGION = "ap-northeast-2" // region 이름
       IAM_ROLE_NAME = "arn:aws:iam::724772081580:role/ecr-registry-full-access"
       ROLE_ACCOUNT = "developer"
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/kps990515/fintech-service.git'
            }
        }
        
        stage('Grant execute permission for gradlew') {
            steps {
                sh 'chmod +x ./gradlew'
            }
        }
        
        stage('Build with Gradle') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('ECR Upload') {
            steps{
                script{
                    try {                       
                        withAWS(region: "${REGION}",credentials: "${AWS_CREDENTIALS}", role: "${IAM_ROLE_NAME}", roleAccount: "${ROLE_ACCOUNT}", externalId: 'externalId') {
                            sh 'aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin  ${ECR_REPO}' // AWS ECR 로그인
                            sh 'docker build -t ${ECR_NAME} .' // fintech-service Docker 이미지 빌드
                            sh 'docker tag ${ECR_NAME}:latest ${ECR_REPO}/${ECR_NAME}:v$BUILD_NUMBER' // fintech-service DockerImage 태그추가
                            sh 'docker push ${ECR_REPO}/${ECR_NAME}:v$BUILD_NUMBER' // ECR에 해당 DockerImage 푸시
                            sh 'docker rmi ${ECR_REPO}/${ECR_NAME}:v$BUILD_NUMBER'
                
                        }
                    }
                    catch(error){
                        print(error)
                        currentBuild.result = 'FAILURE'
                    } 
                }
            }
            post {
                success {
                    echo "The ECR Upload stage successfully."
                }
                failure {
                    echo "The ECR Upload stage failed."
                }
            }
        }
    }
    post { 
        success { 
            slackSend(tokenCredentialId: 'slack-token'
                , channel: '#cicd'
                , color: 'good'
                , message: "빌드성공")
        }
        failure { 
            slackSend(tokenCredentialId: 'slack-token'
                , channel: '#cicd'
                , color: 'danger'
                , message: "빌드실패")
        }
    }
}
```