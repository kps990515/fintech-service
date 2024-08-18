## Jenkins
- docker jenkins 실행 명령어 : sudo docker start jenkins_prod

1. docker plugin 설치 : docker pipleline

### [Item 생성]

### 1. Git Pull 후 Jar 생성 Script
```shell
pipeline {
    agent any
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
    }
    post {
        success {
            archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true
        }
        failure {
            echo 'Build failed. Please check the logs.'
        }
    }
}
```

### 2. Jar로 Docker이미지 빌드
- 빌드 완료 후 실행법 : docker run -d --name fintech-service -p 80:8080 fintech-service:v5

```shell
pipeline {
    agent any
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

        stage('Docker Image Build') {
            steps {
                script {
                    docker.build("fintech-service:v${BUILD_ID}")
                }
            }
        }
    }
}
```


