package app.hongs.dh.search.sorter;

import java.io.IOException;
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
    public FieldComparator<?> newComparator(String fn, int nh, int sp, boolean rv) throws IOException {
        return new Comparator(fn, nh, rv, x, y);
    }

    static public class Comparator extends BaseComparator<String> {

        boolean desc;
        long    x;
        long    y;

        public Comparator(String fieldName, int numHits, boolean desc, long x, long y) {
            super(fieldName, numHits);
            this.desc = desc;
            this.x = x;
            this.y = y;
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
            long     fx = Long.parseLong(xy[0]) - x;
            long     fy = Long.parseLong(xy[1]) - y;
                     fx = (long) Math.sqrt(fx * fx + fy * fy);
            return desc ? 0 - fx : fx;
        }
    }

}
