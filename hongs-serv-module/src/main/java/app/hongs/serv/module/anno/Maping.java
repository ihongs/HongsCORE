package app.hongs.serv.module.anno;

import app.hongs.action.anno.Filter;
import app.hongs.action.anno.VerifyInvoker;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Hongs
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(VerifyInvoker.class)
public @interface Maping {
    String   conf() default "default";
    String   form() default "";
}
