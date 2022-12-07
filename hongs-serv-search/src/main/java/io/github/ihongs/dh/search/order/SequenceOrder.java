package io.github.ihongs.dh.search.order;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 集合顺序排序
 * 用法 new SortField(FIELD_NAME, new SequenceOrder(DIST), DESC)
 * 类似 MySQL 的 ORDER BY FIND_IN_SET(col, set)
 * @author Hongs
 */
public class SequenceOrder extends FieldComparatorSource {

    final Map <String, Long> dist ;

    public SequenceOrder(Collection<String> vals) {
        long  i  = 0 - vals.size();
            dist = new HashMap ( );
        for ( String  val : vals ) {
            dist.put( val , i ++ );
        }
    }

    @Override
    public String toString() {
        return dist.keySet().toString();
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator ( fn, nh, dist);
    }

    private static class Comparator extends BaseComparator {

        final Map <String, Long> dist;
        BinaryDocValues docs ;

        public Comparator(String name, int hits, Map<String, Long> dist) {
            super(name, hits);
            this.dist = dist ;
        }

        @Override
        protected void doSetNextReader(LeafReader r )
        throws IOException {
            docs = DocValues.getBinary(r, name);
        }

        @Override
        protected long toGetCurrDvalue( int d )
        throws IOException {
            try {
                BytesRef br = docs.advanceExact(d) ? docs.binaryValue() : null;
                String   v  = br.utf8ToString();
                Long     bs = dist.get( v );
                if (null != bs) {
                    return  bs;
                } else {
                    return  0L;
                }
            }
            catch (NullPointerException ex) {
                return 0L;
            }
        }

    }

}
