## AWS
1. IAM설정
  - AdministratorAccess
  - AmazonS3FullAccess
  - AWSCodeDeployFullAccess

2. VPC설정

3. EC2설정
  - t2.large 선택
  - 보안그룹
    - SSH 포트 22
    - HTTPS포트 443
    - HTTP 포트 8080, 80
  - 탄력적 IP세팅을 고정 IP 확보

4. AWS 기본 세팅 
   1. 자바설치
      - sudo yum install -y java-17-amazon-corretto-devel
   2. mysql 설치
        - yum install -y mysql
        - mysql -h proejct-rds-mysql8.cjug24qm0gxh.ap-northeast-2.rds.amazonaws.com -u developer -p
        - show databses;
        - create database sideproject;

   3. git clone

   4. 프로젝트 빌드
      - cd /home/ec2-user/fintech-service
      - chmod +x ./gradlew
      - ./gradlew clean build
      - 그럼 api모듈 lib/libs안에 jar파일 생성

5. 도커 
   1. 도커설치
      - sudo i
      - yum install docker -y
      - systemctl enable --now docker
      - docker version

   2. 도커 관리자
      - usermod -aG docker ec2-user
      - id ec2-user
      - exit

   3. Docker command 자동완성 구성
      - sudo curl https://raw.githubusercontent.com/docker/docker-ce/master/components/cli/contrib/completion/bash/docker \
        -o /etc/bash_completion.d/docker.sh
      - exit

   4. XShell로 다시 로그인 :  ec2-user
     - docker version

6. Jenkins 
   1. 설치
      - docker run -d --name jenkins_prod \
        -p 9090:9090 -p 50000:50000 \
        -v /var/run/docker.sock:/var/run/docker.sock \
        --user root   jenkins/jenkins:latest

      - docker exec -it --user root jenkins_prod /bin/bash (비밀번호확인)

      - ip:9090 입력하고 비밀번호 입력해서 install
      - http://13.209.230.183:9090/

        apt-get update && \
        apt-get -y install apt-transport-https \
          ca-certificates \
          curl \
          gnupg2 \
          jq \
          software-properties-common && \
        curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
        add-apt-repository \
          "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
          $(lsb_release -cs) \
          stable" && \
       apt-get update && \
       apt-get -y install docker-ce

   2. jenkins_prod 컨테이너에서 jenkins-server에서 동작중인 데몬을 호출해서 사용가능한지 확인
      - docker ps
