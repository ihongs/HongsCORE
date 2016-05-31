package app.hongs.action;

import app.hongs.HongsException;
import app.hongs.util.Synt;
import app.hongs.util.verify.Default;
import app.hongs.util.verify.Norepeat;
import app.hongs.util.verify.Optional;
import app.hongs.util.verify.Repeated;
import app.hongs.util.verify.Required;
import app.hongs.util.verify.Rule;
import app.hongs.util.verify.Verify;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 数据校验助手
 * @author Hongs
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 0x10f0~0x10ff
 * error.Ex10f1=找不到表单规则
 * </pre>
 */
public class VerifyHelper extends Verify {

    public VerifyHelper() {
        super();
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

}
