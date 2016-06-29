package app.hongs.dh;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import java.util.Map;

/**
 * 模型视图
 * 用于支持不方便继承模型表单的类
 * @author Hongs
 */
public class ModelView extends ModelForm {

    public ModelView(Map fields, Map ftypes, Map dtypes) {
        this.setFields ( fields );
        this.setFtypes ( fields );
        this.setDtypes ( fields );
    }

    public ModelView(String conf , String form)
    throws HongsException {
        this(FormSet.getInstance(conf).getForm(form), null, null);
    }

}
