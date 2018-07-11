package foo.hongs.util.verify;

import foo.hongs.Cnst;
import foo.hongs.Core;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.ActionRunner;
import foo.hongs.util.Data;
import foo.hongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 外键规则
 * <pre>
 * 原名 IsPick, 对应 form 类型名为 pick，改名 fork 意为 foreign key
 * 规则参数:
 *  conf    配置名, 默认为当前配置
 *  form    表单名, 默认同 field.name
 *  checked 不管存在否, 按 string 或 number 检查
 *  data-at 关联动作名
 *  data-vk 关联取值键
 * </pre>
 * @author Hongs
 */
public class IsFork extends Rule {
    @Override
    public Object verify(Object value) throws Wrongs, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String vl = Synt.declare(params.get("checked" ), "");
        if(   "any".equals(vl)) {
            return  value ;
        } else
        if("number".equals(vl)) {
            IsNumber rl;
            rl = new IsNumber( );
            rl.setHelper(helper);
            rl.setParams(params);
            rl.setValues(values);
            rl.setCleans(cleans);
            return rl.verify(value);
        } else
        if("string".equals(vl)) {
            IsString rl;
            rl = new IsString( );
            rl.setHelper(helper);
            rl.setParams(params);
            rl.setValues(values);
            rl.setCleans(cleans);
            return rl.verify(value);
        }

        String at = Synt.declare(params.get("data-at" ), "");
        String vk = Synt.declare(params.get("data-vk" ), "");
        String cl = Synt.declare(params.get(  "conf"  ), "");
        String fl = Synt.declare(params.get(  "form"  ), "");
        String ck = Synt.declare(params.get("__conf__"), "");
        String fk = Synt.declare(params.get("__name__"), "");
        String ap = null;
        String aq = null;

        if ("".equals(vk)) {
            vk = Cnst.ID_KEY;
        }
        if ("".equals(cl)) {
            cl = ck;
        }
        if ("".equals(fl)) {
            fl = fk.replaceFirst("_id$","");
        }
        if ("".equals(at)) {
            at = cl + "/" + fl + "/search" ;
        } else {
            // 尝试解析附加参数
            int ps;
            ps = at.indexOf('?');
            if (ps > 0) {
                aq = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
            ps = at.indexOf('!');
            if (ps > 0) {
                ap = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
        }

        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData(Synt.mapOf(
            Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get()
        ));

        // 请求数据
        Map rd = new HashMap();
        Set rb = Synt.setOf ( vk  );
        Set id = Synt.asSet (value);
        rb.add(vk);
        rd.put(Cnst.RN_KEY, 0);
        rd.put(Cnst.RB_KEY,rb);
        rd.put(Cnst.ID_KEY,id);
        ah.setRequestData (rd);
        // 虚拟路径
        if (ap != null && !"".equals(ap)) {
            if (ActionRunner.getActions()
                        .containsKey(ap)) {
                at = ap ; // 自动行为方法可能被定制开发
            }
            ah.setAttribute(Cnst.ACTION_ATTR, ap + Cnst.ACT_EXT);
        } else {
            ah.setAttribute(Cnst.ACTION_ATTR, at + Cnst.ACT_EXT);
        }
        // 附加参数
        if (aq != null && !"".equals(aq)) {
            if (aq.startsWith("{") && aq.endsWith("}")) {
                rd.putAll( ( Map )  Data.toObject(aq));
            } else {
                rd.putAll(ActionHelper.parseQuery(aq));
            }
        }

        // 获取结果
        new ActionRunner(ah , at).doInvoke();
        Map       sd = ah.getResponseData( );
        List<Map> ls = (List) sd.get("list");

        // 对比结果
        Set vs = Synt.asSet(value);
        Set us = new HashSet();
        if ( null != ls) {
        for(Map um : ls) {
            us.add(um.get(vk));
        }}
        if (vs.size() != us.size() || !vs.containsAll(us)) {
            throw new Wrong("fore.form.is.not.exists", fl);
        }

        return value;
    }
}
