package kr.co.jckang.retrypractice.aspect;

import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.exception.MaximumAttemptsExceededException;
import kr.co.jckang.retrypractice.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.UndeclaredThrowableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class RetryAopHandlerTest {

    public static class Foo {
        @Retry(attempts = 4
                , delay = 2000
                , backoff = 3
                , value = IllegalArgumentException.class
        )
        public void execute() {
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
    @DisplayName("최대시도 초과 시 MaximumAttemptsExceededException 발생")
    void retry_WhenAllFail_ThenThrowFinalException() {
        // given
        AspectJProxyFactory factory = new AspectJProxyFactory(new Foo());
        factory.addAspect(new RetryAspect());
        Foo foo = factory.getProxy();

        long startTime = System.currentTimeMillis();
        // when & then
        assertThatThrownBy(foo::execute)
                .isInstanceOf(MaximumAttemptsExceededException.class)
                .hasMessage("Max retries exceeded");

        long endTime = System.currentTimeMillis();
        log.info("실행시간: {} ms", endTime - startTime);
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

}
