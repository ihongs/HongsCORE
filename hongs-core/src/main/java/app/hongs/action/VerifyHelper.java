package app.hongs.action;

import app.hongs.HongsException;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.verify.Default;
import app.hongs.util.verify.Defiant;
import app.hongs.util.verify.NoRepeat;
import app.hongs.util.verify.Optional;
import app.hongs.util.verify.Repeated;
import app.hongs.util.verify.Required;
import app.hongs.util.verify.Rule;
import app.hongs.util.verify.Verify;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据校验助手
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 0x10f0~0x10ff
 * Ex10f1=找不到表单规则
 * </pre>
 *
 * <h3>特别注意</h3>
 * <p>
 * 在 form.xml 中,
 * 无 type 也无 rule 则同 type="string".
 * 无 required 时默认启用 Optional 规则,
 * 无 repeated 时默认启用 NoRepeat 规则,
 * 但 required=""表示既无 Required 也无 Optional,
 * 同 repeated=""表示既无 Repeated 也无 NoRepeat.
 * 依此规则 type="form" 或 rule 有 IsForm,Intact,
 * 或其他可接受集合类型取值时务必加上repeated="".
 * </p>
 *
 * @author Hongs
 */
public class VerifyHelper extends Verify {

    public VerifyHelper() {
        super();
    }

    public VerifyHelper addRulesByForm(Map map) throws HongsException {
        String conf = Dict.getValue(map, "default", "@", "conf");
        String form = Dict.getValue(map, "unknown", "@", "name");
        return addRulesByForm(conf, form , map);
    }

    public VerifyHelper addRulesByForm(String conf, String form) throws HongsException {
        Map    map  = FormSet.getInstance(conf)
                             .getForm/**/(form);
        return addRulesByForm(conf, form , map);
    }

    public VerifyHelper addRulesByForm(String conf, String form, Map map) throws HongsException {
        FormSet def = FormSet.getInstance("default");
        Map tps = def.getEnum("__types__");
        Map pts = def.getEnum("__patts__");

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            String  name = (String) et.getKey();
            Map     optz = (Map)  et.getValue();
            Map     opts =  new HashMap( optz );
            Object  o;

            opts.put("__conf__", conf);
            opts.put("__form__", form);
            opts.put("__name__", name);

            o = opts.get   ("defiant");
            if (o != null) {
                Rule rule = new Defiant();
                     rule.setParams(opts);
                this.addRule(name, rule);
            }

            o = opts.get   ("default");
            if (o != null) {
                Rule rule = new Default();
                     rule.setParams(opts);
                this.addRule(name, rule);
            }

            o = opts.remove("__required__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Required();
                         rule.setParams(opts );
                    this.addRule(name, rule );
                } else {
                    Rule rule = new Optional();
                         rule.setParams(opts );
                    this.addRule(name, rule );
                }
            }

            o = opts.remove("__repeated__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Repeated();
                         rule.setParams(opts );
                    this.addRule(name, rule );
                } else {
                    Rule rule = new NoRepeat();
                         rule.setParams(opts );
                    this.addRule(name, rule );
                }
            }

            // 可设多个规则, 缺省情况按字符串处理
            List<String> list = Synt.toList (opts.get("__rule__"));
            if (list == null || list.isEmpty( )) {
                String type = (String) opts.get("__type__");
                String item ;
                if (tps.containsKey(type)) {
                    // 类名转换
                    item = ( String ) tps.get(type);
                    String c = item.substring(0, 1);
                    String n = item.substring(   1);
                    item = "Is"+c.toUpperCase()+ n ;
                } else
                if (pts.containsKey(type)) {
                    // 类型正则
                    opts.put( "pattern", type );
                    item = "IsString";
                } else
                if ( ! "@" .equals (name)) {
                    item = "IsString";
                } else {
                    item =   "Ignore";
                }

                list = Synt.listOf (item);
            }

            // 添加规则实例
            for (String item : list) {
                if (! item.contains(".")) {
                    item = Rule.class.getPackage().getName()+"."+item;
                }

                Rule rule;
                try {
                    rule = (Rule) (Class.forName(item).newInstance());
                }
                catch (ClassNotFoundException ex) {
                    throw new HongsException(0x10f1, "Failed to get rule: "+item+" in "+conf+":"+form, ex);
                }
                catch (InstantiationException ex) {
                    throw new HongsException(0x10f1, "Failed to get rule: "+item+" in "+conf+":"+form, ex);
                }
                catch (IllegalAccessException ex) {
                    throw new HongsException(0x10f1, "Failed to get rule: "+item+" in "+conf+":"+form, ex);
                }
                catch (ClassCastException ex) {
                    throw new HongsException(0x10f1, "Failed to get rule: "+item+" in "+conf+":"+form, ex);
                }

                rule.setParams(opts);
                addRule(name , rule);
            }
        }

        return this;
    }

}
