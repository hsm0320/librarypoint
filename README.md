# 도서 대여 서비스
 
# Table of contents
 
 - [도서 대여 서비스](#---)
   - [서비스 시나리오](#서비스-시나리오)  
   - [분석/설계](#분석설계)
   - [구현:](#구현-)
     - [DDD 의 적용](#ddd-의-적용)
     - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
     - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
   - [운영](#운영)
     - [CI/CD 설정](#cicd설정)
     - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
     - [오토스케일 아웃](#오토스케일-아웃)
     - [무정지 재배포](#무정지-재배포)
    
# 서비스 시나리오
 
 기능적 요구사항
 1. 사용자가 도서를 리뷰등록 한다.
 1. 리뷰 등록 시 포인트가 지급되어야 한다.
 1. 포인트가 지급되면 카카오톡 알림메시지를 발송한다.
 
 비기능적 요구사항
 1. 트랜잭션
     1. 포인트시스템이 오류인 경우 리뷰를 할수 없다. (Sync 호출)
 1. 장애격리
     1. 카카오톡 메시지 발송이 수행되지 않더라도 리뷰등록은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
     1. 포인트지급이 과중되면 잠시동안 리뷰등록 받지 않고 잠시후에 하도록 유도한다  Circuit breaker, fallback
 1. 성능
     1. 사용자는 알림에서 본인 포인트 지급내역을 확인할 수 있다 CQRS
 
 
 # 분석/설계
 
## Event Storming 결과
 * MSAEz 로 모델링한 이벤트스토밍 결과: 
![image](https://user-images.githubusercontent.com/75401893/105200796-8e3ae580-5b83-11eb-851a-be255eb89d1d.png)



## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/53402465/104991783-e5956480-5a62-11eb-91e6-69020468ab61.PNG)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd book
mvn spring-boot:run

cd mypage
mvn spring-boot:run 

cd payment
mvn spring-boot:run  

cd rental
mvn spring-boot:run

cd point
mvn spring-boot:run  

cd notice
mvn spring-boot:run


```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 book 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 

```
package library;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Book_table")
public class Book {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookId;
    private String bookStatus;
    private Long memberId;
    private Long rendtalId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public Long getRendtalId() {
        return rendtalId;
    }

    public void setRendtalId(Long rendtalId) {
        this.rendtalId = rendtalId;
    }
}


```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package library;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookRepository extends PagingAndSortingRepository<Book, Long>{
}
```

- 적용 후 REST API 의 테스트 시나리오

1. SAGA
2. CQRS
3. Correlation

```
# 사용자가 리뷰를 등록한다.
http POST http://52.141.63.24:8080/books memberId=3 bookId=3 bookReview="행복"
```

![image](https://user-images.githubusercontent.com/75401893/105201694-7fa0fe00-5b84-11eb-9303-55221242eaf4.png)


```
# 포인트 등록내역 확인
http GET http://52.141.63.24:8080/points/3
```

![image](https://user-images.githubusercontent.com/75401893/105201859-b0813300-5b84-11eb-8370-a4639c35579c.png)


```
# 메시지 발송내역 확인
http GET http://52.141.63.24:8080/notices/3
```

![image](https://user-images.githubusercontent.com/75401893/105201973-d1498880-5b84-11eb-8667-99d5d0773b7b.png)



4. Request / Response

```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 책(book)->포인트(point) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 


```

# (book) PointService.java 내용중

@FeignClient(name="point", url="${api.point.url}")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points")
    public void registership(@RequestBody Point point);

}


```

- 리뷰등록 이후(@PostPersist) 포인트등록 요청하도록 처리
```
# Book.java

    @PostPersist
    public void onPostPersist(){
        ...
        System.out.println("##### 리뷰입니다.");
            Reviewed reviewed = new Reviewed();
            BeanUtils.copyProperties(this, reviewed);
            reviewed.publishAfterCommit();

            //Following code causes dependency to external APIs
            // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
            librarypoint.external.Point point = new librarypoint.external.Point();
            // mappings goes here
            point.setMemberId(this.memberId);
            point.setBookId(this.id);
            point.setBookPoint((long)100);

            BookApplication.applicationContext.getBean(librarypoint.external.PointService.class)
                    .registership(point);
         ...
    }
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 포인트시스템이 장애가 나면 리뷰등록도 못받는다는 것을 확인:


# 포인트(point) 서비스를 잠시 내려놓음

# 리뷰등록 처리
http POST http://52.141.63.24:8080/books memberId=4 bookId=4 bookReview="재미있음"  #Fail 
```

![image](https://user-images.githubusercontent.com/75401893/105203142-2043ed80-5b86-11eb-8710-4b7c8390a73a.png)


```
#포인트 서비스 재기동
cd point
mvn spring-boot:run

#리뷰등록 처리
http POST http://52.141.63.24:8080/books memberId=4 bookId=4 bookReview="재미있음"   #Success
```
![image](https://user-images.githubusercontent.com/75401893/105203471-7add4980-5b86-11eb-8704-649395179b04.png)


- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

포인트 등록 이후 알림메시지 발송은 비 동기식으로 처리하여 알림메시지 시스템 처리로 인해 리뷰등록이 블로킹 되지 않도록 처리한다.
- 이를 위하여 포인트이력에 기록을 남긴 후에 곧바로 등록(registered)이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
# Point.java

@Entity
@Table(name="Point_table")
public class Point {

 ...
    @PostPersist
    public void onPostPersist(){
        System.out.println("##### 포인트 등록시작");
        Registered registered = new Registered();
        BeanUtils.copyProperties(this, registered);
        registered.publishAfterCommit();
        System.out.println("##### 포인트 등록끝");
    }
 ...
}
```
- 알림서비스는 등록완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다

```
# PolicyHandler.java (notice)
...

@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRegistered_(@Payload Registered registered){

        if(registered.isMe()){
            System.out.println("##### 메시지 발송  : ");

            Notice notice = new Notice();

            notice.setId(registered.getId());
            notice.setMemberId(registered.getMemberId());
            notice.setBookId(registered.getBookId());
            notice.setBookPoint(registered.getBookPoint());

            noticeRepository.save(notice);
        }
    }
}

```


알림 시스템은 리뷰등록,포인트와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 도서관리시스템이 유지보수로 인해 잠시 내려간 상태라도 리뷰등록을 받는데 문제가 없다

```

# 알림서비스(notice) 를 잠시 내려놓음

# 리뷰등록 처리
http POST http://52.141.63.24:8080/books memberId=5 bookId=5 bookReview="감동적임"  #Success  

등록은 되나 알림은 조회불가

```
# 알림 상태 확인

:
![image](https://user-images.githubusercontent.com/75401893/105206084-4a4adf00-5b89-11eb-9df7-698d333e415b.png)


``` 

#알림 서비스 기동
cd notice
mvn spring-boot:run

#알림상태 확인
http GET  http://52.141.63.24:8080/notices     # 모든 주문의 상태가 "reserved"으로 확인
```
:
![image](https://user-images.githubusercontent.com/75401893/105206709-00aec400-5b8a-11eb-9b41-9341115c623a.png)



# 운영

## CI/CD 설정


각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.


6. Deploy / Pipeline

![image](https://user-images.githubusercontent.com/75401893/105215138-3ce72200-5b94-11eb-9056-39617f597cd6.png)





## 동기식 호출 / 서킷 브레이킹 / 장애격리

7. Circuit Breaker

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 리뷰등록(book)-->포인트(point) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스(point) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# Point.java 

    @PostPersist
    public void onPostPersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ...
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시

```
$ siege -c100 -t60S -v --content-type "application/json" 'http://52.141.63.24:8080/books POST {"memberId": 13, "bookId": 3, "bookReview": "감동"}'

```
 

![image](https://user-images.githubusercontent.com/75401893/105214766-c34f3400-5b93-11eb-8273-fb85e6267906.png)


- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 29.9%정도 정상적으로 처리되었음.

istio 로 처리

![image](https://user-images.githubusercontent.com/75401893/105220684-69eb0300-5b9b-11eb-80a4-baa40b116506.png)

100% 성공 확인

![image](https://user-images.githubusercontent.com/75401893/105221545-994e3f80-5b9c-11eb-86f2-613800f90a05.png)




8. Autoscale (HPA)
### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 포인트 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy payment --min=1 --max=10 --cpu-percent=15
```
- CB 에서 했던 방식대로 워크로드를 2분 동안 걸어준다.
```
$ siege -c100 -t120S -v --content-type "application/json" 'http://52.141.63.24:8080/books POST {"memberId": 13, "bookId": 3, "bookReview": "감동"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy pay -w
```
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![image](https://user-images.githubusercontent.com/53402465/105116790-c3f1b700-5b0e-11eb-8a8e-80016c453ebd.PNG)

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![image](https://user-images.githubusercontent.com/53402465/105116542-50e84080-5b0e-11eb-8da0-33f742007e41.jpg)


9. Zero-downtime deploy (readiness probe)
## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
- 새버전으로의 배포 시작

```
kubectl set image ...
```

- readiness 설정

![image](https://user-images.githubusercontent.com/53402465/105119450-b25ede00-5b13-11eb-947b-a2d6da8de334.jpg)

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![image](https://user-images.githubusercontent.com/53402465/105119446-b1c64780-5b13-11eb-9af5-c28364c8870c.jpg)

배포기간중 Availability 가 평소 100%에서 97% 대로 떨어지는 것을 확인. 
원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

- readiness 설정 수정

![image](https://user-images.githubusercontent.com/53402465/105119444-b12db100-5b13-11eb-9143-04f44194eb64.jpg)

```
kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![image](https://user-images.githubusercontent.com/53402465/105119438-af63ed80-5b13-11eb-981e-bb5b1c754cea.jpg)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


10. 폴리그랏

notice 는 다른 서비스와 구별을 위해 별도 hsqldb를 사용, 이를 위해 notice내 pom.xml에 dependency를 h2database에서 hsqldb로 변경 하였다.

#notice의 pom.xml dependency를 수정하여 DB변경

  <!--
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>
  -->

  <dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.4.0</version>
    <scope>runtime</scope>
  </dependency>
  
11. gateway
  
  
  server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: rental
          uri: http://localhost:8081
          predicates:
            - Path=/rentals/** 
        - id: payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://localhost:8083
          predicates:
            - Path= /mypages/**
        - id: book
          uri: http://localhost:8084
          predicates:
            - Path=/books/** 
        - id: point
          uri: http://localhost:8085
          predicates:
            - Path=/points/** 
        - id: notice
          uri: http://localhost:8086
          predicates:
            - Path= 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: rental
          uri: http://rental:8080
          predicates:
            - Path=/rentals/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /mypages/**
        - id: book
          uri: http://book:8080
          predicates:
            - Path=/books/** 
        - id: point
          uri: http://point:8080
          predicates:
            - Path=/points/** 
        - id: notice
          uri: http://notice:8080
          predicates:
            - Path= 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

# Gateway 서비스 실행 상태에서 8088과 8085로 각각 서비스 실행하였을 때 동일하게 point 서비스 실행되었다.

![image](https://user-images.githubusercontent.com/75401893/105271019-21066f00-5bda-11eb-889f-61b70911c119.png)

![image](https://user-images.githubusercontent.com/75401893/105271002-121fbc80-5bda-11eb-8417-b53bbabd2e89.png)


