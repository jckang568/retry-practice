package kr.co.jckang.retrypractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RetryPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryPracticeApplication.class, args);
    }

}
