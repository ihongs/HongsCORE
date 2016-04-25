package app.hongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动作注解包裹器
 *
 * 非自动调用不起作用, 仅能用于动作方法上;
 * 其他动作注解需声明此注解, 并指定调用类.
 *
 * @author Hong
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter {
    Class<? extends FilterInvoker> value();
}
