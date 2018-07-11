package foo.hongs.util.verify;

import foo.hongs.HongsException;
import foo.hongs.util.Synt;
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
    public Veri helper ;

    public void setParams(Map  params) {
        this.params = params;
    }
    public void setValues(Map  values) {
        this.values = values;
    }
    public void setCleans(Map  cleans) {
        this.cleans = cleans;
    }
    public void setHelper(Veri helper) {
        this.helper = helper;
    }

    public abstract Object verify(Object value) throws Wrong, Wrongs, HongsException;

    /**
     * 批量将 Ruly 包装为 Rule
     * @param rule
     * @return
     */
    final static public Rule[] wrap(Ruly... rule) {
        Rule[] list = new Rule [rule.length];
        for (int i= 0; i< rule.length; i++ ) {
            list[i] = new Wrap (rule [ i ] );
        }
        return list;
    }

    final static private class Wrap extends Rule  {

      final private Ruly ruly;

        public Wrap(Ruly ruly) {
            this.ruly  = ruly;
        }

        @Override
        public  Object  verify(Object value) throws Wrong, Wrongs, HongsException {
            return ruly.verify(value, this );
        }

    }

}
