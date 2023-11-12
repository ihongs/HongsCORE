package io.github.ihongs.action.anno;

import io.github.ihongs.Cnst;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.PresetHelper;
import io.github.ihongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 预置补充处理器
 * <p>
 * 可使用 ab 参数将指定好的条件注入到请求数据中.
 * 这样可减少终端一些常规参数,
 * 并能在未来更方便地做出调整;
 * 如: ab=v1 附加版本控制标识.
 * </p>
 * @author Hong
 */
public class PresetInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws CruxException {
        Preset   ann  = (Preset) anno;
        String   conf = ann.conf();
        String   form = ann.form();
        String[] deft = ann.deft();
        String[] defs = ann.defs();

        // 默认参数可完全由外部指定
        if (deft == null || deft.length == 0) {
            Map req = helper.getRequestData();
            Set<String> uzed = Synt.toTerms(req.get(Cnst.AB_KEY));
            Set<String> used = new LinkedHashSet ();
            if (null != uzed && ! uzed.isEmpty ( )) {
                for(String item : uzed) {
                    /*
                    // 2021/04/17
                    // 废弃返回数据模式
                    // 规避内部不确定性
                    // 总是返回原始对象
                    if (item.equals("_obj_")) {
                        Core.getInstance().put(Cnst.STRING_MODE, false);
                    } else
                    if (item.equals("_str_")) {
                        Core.getInstance().put(Cnst.STRING_MODE, true );
                    } else
                    */
                    if (item.length() > 1) {
                    char c  = item.charAt(0);
                    if ( c != '_' && c != '.' && c != ':' && c != '!' ) {
                        used.add(form+".def."+item);
                    }}
                }
                deft  = used.toArray(new String[0]);
            }
        }

        // 识别路径
        if (form.length() == 0) {
            form = chains.getEntity();
        }
        if (conf.length() == 0) {
            conf = chains.getModule();
            // 照顾 Module Action 的配置规则. 2018/7/7 改为完全由外部预判
//          if (FormSet.hasConfFile(conf+"/"+form)) {
//              conf = conf+"/"+form ;
//          }
        }

        // 补充参数
        try {
            Map          req;
            PresetHelper pre;

            req = helper.getRequestData();
            pre = new PresetHelper();
            pre.addItemsByForm(conf, form, deft, defs);
            pre.preset(req, helper );
        } catch (CruxException  ex ) {
            int  ec  = ex.getErrno();
            if  (ec != 910 && ec != 911 && ec != 913 ) { // 非枚举缺失
                throw  ex;
            }
        }

        chains.doAction();
    }

}
