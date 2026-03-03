package io.github.ihongs.action.anno;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 转发动作
 *
 * AutoAction 里将会 forward, 无此则 include
 *
 * @author Hongs
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoForward { }
