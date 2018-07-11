package foo.hongs.dh.search.sorter;

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
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, rv, nh, x, y);
    }

    static public class Comparator extends BaseComparator {

        long x;
        long y;

        public Comparator(String name, boolean desc, int hits, long x, long y) {
            super(name, desc, hits);
            this.x = x;
            this.y = y;
        }

        @Override
        protected long worth(int d) {
            BytesRef bytesRef = originalValues.get(d);
            String   fv = bytesRef.utf8ToString();
            String[] xy = fv.split(",");
            long     fx = Long.parseLong(xy[0]);
            long     fy = Long.parseLong(xy[1]);
                     fx = Math.abs (fx - x);
                     fy = Math.abs (fy - y);
            return (long) Math.sqrt(fx * fx + fy * fy);
        }
    }

}
