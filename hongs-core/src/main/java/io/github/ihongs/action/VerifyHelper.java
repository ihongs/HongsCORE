package io.github.ihongs.action;

import io.github.ihongs.HongsException;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Default;
import io.github.ihongs.util.verify.Defiant;
import io.github.ihongs.util.verify.Ordinary;
import io.github.ihongs.util.verify.Optional;
import io.github.ihongs.util.verify.Repeated;
import io.github.ihongs.util.verify.Required;
import io.github.ihongs.util.verify.Rule;
import io.github.ihongs.util.verify.Verify;
import java.util.Iterator;
import java.util.HashMap;
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
 * 无 type 也无 rule 则同 type="string",
 * 无 required 时默认启用 Optional 规则,
 * 无 repeated 时默认启用 Ordinary 规则;
 * 但 rule 以 $ 开头则完全使用自定义规则,
 * 也不自动绑定 default 和 defiant 规则.
 * </p>
 *
 * @author Hongs
 */
public class VerifyHelper extends Verify {

    public VerifyHelper() {
        super();
    }

    public VerifyHelper addRulesByForm(Map fs ) throws HongsException {
        String conf = Dict.getValue( fs, "default", "@", "conf");
        String form = Dict.getValue( fs, "unknown", "@", "form");
        return addRulesByForm( conf, form, fs );
    }

    public VerifyHelper addRulesByForm(String conf, String form) throws HongsException {
        Map fs = FormSet.getInstance(conf /**/)
                        .getForm    (form /**/);
        return addRulesByForm( conf, form, fs );
    }

    public VerifyHelper addRulesByForm(String conf, String form, Map fs) throws HongsException {
        FormSet  formSet;
        formSet= FormSet.getInstance("default");
        Map ts = formSet.getEnum ( "__types__");
        Map ps = formSet.getEnum ( "__patts__");
        Iterator it = fs.entrySet().iterator( );

        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            String  name = (String) et.getKey();
            if ( "@".equals(name) ) {continue;}
            Map     optz = (Map)  et.getValue();
            Map     opts =  new HashMap( optz );
            Object  o;

            /**
             * 一般情况下的字段属性,
             * 要么是选填要么是必填,
             * 要么是单值要么是多值;
             * 但有时候可能要自定义,
             * 比如希望是有序的列表,
             * 需要一个方式可以跳脱 required 和 repeated 限制.
             */
            String  ruls = (String) opts.remove("__rule__");
            boolean skip = ruls != null && ruls.startsWith("$");

            opts.put("__conf__", conf);
            opts.put("__form__", form);
            opts.put("__name__", name);

            if (skip) {
                ruls = ruls.substring( 1 );
            } else {
                o = opts.get   ("defiant");
                if (o != null) {
                    Rule rule = new Defiant();
                    rule.config (opts);
                    this.addRule(name , rule);
                }

                o = opts.get   ("default");
                if (o != null) {
                    Rule rule = new Default();
                    rule.config (opts);
                    this.addRule(name , rule);
                }

                o = opts.remove("__required__");
                if (Synt.declare(o, false)) {
                    Rule rule = new Required();
                    rule.config (opts);
                    this.addRule(name , rule );
                } else {
                    Rule rule = new Optional();
                    rule.config (opts);
                    this.addRule(name , rule );
                }

                o = opts.remove("__repeated__");
                if (Synt.declare(o, false)) {
                    Rule rule = new Repeated();
                    rule.config (opts);
                    this.addRule(name , rule );
                } else {
                    Rule rule = new Ordinary();
                    rule.config (opts);
                    this.addRule(name , rule );
                }
            }

            // 可设多个规则, 缺省情况按字符串处理
            String[] list;
            if (ruls == null || ruls.length() == 0) {
            if (skip) {
                continue ; // 完全跳过其他的校验
            } else {
                String type = (String) opts.get("__type__");
                String item ;
                if (ts.containsKey(type)) {
                    // 类名转换
                    item = ( String ) ts.get (type);
                    String c = item.substring(0, 1);
                    String n = item.substring(   1);
                    item = "Is"+c.toUpperCase()+ n ;
                } else
                if (ps.containsKey(type)) {
                    // 类型正则
                    opts.put( "pattern", type );
                    item = "IsString";
                } else {
                    item = "IsString";
                }

                list = new String[]{item};
            }
            } else {
                list = ruls.split("[,;]");
            }

            // 添加规则实例
            for(String item : list) {
                item = item.trim();
                if ( item.length() == 0 ) {
                    continue;
                }
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

                rule.config (opts);
                this.addRule(name, rule);
            }
        }

        return this;
    }

}
