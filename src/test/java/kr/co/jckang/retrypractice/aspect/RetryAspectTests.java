package kr.co.jckang.retrypractice.aspect;

import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(MyStopwatch.class)
class RetryAopHandlerTest {


    public static class Foo {
        Map<String, Object> attempts;
        @Retry(attempts = 4
                , delay = 1000
                , backoff = 2
                , value = IllegalArgumentException.class
        )
        public void execute() {
            attempts.put("attempts", "1");
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

        long startTime = System.currentTimeMillis();
        try {
            foo.execute();
        } catch (MaximumAttemptsExceededException e) {
            //To-do 최대 회수 시에도 오류 발생 시 로직
        }
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        Mockito.verify(mockFoo, times(expectedAttempts)).execute();
        log.info("executionTime: {}ms", executionTime);
        log.info("expectedDelay: {}ms", expectedDelay);
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

        try {
            foo.execute();
        } catch (MaximumAttemptsExceededException e) {
            //To-do 최대 회수 시에도 오류 발생 시 로직
        }

        Mockito.verify(mockFoo, times(expectedAttempts)).execute();

    }

}