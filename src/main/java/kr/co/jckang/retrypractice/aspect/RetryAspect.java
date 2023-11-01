package kr.co.jckang.retrypractice.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.jckang.retrypractice.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Aspect
public class RetryAspect {
    private int callCount;
    @AfterThrowing(value = "@annotation(kr.co.jckang.retrypractice.annotation.Retry)"
            , throwing = "ex")
    public void afterThrowingAdvice(JoinPoint joinPoint
            , Exception ex) {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        Retry retryAnnotation = method.getAnnotation(Retry.class);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = request.getSession();


        if (retryAnnotation != null) {
            int attempts = retryAnnotation.attempts();
            long delay = retryAnnotation.delay();
            int backoff = retryAnnotation.backoff();
            Class<? extends Throwable>[] retryFor = retryAnnotation.value();

            containsExceptionClass(ex, retryFor);

            for (int i = 0; i < attempts; i++) {
                joinPoint.getSignature().
            }

//            System.out.println("Retry Attempts: " + attempts);
//            System.out.println("Retry Delay: " + delay);
//            System.out.println("Retry Backoff: " + backoff);
//            System.out.println("Retry For Exceptions: " + Arrays.toString(retryFor));


        }
        System.out.println("After Throwing exception in method:" + joinPoint.getSignature());
        System.out.println("Exception is:" + ex.getMessage());
    }

    private static boolean containsExceptionClass(Exception ex, Class<? extends Throwable>[] retryFor) {
        for (Class<? extends Throwable> retryExceptionClass : retryFor) {
            if (ex.getClass().hashCode() == retryExceptionClass.hashCode()) {
                System.out.println("Exception matches with retryFor: " + retryExceptionClass);
                return true;
            }
        }
        return false;
    }
}
