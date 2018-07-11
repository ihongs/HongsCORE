package foo.hongs.dh.lucene.field;

import foo.hongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

/**
 *
 * @author Hongs
 */
public class SearchFiald implements IField {
    @Override
    public Field got(String k, Object v) {
        return null; // 文本类型无法用于过滤, 无法增加过滤字段
    }
    @Override
    public Field srt(String k, Object v) {
        return null; // 文本类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field get(String k, Object v, boolean u) {
        return new TextField(k, Synt.declare(v, ""), u ? Field.Store.NO : Field.Store.YES);
    }
}
