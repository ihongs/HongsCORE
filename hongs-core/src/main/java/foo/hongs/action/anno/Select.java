package foo.hongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 选项补充
 * @author Hong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(SelectInvoker.class)
public @interface Select {
    String   conf() default "default";
    String   form() default "";
    byte     mode() default -1;
}