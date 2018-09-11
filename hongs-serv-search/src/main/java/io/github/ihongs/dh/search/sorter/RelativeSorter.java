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
        return new Comparator(fn, nh, dist);
    }

    @Override
    public String toString() {
        return "Relative("+ dist + ")";
    }

    static public class Comparator extends BaseComparator {

        long dist;

        public Comparator(String name, int hits, long dist) {
            super(name, hits);
            this.dist = dist;
        }

        @Override
        protected long worth(int d) {
            try {
                BytesRef br = originalValues.get(d);
                String   fv = br.utf8ToString( );
                long     fx = Long.parseLong(fv);
                return Math.abs(fx - dist);
            }
            catch (NullPointerException | NumberFormatException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
