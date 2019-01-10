package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 规则基类
 * @author Hongs
 */
public abstract class Rule {

    /**
     * 跳过此值, 有错则可中止
     */
    public static final Object BLANK = Synt.LOOP.NEXT;
    /**
     * 立即终止, 抛弃后续校验
     */
    public static final Object BREAK = Synt.LOOP.LAST;
    /**
     * 空值null
     */
    public static final Object EMPTY = null;

    /**
     * 校验参数
     */
    public Map  params = null;
    /**
     * 原始的请求数据
     */
    public Map  values = null;
    /**
     * 通过校验的数据
     */
    public Map  cleans = null;
    /**
     * 校验助手
     */
    public Veri helper = null;
    /**
     * 是否有值
     */
    public boolean valued = false;

    /**
     * 设置校验参数
     * @param params
     * @return
     */
    public final Rule params(Map params) {
        this.params = params;
        return this;
    }

    public abstract Object verify(Object value) throws Wrong, Wrongs, HongsException;

}
