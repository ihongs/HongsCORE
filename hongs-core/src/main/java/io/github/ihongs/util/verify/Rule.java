package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 常用规则
 * @author Hongs
 */
public abstract class Rule implements Ruly {

    /**
     * 跳过此值, 有错则可中止
     */
    public static final Object BLANK = Synt.LOOP.LAST;

    /**
     * 立即终止, 抛弃后续校验
     */
    public static final Object STAND = Synt.LOOP.NEXT;

    /**
     * 校验参数
     */
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
