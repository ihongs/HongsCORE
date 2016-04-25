package app.hongs.action;

import app.hongs.HongsException;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.verify.Default;
import app.hongs.util.verify.Norepeat;
import app.hongs.util.verify.Optional;
import app.hongs.util.verify.Repeated;
import app.hongs.util.verify.Required;
import app.hongs.util.verify.Rule;
import app.hongs.util.verify.Veri;
import app.hongs.util.verify.Wrong;
import app.hongs.util.verify.Wrongs;
import static app.hongs.util.verify.Rule.FALSE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
 * error.Ex10f1=找不到表单规则
 * error.Ex10f2=找不到规则的类
 * error.Ex10f3=找不到规则的方法
 * error.Ex10f4=无法获取规则方法
 * error.Ex10f5=不正确的规则调用方法
 * error.Ex10f6=执行规则方法时发生异常
 * </pre>
 */
public class VerifyHelper implements Veri {

    private final Map<String, List<Rule>> rules;
    private boolean update;
    private boolean prompt;

    public VerifyHelper() {
        rules = new LinkedHashMap();
    }

    @Override
    public VerifyHelper addRule(String name, Rule... rule) {
        List rulez = rules.get(name);
        if (rulez == null) {
            rulez =  new ArrayList();
            rules.put( name, rulez );
        }
        rulez.addAll(Arrays.asList(rule));
        return this;
    }

    public VerifyHelper addRulesByForm(String conf, String form) throws HongsException {
        FormSet cnf = FormSet.getInstance(conf);
        Map map  = cnf.getForm(form);

        FormSet dfs = FormSet.getInstance("default");
        Map tps  = dfs.getEnum("__types__");
        Map pts  = dfs.getEnum("__patts__");

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            String  code = (String) et.getKey();
            Map     optz = (Map)  et.getValue();
            Map     opts =  new HashMap( optz );
            Object  o;

            o = opts.remove("__required__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Required();
                    Map  prms = new HashMap( );
                         rule.setParams(prms );
                    this.addRule( code, rule );
                } else {
                    Rule rule = new Optional();
                    this.addRule( code, rule );
                }
            }

            o = opts.remove("__repeated__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Repeated();
                    Map  prms = new HashMap( );
                         rule.setParams(prms );
                    this.addRule( code, rule );
                    if (opts.containsKey("distrinct"))
                        prms.put("distrinct", opts.remove("distrinct"));
                    if (opts.containsKey("minrepeat"))
                        prms.put("minrepeat", opts.remove("minrepeat"));
                    if (opts.containsKey("maxrepeat"))
                        prms.put("maxrepeat", opts.remove("maxrepeat"));
                } else {
                    Rule rule = new Norepeat();
                    this.addRule( code, rule );
                }
            }

            o = opts.remove("default");
            if (o != null) {
                Rule rule = new Default();
                Map  prms = new HashMap();
                     rule.setParams(prms);
                this.addRule( code, rule);
                prms.put("default" , o  );
                prms.put("alwayset", opts.remove("default-alwayset"));
                prms.put("increate", opts.remove("default-increate"));
            }

            String rule = (String) opts.get("__rule__");
            if (null == rule || "".equals(rule)) {
                String type = (String) opts.get("__type__");

                // 类型映射
                if ( tps.containsKey(/**/type )) {
                    rule  =  tps.get(/**/type ).toString( );
                } else {
                    rule  =  "string";
                }

                // 预定正则
                if ( pts.containsKey(/**/type )
                && !opts.containsKey("pattern")) {
                    opts.put("pattern" , type );
                }

                // 将 patt 转换为 isType 规则名
                String c = rule.substring(0, 1);
                String n = rule.substring(   1);
                rule = "Is"+c.toUpperCase()+ n ;
            }
            if (! rule.contains(".") ) {
                rule = Rule.class.getPackage().getName()+"."+rule;
            }

            Rule inst;
            try {
                inst = (Rule) (Class.forName(rule).newInstance());
            }
            catch (ClassNotFoundException ex) {
                throw new HongsException(0x10f1, "Failed to get rule: "+rule+" in "+conf+":"+form, ex);
            }
            catch (InstantiationException ex) {
                throw new HongsException(0x10f1, "Failed to get rule: "+rule+" in "+conf+":"+form, ex);
            }
            catch (IllegalAccessException ex) {
                throw new HongsException(0x10f1, "Failed to get rule: "+rule+" in "+conf+":"+form, ex);
            }
            catch (ClassCastException ex) {
                throw new HongsException(0x10f1, "Failed to get rule: "+rule+" in "+conf+":"+form, ex);
            }

            opts.put("__conf__", conf);
            opts.put("__form__", form);
            opts.put("__name__", code);
            inst.setParams(opts);
            addRule(code , inst);
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

    @Override
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

            if (data != FALSE) {
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
                data =  FALSE;
                break;
            } catch (Wrongs w) {
                failed(wrongz, w , name);
                data =  FALSE;
                break;
            }
            if (data == FALSE) {
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
        // 默认对 null      忽略
        Object disregard = params.get("disregard" );

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

                String name3 = name + "." + (i3 ++);
                data3 = verify(rulez, data3, name3, values, wrongz);
                if (data3 !=  FALSE) {
                    data2.add(data3);
                } else if (prompt && !wrongz.isEmpty()) {
                    return FALSE;
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

                String name3 = name +"."+ ( (String) e3.getKey( ) );
                data3 = verify(rulez, data3, name3, values, wrongz);
                if (data3 !=  FALSE) {
                    data2.add(data3);
                } else if (prompt && !wrongz.isEmpty()) {
                    return FALSE;
                }
            }
        }

        // 多个值的数量限制
        int n, c = data2.size();
        n = Synt.declare(params.get("minrepeat"), 0);
        if (n != 0 && c < n) {
            failed(wrongz, new Wrong("fore.form.lt.minrepeat", String.valueOf(n), String.valueOf(c))
                    .setLocalizedSegment((String) params.get("__disp__")), name);
            return FALSE;
        }
        n = Synt.declare(params.get("maxrepeat"), 0);
        if (n != 0 && c > n) {
            failed(wrongz, new Wrong("fore.form.gt.maxrepeat", String.valueOf(n), String.valueOf(c))
                    .setLocalizedSegment((String) params.get("__disp__")), name);
            return FALSE;
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
