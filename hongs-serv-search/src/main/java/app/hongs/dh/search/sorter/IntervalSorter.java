package app.hongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 区间值排序器
 * 用法 new SortField(FIELD_NAME, new IntervalSorter(VALUE), DESC)
 * 字段取值 B,E
 * @author Hongs
 */
public class IntervalSorter extends FieldComparatorSource {

    long dist;

    public IntervalSorter(long dist) {
        this.dist = dist;
    }

    @Override
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) throws IOException {
        return new Comparator(fn, nh, rv, dist);
    }

    static public class Comparator extends BaseComparator<String> {

        boolean desc;
        long    dist;

        public Comparator(String fieldName, int numHits, boolean desc, long dist) {
            super(fieldName, numHits);
            this.desc = desc;
            this.dist = dist;
        }

        @Override
        public String cv2sv(long v) {
            return String.valueOf(v);
        }

        @Override
        public long sv2cv(String t) {
            return Long.parseLong(t);
        }

        @Override
        public long price(int d) {
            BytesRef bytesRef = binaryDocValues.get(d);
            String   fv = bytesRef.utf8ToString();
            String[] xy = fv.split(",");
            long     fx = Long.parseLong(xy[0]);
            long     fy = Long.parseLong(xy[1]);
            if (fx > dist) {
                fx = fx - dist;
            } else
            if (fy < dist) {
                fx = dist - fy;
            } else {
                fx = dist - fx;
                fy = fy - dist;
                fx = fx < fy ? fx : fy;
            }
            return desc ?  0 - fx : fx;
        }
    }

}
