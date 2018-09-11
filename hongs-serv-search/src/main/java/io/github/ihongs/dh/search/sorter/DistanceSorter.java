package io.github.ihongs.dh.search.sorter;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 距离值排序器
 * 用法 new SortField(FIELD_NAME, new DistanceSorter(X, Y), DESC)
 * 字段取值 X,Y
 * @author Hongs
 */
public class DistanceSorter extends FieldComparatorSource {

    long x;
    long y;

    public DistanceSorter(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Distance("+ x +"," + y + ")";
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, nh, x, y);
    }

    static public class Comparator extends BaseComparator {

        long x;
        long y;

        public Comparator(String name, int hits, long x, long y) {
            super(name, hits);
            this.x = x;
            this.y = y;
        }

        @Override
        protected long worth(int d) {
            try {
                BytesRef br = originalValues.get(d);
                String   fv = br.utf8ToString();
                String[] xy = fv.split (",", 2);
                long     fx = Long.parseLong(xy[0]);
                long     fy = Long.parseLong(xy[1]);

                // 三角函数求弦, 仅作平面计算
                         fx = Math.abs (fx - x);
                         fy = Math.abs (fy - y);
                return (long) Math.sqrt(fx * fx + fy * fy);
            }
            catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException ex) {
                return Long.MAX_VALUE;
            }
        }
    }

}
