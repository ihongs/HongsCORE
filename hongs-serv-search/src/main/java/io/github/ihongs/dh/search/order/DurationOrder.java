package io.github.ihongs.dh.search.order;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.util.BytesRef;

/**
 * 平面距离排序
 * 用法 new SortField(FIELD_NAME, new DurationOrder(X, Y), DESC)
 * 字段取值 X,Y
 * @author Hongs
 */
public class DurationOrder extends FieldComparatorSource {

    final long x, y;

    public DurationOrder(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Duration("+ x +","+ y +")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, Pruning sp, boolean rv) {
        return new Comparator(fn, nh, x, y);
    }

    static public class Comparator extends BaseComparator {

        final long x, y;
        SortedDocValues docs ;

        public Comparator(String name, int hits, long x, long y) {
            super(name, hits);
            this.x = x;
            this.y = y;
        }

        @Override
        protected void doSetNextReader(LeafReader r)
        throws IOException {
            docs = DocValues.getSorted(r, name);
        }

        @Override
        protected long toGetCurrDvalue( int d )
        throws IOException {
            try {
                BytesRef br = docs.advanceExact(d) ? docs.lookupOrd(docs.ordValue()) : null;
                String   fv = br.utf8ToString();
                String[] xy = fv.split("," , 2);
                long     fx = Long.parseLong(xy[0]);
                long     fy = Long.parseLong(xy[1]);

                // 三角函数求弦
                fx = Math.abs(fx - x);
                fy = Math.abs(fy - y);
                return (long) Math.sqrt(fx * fx + fy * fy);
            }
            catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
