# Retry 어노테이션 구현
- Retry 로직 구현
    - AOP 활용
    - Expotional backoff 전략 구현
- Retry-AOP 테스트 코드
---
## 1. 개요
어플리케이션 내부에서 특정 서비스를 여러번 재시도가 필요한 경우는 뭐가 있을까?
우선 웹크롤링을 예로 들어보자. 대부분의 웹사이트의 경우, 동일한 ip에서 빠른시간내에
요청을 여러번 계속 보내면 TooManyRequst등의 오류로 응답이 올것이다. 일반적인
사용자까지 막으면 안되므로 크롤링 서비스를 제공하는 입장에선 어느정도의 간격을 두고
요청을 하도록 구현을 해놨을 것이다.\
다른 예로는 rest-api 서비스를 제공하는 업체들의 기본 가이드를 보면 월 제공량,
일일 제공량(RPM) 외로 시간당이나 분당(RPM) 최대 요청량을 제한(Quota)해놓는
경우가 있다. 이런 api 서비스를 이용하여 어플리케이션을 구축할때에는 사용자규모나
상황에 따라 api 요청 횟수를 제한해놔야 할 것이다.

## 2. 구현
구현 방법은 여러가지가 있을 수 있겠다. 특정 서비스를 thread 내부에 구현을 하고
sleep을 이용해서 요청을 할 수도 있고, 서비스 내부에 반복문으로 간격을 특정시간
을 주고도 구현할 수 있을 것이다. 허나, 규모가 커지고 여러 api들을 사용한다고
했을 때 비슷한 역할을 하는 코드들을 반복적으로 작성하는 것은 좋은 프로그래머가
되려면 지양해야 할 점인 것 같다. 스프링에서는 불필요한 코드를 줄여주는 어노테이션
기능을 많이 제공해주고 있고, 오픈소스 라이브러리에서도 어노테이션으로 제공해주는
경우(lombok, junit 등등)가 많다. 이에 커스텀 어노테이션으로 특정 서비스를
재시도 할 수 있게 구현해보려고 한다.

## 3. 필요 사전지식
- Annotation\
기존에 lombok이나 스프링 관련 어노테이션(controller, service 등등)을
현업에서 사용하고 있지만, 동작원리나 구현방식에 있어서는 전혀 지식이 없어, 관련
자료를 한번 찾아보고 진행하려고 한다.\
[Annotation 동작 원리와 사용법](https://hirlawldo.tistory.com/43)\
[Java annotations 이란? 동작 원리 설명 + 활용 (커스텀 어노테이션)](https://velog.io/@anak_2/Java-annotations-%EC%9D%B4%EB%9E%80-%EC%84%A4%EB%AA%85-%ED%99%9C%EC%9A%A9)\
[시의적절한 커스텀 어노테이션](https://techblog.woowahan.com/2684/)
- AOP\
AOP의 경우도 직접 구현해보거나 심도있게 다뤄보지 않아서 간단하게 개념정도 찾아보고
진행하려고 한다. AOP 관련해서는 따로 메인주제로 다뤄볼 예정이다.\
[Spring AOP 로거 개발 가이드](https://hirlawldo.tistory.com/31)\
[AOP 총정리](https://velog.io/@backtony/Spring-AOP-%EC%B4%9D%EC%A0%95%EB%A6%AC)

## 4. 구현
- 예시

```java
@Service
public class FooService {

  @Retry(
          attempts = 3,
          delay = 1000,
          backoff = 2, // expotional backoff
          exceptions = {IllegalArgumentException.class}
  )
  public void foo() {
    System.out.println("foo");
  }
}
```

우선, 특정한 서비스를 다시 요청하기 위해 고려해야할점은 뭐가 있을까? 
1. 시도회수
2. 딜레이(시도 텀)
3. Exponential backoff
>Exponential backoff는 네트워크 통신에서 발생할 수 있는 충돌이나 혼잡을 관리하기 위한 전략 중 하나입니다. 이 기법은 데이터 전송 시간 간격을 조절하여 네트워크 혼잡을 완화하고 효율적인 통신을 도와주는데 사용됩니다.
>간단한 원리는 다음과 같습니다:\
>초기 전송: 먼저 데이터를 전송하려고 시도합니다. 이때 데이터 전송에 성공하면 문제가 없지만, 다른 기기와의 충돌이나 혼잡으로 실패할 수 있습니다.\
>지수 백오프: 데이터 전송이 실패하면 재시도를 하기 전에 일정 시간 동안 기다립니다. 이때, 지수적 백오프를 사용하여 대기 시간을 늘려나갑니다. 즉, 첫 번째 실패 후에는 짧은 시간 동안 대기하고, 두 번째 실패 후에는 더 긴 시간 동안 대기하고, 그 후에는 더 긴 시간 동안 대기하며 이를 반복합니다.\
>재시도: 대기 시간이 끝나면 데이터 전송을 다시 시도합니다. 이때, 대기 시간이 늘어나면 충돌 가능성이 감소하고 효율적인 통신이 가능해집니다.\
>이러한 접근 방식을 통해 네트워크 혼잡을 관리하고 충돌을 최소화하며 데이터 전송의 성공 확률을 높일 수 있습니다. Exponential backoff는 주로 Ethernet과 같은 로컬 네트워크 환경 또는 TCP/IP와 같은 인터넷 프로토콜에서 사용됩니다.
4. 특정 상황에서의 제어를 위한 익셉션

- Retry 어노테이션 선언
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
  int attempts();
  long delay();
  int backoff();
  Class<? extends Throwable>[] value() default {};
}
```

- AOP 구현
```java
@Slf4j
@Component
@Aspect
public class RetryAspect {
    @Around(value = "@annotation(kr.co.jckang.retrypractice.annotation.Retry) && execution(* *(..))")
    public Object afterThrowingAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Retry retryAnnotation = method.getAnnotation(Retry.class);
        int attempts = retryAnnotation.attempts();
        long delay = retryAnnotation.delay();
        int backoff = retryAnnotation.backoff();
        Class<? extends Throwable>[] retryFor = retryAnnotation.value();

        for(int retryCount = 0; retryCount < attempts; retryCount++)
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (containsExceptionClass(e, retryFor)) {
                    log.info("Retry # {} : {}", (retryCount + 1), e.getMessage());
                    try {
                        Thread.sleep(delay * getMultiples(retryCount, backoff));
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        log.info("Max retries exceeded.");
        return null;
    }

    private static int getMultiples(int retryCount, int backoff) {
        if(retryCount == 0) {
            return 1;
        }
        return retryCount * backoff;
    }


    private static boolean containsExceptionClass(Throwable ex, Class<? extends Throwable>[] retryFor) {
        for (Class<? extends Throwable> retryExceptionClass : retryFor) {
            if (retryExceptionClass.isInstance(ex)) {
                log.info("Exception matches with retryFor: " + retryExceptionClass);
                return true;
            }
        }
        return false;
    }
}
```

여러번 시도끝에 일단 구상한 코드는 작성했으나, 이 코드가 정상적으로 작동하는지
테스트 코드를 짜는데 어려움이 있다. 우선 aop관련 테스트 코드작성을 검색해봤는데
내용을 잘 이해를 못하겠다. 모킹과 프록시에 대한 개념은 둘째치고 원시적으로도
테스트 코드 작성이 어렵다. 어떤것부터 연습해봐야 하는걸까...?\
[[토비의 스프링] 6-1. AOP - 단위 테스트와 프록시](https://wwlee94.github.io/category/study/toby-spring/aop/unit-test-and-proxy/#3-%EB%8B%A4%EC%9D%B4%EB%82%B4%EB%AF%B9-%ED%94%84%EB%A1%9D%EC%8B%9C%EC%99%80-%ED%8C%A9%ED%86%A0%EB%A6%AC-%EB%B9%88)




