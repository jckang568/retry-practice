package kr.co.jckang.retrypractice.aspect;

import ch.qos.logback.core.testUtil.MockInitialContext;
import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
import kr.co.jckang.retrypractice.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.Retryable;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(MyStopwatch.class)
class RetryAopHandlerTest {


    public static class Foo {
        @Retry(attempts = 4
                , delay = 1000
                , backoff = 2
                , value = IllegalArgumentException.class
        )
        public void execute() {
            log.info("void execute executed");
            throw new IllegalArgumentException("잘못된 입력값");
        }


        @Retry(attempts = 3
                , delay = 1000
                , backoff = 2
                , value = IllegalArgumentException.class
        )
        public String execute(int attempt) {
            if (attempt == 2)
                return "SUCCESS";
            throw new IllegalArgumentException();
        }

    }

    @Test
    void mockitoTest() throws RuntimeException, NoSuchMethodException {
        Foo mockFoo = Mockito.spy(Foo.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(mockFoo);
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();
        Retry retry = mockFoo.getClass().getMethod("execute").getAnnotation(Retry.class);
        int expectedAttempts = retry.attempts();
        try {
            foo.execute();
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        // Exception exception = assertThrows(Exception.class, foo::execute);
        // assertTrue(exception instanceof MaximumAttemptsExceededException);
        Mockito.verify(mockFoo, times(expectedAttempts)).execute();
    }


    @Test
    @DisplayName("재시도 모두 실패 시 MaximumAttemptsExceededException 발생")
    void retry_WhenAllFail_ThenThrowFinalException() {
        // given
        AspectJProxyFactory factory = new AspectJProxyFactory(new Foo());
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();
        String message = "Max retries exceed";

        // when
        assertThatThrownBy(foo::execute)
                // then
                .isInstanceOf(MaximumAttemptsExceededException.class)
                .hasMessage(message);

    }

    @Test
    @DisplayName("성공시 리턴 값")
    void retry_WhenSuccess_ThenReturnValue() {
        // given
        AspectJProxyFactory factory = new AspectJProxyFactory(new Foo());
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();

        // when
        String result = foo.execute(2);

        // then
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    void testRetryWithDelay() throws NoSuchMethodException {
        Foo mockFoo = Mockito.spy(Foo.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(mockFoo);
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();
        Retry retry = mockFoo.getClass().getMethod("execute").getAnnotation(Retry.class);
        int expectedAttempts = retry.attempts();
        long expectedDelay = retry.delay();

        // Measure the execution time
        long startTime = System.currentTimeMillis();
        try {
            foo.execute();
        } catch (MaximumAttemptsExceededException e) {
            // Handle the MaximumAttemptsExceededException if needed
        }
        long endTime = System.currentTimeMillis();
        /*
        attempts 3
        delay 1000
        backoff 2
        (5000ms)   실행 -> 1000ms -> 실행 -> 2000ms -> 실행 -> 4000ms -> 실행 7024ms
                           1020      2030          4032

         */
        long executionTime = endTime - startTime;

        // Verify that the method was retried the expected number of times
        Mockito.verify(mockFoo, times(expectedAttempts)).execute();
        log.info("executionTime: {}ms", executionTime);
        log.info("expectedDelay: {}ms", expectedDelay);
        // Check if the execution time is greater than or equal to the expected delay
        assertTrue(executionTime >= expectedDelay);
    }


    @Test
    void testRetryWithRetryAnnotation() throws NoSuchMethodException {
        Foo mockFoo = Mockito.spy(Foo.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(mockFoo);
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();

        Retry retry = mockFoo.getClass().getMethod("execute").getAnnotation(Retry.class);
        int expectedAttempts = retry.attempts();
        long expectedDelay = retry.delay();

        // Execute the method
        try {
            foo.execute();
        } catch (MaximumAttemptsExceededException e) {
            // Handle the MaximumAttemptsExceededException if needed
        }

        // Verify that the method was retried the expected number of times
        Mockito.verify(mockFoo, times(expectedAttempts)).execute();

        // Verify that the total execution time is greater than or equal to the expected delay
        //long actualExecutionTime = TimeAcceleration.getElapsedMillis();
        //assertTrue(actualExecutionTime >= expectedDelay);
    }

}