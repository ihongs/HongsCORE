package io.github.ihongs.dh.search.sorter;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 相对值排序器
 * 用法 new SortField(FIELD_NAME, new RelativeSorter(DIST), DESC)
 * @author Hongs
 */
public class RelativeSorter extends FieldComparatorSource {

    long dist;

    public RelativeSorter(long dist) {
        this.dist = dist;
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, rv, nh, dist);
    }

    static public class Comparator extends BaseComparator {

        long dist;

        public Comparator(String name, boolean desc, int hits, long dist) {
            super(name, desc, hits);
            this.dist = dist;
        }

        @Override
        protected long worth(int d) {
            BytesRef br = originalValues.get(d);
            String   fv = br.utf8ToString(  );
            long     fx = Long.parseLong (fv);
            return Math.abs(fx - dist);
        }
    }

}
