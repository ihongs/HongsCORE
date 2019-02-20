package io.github.ihongs.dh.lucene.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Hongs
 */
public class SearchFiald implements IField {
    @Override
    public Field whr(String k, Object v) {
        return null; // 字符类型本身即可用于过滤, 无需额外增加过滤字段
    }
    @Override
    public Field odr(String k, Object v) {
        return new SortedDocValuesField("."+k, new BytesRef(v != null ? v.toString() : ""));
    }
    @Override
    public Field get(String k, Object v, boolean u) {
        return new   TextField(k, v != null ? v.toString() : "", u ? Field.Store.NO : Field.Store.YES);
    }
}
