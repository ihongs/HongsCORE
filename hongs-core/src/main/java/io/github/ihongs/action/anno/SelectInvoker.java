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
 * .enfo 表示要选项数据
 * .info 表示补充默认值
 * .fall 表示处理子表单
 * _text 表示加选项文本
 * _time 表示加数字时间
 * _link 表示加完整链接
 * _fork 表示加关联数据
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
                if (ab.contains(".enfo")) {
                    adds += SelectHelper.ENFO;
                }

                if (ab.contains(".info")) {
                    adds += SelectHelper.INFO;
                }

                if (ab.contains(".fall")) {
                    adds += SelectHelper.FALL;
                }

                if (ab.contains("_fork")) {
                    adds += SelectHelper.FORK;
                }

                if (ab.contains("_link")) {
                    adds += SelectHelper.LINK;
                }

                if (ab.contains("_time")) {
                    adds += SelectHelper.TIME;
                }

                if (ab.contains("_text")) {
                    adds += SelectHelper.TEXT;
                }
            }
        }

        // 向下执行
        chains.doAction();
        if (adds == 0) {
            return;
        }
        Map  rsp  = helper . getResponseData();
        if ( rsp == null) rsp = new HashMap ();
        if (!Synt.declare(rsp.get("ok"),true)) {
            return;
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

            Map data  = (Map) helper.getAttribute("form:"+conf+"!"+form);
            if (data == null) {
                data  = FormSet.getInstance(conf).getForm(form);
            }

            SelectHelper sel = new SelectHelper();
            sel.setItemsInForm( rb );
            sel.addItemsByForm( conf, form, data);
            sel.select ( rsp, adds );
        } catch (HongsException ex ) {
            int  ec  = ex.getErrno();
            if  (ec != 910 && ec != 911 && ec != 912) { // 非表单缺失
                throw  ex;
            }
        }

        // 返回数据
        helper.reply(rsp);
    }

}
