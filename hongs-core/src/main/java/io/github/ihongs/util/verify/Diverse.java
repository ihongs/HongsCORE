package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.util.Dawn;
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
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return STAND;
        }
        if (value.equals("")) {
            return STAND;
        }

        String at = (String) getParam("data-ut" );
        String nk = (String) getParam("__name__");
        String ck = (String) getParam("__conf__");
        String fk = (String) getParam("__form__");
        String ad = null;
        String aq = null;

        if (at == null || at.isEmpty()) {
            at = ck + "/" + fk + "/search" ;
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
        }

        // 请求数据
        Map rd = new HashMap();
        rd.put(Cnst.PN_KEY, 0);
        rd.put(Cnst.RN_KEY, 1);
        rd.put(Cnst.RB_KEY, Synt.setOf (  Cnst.ID_KEY  ) );
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
                rd.put( kn, watch.getValues().get(kn) );
            }
        }
        if (aq != null && !"".equals(aq)) {
            if (aq.startsWith("{") && aq.endsWith("}")) {
                rd.putAll( ( Map )  Dawn.toObject(aq) );
            } else {
                rd.putAll(ActionHelper.parseQuery(aq) );
            }
        }

        // 执行动作
        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData(Synt.mapOf(
            Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get()
        ));
        ah.setRequestData( rd );
        try {
            ActionRunner.newInstance(ah, at).doInvoke();
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }

        // 对比结果
        Map sd  = ah.getResponseData();
        if (sd == null) {
                return value;
        }
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
            if (page == null || page.isEmpty()) {
                return value;
            } else
            if (page.containsKey("count")
            &&  Synt.declare(page.get("count"), 0) == 0) {
                return value;
            } else
            if (page.containsKey("pages")
            &&  Synt.declare(page.get("pages"), 0) == 0) {
                return value;
            }
        }

        throw new Wrong("fore.form.is.not.unique");
    }

}
