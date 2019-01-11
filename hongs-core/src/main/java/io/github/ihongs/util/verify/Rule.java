package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * 常用规则
 * @author Hongs
 */
public abstract class Rule implements Ruly {

    /**
     * 跳过此值, 有错则可中止
     */
    public static final Object BLANK = Synt.LOOP.NEXT;

    /**
     * 立即终止, 抛弃后续校验
     */
    public static final Object BREAK = Synt.LOOP.LAST;

    /**
     * 未定义值, 结束时将抛弃
     */
    public static final Object UNDEF = new String("");

    /**
     * 断言校验值不为 UNDEF 或 null
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface NoUndef {}

    /**
     * 同 NoUndef 且空串时返回 null
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface NoEmpty {}

    private Map params = null;

    /**
     * 设置校验参数
     * @param params
     * @return
     */
    public final Rule config(Map params) {
        this . params = params;
        return this;
    }

    /**
     * 获取单个参数
     * @param key
     * @return
     */
    public final Object getParam(String key) {
        return params != null ? params.get(key): null;
    }

    /**
     * 获取单个参数, 缺失则返回默认值
     * @param <T>
     * @param key
     * @param def
     * @return
     */
    public final <T> T  getParam(String key, T def) {
        return Synt.declare(getParam(key), def);
    }

    /**
     * 获取单个参数, 断言为指定的类型
     * @param <T>
     * @param key
     * @param cls
     * @return
     */
    public final <T> T  getParam(String key, Class<T> cls) {
        return Synt.declare(getParam(key), cls);
    }

}
