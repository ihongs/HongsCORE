package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Synt;
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
 *  data-at 关联动作名
 *  data-vk 关联取值键
 *  pass-id 跳过像ID的
 * </pre>
 * @author Hongs
 */
public class IsFork extends Rule {
    @Override
    @Rule.NoEmpty
    public Object verify(Object value, Veri watch) throws Wrong {
        // 如果像 id 一样只是基本字符组成则跳过
        // 也可通过直接通过 rule 参数不做此检查
        if (Synt.declare(getParam("pass-id"), false)) {
            String sv = value.toString( );
            if (sv.matches("^[\\w\\-]+$")) {
                return value;
            }
        }

        String at = Synt.declare(getParam("data-at" ), "");
        String vk = Synt.declare(getParam("data-vk" ), "");
        String cl = Synt.declare(getParam(  "conf"  ), "");
        String fl = Synt.declare(getParam(  "form"  ), "");
        String ck = Synt.declare(getParam("__conf__"), "");
        String fk = Synt.declare(getParam("__name__"), "");
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
        Set rb = new HashSet();
        Set id = new HashSet();
        id.add(value);
        rb.add(vk   );
        rb.add(Cnst.ID_KEY);
        rd.put(Cnst.ID_KEY,id);
        rd.put(Cnst.RB_KEY,rb);
        rd.put(Cnst.RN_KEY, 0);
        rd.put(Cnst.PN_KEY, 1);
        ah.setRequestData (rd);

        // 附加参数
        if (aq != null && !"".equals(aq)) {
            if (aq.startsWith("{") && aq.endsWith("}")) {
                rd.putAll( ( Map )  Data.toObject(aq));
            } else {
                rd.putAll(ActionHelper.parseQuery(aq));
            }
        }

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

        // 执行动作
        try {
            new ActionRunner(ah,at).doInvoke();
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }

        // 对比结果
        Map  sd = ah.getResponseData( );
        List ls = (List) sd.get("list");
        if ( ls == null || ls.isEmpty()) {
            throw new Wrong("fore.form.is.not.exists", fl);
        }

        return value;
    }
}
