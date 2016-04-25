package app.hongs.cmdlet.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令类识别标识
 *
 * 有此标识的才可能被外部访问到
 *
 * @author Hong
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cmdlet {
    String value() default "";
}
