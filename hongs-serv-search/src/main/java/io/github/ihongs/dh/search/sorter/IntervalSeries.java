package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * 区间近邻排序
 用法 new SortField("开始字段,结束字段", new IntervalSeries(DIST), DESC)
 * @author Hongs
 */
public class IntervalSeries extends FieldComparatorSource {

    final long dist;

    public IntervalSeries(long dist) {
        this.dist = dist;
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, dist);
    }

    @Override
    public String toString() {
        return "Interval("+ dist + ")";
    }

    static public class Comparator extends BaseComparator {

        final long dist;
        NumericDocValues doc0;
        NumericDocValues doc1;

        public Comparator(String name, int hits, long dist) {
            super(name, hits);
            this.dist = dist ;
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

                // 区间外为正值, 仅计算绝对值
                if ( fx > dist) {
                    return  fx - dist;
                } else
                if ( fy < dist) {
                    return  dist - fy;
                }

                // 区间内为负值, 比外面都优先
                if ( fy - dist > dist - fx) {
                    return  fx - dist;
                } else
                {
                    return  dist - fy;
                }
            }
            catch (NullPointerException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
