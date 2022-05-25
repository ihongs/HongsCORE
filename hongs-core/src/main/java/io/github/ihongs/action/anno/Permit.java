package io.github.ihongs.action.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限控制
 *
 * 默认判断当前动作的权限, 注意: 既无斜杆前缀, 也无扩展后缀
 * role 的取值
 *  @开头表示检查对应动作, 如 @xxx/yyy.zzz 检查是否拥有 xxx/yyy.zzz 的动作权限
 *  $开头表示检查对应角色, 如 @xxx/yyy/zzz 检查是否拥有 xxx/yyy/zzz 的角色权限
 *  其他检查对应角色, 与上不同, 用户没有对应角色即无权, 上面当未配置时认为有权
 *
 * @author Hong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(PermitInvoker.class)
public @interface Permit {
    String   conf() default "default";
    String[] role() default {};
}
