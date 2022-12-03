package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.util.Synt;
import java.util.Set;

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
    public Object verify(Value watch) throws Wrong, Wrongs {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return PASS;
        }
        if (value.equals("")) {
            return null;
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
        boolean update = watch.isUpdate();
        boolean prompt = watch.isPrompt();

        /**
         * 字段外部可能会有特殊要求
         * 如下层总是要作为整体保存
         */
        Set <String> flag = Synt.toTerms(getParam("form-in"));
        if (flag != null) {
            if (flag.contains("create")) {
                update = false;
            } else
            if (flag.contains("update")) {
                update = true ;
            }
            if (flag.contains("prompt")) {
                prompt = true ;
            }
        }

        /**
         * 填充字段配置所指定的规则
         */
        try {
            hlpr.addRulesByForm (conf , name);
        } catch ( HongsException ex) {
            throw ex.toExemption(  );
        }

        return  hlpr.verify(Synt.asMap(value), update, prompt);
    }
}
