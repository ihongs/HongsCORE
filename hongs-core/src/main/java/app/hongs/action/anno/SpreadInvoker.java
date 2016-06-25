package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.SpreadHelper;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 关联扩展处理器
 * <pre>
 * 参数含义:
 * md=4  表示要关联数据
 * </pre>
 * @author Hongs
 */
public class SpreadInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Spread   ann  = (Spread) anno;
        String   conf = ann.conf();
        String   form = ann.form();
        byte     mode = ann.mode();

        if (mode == -1) {
            mode = Synt.declare(helper.getParameter(Cnst.MD_KEY), (byte) -1);
        }

        // 执行动作
        chains.doAction();
        Map rsp = helper.getResponseData();

        if (mode != -1 && 4 != (4 & mode)) {
            return;
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

        // 填充数据
        try {
            Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
            if (data == null) {
                data  = FormSet.getInstance(conf).getForm(form);
            }

            SpreadHelper sup ;
            sup = new SpreadHelper().addItemsByForm(conf, data);
            sup.spread ( rsp);
        } catch (HongsException  ex) {
            int  ec  = ex.getErrno();
            if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10ea ) {
                throw  ex;
            }
        }

        // 返回数据
        helper.reply(rsp);
    }

}
