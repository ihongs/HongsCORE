package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 球面距离排序
 * 用法 new SortField(FIELD_NAME, new SortByDids(O, A), DESC)
 * 字段取值 O,A
 * @author Hongs
 */
public class SortByDis extends FieldComparatorSource {

    long o;
    long a;

    public SortByDis(float o, float a) {
        this.o = (long) (o * 10000000);
        this.a = (long) (a * 10000000);
    }

    @Override
    public String toString() {
        return "Distance(" + o +","+ a + ")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, o, a);
    }

    static public class Comparator extends BaseComparator {

        long o;
        long a;
        BinaryDocValues docs ;
//      static double EARTH_WIDTH = 6371000 * 2; // 地球直径(米)

        public Comparator(String name, int hits, long o, long a) {
            super(name, hits);
            this.o = o;
            this.a = a;
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
                BytesRef br = docs.get( d );
                String   fv = br.utf8ToString();
                String[] xy = fv.split("," , 2);
                long     fo = Long.parseLong(xy[0]);
                long     fa = Long.parseLong(xy[1]);

                fo = Math.abs(fo - o);
                fa = Math.abs(fa - a);
                double h = Math.sin(fo / 2);
                double v = Math.sin(fa / 2);
                v  = Math.asin(Math.sqrt(Math.cos(fa) * Math.cos(a) * (h * h) + (v * v)));
                return (long) v; // return (long) (v * EARTH_WIDTH);
            }
            catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
