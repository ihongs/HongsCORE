package foo.hongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据校验
 * @author Hong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(VerifyInvoker.class)
public @interface Verify {
    String   conf() default "default";
    String   form() default "";
    byte     mode() default -1;
    byte     type() default -1;
    byte     trim() default -1;
}
