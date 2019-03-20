package io.github.ihongs.action.anno;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.SelectHelper;
import io.github.ihongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 选项补充处理器
 * <pre>
 * ab 参数含义:
 * !enum 表示不需要执行, 重置为 .enum
 * !info 表示不需要执行, 类似于 .defs
 * .enum 表示要选项数据
 * _text 表示加选项文本
 * _time 表示加数字时间
 * _link 表示加完整链接
 * _fork 表示加关联数据
 * .form 表示处理子表单
 * .defs 表示补充默认值
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
        byte     adds = ann.adds();

        if (adds == 0) {
            Set ab  = Synt.toTerms(
                helper.getRequestData ( )
                      .get( Cnst.AB_KEY )
            );
            if (ab != null) {
                if (ab.contains("!enum" )
                ||  ab.contains("!info")) {
                    adds = 0;
                    if (ab.contains("!enum")) {
                        adds -= SelectHelper.ENUM;
                    }
                    if (ab.contains("!info")) {
                        adds -= SelectHelper.INFO;
                    }
                    if (ab.contains("_time")) {
                        adds += SelectHelper.TIME;
                    }
                    if (ab.contains("_link")) {
                        adds += SelectHelper.LINK;
                    }
                    if (ab.contains("_fork")) {
                        adds += SelectHelper.FORK;
                    }
                    if (ab.contains(".form")) {
                        adds -= SelectHelper.FORM;
                    }
                } else {
                    if (ab.contains(".enum")) {
                        adds += SelectHelper.ENUM;
                    }
                    if (ab.contains("_enum" ) // 兼容旧名称
                    ||  ab.contains("_text")) {
                        adds += SelectHelper.TEXT;
                    }
                    if (ab.contains("_time")) {
                        adds += SelectHelper.TIME;
                    }
                    if (ab.contains("_link")) {
                        adds += SelectHelper.LINK;
                    }
                    if (ab.contains("_fork")) {
                        adds += SelectHelper.FORK;
                    }
                    if (ab.contains(".form")) {
                        adds += SelectHelper.FORM;
                    }
                    if (ab.contains(".defs")) {
                        adds += SelectHelper.INFO;
                    }
                }
            }
        }

        // 为负则不执行, 仅取选项数据
        Map rsp;
        if (adds == 0 ) {
            chains.doAction(  );
            return;
        } else
        if (adds >  0 ) {
            chains.doAction(  );
            rsp = helper.getResponseData(  );
            if (! Synt.declare(rsp.get("ok"), false)) {
                return;
            }
        } else {
            adds=(byte)(0-adds);
            rsp = new HashMap();
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

        // 填充数据
        try {
            Set rb = Synt.toTerms(
                helper.getRequestData()
                      .get(Cnst.RB_KEY)
            );

            Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
            if (data == null) {
                data  = FormSet.getInstance(conf).getForm(form);
            }

            SelectHelper sel = new SelectHelper();
            sel.setItemsInForm( rb );
            sel.addItemsByForm( conf, form, data);
            sel.select ( rsp, adds );
        } catch (HongsException ex ) {
            int  ec  = ex.getErrno();
            if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10ea ) {
                throw  ex;
            }
        }

        // 返回数据
        helper.reply(rsp);
    }

}
