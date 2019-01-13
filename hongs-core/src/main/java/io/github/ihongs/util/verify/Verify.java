package io.github.ihongs.util.verify;

import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import static io.github.ihongs.util.verify.Rule.BLANK;
import static io.github.ihongs.util.verify.Rule.BREAK;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 数据校验助手
 * @author Hongs
 *
 * <p>
 * 校验过程中的状态全部通过参数传递,
 * 如果绑定的规则内也不存储过程状态,
 * 那就是整体线程安全的,
 * 可以一次构建反复使用.
 * </p>
 *
 * <p>Java 8 中用函数式可简化代码:</p>
 * <pre>
 *  values = new Verify()
 *      .addRule("f1", (v, w)->{
 *          return v != null ? v : Rule.BLANK;
 *      })
 *      .addRule("f2", (v, w)->{
 *          return v != null ? v : Rule.BREAK;
 *      })
 *      .verify(values);
 * </pre>
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 0x10f0~0x10ff
 * Ex10f0=规则格式错误
 * </pre>
 */
public class Verify {

    private final Map<String, List<Ruly>> rules;
    private boolean update;
    private boolean prompt;

    public Verify() {
        rules = new LinkedHashMap();
    }

    /**
     * 获取规则
     * @return
     */
    public Map<String, List<Ruly>> getRules() {
        return rules ;
    }

    /**
     * 设置规则
     * @param name
     * @param rule
     * @return
     */
    public Verify setRule(String name, Ruly... rule) {
        rules.put(name , new ArrayList(Arrays.asList(rule)));
        return this;
    }

    /**
     * 添加规则
     * @param name
     * @param rule
     * @return
     */
    public Verify addRule(String name, Ruly... rule) {
        List rulez = rules . get(name);
        if (rulez != null) {
            rulez.addAll(Arrays.asList(rule));
            return   this;
        }
        rules.put(name , new ArrayList(Arrays.asList(rule)));
        return this;
    }

    public boolean isUpdate() {
        return update;
    }
    public boolean isPrompt() {
        return prompt;
    }
    public void isUpdate(boolean update) {
        this.update = update;
    }
    public void isPrompt(boolean prompt) {
        this.prompt = prompt;
    }

    /**
     * 校验数据
     * @param values
     * @return
     * @throws Wrongs
     */
    public Map verify(Map values) throws Wrongs {
        Map<String, Wrong > wrongz = new LinkedHashMap();
        Map<String, Object> cleans = new LinkedHashMap();

        if (values == null) {
            values =  new HashMap();
        }

        for(Map.Entry<String , List<Ruly>> et : rules.entrySet() ) {
            List<Ruly> rulez = et.getValue();
            String     name  = et.getKey(  );
            Object     data  ;

            data = Dict.get( values, BLANK, Dict.splitKeys( name ) );

            data = verify(values, cleans, wrongz, rulez, name, data);

            if (prompt && ! wrongz.isEmpty()) {
                break;
            } else
            if (data == BREAK) {
                break;
            } else
            if (data == BLANK) {
                continue;
            }

            Dict.setParam(cleans, data, name);
        }

        if (!wrongz.isEmpty()) {
            throw new Wrongs(wrongz);
        }

        return cleans;
    }

    /**
     * 校验单个字段的值
     * @param values
     * @param cleans
     * @param wrongz
     * @param rulez
     * @param name
     * @param data
     * @return
     * @throws Wrongs
     */
    private Object verify(Map values, Map cleans, Map wrongz, List<Ruly> rulez, String name, Object data)
    throws Wrongs {
        Veriby      veri ;
        if (data == BLANK) {
            data  = null ;
            veri  = new Veriby(this, values, cleans, false);
        } else {
            veri  = new Veriby(this, values, cleans, true );
        }

        int i = 0;
        int j =  rulez. size();
        for(Ruly rule : rulez) {
            i ++ ;

            data = verify( wrongz, veri, rule, name, data );
            if (data == BLANK) {
                break;
            }
            if (data == BREAK) {
                break;
            }

            if (rule instanceof Rulx) {
                data = verify(values, cleans, wrongz, rulez.subList(i, j), name, data, veri, (Rulx) rule);
                break;
            }
        }
        return  data ;
    }

    /**
     * 校验字段的多个值
     * @param values
     * @param cleans
     * @param wrongz
     * @param rulez
     * @param name
     * @param data
     * @param veri
     * @param rule
     * @return
     * @throws Wrongs
     */
    private Object verify(Map values, Map cleans, Map wrongz, List<Ruly> rulez, String name, Object data, Veri veri, Rulx rule)
    throws Wrongs {
        Collection data2 = rule.getContext();
        Collection skips = rule.getDefiant();

        // 将后面的规则应用于每一个值
        if (data instanceof Object [ ]) {
            data = Arrays.asList (data);
        }
        if (data instanceof Collection) {
            int i3 = -1;
            for(Object data3 :  ( Collection )  data  ) {
                i3 += 1;

                if (data3 == null || skips.contains(data3)) {
                    continue;
                }

                String name3 = name + "[" + i3 + "]";
                data3 = verify(values, cleans, wrongz, rulez, name3, data3);
                if (data3 !=  BLANK) {
                    data2.add(data3);
                } else if (prompt && !wrongz.isEmpty()) {
                    return BLANK;
                }
            }
        } else if (data instanceof Map) {
            for(Object i3 : ( ( Map ) data).entrySet()) {
                Map.Entry e3 = (Map.Entry) i3;
                Object data3 = e3.getValue( );

                if (data3 == null || skips.contains(data3)) {
                    continue;
                }

                String name3 = name + "." + e3.getKey();
                data3 = verify(values, cleans, wrongz, rulez, name3, data3);
                if (data3 !=  BLANK) {
                    data2.add(data3);
                } else if (prompt && !wrongz.isEmpty()) {
                    return BLANK;
                }
            }
        }

        // 完成后还需再次校验一下结果
        return  remedy(wrongz, veri, rule, name, data2);
    }

    /**
     * 校验取值
     * @param wrongz
     * @param veri
     * @param rule
     * @param name
     * @param data
     * @return
     */
    private Object verify(Map wrongz, Veri veri, Ruly rule, String name, Object data) {
        try {
            return rule.verify(data,veri);
        } catch (Wrong  w) {
            // 设置字段标签
            if (w.getLocalizedCaption( ) == null) {
                if (rule instanceof Rule) {
                    Rule r = (Rule) rule ;
                    w.setLocalizedCaption(Synt.defxult(
                            Synt.asString(r.getParam("__text__")),
                            Synt.asString(r.getParam("__name__")),
                                          name) );
                } else {
                    w.setLocalizedCaption(name);
                }
            }
            fail(wrongz, w, name);
            return BLANK;
        } catch (Wrongs  w) {
            fail(wrongz, w, name);
            return BLANK;
        }
    }

    /**
     * 校准集合
     * @param wrongz
     * @param veri
     * @param rule
     * @param name
     * @param data
     * @return
     */
    private Object remedy(Map wrongz, Veri veri, Rulx rule, String name, Collection data) {
        try {
            return rule.remedy(data,veri);
        } catch (Wrong  w) {
            // 设置字段标签
            if (w.getLocalizedCaption( ) == null) {
                if (rule instanceof Rule) {
                    Rule r = (Rule) rule ;
                    w.setLocalizedCaption(Synt.defxult(
                            Synt.asString(r.getParam("__text__")),
                            Synt.asString(r.getParam("__name__")),
                                          name) );
                } else {
                    w.setLocalizedCaption(name);
                }
            }
            fail(wrongz, w, name);
            return BLANK;
        } catch (Wrongs  w) {
            fail(wrongz, w, name);
            return BLANK;
        }
    }

    /**
     * 记录下层多个错误
     * @param wrongz
     * @param wrongs
     * @param name
     */
    public static void fail(Map<String, Wrong> wrongz, Wrongs wrongs, String name) {
        for (Map.Entry<String, Wrong> et : wrongs.getWrongs().entrySet()) {
            String n = et.getKey(   );
            Wrong  e = et.getValue( );
            wrongz.put(name+"."+n, e);
        }
    }

    /**
     * 记录现发现的错误
     * @param wrongz
     * @param wrong
     * @param name
     */
    public static void fail(Map<String, Wrong> wrongz, Wrong  wrong , String name) {
            wrongz.put(name  , wrong);
    }

}
