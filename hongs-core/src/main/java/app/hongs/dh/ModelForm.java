package app.hongs.dh;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import java.util.Map;

/**
 * 模型表单
 * 用于支持不方便继承模型视图的类
 * @author Hongs
 */
public class ModelForm extends ModelView {

    public ModelForm(Map fields, Map ftypes, Map dtypes) {
        this.setFields ( fields );
        this.setFtypes ( fields );
        this.setDtypes ( fields );
    }

    public ModelForm(String conf , String form)
    throws HongsException {
        this(FormSet.getInstance(conf).getForm(form), null, null);
    }

}
