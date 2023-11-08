package kr.co.jckang.retrypractice.aspect;

import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class RetryAspect {
    @Around(value = "@annotation(kr.co.jckang.retrypractice.annotation.Retry) && execution(* *(..))")
    public Object afterThrowingAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        return tryInvoke(joinPoint, retryAnnotation(joinPoint), 0);
    }

    private Object tryInvoke(
            ProceedingJoinPoint joinPoint,
            Retry retry,
            int retryCount
    ) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            if (retryCount >= retry.attempts() - 1) {
                throw new MaximumAttemptsExceededException("Max retries exceeded");
            }
            assessmentPolicy(e, retry, retryCount);
            return tryInvoke(joinPoint, retry, retryCount + 1);
        }
    }

    private void assessmentPolicy(Throwable e, Retry retry, int retryCount) throws Throwable {
        if (!containsExceptionClass(e, retry.value())) {
            throw e;
        }

        log.info("Retry # {} : {}", (retryCount + 1), e.getMessage());
        try {
            Thread.sleep(calculateMillis(retry, retryCount));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private long calculateMillis(Retry retry, int retryCount) {
        return retry.delay() *
                getMultiples(
                        retryCount,
                        retry.backoff()
                );
    }

    private Retry retryAnnotation(ProceedingJoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(Retry.class);
    }

    private int getMultiples(int retryCount, int backoff) {
        if(retryCount == 0) {
            return 1;
        }
        return retryCount * backoff;
    }


    private boolean containsExceptionClass(
            Throwable ex,
            Class<? extends Throwable>[] retryFor
    ) {
        for (Class<? extends Throwable> retryExceptionClass : retryFor) {
            if (retryExceptionClass.isInstance(ex)) {
                log.info("Exception matches with retryFor: " + retryExceptionClass);
                return true;
            }
        }
        return false;
    }
}
