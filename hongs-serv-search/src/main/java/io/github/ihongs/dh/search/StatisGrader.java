package io.github.ihongs.dh.search;

import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.dh.search.StatisHandle.Range;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;

/**
 * 分类统计工具
 * @author Kevin
 */
public class StatisGrader {

    public static final int COUNT = 0;
    public static final int ASSET = 1;
    public static final int RANGE = 2; // Range Count
    public static final int TALLY = 3;
    public static final int TOTAL = 4;

    private final IndexSearcher finder;
    private       Field[]       fields;
    private       Query         query ;

    private final Map<String, Map> counts = new HashMap();
    private final Map<String, Set> countx = new HashMap();
    private final Map<String, Integer> styles = new HashMap();

    public StatisGrader (IndexSearcher finder) {
        this.finder = finder;
    }

    /**
     * 限定条件
     * @param query
     * @return
     */
    public StatisGrader where(Query    query ) {
        this.query  = query ;
        return this;
    }

    /**
     * 限定字段
     * @param fields
     * @return
     */
    public StatisGrader field(Field... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * 排除统计项
     * @param alias  字段别名
     * @param values 排除项
     * @return
     */
    public StatisGrader repel(String alias, Set values) {
        countx.put(alias, values);
        return this;
    }

    /**
     * 指定统计项
     * @param alias  字段别名
     * @param values 指定项
     * @return
     */
    public StatisGrader allow(String alias, Set values) {
        int style = 0;
        for (Object value : values) {
            if (value instanceof Range) {
                style = RANGE;
            } else {
                style = ASSET;
            }
            break;
        }
        return allow(alias, values, style);
    }

    /**
     * 指定统计项
     * @param alias  字段别名
     * @param values 指定项
     * @param style  TOTAL 求和, ASSET 限定...
     * @return
     */
    public StatisGrader allow(String alias, Set values, int style) {
        styles.put(alias, style);
        Map countz = new HashMap();
        switch (style) {
        case TOTAL:
            for (Object value : values) {
                countz.put(value, new Total((Range) value));
            } break;
        case TALLY:
            for (Object value : values) {
                countz.put(value, new Tally((Range) value));
            } break;
        default:
            for (Object value : values) {
                countz.put(value, new Count(value));
            } break;
        }
        counts.put(alias, countz);
        return this;
    }

    /**
     * 执行统计
     * @return
     * @throws IOException
     */
    public Map<String, Map> acount() throws IOException {
        if (query == null) {
            query =  new MatchAllDocsQuery();
        }
        finder.search(query, new Filch(fields, counts, countx, styles));
     // finder.search(query, new Fetch(fields, counts, countx, styles)); // Deprecated
        return counts;
    }

    /**
     * 归并器
     */
    public static class Filch implements CollectorManager<Fetch, Integer> {

        private final Field[] fields;
        private final Map<String, Map> counts;
        private final Map<String, Set> countx;
        private final Map<String, Integer> styles;

        public Filch (Field[] fields,
                      Map<String, Map> counts,
                      Map<String, Set> countx,
                      Map<String, Integer> styles) {
            if (fields == null || fields.length == 0) {
                throw new NullPointerException("Fields required");
            }

            this.fields = fields;
            this.counts = counts;
            this.countx = countx;
            this.styles = styles;
        }

        @Override
        public Fetch newCollector() throws IOException {
            Map countz = new HashMap(counts);
            return new Fetch(fields, countz, countx, styles);
        }

        @Override
        public Integer reduce(Collection<Fetch> fetchs) throws IOException {
            int count  = 0;
            for (Fetch fetch : fetchs ) {
                Map<String, Map> countz;
                countz = fetch.counts();
                count += fetch.count ();

                for (Field field : fields) {
                    String k = field.alias;
                    Map vs = counts.get(k);
                    Map vz = countz.get(k);

                    for(Object ot : vz.entrySet()) {
                        Map.Entry et = (Map.Entry) ot;
                        Object    wk = et.getKey();
                        Object [] wz = (Object []) et.getValue();
                        Object [] ws = (Object []) vs.get ( wk );

                        if (ws == null) {
                            vs.put(wk, wz);
                        } else {
                            if (ws.length > 2) ws[2] = Synt.asInt(ws[2]) + Synt.asInt(wz[2]); // 计数
                            if (ws.length > 3) ws[3] = Double.sum(Synt.asDouble(ws[3]), Synt.asDouble(wz[3])); // 求和
                            if (ws.length > 4) ws[4] = Double.min(Synt.asDouble(ws[4]), Synt.asDouble(wz[4])); // 最小
                            if (ws.length > 5) ws[5] = Double.max(Synt.asDouble(ws[5]), Synt.asDouble(wz[5])); // 最大
                        }
                    }
                }
            }
            return count;
        }

    }

    /**
     * 采集器
     * 可以重写 collect 采集数据
     */
    public static class Fetch implements Collector, LeafCollector {

        private final Field[] fields;
        private final Map<String, Map> counts;
        private final Map<String, Set> countx;
        private final Map<String, Integer> styles;

        private int count = 0 ;

        public Fetch (Field[] fields,
                      Map<String, Map> counts,
                      Map<String, Set> countx,
                      Map<String, Integer> styles) {
            if (fields == null || fields.length == 0) {
                throw new NullPointerException("Fields required");
            }

            this.fields = fields;
            this.counts = counts;
            this.countx = countx;
            this.styles = styles;
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext c) throws IOException {
            LeafReader r = c.reader();
            for(Field field : fields) {
                field.prepare(r);
            }
            return this;
        }

        @Override
        public void collect(int i) throws IOException {
            for(Field  f : fields) {
                f.collect( i );

                String   k = f.alias;
                Iterable a = f.getValues();

                Set cntx = countx.get(k);
                Map cntc = counts.get(k);
                if (cntc == null) {
                    cntc = new HashMap();
                    counts.put(k, cntc );
                }

                switch (Synt.defxult(styles.get(k), 0)) {
                case TOTAL:   // 计数,求和,最小,最大
                case TALLY:   // 计数,0,最小,最大
                case RANGE: { // 计数
                    Map<Range, Coach> cntc2 = cntc;
                    Set<Range       > cntx2 = cntx;
                    F : for(Object o : a) {
                            Number v = (Number) o ;
                        if (  null  != cntx2) {
                        for(Range  w : cntx2) {
                            if (w.covers(v)) {
                                continue F ;
                            }
                        }}
                        for(Map.Entry<Range, Coach> rr : cntc2.entrySet()) {
                            Coach w = rr.getValue();
                            Range m = rr.getKey  ();
                            if (m.covers(v)) {
                                w.tap   (v);
                            }
                        }
                    }
                } break;
                case ASSET: {
                    /**
                     * 限定只管指定的选项,
                     * 不去理会其他的取值.
                     */
                    for ( Object v : a  ) {
                        if (cntx != null
                        &&  cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                          ((Count) cntc.get (v)).tap();
                        }
                    }
                } break;
                default: {
                    for ( Object v : a  ) {
                        if (cntx != null
                        &&  cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                          ((Count) cntc.get (v)).tap();
                        } else {
                            Count c = new Count(v);
                                  c.tap( );
                            cntc.put(v, c);
                        }
                    }
                }}
            }

            count ++;
        }

        @Override
        public void setScorer(Scorable s) {
            // 不需要打分
        }

        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }

        public Map counts() {
            return counts;
        }

        public int count() {
            return count;
        }

    }

    /**
     * 计算结果
     */
    public static class Total extends Coach {
  final private Range  rng;
        private Object txt = null;
        private int    cnt = 0;
        private double sum = 0;
        private double min = Double.NEGATIVE_INFINITY;
        private double max = Double.POSITIVE_INFINITY;

        public Total(Range rng) {
            this.rng = rng;
        }

        @Override
        public int cnt() {
            return cnt;
        }

        @Override
        public void tap() {
            cnt = 1 + cnt;
        }

        @Override
        public void tap(Number v) {
            tap(v.doubleValue( ));
        }

        @Override
        public void tap(double v) {
            cnt = 1 + cnt;
            sum = v + sum;
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
            }
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0 : return rng;
                case 1 : return txt;
                case 2 : return cnt;
                case 3 : return sum;
                case 4 : return min;
                case 5 : return max;
            }
            throw new IndexOutOfBoundsException("Index out of range("+5+"): "+i);
        }

        @Override
        public Object set(int i, Object v) {
            if (i == 1) {
                Object o;
                o =  txt;
                txt  = v;
                return o;
            }
            throw new IndexOutOfBoundsException("Only text(index 1) can be set");
        }

        @Override
        public int size() {
            return 6;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Tally) {
                Tally  tt = (Tally) o;
                return tt.cnt == cnt && tt.sum == sum
                    && tt.min == min && tt.max == max;
            }
            return false;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Total) {
                Total  tt = (Total) o;

                if (tt.cnt > cnt) {
                    return -1;
                }
                if (tt.cnt < cnt) {
                    return  1;
                }

                if (tt.sum > sum) {
                    return -1;
                }
                if (tt.sum < sum) {
                    return  1;
                }

                if (tt.max > max) {
                    return -1;
                }
                if (tt.max < max) {
                    return  1;
                }

                if (tt.min > min) {
                    return -1;
                }
                if (tt.min < min) {
                    return  1;
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects . hash(cnt, sum, min, max);
        }

        @Override
        public String toString() {
            return cnt +","+ sum +","+ min +","+ max ;
        }

        @Override
        public Object[] toArray() {
            return new Object [] {rng, txt, cnt, sum, min, max};
        }

        @Override
        public Object[] toArray(Object[] a) {
            return new Object [] {rng, txt, cnt, sum, min, max};
        }

    }

    /**
     * 计算结果
     */
    public static class Tally extends Coach {
  final private Range  rng;
        private Object txt = null;
        private int    cnt = 0;
        private double sum = 0;
        private double min = Double.NEGATIVE_INFINITY;
        private double max = Double.POSITIVE_INFINITY;

        public Tally(Range rng) {
            this.rng = rng;
        }

        @Override
        public int cnt() {
            return cnt;
        }

        @Override
        public void tap() {
            cnt  = 1 + cnt;
        }

        @Override
        public void tap(Number v) {
            tap(v.doubleValue( ));
        }

        @Override
        public void tap(double v) {
            cnt  = 1 + cnt;
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
            }
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0 : return rng;
                case 1 : return txt;
                case 2 : return cnt;
                case 3 : return sum;
                case 4 : return min;
                case 5 : return max;
            }
            throw new IndexOutOfBoundsException("Index out of range("+5+"): "+i);
        }

        @Override
        public Object set(int i, Object v) {
            if (i == 1) {
                Object o;
                o =  txt;
                txt  = v;
                return o;
            }
            throw new IndexOutOfBoundsException("Only text(index 1) can be set");
        }

        @Override
        public int size() {
            return 6;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Tally) {
                Tally  tt = (Tally) o;
                return tt.cnt == cnt && tt.sum == sum
                    && tt.min == min && tt.max == max;
            }
            return false;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Total) {
                Total  tt = (Total) o;

                if (tt.cnt > cnt) {
                    return -1;
                }
                if (tt.cnt < cnt) {
                    return  1;
                }

                if (tt.max > max) {
                    return -1;
                }
                if (tt.max < max) {
                    return  1;
                }

                if (tt.min > min) {
                    return -1;
                }
                if (tt.min < min) {
                    return  1;
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects . hash(cnt, sum, min, max);
        }

        @Override
        public String toString() {
            return cnt +","+ sum +","+ min +","+ max ;
        }

        @Override
        public Object[] toArray() {
            return new Object [] {rng, txt, cnt, sum, min, max};
        }

        @Override
        public Object[] toArray(Object[] a) {
            return new Object [] {rng, txt, cnt, sum, min, max};
        }

    }

    public static class Count extends Coach {
  final private Object val;
        private Object txt = null;
        private int    cnt = 0;

        public Count(Object val) {
            this.val = val;
        }

        @Override
        public int cnt() {
            return cnt;
        }

        @Override
        public void tap() {
            cnt  = 1 + cnt;
        }

        @Override
        public void tap(Number v) {
            tap();
        }

        @Override
        public void tap(double v) {
            tap();
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0 : return val;
                case 1 : return txt;
                case 2 : return cnt;
            }
            throw new IndexOutOfBoundsException("Index out of range("+2+"): "+i);
        }

        @Override
        public Object set(int i, Object v) {
            if (i == 1) {
                Object o;
                o =  txt;
                txt  = v;
                return o;
            }
            throw new IndexOutOfBoundsException("Only text(index 1) can be set");
        }

        @Override
        public int size() {
            return 3;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Count) {
                Count  tt = (Count) o;
                return tt.cnt == cnt ;
            }
            return false;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Count) {
                Count  tt = (Count) o;
                if (tt.cnt > cnt) {
                    return -1;
                }
                if (tt.cnt < cnt) {
                    return  1;
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects . hash(cnt);
        }

        @Override
        public String toString() {
            return cnt + "";
        }

        @Override
        public Object[] toArray() {
            return new Object [] {val, txt, cnt};
        }

        @Override
        public Object[] toArray(Object[] a) {
            return new Object [] {val, txt, cnt};
        }

    }

    /**
     * 基础计数容器
     * 需要实现 tap,get,set.size 用于计数和读取,
     * 还需实现 equals,compareTo,hashCode,toString,toArray
     */
    abstract public static class Coach implements Comparable, List {

        abstract public  int cnt();
        abstract public void tap();
        abstract public void tap(Number v);
        abstract public void tap(double v);

        private int top ;
        public  int top() {
            return  top ;
        }
        public void top(int i) {
            this.top = i;
        }

        @Override
        public Iterator iterator() {
            final Coach o = this;
            return new  Iterator() {
                int i = 0;

                @Override
                public Object  next() {
                    return o.get (i ++);
                }

                @Override
                public boolean hasNext() {
                    return o.size() > i;
                }

            };
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        //** 有限只读, 下列方法均不支持 */

        @Override
        @Deprecated
        public boolean add(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean contains(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean addAll(int i, Collection c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public boolean containsAll(Collection c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public int indexOf(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public void add(int i, Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public Object remove(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public List subList(int f, int t) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public ListIterator listIterator() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        @Deprecated
        public ListIterator listIterator(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

}
