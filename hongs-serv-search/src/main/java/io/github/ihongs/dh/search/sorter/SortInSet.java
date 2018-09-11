package io.github.ihongs.dh.search.sorter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 按指定集合顺序排列
 * 类似 MySQL 的 ORDER BY FIND_IN_SET(col, set)
 * @author hong
 */
public class SortInSet extends FieldComparatorSource {

    private final Map<String, Long> map;

    public SortInSet(Collection<String> set) {
        long  i = 0 - set.size();
            map = new HashMap ();
        for ( String val : set ) {
            map.put( val , i++ );
        }
    }

    @Override
    public String toString() {
        return  map.keySet().toString();
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return  new Comparator (fn, nh, map);
    }

    private static class Comparator extends BaseComparator {

        private final Map<String, Long> map ;

        public Comparator(String name, int hits, Map<String, Long> map) {
            super(name, hits);
            this.map  = map  ;
        }

        @Override
        protected long worth(int d) {
            try {
                BytesRef br = originalValues.get(d);
                String   fv = br.utf8ToString( );
                Long     bs = map.get ( fv );

                if (null != bs) {
                    return  bs;
                } else {
                    return  0L;
                }
            }
            catch (NullPointerException ex ) {
                return 0L;
            }
        }

    }

}
