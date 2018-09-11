package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.SimpleFieldComparator;

/**
 * 基础对比类
 * @author Hongs
 */
public abstract class BaseComparator extends SimpleFieldComparator<Long> {

    protected final String    name;
    protected       long      top ;
    protected       long      bot ;
    protected final long [  ] comparedValues;
    protected BinaryDocValues originalValues;

    public BaseComparator(String name, int hits) {
        this.name = name;
        comparedValues = new long[hits];
    }

    @Override
    protected void doSetNextReader(LeafReaderContext lrc) throws IOException {
        originalValues = lrc.reader().getSortedDocValues(name);
    }

    @Override
    public void setTopValue(Long t) {
        top  = t;
    }

    @Override
    public void setBottom(int i) {
        bot  = comparedValues[i];
    }

    @Override
    public Long value (int i ) {
        return comparedValues[i];
    }

    @Override
    public void copy  (int i , int d ) {
        comparedValues[i] = worth( d );
    }

    @Override
    public int compare(int i0, int i1) {
        if (comparedValues[i0] < comparedValues[i1]) {
            return -1;
        }
        if (comparedValues[i0] > comparedValues[i1]) {
            return  1;
        }
        return 0;
    }

    @Override
    public int compareTop   (int d) {
        long dist = worth(d);
        if (top < dist) {
            return -1;
        }
        if (top > dist) {
            return  1;
        }
        return 0;
    }

    @Override
    public int compareBottom(int d) {
        long dist = worth(d);
        if (bot < dist) {
            return -1;
        }
        if (bot > dist) {
            return  1;
        }
        return 0;
    }

    /**
     * 取得并转化数据
     * @param d 文档索引
     * @return
     * @throws IOException
     */
    protected abstract long worth(int d);

}
