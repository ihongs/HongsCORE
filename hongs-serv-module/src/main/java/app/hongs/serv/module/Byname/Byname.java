package app.hongs.serv.module.Byname;

import app.hongs.action.anno.Filter;
import app.hongs.action.anno.VerifyInvoker;
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
@Filter(VerifyInvoker.class)
public @interface Byname {
    String   conf() default "default";
    String   form() default "";
}
