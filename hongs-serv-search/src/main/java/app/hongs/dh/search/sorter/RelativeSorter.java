package app.hongs.dh.search.sorter;

import java.io.IOException;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.util.BytesRef;

/**
 * 相对值排序器
 * 用法 new SortField(FIELD_NAME, new RelativeSorter(VALUE), DESC)
 * @author Hongs
 */
public class RelativeSorter extends FieldComparatorSource {

    long dist;

    public RelativeSorter(long dist) {
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
            BytesRef br = binaryDocValues.get(d);
            String   fv = br.utf8ToString(  );
            long     fx = Long.parseLong (fv);
                     fx = Math.abs(dist - fx);
            return desc ? 0 - fx : fx;
        }
    }

}
