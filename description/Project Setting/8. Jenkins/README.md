## Jenkins

# Pipeline : pipeline script
# Jenkins 설치시 추천 플러그인 설치하면 git 관련된 플러그인이 포함되어 있음
# Checkout 스테이지에서 git source 다운로드
# Build 스테이지에서 sh 플러그인을 통해 mvnw  실행
# Build 작업 완료후 생성된 jar 파일을 아카이브아키펙트로 전달 
pipeline {
    agent any
        stages {
            
            stage('Checkout') {
                steps {
                    git branch: 'master', url:'https://github.com/kps990515/fintech-service.git'
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
            
            stage('Build') {
                steps {
                    sh "./gradlew clean build"
                }
            
            stage('Deploy to EC2') {
                steps {
                    sh '''
                        ssh -i /path/to/your/private-key.pem ec2-user@http://13.209.230.183
                        cd /home/ec2-user/fintech-service/api/build/libs/
                        sudo pkill -f 'java -jar' || true
                        nohup sudo java -jar api-1.0-SNAPSHOT.jar > app.log 2>&1 &
                        EOF
                    '''
                }
            }
        }
    }
}

# 빌드 진행 :  [지금 빌드]
# 콘솔 로그 보기
# POST : 아카이브아티팩트 링크 확인
