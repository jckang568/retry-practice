package kr.co.jckang.retrypractice.aspect;

import jakarta.validation.constraints.Max;
import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
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
        throw new MaximumAttemptsExceededException("Max retries exceeded");
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
