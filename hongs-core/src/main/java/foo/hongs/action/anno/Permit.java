package foo.hongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验
 * @author Hong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(PermitInvoker.class)
public @interface Permit {
    String   conf() default "default";
    String[] role() default {};
}
