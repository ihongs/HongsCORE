package io.github.ihongs.dh.search.order;

import java.io.IOException;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.SimpleFieldComparator;

/**
 * 基础对比类
 * @author Hongs
 */
public abstract class BaseComparator extends SimpleFieldComparator<Long> {

    protected final String name;
    protected final long[] vals;
    protected       long   top ;
    protected       long   bot ;

    public BaseComparator(String name, int hits) {
        this.name = name ;
        this.vals = new long[hits] ;
    }

    @Override
    protected void doSetNextReader ( LeafReaderContext c) throws IOException {
        doSetNextReader(c.reader());
    }

    protected abstract void doSetNextReader(LeafReader r) throws IOException;

    protected abstract long toGetCurrDvalue(    int    d) throws IOException;

    @Override
    public Long value (int i ) {
        return vals[i];
    }

    @Override
    public void copy  (int i , int d ) throws IOException {
        vals[i] = toGetCurrDvalue( d );
    }

    @Override
    public int compare(int i0, int i1) {
        if (vals[i0] < vals[i1]) {
            return -1;
        }
        if (vals[i0] > vals[i1]) {
            return  1;
        }
        return 0;
    }

    @Override
    public int compareTop   (int d) throws IOException {
        long dist = toGetCurrDvalue(d);
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
        long dist = toGetCurrDvalue(d);
        if (bot < dist) {
            return -1;
        }
        if (bot > dist) {
            return  1;
        }
        return 0;
    }

    @Override
    public void setBottom  (int  i) {
        bot = vals[i];
    }

    @Override
    public void setTopValue(Long t) {
        top = /**/ t ;
    }

}
