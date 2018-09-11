package io.github.ihongs.dh.search.sorter;

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
 * 用法 new SortField(FIELD_NAME, new SortInSet(DIST), DESC)
 * 类似 MySQL 的 ORDER BY FIND_IN_SET(col, set)
 * @author hong
 */
public class SortInSet extends FieldComparatorSource {

    private final Map<String, Long> dist;

    public SortInSet(Collection<String> vals) {
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

        private final Map<String, Long> dist ;
        BinaryDocValues docs ;

        public Comparator(String name, int hits, Map<String, Long> dist) {
            super(name, hits);
            this.dist = dist ;
        }

        @Override
        protected void doSetNextReader(LeafReader r ) throws IOException {
            docs = DocValues.getBinary(r, name);
        }

        @Override
        protected long toGetCurrDvalue(int d) {
            try {
                BytesRef br = docs.get(d);
                String   v  = br.utf8ToString();
                Long     bs = dist.get(v);
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
