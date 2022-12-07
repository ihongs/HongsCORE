package io.github.ihongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 区间近邻排序
 * 用法 new SortField(FIELD_NAME, new IntervalSorter(DIST), DESC)
 * 字段取值 B,E
 * @author Hongs
 */
public class IntervalSorter extends FieldComparatorSource {

    final long dist;

    public IntervalSorter(long dist) {
        this.dist = dist;
    }

    @Override
    public String toString() {
        return "Interval("+ dist +")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, dist);
    }

    static public class Comparator extends BaseComparator {

        final long dist;
        BinaryDocValues docs ;

        public Comparator(String name, int hits, long dist) {
            super(name, hits);
            this.dist = dist ;
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

                // 区间外为正值, 仅计算绝对值
                if (fx > dist) {
                    return fx - dist ;
                } else
                if (fy < dist) {
                    return dist - fy ;
                }

                // 区间内为负值, 比外面都优先
                if (fy - dist > dist - fx) {
                    return fx - dist ;
                } else
                {
                    return dist - fy ;
                }
            }
            catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
