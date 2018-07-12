package io.github.ihongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 预置补充
 * @author Hong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(PresetInvoker.class)
public @interface Preset {
    String   conf() default "default";
    String   form() default "";
    String[] deft() default {};
    String[] defs() default {};
}