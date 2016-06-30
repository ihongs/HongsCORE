package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.VerifyHelper;
import app.hongs.util.verify.Wrongs;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * 数据校验处理器
 * <pre>
 * 参数含义:
 * md=1  一般错误结构
 * md=2  层级错误结构
 * 默认仅取第一个错误
 * 如果 action 不是 create/update
 * 则需通过参数 id 来判断是创建还是更新
 * 或设置 save 标识
 * </pre>
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
        byte    mods = ann.save();
        boolean tidy = ann.tidy();

        // 准备数据
        Map<String, Object> dat = helper.getRequestData();
        Object  id  = dat.get(Cnst.ID_KEY);
        String  act = chains.getAction(  );
        if (mode == -1) {
            mode = Synt.declare(helper.getParameter(Cnst.MD_KEY), (byte) -1);
        }
        if (mods == -1) {
            mods = act.endsWith("/update") || (null != id && !"".equals(id))
                   ? ( byte ) 1 : ( byte ) 0;
        }
        boolean prp = mode <= 0 ;
        boolean upd = mods == 1 ;

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
            if (tidy) dat.clear (   );
            dat.putAll(  vls);
        } catch (Wrongs  err) {
            dat = new HashMap();
            dat.put("ok",false);

            if (prp) {
                dat.put("sig", "Er400");
                dat.put("msg", err.getLocalizedMessage());
            } else {
                Map ers;
                if (mode == 2) {
                    ers = err.getErrmap();
                } else {
                    ers = err.getErrors();
                }
                dat.put("errs",  ers  );
                dat.put("sig", "Er400");
                dat.put("msg", CoreLocale.getInstance ( )
                        .translate("fore.form.invalid" ));
            }

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
