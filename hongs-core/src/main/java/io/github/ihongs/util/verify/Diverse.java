package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 唯一规则
 *
 * <pre>
 * 规则参数:
 *  data-ut 查询动作名, 末尾可添加 #其他参与唯一的字段
 *  diverse 并非用在这, 而是被用于 Repeated 中去除重复
 * </pre>
 *
 * @author Hongs
 */
public class Diverse extends Rule {

    @Override
    public Object verify(Object value, Wheel watch) throws Wrong {
        // 跳过空值和空串
        if (null== value || "".equals(value)) {
            return AVOID;
        }

        String at = Synt.declare(getParam("data-ut" ), "");
        String ck = Synt.declare(getParam("__conf__"), "");
        String fk = Synt.declare(getParam("__form__"), "");
        String nk = Synt.declare(getParam("__name__"), "");
        String ap = null;
        String aq = null;
        String ad = null;

        if ("".equals(at)) {
            at = ck + "/" + fk + "/search";
        } else {
            // 尝试解析附加参数
            int ps;
            ps = at.indexOf('#');
            if (ps > 0) {
                ad = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
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
        ah.setRequestData (rd);
        rd.put(Cnst.PN_KEY, 0);
        rd.put(Cnst.RN_KEY, 1);
        rd.put(Cnst.RB_KEY, Synt.setOf(Cnst.ID_KEY));
        rd.put(nk , value);
        if (watch.isUpdate( )) { // 更新需排除当前记录
            Object vo = watch.getValues().get(Cnst.ID_KEY);
            Map ne = new HashMap( );
            ne.put(Cnst.NE_REL, vo);
            rd.put(Cnst.ID_KEY, ne);
        }

        // 附加参数
        if (ad != null && !"".equals(ad)) {
            Set<String> ks = Synt.toTerms ( ad );
            if (null != ks) for (String kn: ks ) {
                rd.put( kn, watch.getValues().get(kn));
            }
        }
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

        // 判断记录是否存在
        Map sd  = ah.getResponseData();
        if (sd.containsKey("list")) {
           List list = (List) sd.get("list");
            if (list == null || list.isEmpty()) {
                return value;
            }
        } else
        if (sd.containsKey("info")) {
            Map info = (Map ) sd.get("info");
            if (info == null || info.isEmpty()) {
                return value;
            }
        } else
        if (sd.containsKey("page")) {
            Map page = (Map ) sd.get("page");
            if (page == null) {
                return value;
            } else
            if (page.containsKey("pagecount")
            &&  Synt.declare(page.get("pagecount"), 0) == 0) {
                return value;
            } else
            if (page.containsKey("rowscount")
            &&  Synt.declare(page.get("rowscount"), 0) == 0) {
                return value;
            }
        }

        throw new Wrong("fore.form.is.not.unique");
    }

}
