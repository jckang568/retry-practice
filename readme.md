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
경우(`lombok`, `junit` 등등)가 많다. 이에 커스텀 어노테이션으로 특정 서비스를
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

### 어노테이션의 기능 나열화
  - 시도회수
  - 딜레이 시간
  - 백오프값
  - 익셉션 배열\
각 기능들에 대해서 단위테스트를 진행해보고 싶다.\
그럼 given-when-then에 따라 테스트 코드를 작성했을 때, given을
어떤 객체에 주고 어떤 메서드를 호출하고 assert는 어떻게 작성해야하는지 감이 잡히질 않는다.

### 심화과제
  - 테스트 코드 보완
    1. 실행횟수 검증
    2. 재시도 간 지연된 시간 검증
  - RetryAspect 고도화
    1. Jitter 개념 적용\
[[기타] Retry 전략에 대해서(Exponential Backoff, Jitter)](https://jungseob86.tistory.com/12)\
[좀 더 우아한 Retry (Expenential Backoff with Jitter)](https://velog.io/@jazz_avenue/%EC%A2%80-%EB%8D%94-%EC%9A%B0%EC%95%84%ED%95%9C-Retry-Expenential-Backoff-with-Jitter)


### Jitter 전략
우선 **jitter**의 의미를 먼저 찾아봤다. 지연변이라고도 불리우는 jitter는 사실
전략이라기 보다 오류에 가까운 용어이다. [What is jitter](https://www.techtarget.com/searchunifiedcommunications/definition/jitter)
의 내용을 온전히 이해할 수는 없었지만 **대기시간의 변화** 에 초점을 맞춰보자.

> IP(인터넷 프로토콜) 네트워크의 지터 는 일부 패킷이 한 시스템에서
> 다른 시스템으로 이동하는 데 더 오랜 시간이 걸릴 때 두 시스템 간의
> 패킷 흐름 에 대한 대기시간의 변화입니다. 지터는 네트워크 정체, 
> 타이밍 드리프트 및 경로 변경으로 인해 발생합니다.

한가지 예를 PvP FPS게임으로 예를 들어보겠다. 정확히 통신방식이나 그런 정책에
대해서는 정확히 알지 못하지만, 총을 쏘거나, 움직이거나 하는것들을 구현을 하려면
실시간으로 서버와 통신을 해야할 것이다. 패킷을 어떤 간격으로 전송(서버와 통신)
할지는 게임마다 다르겠지만 공정성을 위해 규칙적으로 보내고 있을것이다. 하지만,
실제 운영시에는 사용자의 인터넷환경의 문제가 생길수도 있고, 서버에 부하가 생겨
문제가 생길수도 있는등의 여러가지 내외부적인 요인이 발생할 것이다. 이때 주기적
으로 전송하려했던 패킷이 끊기거나 지연이 되면 흔히 얘기하는 랙(?)걸린다 라는
상황이 발생하는 것이다.(갑자기 순간이동하거나, 움직임이 끊겨보인다거나 등등)

간단하게 정리해보자면 동일한 대기시간(위에서는 같은간격)으로 어떤 데이터를
보내길 원했으나, 그 대기시간이 불규칙적으로 변화했다는 현상을 **jitter**라고
봐도 무방할 것이다.(정확히는 대기시간의 편차 또는 변위)

자 그럼, jitter 전략이라는 말은 왜 생겨났을까? 일부러 오류를 발생시켜야하나?
대기시간을 불규칙적으로 변화하는 전략은 어떤경우에 사용해야할까? 위에서 진행해봤던
Retry 전략은 그다지 크게 유용하지 않을 상황이 발생할 수 있다. 위에서 진행했던
Backoff 전략은 재시도 횟수가 증가할수록 Backoff 시간이 증가하므로 네크워크에
갑작스럽게 트래픽을 부담시키는 것을 피할 수 있다. 하지만 이 방법도 한계가 있다는
것을 금방 알 수 있다. 어차피 동시에 요청이 몰린다면 똑같은 시간 간격으로 모든 재시도가
동일하게 몰릴 것이기 때문이다. 따라서 조금 더 똑똑한, 교묘한 방법이 필요하다.
이런 상황에서 등장하는 개념이 jitter인 것이다.
jitter 전략을 retry에 이용하면
동일한 재시도 시간간격에 불규칙성을 추가하여 동시에 들어온 요청들을 분산시켜 시스템의
부하를 미연에 방지하여 시스템 정체를 줄여줄 것이다.

전략이 없는 재시도는 문제를 개선하는것이 아니라 더욱 악화시킬 수 있기에, 전략적으로
접근해야한다는 것을 알았다. 효과적인 백오프와 지터 전략은 하단 [레퍼런스](#references)의
Exponential Backoff And Jitter 링크를 참고하자.

---
## References

[Exponential Backoff And Jitter](https://aws.amazon.com/ko/blogs/architecture/exponential-backoff-and-jitter/)\
[시간 제한, 재시도 및 지터를 사용한 백오프](https://aws.amazon.com/ko/builders-library/timeouts-retries-and-backoff-with-jitter/)
