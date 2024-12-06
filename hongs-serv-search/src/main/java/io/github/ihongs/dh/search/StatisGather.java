package io.github.ihongs.dh.search;

import io.github.ihongs.dh.search.StatisHandle.TYPE;
import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.dh.search.StatisHandle.Range;
import io.github.ihongs.dh.search.StatisHandle.Valuer;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.util.NumericUtils;

/**
 * 聚合统计工具
 * @author Hongs
 */
public class StatisGather {

    private final IndexSearcher finder;
    private       Dimen[]       dimans;
    private       Index[]       indics;
    private       Query         query ;

    public StatisGather (IndexSearcher finder) {
        this.finder = finder;
    }

    /**
     * 限定条件
     * @param query
     * @return
     */
    public StatisGather where(Query    query ) {
        this.query  = query ;
        return this;
    }

    /**
     * 分组维度
     * @param fields
     * @return
     */
    public StatisGather group(Dimen... fields) {
        this.dimans = fields;
        return this;
    }

    /**
     * 指标字段
     * @param fields
     * @return
     */
    public StatisGather count(Index... fields) {
        this.indics = fields;
        return this;
    }

    /**
     * 执行统计
     * @return
     * @throws IOException
     */
    public Collection<Map> assort() throws IOException {
        if (query == null) {
            query =  new MatchAllDocsQuery();
        }

        Filch  filch = new Filch(dimans, indics);
        return finder.search(query, filch);

        /* // Deprecated
        Fetch  fetch = new Fetch(dimans, indics);
               finder.search(query, fetch);
        return fetch .result();
        */
    }

    /**
     * 分组键
     * 用于将 Array 作为 Map 的键
     */
    public static class Group {

        private final Object[] values ;

        public Group( Object[] values ) {
            this.values = values;
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Group)) {
                return false;
            }
            return Arrays. equals (values, ((Group) o).values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

    }

    public static class Filch implements CollectorManager<Fetch, Collection<Map>> {

        private final Dimen[] dimans;
        private final Index[] indics;

        public Filch( Dimen[] dimans, Index[] indics ) {
            if (dimans == null) dimans = new Dimen[]{};
            if (indics == null) indics = new Index[]{};
            if (dimans.length==0 && indics.length==0 ) {
                throw new NullPointerException("Fields required");
            }

            this.dimans = dimans;
            this.indics = indics;
        }

        @Override
        public Fetch newCollector() throws IOException {
            return new Fetch(dimans, indics);
        }

        @Override
        public Collection<Map> reduce(Collection<Fetch> fetchs) throws IOException {
            Map<Group, Map> dist = new HashMap ();
            for(Fetch fetch : fetchs) {
            Map<Group, Map> dict = fetch.groups();
                for (Map.Entry<Group, Map> et : dict.entrySet()) {
                    Group g = et.getKey ( );
                    Map  vz = et.getValue();
                    Map  vs = dist. get (g);
                    if ( vs == null ) {
                        dist.put(g, vz);
                    } else {
                        for(Index index : indics) {
                            String f  = index.alias;
                            Object v0 = vs.get(f);
                            Object v1 = vz.get(f);
                            if (v1 == null) {
                                continue;
                            }
                            if (v0 == null) {
                                vs.put(f, v1);
                            } else {
                                vs.put(f, index.combine(v0, v1));
                            }
                        }
                    }
                }
            }
            return dist.values();
        }

    }

    /**
     * 采集器
     */
    public static class Fetch implements Collector, LeafCollector {

        private final Dimen[] dimans;
        private final Index[] indics;
        private final Map<Group, Map> groups;

        public Fetch( Dimen[] dimans, Index[] indics ) {
            if (dimans == null) dimans = new Dimen[]{};
            if (indics == null) indics = new Index[]{};
            if (dimans.length==0 && indics.length==0 ) {
                throw new NullPointerException("Fields required");
            }

            this.dimans = dimans;
            this.indics = indics;
            this.groups = new HashMap();
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc) throws IOException {
            LeafReader lr = lrc.reader();
            for(Dimen diman : dimans) {
                diman.prepare(lr);
            }
            for(Index index : indics) {
                index.prepare(lr);
            }
            return this;
        }

        @Override
        public void collect(int id) throws IOException {
            Object[] values = new Object[dimans.length];

            for(int i = 0; i < dimans.length; i ++) {
                values[i] = dimans[i].collecs( id );
            }

            // 获取分组条目
            Group group  = new Group(values);
            Map   entry  = groups.get(group);
            if (  entry == null  ) {
                  entry  = new HashMap();
                  groups.put(group, entry);
            for(int i = 0; i < dimans.length; i ++) {
                String n = dimans[i].alias;
                Object v = values[i];
                entry.put(n, v);
            }}

            for(int i = 0; i < indics.length; i ++) {
                String n = indics[i].alias;
                Object v = entry.get ( n );
                v = indics[i].collecs(id, v);
                entry.put(n, v);
            }
        }

        @Override
        public void setScorer(Scorable s) {
            // 不需要打分
        }

        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }

        public Collection<Map> result() {
            return groups.values();
        }

        public Map<Group, Map> groups() {
            return groups;
        }

    }

    /**
     * 维度字段
     */
    abstract public static class Dimen extends Field {

        public Dimen(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        abstract public Object collecs(int i) throws IOException;

    }

    /**
     * 字段取值
     */
    public static class Datum extends Dimen {

        public Datum(TYPE type, String field, String alias) {
            super(type, field, alias);

            // 仅支持单一取值字段
            if (type.ordinal() > TYPE.STRING.ordinal()) {
                throw new UnsupportedOperationException("Only supports single value field for " + alias);
            }
        }

        @Override
        public Object collecs(int i) throws IOException {
            collect (i);

            Valuer values = getValues();
            return values.hasNext()
                 ? values.   next()
                 : null  ;
        }

    }

    /**
     * 区间取值
     */
    public static class Scope extends Dimen {

        private final Range[] ranges;

        public Scope(TYPE type, String field, String alias, Range ... ranges) {
            super(type, field, alias);

            // 仅支持单一数值字段
            if (type.ordinal() > TYPE.DOUBLE.ordinal()) {
                throw new UnsupportedOperationException("Only supports single digit field for " + alias);
            }

            this.ranges = ranges;
        }

        public Scope(TYPE type, String field, String alias, String... ranges) {
            super(type, field, alias);

            // 仅支持单一数值字段
            if (type.ordinal() > TYPE.DOUBLE.ordinal()) {
                throw new UnsupportedOperationException("Only supports single digit field for " + alias);
            }

            // 区间字串转区间对象
                this.ranges = new Range[ranges.length];
            for(int i = 0 ; i < ranges.length ; i ++) {
                this.ranges[i] = new Range (ranges[i]);
            }
        }

        @Override
        public Object collecs(int i) throws IOException {
            collect(i);

            Valuer values = getValues();
            if ( ! values.hasNext()) {
                return null;
            }

            Number item = ( Number ) values.next();

            // 未指定区间则返回原始值
            if (ranges.length == 0 ) {
                return item;
            }

            // 逐一检查判断所处的区间
            for(Range  range : ranges ) {
            if (range.covers ( item ) ) {
                return range.toString();
            }}

            return null;
        }

    }

    /**
     * 指标字段
     * @param <V>
     */
    abstract public static class Index<V> extends Field {

        public Index(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        public Object collecs(int i, Object data) throws IOException {
            collect (i);

            Valuer values = getValues();
            if ( ! values.hasNext ( ) ) {
                return data;
            }

            return collect(i, (V) data);
        }

        abstract public Object collect(int i , V v) throws IOException;

        abstract public Object combine(Object v0, Object v1);
    }

    /**
     * 取首个值
     */
    public static class First extends Index {

        public First(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        @Override
        public Object collect(int i, Object v) throws IOException {
            if (v == null) {
                v  = getValues().next( );
            }
            return v;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            return v0;
        }

    }

    /**
     * 取所有值
     */
    public static class Flock extends Index<Set> {

        public Flock(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        @Override
        public Object collect(int i, Set v) throws IOException {
            if (v == null) {
                v  = new LinkedHashSet();
            }
            for(Object o : getValues() ) {
                v.add( o );
            }
            return v;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            Set w0 = (Set) v0;
            Set w1 = (Set) v1;
            w0.addAll(w1 );
            return w0;
        }

    }

    /**
     * 去重计数
     */
    public static class Crowd extends Index<Cnt> {

        public Crowd(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        @Override
        public Object collect(int i, Cnt v) throws IOException {
            if (v == null) {
                v  = new Cnt();
            }
            for(Object o : getValues() ) {
                v.add( o );
            }
            return v;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            Cnt w0 = (Cnt) v0;
            Cnt w1 = (Cnt) v1;
            w0.addAll(w1);
            return w0;
        }

    }

    /**
     * 计数
     */
    public static class Count extends Index<Long> {

        public Count(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        @Override
        public Object collect(int i, Long n) throws IOException {
            if (n == null) {
                n  =  0L;
            }
            switch (type) {
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE: {
                //  NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    n += 1;
                    break;
                }
                case STRING: {
                //  SortedDocValues  strValues = ((SortedDocValues ) getDocValues());
                //  if (! strValues.advanceExact(i)) {
                //      break;
                //  }
                    n += 1L;
                    break;
                }
                case INTS:
                case LONGS:
                case FLOATS:
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    n += numValues.docValueCount( );
                    break;
                }
                case STRINGS: {
                    SortedSetDocValues     strValues = ((SortedSetDocValues    ) getDocValues());
                //  if (! strValues.advanceExact(i)) {
                //      break;
                //  }
                //  n += docValueCount( strValues );
                    n += strValues.docValueCount( );
                    break;
                }
            }
            return  n ;
        }

        /* // 不再需要
        private long docValueCount(SortedSetDocValues strValues) throws IOException {
            long n  = 1L;
            while (SortedSetDocValues.NO_MORE_ORDS != strValues.nextOrd()) {
                 n += 1L;
            }
            return  n ;
        }
        */

        @Override
        public Object combine(Object v0, Object v1) {
            Long w0 = (Long) v0;
            Long w1 = (Long) v1;
            return w0 + w1;
        }

    }

    /**
     * 量化
     */
    public static class Tally extends Index<Number[]> {

        public Tally(TYPE type, String field, String alias) {
            super (type, field, alias);

            if (type == TYPE.STRING
            ||  type == TYPE.STRINGS ) {
                throw new UnsupportedOperationException("Unsupported type String(s) for " + alias);
            }
        }

        @Override
        public Object collect(int i, Number[] n) throws IOException {
            switch (type) {
                case INT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.longValue();
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 0,
                                 Int .min( x , n[2]. intValue()),
                                 Int .max( x , n[3]. intValue())};
                    } else {
                        n  = new Number[]{1L , 0, x, x };
                    }
                    break;
                }
                case LONG: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.longValue();
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 0,
                                 Long.min( x , n[2].longValue()),
                                 Long.max( x , n[3].longValue())};
                    } else {
                        n  = new Number[]{1L , 0, x, x };
                    }
                    break;
                }
                case FLOAT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               0,
                               Float .min( x , n[2]. floatValue()),
                               Float .max( x , n[3]. floatValue())};
                    } else {
                        n  = new Number[]{1L , 0, x, x };
                    }
                    break;
                }
                case DOUBLE: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               0,
                               Double.min( x , n[2].doubleValue()),
                               Double.max( x , n[3].doubleValue())};
                    } else {
                        n  = new Number[]{1L , 0, x, x };
                    }
                    break;
                }
                case INTS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x = (int ) numValues.nextValue();
                    int  min = x ;
                    int  max = x ;
                    int  j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = (int ) numValues.nextValue();
                         min = Int .min(min, x);
                         max = Int .max(max, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 0,
                                 Int .min(min, n[2]. intValue()),
                                 Int .max(max, n[3]. intValue())};
                    } else {
                        n  = new Number[]{1L , 0, min, max};
                    }
                    break;
                }
                case LONGS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = (long) numValues.nextValue();
                    long min = x ;
                    long max = x ;
                    int  j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = (long) numValues.nextValue();
                         min = Long.min(min, x);
                         max = Long.max(max, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 0,
                                 Long.min(min, n[2].longValue()),
                                 Long.max(max, n[3].longValue())};
                    } else {
                        n  = new Number[]{1L , 0, min, max};
                    }
                    break;
                }
                case FLOATS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                    float  min = x ;
                    float  max = x ;
                    int    j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                         min = Float .min(min, x);
                         max = Float .max(max, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               0,
                               Float .min(min, n[2]. floatValue()),
                               Float .max(max, n[3]. floatValue())};
                    } else {
                        n  = new Number[]{1L , 0, min, max};
                    }
                    break;
                }
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                    double min =  x ;
                    double max =  x ;
                    int    j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                         min = Double.min(min, x);
                         max = Double.max(max, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               0,
                               Double.min(min, n[2].doubleValue()),
                               Double.max(max, n[3].doubleValue())};
                    } else {
                        n  = new Number[]{1L , 0, min, max};
                    }
                    break;
                }
            }
            return  n ;
        }

        @Override
        public Number[] combine(Object v0, Object v1) {
            Number[] w0 = (Number[]) v0;
            Number[] w1 = (Number[]) v1;
            switch (type) {
                case INT :
                case INTS:
                    w0[3] = Int.max(w0[3].intValue(), w1[3].intValue());
                    w0[2] = Int.min(w0[2].intValue(), w1[2].intValue());
                //  w0[1] = Long.sum(w0[1].longValue(), w1[1].longValue());
                    break;
                case LONG :
                case LONGS:
                    w0[3] = Long.max(w0[3].longValue(), w1[3].longValue());
                    w0[2] = Long.min(w0[2].longValue(), w1[2].longValue());
                //  w0[1] = Long.sum(w0[1].longValue(), w1[1].longValue());
                    break;
                case FLOAT :
                case FLOATS:
                    w0[3] = Float.max(w0[3].floatValue(), w1[3].floatValue());
                    w0[2] = Float.min(w0[2].floatValue(), w1[2].floatValue());
                //  w0[1] = Double.sum(w0[1].doubleValue(), w1[1].doubleValue());
                    break;
                case DOUBLE :
                case DOUBLES:
                    w0[3] = Double.max(w0[3].doubleValue(), w1[3].doubleValue());
                    w0[2] = Double.min(w0[2].doubleValue(), w1[2].doubleValue());
                //  w0[1] = Double.sum(w0[1].doubleValue(), w1[1].doubleValue());
                    break;
            }
            w0[0] = Long.sum(w0[0].longValue(), w1[0].longValue());
            return  w0;
        }

    }

    /**
     * 量化(求和)
     */
    public static class Total extends Index<Number[]> {

        public Total(TYPE type, String field, String alias) {
            super (type, field, alias);

            if (type == TYPE.STRING
            ||  type == TYPE.STRINGS ) {
                throw new UnsupportedOperationException("Unsupported type String(s) for " + alias);
            }
        }

        @Override
        public Object collect(int i, Number[] n) throws IOException {
            switch (type) {
                case INT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.longValue();
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 Long.sum( x , n[1].longValue()),
                                 Int .min( x , n[2]. intValue()),
                                 Int .max( x , n[3]. intValue())};
                    } else {
                        n  = new Number[]{1L , x, x, x };
                    }
                    break;
                }
                case LONG: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.longValue();
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 Long.sum( x , n[1].longValue()),
                                 Long.min( x , n[2].longValue()),
                                 Long.max( x , n[3].longValue())};
                    } else {
                        n  = new Number[]{1L , x, x, x };
                    }
                    break;
                }
                case FLOAT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               Double.sum( x , n[1].doubleValue()),
                               Float .min( x , n[2]. floatValue()),
                               Float .max( x , n[3]. floatValue())};
                    } else {
                        n  = new Number[]{1L , x, x, x };
                    }
                    break;
                }
                case DOUBLE: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               Double.sum( x , n[1].doubleValue()),
                               Double.min( x , n[2].doubleValue()),
                               Double.max( x , n[3].doubleValue())};
                    } else {
                        n  = new Number[]{1L , x, x, x };
                    }
                    break;
                }
                case INTS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x = (int ) numValues.nextValue();
                    int  min = x ;
                    int  max = x ;
                    long sum = x ;
                    int  j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = (int ) numValues.nextValue();
                         min = Int .min(min, x);
                         max = Int .max(max, x);
                         sum = Long.sum(sum, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 Long.sum(sum, n[1].longValue()),
                                 Int .min(min, n[2]. intValue()),
                                 Int .max(max, n[3]. intValue())};
                    } else {
                        n  = new Number[]{1L , sum, min, max};
                    }
                    break;
                }
                case LONGS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = (long) numValues.nextValue();
                    long min = x ;
                    long max = x ;
                    long sum = x ;
                    int  j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = (long) numValues.nextValue();
                         min = Long.min(min, x);
                         max = Long.max(max, x);
                         sum = Long.sum(sum, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                                 Long.sum(sum, n[1].longValue()),
                                 Long.min(min, n[2].longValue()),
                                 Long.max(max, n[3].longValue())};
                    } else {
                        n  = new Number[]{1L , sum, min, max};
                    }
                    break;
                }
                case FLOATS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                    float  min = x ;
                    float  max = x ;
                    double sum = x ;
                    int    j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                         min = Float .min(min, x);
                         max = Float .max(max, x);
                         sum = Double.sum(sum, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               Double.sum(sum, n[1].doubleValue()),
                               Float .min(min, n[2]. floatValue()),
                               Float .max(max, n[3]. floatValue())};
                    } else {
                        n  = new Number[]{1L , sum, min, max};
                    }
                    break;
                }
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                    double min = x ;
                    double max = x ;
                    double sum = x ;
                    int    j = 1 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                         min = Double.min(min, x);
                         max = Double.max(max, x);
                         sum = Double.sum(sum, x);
                    }
                    if (n != null) {
                        n  = new Number[]{1L + n[0].longValue() ,
                               Double.sum(sum, n[1].doubleValue()),
                               Double.min(min, n[2].doubleValue()),
                               Double.max(max, n[3].doubleValue())};
                    } else {
                        n  = new Number[]{1L , sum, min, max};
                    }
                    break;
                }
            }
            return  n ;
        }

        @Override
        public Number[] combine(Object v0, Object v1) {
            Number[] w0 = (Number[]) v0;
            Number[] w1 = (Number[]) v1;
            switch (type) {
                case INT :
                case INTS:
                    w0[3] = Int.max(w0[3].intValue(), w1[3].intValue());
                    w0[2] = Int.min(w0[2].intValue(), w1[2].intValue());
                    w0[1] = Long.sum(w0[1].longValue(), w1[1].longValue());
                    break;
                case LONG :
                case LONGS:
                    w0[3] = Long.max(w0[3].longValue(), w1[3].longValue());
                    w0[2] = Long.min(w0[2].longValue(), w1[2].longValue());
                    w0[1] = Long.sum(w0[1].longValue(), w1[1].longValue());
                    break;
                case FLOAT :
                case FLOATS:
                    w0[3] = Float.max(w0[3].floatValue(), w1[3].floatValue());
                    w0[2] = Float.min(w0[2].floatValue(), w1[2].floatValue());
                    w0[1] = Double.sum(w0[1].doubleValue(), w1[1].doubleValue());
                    break;
                case DOUBLE :
                case DOUBLES:
                    w0[3] = Double.max(w0[3].doubleValue(), w1[3].doubleValue());
                    w0[2] = Double.min(w0[2].doubleValue(), w1[2].doubleValue());
                    w0[1] = Double.sum(w0[1].doubleValue(), w1[1].doubleValue());
                    break;
            }
            w0[0] = Long.sum(w0[0].longValue(), w1[0].longValue());
            return  w0;
        }

    }

    /**
     * 求和
     */
    public static class Sum extends Index<Number>  {

        public Sum(TYPE type, String field, String alias) {
            super (type, field, alias);

            if (type == TYPE.STRING
            ||  type == TYPE.STRINGS ) {
                throw new UnsupportedOperationException("Unsupported type String(s) for " + alias);
            }
        }

        @Override
        public Object collect(int i, Number n) throws IOException {
            switch (type) {
                case INT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = numValues.longValue( );
                    if (n != null) {
                        n  = Long.sum(x, n.longValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case LONG: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = numValues.longValue( );
                    if (n != null) {
                        n  = Long.sum(x, n.longValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case FLOAT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                    if (n != null) {
                        n  = Double.sum(x, n.doubleValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case DOUBLE: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                    if (n != null) {
                        n  = Double.sum(x, n.doubleValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case INTS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = 0L;
                    int  j = 0 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Long.sum(numValues.nextValue(), x);
                    }
                    n = n != null ? Long.sum (n.longValue(), x) : x;
                    break;
                }
                case LONGS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x = 0L;
                    int  j = 0 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Long.sum(numValues.nextValue(), x);
                    }
                    n = n != null ? Long.sum (n.longValue(), x) : x;
                    break;
                }
                case FLOATS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = 0D;
                    int    j = 0 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Double.sum((double) NumericUtils. sortableIntToFloat ((int ) numValues.nextValue()), x);
                    }
                    n = n != null ? Double.sum (n.doubleValue(), x) : x;
                    break;
                }
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = 0D;
                    int    j = 0 ;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Double.sum((double) NumericUtils.sortableLongToDouble((long) numValues.nextValue()), x);
                    }
                    n = n != null ? Double.sum (n.doubleValue(), x) : x;
                    break;
                }
            }
            return  n ;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            Number w0 = (Number) v0;
            Number w1 = (Number) v1;
            switch (type) {
                case INT  :
                case INTS :
                case LONG :
                case LONGS:
                    w0 = Long.sum(w0.longValue(), w1.longValue());
                    break;
                case FLOAT  :
                case FLOATS :
                case DOUBLE :
                case DOUBLES:
                    w0 = Double.sum(w0.doubleValue(), w1.doubleValue());
                    break;
            }
            return w0;
        }

    }

    /**
     * 求最大值
     */
    public static class Max extends Index<Number>  {

        public Max(TYPE type, String field, String alias) {
            super (type, field, alias);

            if (type == TYPE.STRING
            ||  type == TYPE.STRINGS ) {
                throw new UnsupportedOperationException("Unsupported type String(s) for " + alias);
            }
        }

        @Override
        public Object collect(int i, Number n) throws IOException {
            switch (type) {
                case INT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.longValue( );
                    if (n != null) {
                        n  = Int . max(x , n. intValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case LONG: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.longValue( );
                    if (n != null) {
                        n  = Long. max(x , n.longValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case FLOAT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                    if (n != null) {
                        n  = Float .max(x,n. floatValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case DOUBLE: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                    if (n != null) {
                        n  = Double.max(x,n.doubleValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case INTS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.nextValue();
                    int  j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Int .max((int ) numValues.nextValue(), x);
                    }
                    n = n != null ? Int .max(n. intValue(), x) : x;
                    break;
                }
                case LONGS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.nextValue();
                    int  j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Long.max((long) numValues.nextValue(), x);
                    }
                    n = n != null ? Long.max(n.longValue(), x) : x;
                    break;
                }
                case FLOATS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                    int    j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Float .max(NumericUtils. sortableIntToFloat ((int ) numValues.nextValue()), x);
                    }
                    n = n != null ? Float .max(n. floatValue(), x) : x;
                    break;
                }
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                    int    j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Double.max(NumericUtils.sortableLongToDouble((long) numValues.nextValue()), x);
                    }
                    n = n != null ? Double.max(n.doubleValue(), x) : x;
                    break;
                }
            }
            return  n ;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            Number w0 = (Number) v0;
            Number w1 = (Number) v1;
            switch (type) {
                case INT :
                case INTS:
                    w0 = Int.max(w0.intValue(), w1.intValue());
                    break;
                case LONG :
                case LONGS:
                    w0 = Long.max(w0.longValue(), w1.longValue());
                    break;
                case FLOAT :
                case FLOATS:
                    w0 = Float.max(w0.floatValue(), w1.floatValue());
                    break;
                case DOUBLE :
                case DOUBLES:
                    w0 = Double.max(w0.doubleValue(), w1.doubleValue());
                    break;
            }
            return w0;
        }

    }

    /**
     * 求最小值
     */
    public static class Min extends Index<Number>  {

        public Min(TYPE type, String field, String alias) {
            super (type, field, alias);

            if (type == TYPE.STRING
            ||  type == TYPE.STRINGS ) {
                    throw new UnsupportedOperationException("Unsupported type String(s) for " + alias);
            }
        }

        @Override
        public Object collect(int i, Number n) throws IOException {
            switch (type) {
                case INT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.longValue( );
                    if (n != null) {
                        n  = Int . min(x , n. intValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case LONG: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.longValue( );
                    if (n != null) {
                        n  = Long. min(x , n.longValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case FLOAT: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                    if (n != null) {
                        n  = Float .min(x,n. floatValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case DOUBLE: {
                    NumericDocValues numValues = ((NumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                    if (n != null) {
                        n  = Double.min(x,n.doubleValue());
                    } else {
                        n  = x;
                    }
                    break;
                }
                case INTS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    int  x =(int ) numValues.nextValue();
                    int  j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Int .min((int ) numValues.nextValue(), x);
                    }
                    n = n != null ? Int .min(n. intValue(), x) : x;
                    break;
                }
                case LONGS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    long x =(long) numValues.nextValue();
                    int  j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Long.min((long) numValues.nextValue(), x);
                    }
                    n = n != null ? Long.min(n.longValue(), x) : x;
                    break;
                }
                case FLOATS: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    float  x = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                    int    j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Float .min(NumericUtils. sortableIntToFloat ((int ) numValues.nextValue()), x);
                    }
                    n = n != null ? Float .min (n. floatValue(), x) : x;
                    break;
                }
                case DOUBLES: {
                    SortedNumericDocValues numValues = ((SortedNumericDocValues) getDocValues());
                //  if (! numValues.advanceExact(i)) {
                //      break;
                //  }
                    double x = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                    int    j = 1;
                    for( ; j < numValues.docValueCount(); j ++) {
                         x = Double.min(NumericUtils.sortableLongToDouble((long) numValues.nextValue()), x);
                    }
                    n = n != null ? Double.min (n.doubleValue(), x) : x;
                    break;
                }
            }
            return  n ;
        }

        @Override
        public Object combine(Object v0, Object v1) {
            Number w0 = (Number) v0;
            Number w1 = (Number) v1;
            switch (type) {
                case INT :
                case INTS:
                    w0 = Int.min(w0.intValue(), w1.intValue());
                    break;
                case LONG :
                case LONGS:
                    w0 = Long.min(w0.longValue(), w1.longValue());
                    break;
                case FLOAT :
                case FLOATS:
                    w0 = Float.min(w0.floatValue(), w1.floatValue());
                    break;
                case DOUBLE :
                case DOUBLES:
                    w0 = Double.min(w0.doubleValue(), w1.doubleValue());
                    break;
            }
            return w0;
        }

    }

    /**
     * 去重计数容器
     */
    private static final class Cnt extends Number implements Supplier<Integer> {
        public Set a = new HashSet();
        public void add(Object v) {
            a.add (v);
        }
        public void addAll(Cnt w) {
            a.addAll ( w.a );
        }
        @Override
        public Integer get() {
            return a.size ();
        }
        @Override
        public int intValue() {
            return get();
        }
        @Override
        public long longValue() {
            return get().longValue();
        }
        @Override
        public float floatValue() {
            return get().floatValue();
        }
        @Override
        public double doubleValue() {
            return get().doubleValue();
        }
        @Override
        public String toString() {
            return Synt.asString(get());
        }
    }

    /**
     * 重度对齐强迫症专用
     */
    private static final class Int {
        public static int  sum(int a, int b) {
            return Integer.sum(a , b);
        }
        public static int  min(int a, int b) {
            return Integer.min(a , b);
        }
        public static int  max(int a, int b) {
            return Integer.max(a , b);
        }
    }

}
