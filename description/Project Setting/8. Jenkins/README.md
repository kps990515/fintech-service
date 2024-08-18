## Jenkins
- docker jenkins 실행 명령어 : sudo docker start jenkins_prod

1. docker plugin 설치 : docker pipleline

### [Item 생성]

1. ### Git Pull 후 Jar 생성 Script
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


