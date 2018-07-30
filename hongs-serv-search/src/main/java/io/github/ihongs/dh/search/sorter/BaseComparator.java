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
    protected final boolean   desc;
    protected       long      top;
    protected       long      bot;
    protected final long[]    comparedValues;
    protected BinaryDocValues originalValues;

    public BaseComparator(String name, boolean desc, int hits) {
        this.name = name;
        this.desc = desc;
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
    public Long value(int i) {
        return comparedValues[i];
    }

    @Override
    public int compare(int i0, int i1) {
        if (comparedValues[i0] > comparedValues[i1]) {
            return 1;
        }
        if (comparedValues[i0] < comparedValues[i1]) {
            return -1;
        }
        return 0;
    }

    @Override
    public int compareTop(int d) throws IOException {
        long dist = wxrth(d);
        if (top < dist) {
            return -1;
        }
        if (top > dist) {
            return  1;
        }
        return 0;
    }

    @Override
    public int compareBottom(int d) throws IOException {
        long dist = wxrth(d);
        if (bot < dist) {
            return -1;
        }
        if (bot > dist) {
            return  1;
        }
        return 0;
    }

    @Override
    public void copy(int i, int d) throws IOException {
        comparedValues[i] = wxrth(d);
    }

    /**
     * 将值做逆序转换
     * @param d 文档索引
     * @return
     * @throws IOException 
     */
    protected /*desc*/ long wxrth(int d) throws IOException {
        return  desc  ? 0 - worth(d) : worth(d);
    }

    /**
     * 取得并转化数据
     * @param d 文档索引
     * @return
     * @throws IOException
     */
    protected abstract long worth(int d) throws IOException;

}
