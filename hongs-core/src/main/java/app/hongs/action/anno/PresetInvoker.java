package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.PresetHelper;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 预置补充处理器
 * <pre>
 * ab 参数含义:
 * _obj_ 将启用对象模式
 * _str_ 将启用字串模式
 * </pre>
 * <p>
 * 可使用 ab 参数将指定好的条件注入到请求数据中.
 * 这样可减少终端一些常规参数,
 * 并能在未来更方便地做出调整;
 * 如 ab=v1 附加版本 1 的参数.
 * </p>
 * @author Hong
 */
public class PresetInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Preset   ann  = (Preset) anno;
        String   conf = ann.conf();
        String   form = ann.form();
        String[] deft = ann.deft();
        String[] defs = ann.defs();

        // 默认参数可完全由外部指定
        if (deft == null || deft.length == 0) {
            Set<String> uzed = Synt.toTerms(helper.getParameter(Cnst.AB_KEY));
            Set<String> used = new LinkedHashSet ();
            if (null != uzed && ! uzed.isEmpty ( )) {
                for(String item : uzed) {
                    if (item.startsWith("_" )
                    ||  item.startsWith("." )
                    ||  item.startsWith("!")) {
                    if (item.equals("_obj_")) {
                        Core.getInstance().put(Cnst.OBJECT_MODE, true);
                    } else
                    if (item.equals("_str_")) {
                        Core.getInstance().put(Cnst.OBJECT_MODE,false);
                    }
                        continue;
                    }
                    used.add("!"+ item);
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
            // 照顾 Module Action 的配置规则
            if (FormSet.hasConfFile(conf+"/"+form)) {
                conf = conf+"/"+form ;
            }
        }

        // 补充参数
        try {
            Map          req;
            PresetHelper pre;

            req = helper.getRequestData();
            pre = new PresetHelper();
            pre.addItemsByForm(conf, form, deft, defs);
            pre.preset(req, helper );
        } catch (HongsException  ex) {
            int  ec  = ex.getErrno();
            if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10eb ) {
                throw ex ;
            }
        }

        chains.doAction();
    }

}
