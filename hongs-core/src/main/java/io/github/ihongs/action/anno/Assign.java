package io.github.ihongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将配置信息告知 ActionRunner 和动作注解过滤
 *
 * @author Hongs
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Assign {
    String conf() default "";
    String name() default "";
}
