package foo.hongs.serv.matrix.byname;

import foo.hongs.action.anno.Filter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段映射
 * @author Hongs
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(BynameDriver.class)
public @interface Byname {
    String   conf() default "default";
    String   form() default "";
    String   code() default "code";
}
