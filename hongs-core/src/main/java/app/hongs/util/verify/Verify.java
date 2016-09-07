package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import static app.hongs.util.verify.Rule.BREAK;
import static app.hongs.util.verify.Rule.BLANK;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据校验助手
 * @author Hongs
 *
 * Java8 中利用 Verfiy.Func 使用函数式, 可简化代码, 如:
 * <pre>
 *  values = new Verify()
 *      .addRule("f1", (v, r)->{
 *          return v != null ? v : BLANK;
 *      })
 *      .addRule("f2", (v, r)->{
 *          return v != null ? v : EMPTY;
 *      })
 *      .verify(values);
 * </pre>
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 0x10f0~0x10ff
 * error.Ex10f0=规则格式错误
 * </pre>
 */
public class Verify implements Veri {

    private final Map<String, List<Rule>> rules;
    private boolean update;
    private boolean prompt;

    public Verify() {
        rules = new LinkedHashMap();
    }

    /**
     * 获取规则
     * @return
     */
    @Override
    public Map<String, List<Rule>> getRules() {
        return rules ;
    }

    /**
     * 设置规则
     * @param name
     * @param rule
     * @return
     */
    @Override
    public Verify setRule(String name, Rule... rule) {
        rules.put(name , Arrays.asList(rule));
        return this;
    }

    /**
     * 添加规则
     * @param name
     * @param rule
     * @return
     */
    @Override
    public Verify addRule(String name, Rule... rule) {
        List rulez = rules.get(name);
        if (rulez == null) {
            rulez =  new ArrayList();
            rules.put( name, rulez );
        }
        rulez.addAll(Arrays.asList(rule));
        return this;
    }

    /**
     * 利用 Rune 的 Func 接口可使用 Java8 函数式方法
     * @param name
     * @param rule
     * @return
     */
    public Verify addRule(String name, Func... rule) {
        for (  Func rune : rule  ) {
            addRule(name , new Rune(rune));
        }
        return this;
    }

    @Override
    public boolean isUpdate() {
        return update;
    }
    @Override
    public boolean isPrompt() {
        return prompt;
    }
    @Override
    public void isUpdate(boolean update) {
        this.update = update;
    }
    @Override
    public void isPrompt(boolean prompt) {
        this.prompt = prompt;
    }

    /**
     * 校验数据
     * @param values
     * @return
     * @throws Wrongs
     * @throws HongsException
     */
    @Override
    public Map verify(Map values) throws Wrongs, HongsException {
        Map<String, Wrong > wrongz = new LinkedHashMap();
        Map<String, Object> cleans = new LinkedHashMap();

        if (values == null) {
            values =  new HashMap();
        }

        for(Map.Entry<String, List<Rule>> et : rules.entrySet()) {
            List<Rule> rulez = et.getValue();
            String     name  = et.getKey(  );
            Object     data  = Dict.getParam(values, name );

            data = verify(rulez, data, name, values, cleans, wrongz);

            if (data == BREAK) {
                    break;
            } else
            if (data == BLANK) {
                if (prompt && ! wrongz.isEmpty()) {
                    break;
                }
            } else {
                Dict.setParam(cleans, data, name);
            }
        }

        if (!wrongz.isEmpty()) {
            throw new Wrongs(wrongz);
        }

        return cleans;
    }

    private Object verify(List<Rule> rulez, Object data, String name, Map values, Map cleans, Map wrongz) throws HongsException {
        int i =0;
        for (Rule  rule  :  rulez) {
            i ++;

            if (rule.params==null) {
                rule.setParams(new HashMap());
            }
            rule.setValues(values);
            rule.setCleans(cleans);
            rule.setHelper( this );

            try {
                data = rule.verify(data);
            } catch (Wrong  w) {
                // 设置字段标签和取值
                if (w.getLocalizedSegment() == null) {
                    w.setLocalizedSegment(( String ) rule.params.get("__disp__"));
                }
                failed(wrongz, w , name);
                data =  BLANK;
                break;
            } catch (Wrongs w) {
                failed(wrongz, w , name);
                data =  BLANK;
                break;
            }
            if (data == BLANK) {
                break;
            }
            if (data == BREAK) {
                break;
            }

            if (rule instanceof Repeated) {
                List<Rule> rulex = rulez.subList(i, rulez.size());
                data = repeat(rulex, data, name, values, wrongz, cleans, rule.params);
                break;
            }
        }
        return  data ;
    }

    private Object repeat(List<Rule> rulez, Object data, String name, Map values, Map cleans, Map wrongz, Map params)
    throws HongsException {
        // 可设置 defiant   为某个要忽略的值
        // 页面加 hidden    值设为要忽略的值
        // 此时如 check     全部未选代表清空
        // 默认对 null/空串 忽略
        Set d  =  Synt.asTerms(params.get( "defiant" ));
        if (d  == null || d.isEmpty( )) {
            d  =  new  HashSet();
            d.add ("");
        }

        // 是否必须不同的值
        Collection data2;
        if (Synt.declare(params.get("diverse"), false)) {
            data2 = new LinkedHashSet();
        } else {
            data2 = new ArrayList(/**/);
        }

        // 将后面的规则应用于每一个值
        if (data instanceof Collection) {
            int i3 = 0;
            for(Object data3  :  ( Collection ) data  ) {
                if (data3 == null || d.contains(data3)) {
                    continue;
                }

                String name3 = name + "." + (  i3 ++  );
                data3 = verify(rulez, data3, name3, values, cleans, wrongz);
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

                if (data3 == null || d.contains(data3)) {
                    continue;
                }

                String name3 = name + "." + ((String) e3.getKey() );
                data3 = verify(rulez, data3, name3, values, cleans, wrongz);
                if (data3 !=  BLANK) {
                    data2.add(data3);
                } else if (prompt && !wrongz.isEmpty()) {
                    return BLANK;
                }
            }
        }

        // 多个值的数量限制
        int n, c = data2.size();
        n = Synt.declare(params.get("minrepeat"), 0);
        if (n != 0 && c < n) {
            failed(wrongz, new Wrong("fore.form.lt.minrepeat", String.valueOf(n), String.valueOf(c))
                    .setLocalizedSegment((String) params.get("__disp__")), name);
            return BLANK;
        }
        n = Synt.declare(params.get("maxrepeat"), 0);
        if (n != 0 && c > n) {
            failed(wrongz, new Wrong("fore.form.gt.maxrepeat", String.valueOf(n), String.valueOf(c))
                    .setLocalizedSegment((String) params.get("__disp__")), name);
            return BLANK;
        }

        return data2;
    }

    public static void failed(Map<String, Wrong> wrongz, Wrong  wrong , String name) {
        wrongz.put(name, wrong);
    }

    public static void failed(Map<String, Wrong> wrongz, Wrongs wrongs, String name) {
        for (Map.Entry<String, Wrong> et : wrongs.getWrongs().entrySet()) {
            String n = et.getKey(   );
            Wrong  e = et.getValue( );
            wrongz.put(name+"."+n, e);
        }
    }

    public class Rune extends Rule {

      private final Func func;

        public Rune(Func func) {
            this.func  = func;
        }

        @Override
        public Object verify(Object value) throws Wrong, Wrongs, HongsException {
            return func.run (value, this );
        }

    }

    public static interface Func {

        public Object run(Object value, Rule rule);

    }

}
