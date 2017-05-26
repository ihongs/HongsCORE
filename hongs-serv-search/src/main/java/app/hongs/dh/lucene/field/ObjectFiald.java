package app.hongs.dh.lucene.field;

import app.hongs.util.Data;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class ObjectFiald implements IField {
    @Override
    public Field got(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field srt(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field get(String k, Object v, boolean u) {
        if (v == null || "".equals(v)) {
            v  = "{}" ;
        } else
        if (! ( v instanceof String )) {
            v  = Data.toString(v);
        }
        return new StoredField(k, v.toString());
    }
}
