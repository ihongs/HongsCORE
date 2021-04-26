package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
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
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return STAND;
        }
        if (value.equals("")) {
            return STAND;
        }

        // 如果像 id 一样只是基本字符组成则跳过
        // 也可用 rule 设置其他的校验规则来跳过
        if (Synt.declare(getParam("pass-id"), false)) {
            String sv = value.toString( );
            if (sv.matches("^[\\w\\-]+$")) {
                return value;
            }
        }

        String at = (String) getParam("data-at" );
        String vk = (String) getParam("data-vk" );
        String fk = (String) getParam("__name__");
        String ck = (String) getParam("__conf__");
        String fl = (String) getParam(  "form"  );
        String cl = (String) getParam(  "conf"  );

        if (cl == null || cl.isEmpty()) {
            cl = ck;
        }
        if (fl == null || fl.isEmpty()) {
            fl = fk.replaceFirst("_id$","");
        }
        if (at == null || at.isEmpty()) {
            at = cl + "/" + fl + "/search" ;
        }

        // 请求数据
        Map cd = new HashMap( );
        Map rd = new HashMap( );
        Set rb = new HashSet( );
        Set id = new HashSet( );
        id.add(value);
        rb.add(vk   );
        rb.add(Cnst.ID_KEY);
        rd.put(Cnst.ID_KEY, id);
        rd.put(Cnst.RB_KEY, rb);
        rd.put(Cnst.RN_KEY, 0 );
        rd.put(Cnst.PN_KEY, 1 );

        // 执行动作
        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData( cd );
        ah.setRequestData( rd );
        try {
            ActionRunner.newInstance(ah, at).doInvoke();
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }

        // 对比结果
        Map  sd  = ah.getResponseData();
        if ( sd != null ) {
            Map  nf  = (Map ) sd.get ("info");
            if ( nf != null && ! nf.isEmpty()) {
                return value;
            }
            List ls  = (List) sd.get ("list");
            if ( ls != null && ! ls.isEmpty()) {
                return value;
            }
        }

        throw new Wrong("fore.form.is.not.exists");
    }
}
