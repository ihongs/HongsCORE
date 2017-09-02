package app.hongs.action;

import app.hongs.HongsException;
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
 * error.Ex10f1=找不到表单规则
 * </pre>
 *
 * @author Hongs
 */
public class VerifyHelper extends Verify {

    public VerifyHelper() {
        super();
    }

    public VerifyHelper addRulesByForm(String conf, String form) throws HongsException {
        FormSet cnf = FormSet.getInstance(conf);
        Map map  = cnf.getForm(form);
        return  addRulesByForm(conf, form, map);
    }

    public VerifyHelper addRulesByForm(String conf, String form, Map map) throws HongsException {
        FormSet def = FormSet.getInstance("default");
        Map tps = def.getEnum("__types__");
        Map pts = def.getEnum("__patts__");

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            String  code = (String) et.getKey();
            Map     optz = (Map)  et.getValue();
            Map     opts =  new HashMap( optz );
            Object  o;

            opts.put("__conf__", conf);
            opts.put("__form__", form);
            opts.put("__name__", code);

            o = opts.remove("defiant");
            if (o != null) {
                Rule rule = new Defiant();
                Map  prms = new HashMap();
                     rule.setParams(prms);
                this.addRule( code, rule);
                prms.put("defiant" , o  );
            }

            o = opts.remove("default");
            if (o != null) {
                Rule rule = new Default();
                Map  prms = new HashMap();
                     rule.setParams(prms);
                this.addRule( code, rule);
                prms.put("default" , o  );
                if (opts.containsKey("default-create"))
                    prms.put("default-create", opts.remove("default-create"));
                if (opts.containsKey("default-always"))
                    prms.put("default-always", opts.remove("default-always"));
            }

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
                    if (opts.containsKey( "defiant" ))
                        prms.put( "defiant" , opts.remove( "defiant" ));
                    if (opts.containsKey( "diverse" ))
                        prms.put( "diverse" , opts.remove( "diverse" ));
                    if (opts.containsKey("minrepeat"))
                        prms.put("minrepeat", opts.remove("minrepeat"));
                    if (opts.containsKey("maxrepeat"))
                        prms.put("maxrepeat", opts.remove("maxrepeat"));
                } else {
                    Rule rule = new NoRepeat();
                    this.addRule( code, rule );
                }
            }

            // 可设多个规则, 缺省情况按字符串处理
            List<String> list = Synt.toList (opts.get("__rule__"));
            if (list == null || list.isEmpty( )) {
                String type = (String) opts.get("__type__");
                String item =  tps.containsKey (   type   )
                            ? (String)  tps.get(   type   )
                            : "string";

                // 类型正则
                if ( pts.containsKey(   type   )
                && !opts.containsKey("pattern")) {
                    opts.put("pattern", type   );
                }

                // 转为类名
                String c = item.substring(0 , 1);
                String n = item.substring(    1);
                item = "Is"+c.toUpperCase() + n ;

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
                addRule(code , rule);
            }
        }

        return this;
    }

}
