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
import java.util.Set;

/**
 * 选项补充处理器
 * <pre>
 * ab 参数含义:
 * !enum 表示不需要执行, ab将被重置为.enum
 * .enum 表示要选项数据
 * _enum 表示加选项文本
 * _time 表示加数字时间
 * _link 表示加完整链接
 * _fork 表示加关联数据
 * !form 表示处理子表单
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
            Set ab = Synt.toTerms(helper.getRequestData().get(Cnst.AB_KEY));
            if (ab != null) {
                if (ab.contains("!enum")) {
                    mode  = 0;
                } else {
                if (ab.contains(".enum")) {
                    mode += SelectHelper.ENUM;
                }
                if (ab.contains("_enum")) {
                    mode += SelectHelper.TEXT;
                }
                if (ab.contains("_time")) {
                    mode += SelectHelper.TIME;
                }
                if (ab.contains("_link")) {
                    mode += SelectHelper.LINK;
                }
                if (ab.contains("_fork")) {
                    mode += SelectHelper.FORK;
                }
                if (ab.contains("!form")) {
                    mode += SelectHelper.FORM;
                }
                if (mode >= 0) {
                    mode += 1;
                }
                }
            }
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

            SelectHelper sel = new SelectHelper();
            sel.addItemsByForm (conf, form, data);
            sel.select ( rsp, mode );
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
