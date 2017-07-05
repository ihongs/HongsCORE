package app.hongs.util.verify;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 外键规则
 * <pre>
 * 原名 IsPick, 对应 form 类型名为 pick，改名 fork 意为 foreign key
 * 规则参数:
 *  conf    配置名, 默认为当前配置
 *  form    表单名, 默认同 field.name
 *  data-at 关联动作名
 *  data-vk 关联取值键
 * </pre>
 * @author Hongs
 */
public class IsFork extends Rule {
    @Override
    public Object verify(Object value) throws Wrongs, HongsException {
        if (null == value) {
            return  value;
        }

        String vl = Synt.declare(params.get("verify-type"), "");
        if("any".equals(vl)) {
            return value;
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
        String cl = Synt.declare(params.get("conf"    ), "");
        String fl = Synt.declare(params.get("form"    ), "");
        String ck = Synt.declare(params.get("__conf__"), "");
        String fk = Synt.declare(params.get("__name__"), "");
        String aq = null;

        if ("".equals(cl)) {
            cl =  ck ;
        }
        if ("".equals(fl)) {
            fl =  fk.replaceFirst("_id$", "");
        }
        if ("".equals(vk)) {
            vk = "id";
        }
        if ("".equals(at)) {
            at =  cl + "/" + fl + "/search"  ;
        } else {
            // 尝试解析附加参数
            int ps = at.indexOf("?");
            if (ps > 0) {
                aq = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
        }

        // 请求数据
        Map rd = new HashMap();
        Set rb = new HashSet();
        rb.add(vk);
        rd.put(Cnst.RN_KEY, 0);
        rd.put(Cnst.RB_KEY,rb);
        rd.put(Cnst.ID_KEY,value);
        // 附加参数
        if (aq != null && aq.length() != 0) {
            if (aq.startsWith("{") && aq.endsWith("}")) {
                rd.putAll( ( Map )  Data.toObject(aq));
            } else {
                rd.putAll(ActionHelper.parseQuery(aq));
            }
        }

        // 获取结果
        ActionHelper ah = ActionHelper.newInstance();
        ah.setAttribute( "fork", true );
        /* Get */ ah.setRequestData(rd);
        new ActionRunner(at, ah).doInvoke( );
        Map  sd = ah.getResponseData( );
        List<Map> ls = (List) sd.get("list");

        // 对比结果
        Set vs = Synt.declare(value, Set.class);
        Set us = new HashSet();
        for(Map um : ls) {
            us.add(um.get(vk));
        }
        if (vs.size() != us.size() || !vs.containsAll(us)) {
            throw new Wrong("fore.form.is.not.exists", fl);
        }

        return value;
    }
}
