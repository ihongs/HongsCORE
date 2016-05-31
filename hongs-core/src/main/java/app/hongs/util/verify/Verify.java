package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import static app.hongs.util.verify.Rule.BLANK;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 数据校验助手
 * @author Hongs
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 0x10f0~0x10ff
 * error.Ex10f0=规则格式错误
 * </pre>
 */
public class Verify {

    private final Map<String, List<Rule>> rules;
    private boolean update;
    private boolean prompt;

    public Verify() {
        rules = new LinkedHashMap();
    }

    public Verify addRule(String name, Rule... rule) {
        List rulez = rules.get(name);
        if (rulez == null) {
            rulez =  new ArrayList();
            rules.put( name, rulez );
        }
        rulez.addAll(Arrays.asList(rule));
        return this;
    }
    public Map<String, List<Rule>> getRules() {
        return rules ;
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

    public Map verify(Map values) throws Wrongs, HongsException {
        Map<String, Object> valuez = new LinkedHashMap();
        Map<String, Wrong > wrongz = new LinkedHashMap();

        if (values == null) {
            values =  new HashMap();
        }

        for(Map.Entry<String, List<Rule>> et : rules.entrySet()) {
            List<Rule> rulez = et.getValue();
            String     name  = et.getKey(  );
            Object     data  = Dict.getParam(values, name  );

            data = verify(rulez, data, name, valuez, wrongz);

            if (data != BLANK) {
                Dict.setParam( valuez, data, name );
            } else if (prompt && !wrongz.isEmpty()) {
                break;
            }
        }

        if (!wrongz.isEmpty()) {
            throw new Wrongs(wrongz);
        }

        return valuez;
    }

    private Object verify(List<Rule> rulez, Object data, String name, Map values, Map wrongz) throws HongsException {
        int i =0;
        for (Rule  rule  :  rulez) {
            i ++;

            if (rule.params==null) {
                rule.setParams(new HashMap());
            }
            rule.setValues(values);
            rule.setHelper( this );

            try {
                data = rule.verify(data);
            } catch (Wrong  w) {
                // 设置字段标签
                w.setLocalizedSegment( (String) rule.params.get("__disp__") );
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

            if (rule instanceof Repeated) {
                List<Rule> rulex = rulez.subList(i, rulez.size());
                data = repeat(rulex, data, name, values, wrongz, rule.params);
                break;
            }
        }
        return  data ;
    }

    private Object repeat(List<Rule> rulez, Object data, String name, Map values, Map wrongz, Map params)
    throws HongsException {
        // 是否必须不同的值
        Collection data2;
        if (Synt.declare(params.get("distrinct"), false)) {
            data2 = new LinkedHashSet();
        } else {
            data2 = new ArrayList(/**/);
        }

        // 可设置 disregard 为某个要忽略的值
        // 页面中 checkbox  如果不选代表清空
        // 可以加 hidden    值设为要忽略的值
        // 默认对 null/空串 忽略
        Object disregard = Synt.declare(params.get("disregard"),"");

        // 将后面的规则应用于每一个值
        if (data instanceof Collection) {
            int i3 = 0;
            for(Object data3 : ( Collection ) data) {
                if (disregard != null) {
                    if (disregard.equals( data3 ) ) {
                        continue;
                    }
                } else {
                    if (data3 == null) {
                        continue;
                    }
                }

                String name3 = name + "." + ( i3  ++ );
                data3 = verify(rulez, data3, name3, values, wrongz);
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

                if (disregard != null) {
                    if (disregard.equals( data3 ) ) {
                        continue;
                    }
                } else {
                    if (data3 == null) {
                        continue;
                    }
                }

                String name3 = name + "." + ((String) e3.getKey() );
                data3 = verify(rulez, data3, name3, values, wrongz);
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

}
