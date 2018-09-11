package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * 球面距离排序
 * 用法 new SortField("经度字段,纬度字段", new DurationSorter(经度,纬度), DESC)
 * @author Hongs
 */
public class DistanceSorter extends FieldComparatorSource {

    float o, a;

    public DistanceSorter(float o, float a) {
        this.o = o;
        this.a = a;
    }

    @Override
    public String toString() {
        return "Dictance(" + a +","+ o + ")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, o, a);
    }

    static public class Comparator extends BaseComparator {

        float a, o;
        NumericDocValues doc0;
        NumericDocValues doc1;
//      static double EARTH_WIDTH = 6371000 * 2; // 地球直径(米)

        public Comparator(String name, int hits, float o, float a) {
            super(name, hits);
            this.o = o;
            this.a = a;
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
                float fo = Float.intBitsToFloat((int) doc0.get(d)); // 经度
                float fa = Float.intBitsToFloat((int) doc1.get(d)); // 维度

                fo = Math.abs(fo - o);
                fa = Math.abs(fa - a);
                double h = Math.sin(fo / 2);
                double v = Math.sin(fa / 2);
                v  = Math.asin(Math.sqrt(Math.cos(fa) * Math.cos(a) * (h * h) + (v * v)));
                return (long) v; // return (long) (v * EARTH_WIDTH);
            }
            catch (NullPointerException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
