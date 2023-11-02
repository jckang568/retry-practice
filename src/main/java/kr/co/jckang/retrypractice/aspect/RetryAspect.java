package kr.co.jckang.retrypractice.aspect;

import kr.co.jckang.retrypractice.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
@Aspect
public class RetryAspect {
    @Around(value = "@annotation(kr.co.jckang.retrypractice.annotation.Retry) && execution(* *(..))")
    public Object afterThrowingAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        return retryLogic(joinPoint, 3); // 여기에서 최대 재시도 횟수를 지정
    }

    private Object retryLogic(ProceedingJoinPoint joinPoint, int maxRetries) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        Retry retryAnnotation = method.getAnnotation(Retry.class);

        int attempts = retryAnnotation.attempts();
        long delay = retryAnnotation.delay();
        int backoff = retryAnnotation.backoff();
        Class<? extends Throwable>[] retryFor = retryAnnotation.value();

        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                return joinPoint.proceed(); // 성공 시 결과 반환
            } catch (Throwable e) {
                if (retryCount < attempts && containsExceptionClass(e, retryFor)) {
                    // 예외가 retryFor에 지정된 예외 중 하나이며, 재시도 횟수가 남은 경우 다시 시도
                    System.out.println("Retry #" + (retryCount + 1) + ": " + e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // 예외가 retryFor에 지정된 예외가 아니거나, 재시도 횟수가 다 떨어진 경우 예외 던지기
                    throw e;
                }
            }
        }
        // 여기에 최종 실패 처리 로직을 추가할 수 있습니다.
        System.out.println("Max retries exceeded.");
        return null;
    }

    private static boolean containsExceptionClass(Throwable ex, Class<? extends Throwable>[] retryFor) {
        for (Class<? extends Throwable> retryExceptionClass : retryFor) {
            if (retryExceptionClass.isInstance(ex)) {
                System.out.println("Exception matches with retryFor: " + retryExceptionClass);
                return true;
            }
        }
        return false;
    }
}
