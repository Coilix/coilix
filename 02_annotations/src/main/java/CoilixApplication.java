package io.github.coilix.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface CoilixApplication {
    String[] scanBasePackages() default {};
}
