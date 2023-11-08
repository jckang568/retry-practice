//package kr.co.jckang.retrypractice.aspect;
//
//import kr.co.jckang.retrypractice.annotation.Retry;
//import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Component
//@Aspect
//public class BrokenRetryAspect {
//    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//
//    @Around(value = "@annotation(kr.co.jckang.retrypractice.annotation.Retry) && execution(* *(..))")
//    public Object afterThrowingAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
//        Retry retryAnnotation = ((MethodSignature) joinPoint.getSignature())
//                .getMethod()
//                .getAnnotation(Retry.class);
//        return retry(joinPoint, retryAnnotation);
//    }
//
//    private Object retry(ProceedingJoinPoint joinPoint, Retry retryAnnotation) throws Throwable {
//        return retry(joinPoint, retryAnnotation, 0);
//    }
//
//    private Object retry(ProceedingJoinPoint joinPoint, Retry retryAnnotation, int count) throws Throwable {
//        try {
//            return joinPoint.proceed();
//        } catch (Throwable e) {
//            if (shouldRetry(e, retryAnnotation, count)) {
//                throw e;
//            }
//            return scheduleRetry(joinPoint, retryAnnotation, count);
//        }
//        /*throw new MaximumAttemptsExceededException("Max retries exceeded");*/
//    }
//
//    private Object scheduleRetry(ProceedingJoinPoint joinPoint
//            , Retry retryAnnotation
//            , int count) throws RuntimeException {
//        return executorService.schedule(() -> {
//                    try {
//                        return retry(joinPoint, retryAnnotation, count + 1);
//                    } catch (Throwable e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                , calculateRetryBackoff(retryAnnotation, count)
//                , TimeUnit.MILLISECONDS);
//    }
//
//    private static long calculateRetryBackoff(Retry retryAnnotation
//            , int count) {
//        return count == 0
//                ? retryAnnotation.delay()
//                : count * retryAnnotation.delay() * retryAnnotation.backoff();
//    }
//
//    private static boolean shouldRetry(Throwable ex
//            , Retry retryFor
//            , int count) {
//        if (count == retryFor.attempts()) {
//            throw new MaximumAttemptsExceededException("Max retries exceeded");
//        }
//        return containsExceptionClass(ex, retryFor);
//    }
//
//    private static boolean containsExceptionClass(Throwable ex
//            , Retry retryFor) {
//        for (Class<? extends Throwable> retryExceptionClass : retryFor.value()) {
//            if (retryExceptionClass.isInstance(ex)) {
//                log.info("Exception matches with retryFor: " + retryExceptionClass);
//                return true;
//            }
//        }
//        return false;
//    }
//}
