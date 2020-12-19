package io.github.ihongs.action.anno;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.util.verify.Wrongs;
import io.github.ihongs.util.Synt;
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
 * 默认仅取第一个错误,
 * 如果当前动作名不是 create/update,
 * 则通过 id 参数判断是否为更新操作;
 * 或设置 type 标识符.
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
            Boolean up = Synt.asBool(helper.getAttribute(Cnst.UPDATE_MODE));
            if (up == null) {
                Object hd = chains.getHandle(  );
                Object id = dat.get(Cnst.ID_KEY);
                type = /***/ "update".equals(hd)
                  || (null!=id && !"".equals(id))
                          ? (byte) 1 : (byte) 0 ;
            } else {
                type = up ? (byte) 1 : (byte) 0 ;
            }
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
            // 照顾 Module Action 的配置规则. 2018/7/7 改为完全由外部预判
//          if (FormSet.hasConfFile(conf+"/"+form)) {
//              conf = conf+"/"+form ;
//          }
        }

        // 执行校验
        try {
            Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
            if (data == null) {
                data  = FormSet.getInstance(conf).getForm(form);
            }

            VerifyHelper ver = new VerifyHelper();
            ver.addRulesByForm (conf, form, data);
            Map vls = ver.verify( dat, upd, prp );
            if (cln)  dat.clear ( );
            dat.putAll(  vls);
        } catch (Wrongs  err) {
            dat = err.toReply(prp ? 0 : mode);

            helper.reply(dat);

            // Servlet 环境下设置状态码为 400 (错误的请求)
            if (helper.getResponse() != null) {
                helper.getResponse().setStatus(SC_BAD_REQUEST);
            }

            // 记录可能引起错误的原因
            if (0 != Core.DEBUG ) {
                Throwable tr = err.getCause();
                if ( null != tr ) {
                    CoreLogger.error (  tr  );
                }
            }

            return;
        } catch (HongsException  ex) {
            int  ec  = ex.getErrno();
            if  (ec != 910 && ec != 911 && ec != 912) {
                throw  ex;
            }
        }

        chains.doAction();
    }

}
