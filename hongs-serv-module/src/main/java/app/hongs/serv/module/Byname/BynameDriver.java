package app.hongs.serv.module.Byname;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.FilterInvoker;
import app.hongs.action.anno.Verify;
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
        Verify  ann  = (Verify) anno;
        String  conf = ann.conf();
        String  form = ann.form();
        
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

        // 准备转行
        Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
        if (data == null) {
            data  = FormSet.getInstance(conf).getForm(form);
        }
        BynameHelper  dm = new BynameHelper(data);
        
        Map req = helper.getRequestData();
        req.clear();
        dm.checkRequest(req);
    }
    
}
