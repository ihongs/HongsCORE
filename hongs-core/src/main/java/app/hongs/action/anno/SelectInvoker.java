package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.SelectHelper;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * 选项补充处理器
 * <pre>
 * 参数含义:
 * md=0  表示不需要执行, md将被重置为1
 * md=1  表示要选项数据
 * md=2  表示要显示数据
 * </pre>
 * @author Hong
 */
public class SelectInvoker implements FilterInvoker {
    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Select   ann  = (Select) anno;
        String   conf = ann.conf();
        String   form = ann.form();
        byte     mode = ann.mode();

        if (mode == -1) {
            mode = Synt.declare(helper.getParameter(Cnst.MD_KEY), (byte) -1);
        }

        // 为 0 则不执行, 仅取 enum 数据
        Map rsp;
        if (mode ==  0) {
            mode =   1;
            rsp = new HashMap();
        } else
        if (mode == -1) {
            chains.doAction(  );
            return;
        } else {
            chains.doAction(  );
            rsp = helper.getResponseData(  );
            if (! Synt.declare(rsp.get("ok"), false)) {
                return;
            }
        }

        // 识别路径
        if (form.length() == 0 || conf.length() == 0) {
            form = chains.getEntity();
            conf = chains.getModule();
            if (FormSet.hasConfFile( conf+"/"+form )) {
                conf = conf+"/"+form ;
            }
        }

        // 填充数据
        try {
            SelectHelper sup;
            sup = new SelectHelper().addEnumsByForm(conf,form);
            sup.select ( rsp, mode );
        } catch (HongsException  ex) {
            int  ec  = ex.getErrno();
            if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10ea) {
                throw  ex;
            }
        }

        // 返回数据
        helper.reply(rsp);
    }

}
