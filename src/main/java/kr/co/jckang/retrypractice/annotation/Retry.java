package kr.co.jckang.retrypractice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    int attempts();
    long delay();
    int backoff();
    Class<? extends Throwable>[] value() default {};
}
