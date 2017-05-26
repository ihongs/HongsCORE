package app.hongs.dh.lucene.field;

import org.apache.lucene.document.Field;

/**
 *
 * @author Hongs
 */
public interface IField {
    public Field get(String k, Object v, boolean u); // 存储字段
    public Field got(String k, Object v); // 筛选字段
    public Field srt(String k, Object v); // 排序字段
}
