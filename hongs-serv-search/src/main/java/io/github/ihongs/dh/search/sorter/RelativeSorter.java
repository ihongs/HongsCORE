package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * 相对值排序器
 * 用法 new SortField(FIELD_NAME, new RelativeSorter(DIST), DESC)
 * @author Hongs
 */
public class RelativeSorter extends FieldComparatorSource {

    final long dist;

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

        final long dist;
        NumericDocValues docs;

        public Comparator(String name, int hits, long dist) {
            super(name, hits);
            this.dist = dist ;
        }

        @Override
        protected void doSetNextReader (LeafReader r)
        throws IOException {
            docs = DocValues.getNumeric(r, name);
        }

        @Override
        protected long toGetCurrDvalue ( int d )
        throws IOException {
            try {
                long fx = docs.advanceExact(d) ? docs.longValue() : 0;
                if ( fx > dist) {
                    return fx - dist ;
                } else
                {
                    return dist - fx ;
                }
            }
            catch (NullPointerException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
