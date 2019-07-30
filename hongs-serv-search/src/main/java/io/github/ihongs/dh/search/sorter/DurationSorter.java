package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * 平面距离排序
 * 用法 new SortField("横标字段,纵标字段", new DurationSorter(横标,纵标), DESC)
 * @author Hongs
 */
public class DurationSorter extends FieldComparatorSource {

    final long x, y;

    public DurationSorter(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, x, y);
    }

    @Override
    public String toString() {
        return "Duration("+ x +","+ y + ")";
    }

    static public class Comparator extends BaseComparator {

        final long x, y;
        NumericDocValues doc0;
        NumericDocValues doc1;

        public Comparator(String name, int hits, long x, long y) {
            super(name, hits);
            this.x = x;
            this.y = y;
        }

        @Override
        protected void doSetNextReader (LeafReader r)
        throws IOException {
            String[] names = name.split(",", 2);
            doc0 = DocValues.getNumeric(r, names [0]);
            doc1 = DocValues.getNumeric(r, names [1]);
        }

        @Override
        protected long toGetCurrDvalue (int  d)
        throws IOException {
            try {
                long fx = doc0.advanceExact(d) ? doc0.longValue() : 0;
                long fy = doc1.advanceExact(d) ? doc1.longValue() : 0;

                // 三角函数求弦
                fx = Math.abs(fx - x);
                fy = Math.abs(fy - y);
                return (long) Math.sqrt(fx * fx + fy * fy);
            }
            catch (NullPointerException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
