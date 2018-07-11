package foo.hongs.dh.search.sorter;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 区间值排序器
 * 用法 new SortField(FIELD_NAME, new IntervalSorter(DIST), DESC)
 * 字段取值 B,E
 * @author Hongs
 */
public class IntervalSorter extends FieldComparatorSource {

    long dist;

    public IntervalSorter(long dist) {
        this.dist = dist;
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) {
        return new Comparator(fn, rv, nh, dist);
    }

    static public class Comparator extends BaseComparator {

        long dist;

        public Comparator(String name, boolean desc, int hits, long dist) {
            super(name, desc, hits);
            this.dist = dist;
        }

        @Override
        protected long worth(int d) {
            BytesRef bytesRef = originalValues.get(d);
            String   fv = bytesRef.utf8ToString();
            String[] xy = fv.split(",");
            long     fx = Long.parseLong(xy[0]);
            long     fy = Long.parseLong(xy[1]);
            if (fx > dist) {
                return fx - dist;
            } else
            if (fy < dist) {
                return dist - fy;
            }
            // 区间内为负值, 比外面都优先
            if (fy - dist > dist - fx) {
                return fx - dist;
            } else
            {
                return dist - fy;
            }
        }
    }

}
