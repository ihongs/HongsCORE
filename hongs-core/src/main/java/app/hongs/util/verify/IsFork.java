package app.hongs.util.verify;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
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

        String ck = Synt.declare(params.get("__conf__"), "");
        String fk = Synt.declare(params.get("__name__"), "");
        String cf = Synt.declare(params.get( "conf"   ), "");
        String fm = Synt.declare(params.get( "form"   ), "");
        String at = Synt.declare(params.get( "data-at"), "");
        String vk = Synt.declare(params.get( "data-vk"), "");

        // 明确指定为 # 则检查是否是编号
        if("#".equals(at)) {
            Map    ps = FormSet.getInstance(/**/).getEnum("__patts__");
            String pn = Synt.declare(ps.get("id"), "^[A-Z0-9_]{1,32}");
            if (! Pattern.matches(pn, at)) {
                throw new Wrong("fore.form.haserror");
            }
            return  value;
        }
        // 明确指定为 * 则表示不需要校验
        if("*".equals(at)) {
            return  value;
        }

        if ("".equals(cf)) {
            cf =  ck ;
        }
        if ("".equals(fm)) {
            fm =  fk.replaceFirst("_id$" , "");
        }
        if ("".equals(at)) {
            at =  cf+ "/" +fm+ "/retrieve.act";
        }
        if ("".equals(vk)) {
            vk = "id";
        }

        // 请求数据
        Set rb = new HashSet();
        Map rd = new HashMap();
        rb.add(vk);
        rd.put(Cnst.RN_KEY, 0);
        rd.put(Cnst.RB_KEY,rb);
        rd.put(Cnst.ID_KEY,value);

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
            throw new Wrong("fore.form.is.not.exists", fm);
        }

        return value;
    }
}
