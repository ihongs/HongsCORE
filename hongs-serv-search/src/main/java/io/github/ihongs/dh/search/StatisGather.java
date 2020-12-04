package io.github.ihongs.dh.search;

import io.github.ihongs.dh.search.StatisHandle.TYPE;
import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.dh.search.StatisGrader.Range;
import io.github.ihongs.dh.search.StatisHandle.Valuer;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.NumericUtils;

/**
 * 聚合统计工具
 * @author Hongs
 */
public class StatisGather {

    private final IndexSearcher finder;
    private       Diman[]       dimans;
    private       Index[]       indics;
    private       Query         query ;

    public StatisGather (IndexSearcher finder) {
        this.finder = finder;
    }

    public StatisGather group(Diman... fields) {
        this.dimans = fields;
        return this;
    }

    public StatisGather count(Index... fields) {
        this.indics = fields;
        return this;
    }

    public StatisGather where(Query    query ) {
        this.query  = query ;
        return this;
    }

    public List<Map> fetch() throws IOException {
        Fetch  fetch = new Fetch(dimans, indics);
        finder.search(query, fetch);
        return fetch . getResult( );
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

    /**
     * 采集器
     */
    public static class Fetch implements Collector, LeafCollector {

        private final Diman[] dimans;
        private final Index[] indics;
        private final Map <Group, Map> dict; // 分组字典
        private final List<       Map> list; // 结果列表

        public Fetch( Diman[] dimans, Index[] indics ) {
            this.dimans = dimans;
            this.indics = indics;
            this.dict = new HashMap();
            this.list = new LinkedList();
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc) throws IOException {
            LeafReader lr = lrc.reader();
            for(Diman diman : dimans) {
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
            Map   entry  = dict.get(group);
            if (  entry == null  ) {
                  entry  = new HashMap();
                  dict.put(group, entry);
                  list.add(       entry);
            for(int i = 0; i < dimans.length; i ++) {
                String n = dimans[i].alias;
                Object v = values[i];
                entry.put(n, v);
            }}

            for(int i = 0; i < indics.length; i ++) {
                String n = indics[i].alias;
                Object v = entry.get ( n );
                v = indics[i].collecs(i,v);
                entry.put(n, v);
            }
        }

        @Override
        public void setScorer(Scorer s) {
            // 不需要打分
        }

        @Override
        public boolean needsScores( ) {
            return false;
        }

        public List<Map> getResult( ) {
            return list ;
        }

    }

    /**
     * 维度字段
     */
    abstract public static class Diman extends Field {

        public Diman(TYPE type, String field, String alias) {
            super(type, field, alias);
        }

        abstract public Object collecs(int i) throws IOException;

    }

    /**
     * 字段取值
     */
    public static class Datum extends Diman {

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
    public static class Scope extends Diman {

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
                //  if (!strValues.advanceExact(i)) {
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
                    n += numValues.docValueCount();
                    break;
                }
                case STRINGS: {
                    SortedSetDocValues     strValues = ((SortedSetDocValues    ) getDocValues());
                //  if (!strValues.advanceExact(i)) {
                //      break;
                //  }
                    while (strValues.nextOrd() != SortedSetDocValues.NO_MORE_ORDS) {
                        n += 1L;
                    }
                    break;
                }
            }
            return  n ;
        }

    }

    /**
     * 量化
     */
    public static class Ratio extends Index<Number[]> {

        public Ratio(TYPE type, String field, String alias) {
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
                    double min =  x ;
                    double max =  x ;
                    double sum = x;
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

    }

    /**
     * 重度对齐强迫症专用
     */
    private static final class Int {
        public static int  min(int a, int b) {
            return Integer.min(a , b);
        }
        public static int  max(int a, int b) {
            return Integer.max(a , b);
        }
    }

}
