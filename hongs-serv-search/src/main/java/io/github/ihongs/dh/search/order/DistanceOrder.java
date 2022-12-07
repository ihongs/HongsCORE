package io.github.ihongs.dh.search.order;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 球面距离排序
 * 用法 new SortField(FIELD_NAME, new DistanceOrder(O, A), DESC)
 * 字段取值 O,A
 * @author Hongs
 */
public class DistanceOrder extends FieldComparatorSource {

    final float  o;
    final float  a;
    final  long  w;

    public DistanceOrder(float o, float a) {
        this(o , a, 12756000L); // 地球直径(米)
    }

    public DistanceOrder(float o, float a, long w) {
        this.o = o;
        this.a = a;
        this.w = w;
    }

    @Override
    public String toString() {
        return "Distance(" + o +","+ a + ")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, o, a, w);
    }

    static public class Comparator extends BaseComparator {

        final float  o;
        final float  a;
        final  long  w;
        BinaryDocValues docs ;

        public Comparator(String name, int hits, float o, float a, long w) {
            super(name, hits);
            this.o = o;
            this.a = a;
            this.w = w;
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
                float    fo = Float.parseFloat(xy[0]);
                float    fa = Float.parseFloat(xy[1]);

                fo = Math.abs(fo - o);
                fa = Math.abs(fa - a);
                double h = Math.sin(fo / 2);
                double v = Math.sin(fa / 2);
                v  = Math.asin(Math.sqrt(Math.cos(fa) * Math.cos(a) * (h * h) + (v * v)));
                return (long) (v * w);
            }
            catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
