package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.action.VerifyHelper;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 子表检验
 * <pre>
 * 规则参数:
 *  conf 配置名, 默认为当前配置
 *  form 表单名, 默认同 field.name
 * </pre>
 * @author Hongs
 */
public class IsForm extends Rule {
    @Override
    public Object verify(Object value) throws Wrongs, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String conf = Synt.asString(params.get("conf"));
        String name = Synt.asString(params.get("form"));
        if (conf == null || "".equals(conf)) {
            conf = Synt.declare(params.get("__conf__"), "");
        }
        if (name == null || "".equals(name)) {
            name = Synt.declare(params.get("__name__"), "");
        }

        Map data = Synt.asMap(value);
        VerifyHelper hlpr = new VerifyHelper();
        hlpr.addRulesByForm(conf, name );
        hlpr.isUpdate(helper.isUpdate());
        hlpr.isPrompt(helper.isPrompt());
        return hlpr.verify(data);
    }
}
