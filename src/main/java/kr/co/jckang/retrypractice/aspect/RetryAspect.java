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
        Retry retryAnnotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(Retry.class);
        return retryInvoke(joinPoint, retryAnnotation);
    }

    private Object retryInvoke(ProceedingJoinPoint joinPoint
            , Retry retryAnnotation) throws Throwable {
        return retryInvoke(joinPoint, retryAnnotation, 0);
    }

    private Object retryInvoke(ProceedingJoinPoint joinPoint
            , Retry retryAnnotation
            , int count) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            if (!shouldRetry(e, retryAnnotation, count)) {
                throw e;
            }
            Thread.sleep(calculateRetryBackoff(retryAnnotation, count));
            return retryInvoke(joinPoint, retryAnnotation, count + 1);
        }
    }


    private static long calculateRetryBackoff(Retry retryAnnotation
            , int count) {
        return count == 0
                ? retryAnnotation.delay()
                : retryAnnotation.delay() * (int) Math.pow(retryAnnotation.backoff(), count);
    }

    private static boolean shouldRetry(Throwable ex
            , Retry retryFor
            , int count) {
        if (retryFor.attempts() == count + 1) {
            throw new MaximumAttemptsExceededException("Max retries exceed");
        }
        return containsExceptionClass(ex, retryFor);
    }

    private static boolean containsExceptionClass(Throwable ex
            , Retry retryFor) {
        for (Class<? extends Throwable> retryExceptionClass : retryFor.value()) {
            if (retryExceptionClass.isInstance(ex)) {
                log.info("Exception matches with retryFor: " + retryExceptionClass);
                return true;
            }
        }
        return false;
    }
}
