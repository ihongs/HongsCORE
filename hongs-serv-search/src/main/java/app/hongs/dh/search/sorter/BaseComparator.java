package app.hongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.SimpleFieldComparator;

/**
 * 基础对比类
 * @author Hongs
 * @param <T>
 */
public abstract class BaseComparator<T> extends SimpleFieldComparator<T> {

    protected final String fieldName;
    protected final long[] values;
    protected       long   top;
    protected       long   bot;

    protected BinaryDocValues binaryDocValues;

    public BaseComparator(String fieldName, int numHits) {
        this.fieldName = fieldName;
        values = new long[numHits];
    }

    @Override
    protected void doSetNextReader(LeafReaderContext lrc) throws IOException {
        binaryDocValues = lrc.reader().getSortedDocValues(fieldName);
    }

    @Override
    public int compare(int i0, int i1) {
        if (values[i0] > values[i1]) {
            return 1;
        }
        if (values[i0] < values[i1]) {
            return -1;
        }
        return 0;
    }

    @Override
    public int compareTop(int d) throws IOException {
        long dist = price(d);
        if (top < dist) {
            return -1;
        }
        if (top > dist) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareBottom(int d) throws IOException {
        long dist = price(d);
        if (bot < dist) {
            return -1;
        }
        if (bot > dist) {
            return 1;
        }
        return 0;
    }

    @Override
    public void setTopValue(T t) {
        top = sv2cv (t);
    }

    @Override
    public void setBottom(int i) {
        bot = values[i];
    }

    @Override
    public void copy(int i, int d) throws IOException {
        values[i] = price(d);
    }

    @Override
    public T value(int i) {
        return cv2sv(values[i]);
    }

    /**
     * 对比值转存储值
     * @param v
     * @return
     */
    public abstract T cv2sv(long v);

    /**
     * 存储值转对比值
     * @param t
     * @return
     */
    public abstract long sv2cv(T t);

    /**
     * 获取存储的取值
     * @param d
     * @return
     */
    public abstract long price(int d);
}
