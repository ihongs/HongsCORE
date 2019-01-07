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
 * 无 repeated 时默认启用 Ordinary 规则,
 * 但 required=""表示既无 Required 也无 Optional,
 * 同 repeated=""表示既无 Repeated 也无 Ordinary.
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

            opts.put("__conf__", conf);
            opts.put("__form__", form);
            opts.put("__name__", name);

            o = opts.get   ("defiant");
            if (o != null) {
                Rule rule = new Defiant();
                     rule.setParams(opts);
                this.addRule(name , rule);
            }

            o = opts.get   ("default");
            if (o != null) {
                Rule rule = new Default();
                     rule.setParams(opts);
                this.addRule(name , rule);
            }

            o = opts.remove("__required__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Required();
                         rule.setParams(opts );
                    this.addRule(name , rule );
                } else {
                    Rule rule = new Optional();
                         rule.setParams(opts );
                    this.addRule(name , rule );
                }
            }

            o = opts.remove("__repeated__");
            if (! "".equals(o)) {
                if (Synt.declare(o, false)) {
                    Rule rule = new Repeated();
                         rule.setParams(opts );
                    this.addRule(name , rule );
                } else {
                    Rule rule = new Ordinary();
                         rule.setParams(opts );
                    this.addRule(name , rule );
                }
            }

            // 可设多个规则, 缺省情况按字符串处理
            List<String> list = Synt.toList (opts.get("__rule__"));
            if (list == null || list.isEmpty( )) {
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
