package app.hongs.serv.matrix.byname;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.FilterInvoker;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 字段映射处理
 * @author Hongs
 */
public class BynameDriver implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Byname  ann  = (Byname) anno;
        String  conf = ann.conf();
        String  form = ann.form();
        String  code = ann.code();

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

        // 准备转换
        Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
        if (data == null) {
            data  = FormSet.getInstance(conf).getForm(form);
        }
        BynameHelper driver = new BynameHelper(data , code);

        Map req = helper.getRequestData();
            req.clear(  );
        driver.checkRequest(req);
    }

}
