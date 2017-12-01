package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.VerifyHelper;
import app.hongs.util.verify.Wrongs;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * 数据校验处理器
 * <pre>
 * ab 参数含义:
 * .errs 一般错误结构
 * !errs 层级错误结构
 * </pre>
 * <p>
 默认仅取第一个错误,
 如果当前动作名不是 create/update,
 则通过 id 参数判断是否为更新操作;
 或设置 type 标识符.
 </p>
 * @author Hong
 */
public class VerifyInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Verify  ann  = (Verify) anno;
        String  conf = ann.conf();
        String  form = ann.form();
        byte    mode = ann.mode();
        byte    type = ann.type();
        byte    trim = ann.trim();

        // 准备数据
        Map<String, Object> dat = helper.getRequestData();
        Object  id = dat.get(Cnst.ID_KEY);
        String  at = chains.getAction(  );
        if (mode == -1) {
            Set ab = Synt.toTerms(helper.getRequestData().get(Cnst.AB_KEY));
            if (ab != null) {
                if (ab.contains(".errs")) {
                    mode  = 1;
                } else
                if (ab.contains("!errs")) {
                    mode  = 2;
                }
            }
        }
        if (type == -1) {
            type = at.endsWith("/update") || (null != id && !"".equals(id))
                 ? ( byte ) 1:
                   ( byte ) 0;
        }
        boolean prp = mode <= 0 ;
        boolean upd = type == 1 ;
        boolean cln = trim == 1 ;

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

        // 执行校验
        try {
            Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
            if (data == null) {
                data  = FormSet.getInstance(conf).getForm(form);
            }

            VerifyHelper ver = new VerifyHelper();
            ver.addRulesByForm(conf, form, data );
            ver.isPrompt(prp);
            ver.isUpdate(upd);
            Map vls = ver.verify(dat);
            if (cln)  dat.clear (   );
            dat.putAll(  vls);
        } catch (Wrongs  err) {
            dat = err.toReply(prp ? 0 : mode);

            helper.reply(dat);

            // Servlet 环境下设置状态码为 400 (错误的请求)
            if (helper.getResponse() != null) {
                helper.getResponse().setStatus(SC_BAD_REQUEST);
            }

            return;
        } catch (HongsException  ex) {
            int  ec  = ex.getErrno();
            if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10ea) {
                throw  ex;
            }
        }

        chains.doAction();
    }

}
