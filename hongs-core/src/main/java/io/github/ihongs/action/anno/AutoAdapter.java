package io.github.ihongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 适配动作
 *
 * ActionRunner.newInstance 里对 at 上溯可适配, 无此则跳过
 *
 * @author Hongs
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoAdapter { }
