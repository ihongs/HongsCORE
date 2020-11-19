package io.github.ihongs.dh.search;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * 分类统计工具
 * @author Kevin
 */
public class StatisGrader {

    public static enum TYPE {
        INT , LONG , FLOAT , DOUBLE , STRING , // 单数
        INTS, LONGS, FLOATS, DOUBLES, STRINGS  // 复数
    };

    private final IndexSearcher finder;
    private       Query   query ;

    public StatisGrader (IndexSearcher finder) {
        this.finder = finder;
    }

    public StatisGrader where(Query query) {
        this.query = query;
        return this;
    }

    public Map<String, Map<Object, Object[]>> fetch() throws IOException {
        return null;
    }

}
