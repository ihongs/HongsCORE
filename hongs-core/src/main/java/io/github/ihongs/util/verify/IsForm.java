package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.util.Synt;

/**
 * 子表检验
 * <pre>
 * 规则参数:
 *  conf    配置名, 默认为当前配置
 *  form    表单名, 默认同 field.name
 * </pre>
 * @author Hongs
 */
public class IsForm extends Rule {
    @Override
    public Object verify(Object value, Veri helper) throws Wrong, Wrongs {
        // 跳过空值和空串
        if (null == value || "".equals(value)) {
            return  null;
        }

        String conf = Synt.asString(getParam("conf"));
        String name = Synt.asString(getParam("form"));
        if (conf == null || "".equals(conf)) {
            conf = Synt.asString(getParam("__conf__"));
        }
        if (name == null || "".equals(name)) {
            name = Synt.asString(getParam("__name__"));
        }

        VerifyHelper hlpr = new VerifyHelper();
        hlpr.isUpdate(helper.isUpdate());
        hlpr.isPrompt(helper.isPrompt());
        try {
            hlpr.addRulesByForm ( conf, name );
        } catch ( HongsException ex) {
            throw ex.toExemption(  );
        }
        return  hlpr.verify(Synt.asMap(value));
    }
}
