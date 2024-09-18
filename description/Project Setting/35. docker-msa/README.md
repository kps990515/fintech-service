## MSA 만들기
- 굳이 project안에 없어도 폴더에 따로 docker-compose.yml파일이 있으면 됨
- 각 서비스들은 아래와 같은 이름으로 docker image생성
    ```shell
    docker build -t delivery-service . 
    ```


- docker-compose 예제
```yml
 version: "3.8"

 services:
   mysql-server:
     image: mysql:latest
     environment:
       - MYSQL_ROOT_PASSWORD=root1234!!
     ports:
       - "3307:3306"
     depends_on:
       - myredis

   myredis:
     image: redis
     hostname: myredis
     ports:
       - "6379:6379"
     depends_on:
       cassandra-node-0:
         condition: service_healthy

   cassandra-node-0:
     image: cassandra
     environment:
       - CASSANDRA_SEEDS=cassandra-node-0 #클러스터 내부 노드
       - CASSANDRA_CLUSTER_NAME=MyCluster #클러스터 이름
       - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch #각 카산드라에 전파 방법(gosship)
       - CASSANDRA_DC=dc1
     ports:
       - "7000:7000"   # 노드간 클러스터 내부 통신
       - "7001:7001"   # 노드간 보안 통신에 사용
       - "9042:9042"   # CQL 클라이언트와 통신
     healthcheck:
       test: ["CMD-SHELL", "[ $$(nodetool statusgossip) = running ]"]
       interval: 30s
       timeout: 10s
       retries: 5

   cassandra-node-1:
     image: cassandra
     environment:
       - CASSANDRA_SEEDS=cassandra-node-0 #클러스터 내부 노드
       - CASSANDRA_CLUSTER_NAME=MyCluster #클러스터 이름
       - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch #각 카산드라에 전파 방법(gosship)
       - CASSANDRA_DC=dc1
     ports:
       - "17000:7000"   # 노드간 클러스터 내부 통신
       - "17001:7001"   # 노드간 보안 통신에 사용
       - "19042:9042"   # CQL 클라이언트와 통신
     healthcheck:
       test: [ "CMD-SHELL", "[ $$(nodetool statusgossip) = running ]" ]
       interval: 30s
       timeout: 10s
       retries: 5

   cassandra-node-2:
     image: cassandra
     environment:
       - CASSANDRA_SEEDS=cassandra-node-0 #클러스터 내부 노드
       - CASSANDRA_CLUSTER_NAME=MyCluster #클러스터 이름
       - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch #각 카산드라에 전파 방법(gosship)
       - CASSANDRA_DC=dc1
     ports:
       - "27000:7000"   # 노드간 클러스터 내부 통신
       - "27001:7001"   # 노드간 보안 통신에 사용
       - "29042:9042"   # CQL 클라이언트와 통신
     healthcheck:
       test: [ "CMD-SHELL", "[ $$(nodetool statusgossip) = running ]" ]
       interval: 30s
       timeout: 10s
       retries: 5

   member-service:
     image: member-service
     ports:
       - "8081:8080"
     depends_on:
       - mysql-server

   payment-service:
     image: payment-service
     ports:
       - "8082:8080"
     depends_on:
       - mysql-server

   delivery-service:
     image: delivery-service
     ports:
       - "8083:8080"
     depends_on:
       - mysql-server

   search-service:
     image: search-service
     ports:
       - "8084:8080"
     depends_on:
       - mysql-server

   catalog-service:
     image: catalog-service
     ports:
       - "8085:8080"
     depends_on:
       - mysql-server

   order-service:
     image: order-service
     ports:
       - "8086:8080"
     depends_on:
       - mysql-server
```