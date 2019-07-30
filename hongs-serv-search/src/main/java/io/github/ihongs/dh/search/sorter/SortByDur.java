package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 平面距离排序
 * 用法 new SortField(FIELD_NAME, new SortByDur(X, Y), DESC)
 * 字段取值 X,Y
 * @author Hongs
 */
public class SortByDur extends FieldComparatorSource {

    final long x, y;

    public SortByDur(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Duration("+ x +","+ y +")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, x, y);
    }

    static public class Comparator extends BaseComparator {

        final long x, y;
        BinaryDocValues docs ;

        public Comparator(String name, int hits, long x, long y) {
            super(name, hits);
            this.x = x;
            this.y = y;
        }

        @Override
        protected void doSetNextReader(LeafReader r)
        throws IOException {
            docs = DocValues.getBinary(r, name);
        }

        @Override
        protected long toGetCurrDvalue( int d )
        throws IOException {
            try {
                BytesRef br = docs.advanceExact(d) ? docs.binaryValue() : null;
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
