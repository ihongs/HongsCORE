package io.github.ihongs.dh.lucene;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.JFigure;
import io.github.ihongs.dh.IReflux;
import io.github.ihongs.dh.lucene.conn.Conn;
import io.github.ihongs.dh.lucene.conn.ConnGetter;
import io.github.ihongs.dh.lucene.conn.DirectConn;
import io.github.ihongs.dh.lucene.query.*;
import io.github.ihongs.dh.lucene.quest.*;
import io.github.ihongs.dh.lucene.stock.*;
import io.github.ihongs.dh.lucene.value.*;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.AlreadyClosedException;

/**
 * Lucene 记录模型
 *
 * <pre>
 * 可选字段配置参数:
 *  lucene-tokenizer    Lucene 分词器类
 *  lucene-char-filter  存储时使用的 CharFilter  类
 *  lucene-token-filter 存储时使用的 TokenFilter 类
 *  lucene-find-filter  查询时使用的 CharFilter  类
 *  lucene-query-filter 查询时使用的 TokenFilter 类
 *  lucene-smart-parse  为 true 可解析查询语句
 *  lucene-light-match  为 true 则词间用或关系
 *  lucene-phrase-slop  短语搜索斜率, 整数
 *  lucene-fuzzy-pre-len  模糊查询前缀长度, 整数
 *  lucene-fuzzy-min-sim  最小的相似匹配度, 小数
 * 另外还有:
 *  lucene-parser-split-on-whitespace
 *  lucene-parser-analyze-range-terms
 *  lucene-parser-allow-leading-wildcard
 *  lucene-parser-lowercase-expanded-terms
 *  lucene-parser-enable-position-increments
 *  lucene-parser-auto-generate-phrase-queries
 * 可以参阅 QueryParserBase 对应方法.
 *
 * 可以在默认配置 default.properties 中的 core.lucene.conn.getter.class 指定连接工厂类, 现有:
 *  io.github.ihongs.dh.lucene.conn.DirectConn$Getter 标准直连, 提交立即写入磁盘
 *  io.github.ihongs.dh.lucene.conn.FlashyConn$Getter 近实时连, 间隔时间写入磁盘
 * </pre>
 *
 * @author Hongs
 */
public class LuceneRecord extends JFigure implements IEntity, IReflux, AutoCloseable {

    protected boolean REFLUX_MODE = false;
    protected int     QUERY_DEPTH = 2    ;

    private String dbpath;
    private String dbname;
    private Conn   dbconn;
    private Map<String, Document> writes = new LinkedHashMap();

    /**
     * 构造方法
     *
     * 注意 : path 务必使用绝对地址
     *
     * @param form 字段配置, 可覆盖 getFields
     * @param path 存储路径, 可覆盖 getDbPath
     * @param name 存储名称, 可覆盖 getDbName
     */
    public LuceneRecord(Map form, String path, String name) {
        setFields (form);
        setDbPath (path);
        setDbName (name);
    }

    /**
     * 获取实例
     * 存储为 conf/form 表单为 conf.form
     * 表单缺失则尝试获取 conf/form.form
     * 实例生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws CruxException
     */
    public static LuceneRecord getInstance(String conf, String form) throws CruxException {
        String code = LuceneRecord.class.getName() +":"+ conf +":"+ form;
        Core   core = Core.getInstance( );
        LuceneRecord  inst = (LuceneRecord) core.get(code);
        if (inst == null) {
            String path = conf +"/"+ form;
            String name = conf +":"+ form;
            Map    fxrm = FormSet.getInstance(conf).getForm(form);

            // 表单配置中可指定数据路径
            Map c = (Map) fxrm.get("@");
            if (c!= null) {
                String p;
                p = (String) c.get("db-path");
                if (null != p && p.length() != 0) {
                    path  = p;
                }
                p = (String) c.get("db-name");
                if (null != p && p.length() != 0) {
                    name  = p;
                }
            }

            inst = new LuceneRecord(fxrm, path, name);
            core.set(code , inst);
        }
        return inst;
    }

    //** 实体方法 **/

    /**
     * 获取数据
     *
     * <pre>
     * 特殊参数:
     * id   ID
     * rn   行数, 明确指定为 0 则不分页
     * gn   分页
     * pn   页码
     * wd   搜索
     * ob   排序
     * rb   字段
     * or   多组"或"关系条件
     * nr   多组"否"关系条件
     * ar   串联多组关系条件
     * 请注意尽量避免将其作为字段名(id,wd除外)
     * </pre>
     *
     * @param rd
     * @return 结构: {list: [], page: {}}
     * @throws CruxException
     */
    @Override
    public Map search(Map rd) throws CruxException {
        // TODO: 临时兼容获取详情
        Object id = rd.get(Cnst.ID_KEY);
        if (id != null && ! "".equals(id) && (id instanceof String || id instanceof Number)) {
            return recite (rd);
        }

        // 默认仅返回可以列举的字段
        Set rb = Synt.declare(rd.get(Cnst.RB_KEY), Set.class);
        if (rb == null || rb.isEmpty( )) {
            rd =  new LinkedHashMap (rd);
            rd.put( Cnst.RB_KEY , getListable() );
        }

        int rn = Synt.declare(rd.get(Cnst.RN_KEY), Cnst.RN_DEF);
        if (rn < 0) {
            throw new CruxException (400 , "Wrong param " + Cnst.RN_KEY);
        }
        int pn = Synt.declare(rd.get(Cnst.PN_KEY), Cnst.PN_DEF);
        if (pn < 0) {
            throw new CruxException (400 , "Wrong param " + Cnst.PN_KEY);
        }

        // 指定行数 0, 则获取全部
        if (rn == 0) {
            Map  data = new HashMap(6);
            List list = getAll(rd);
            data.put("list", list);
            return data;
        }

        // 指定页码 0, 仅获取分页
        int bn  = 0;
        if (pn != 0) {
            bn  = ((pn - 01) * rn);
        }

        Map  resp = new HashMap(6);
        Map  page = new HashMap(5);
        page.put(Cnst.RN_KEY , rn);
        page.put(Cnst.PN_KEY , pn);

        Loop roll = search(rd, bn, rn);

        if (pn != 0) {
            List list = roll.toList( );
            resp.put ( "list" , list );
        }   resp.put ( "page" , page );

        long rc = roll.count();
        long pc = (long) Math.ceil((double) rc / rn);
        int  st = rc>bn ? 1: 0;

        page.put("count", rc );
        page.put("total", pc );
        page.put("state", st );

        return resp;
    }

    /**
     * 获取数据
     *
     * <pre>
     * 特殊参数:
     * id   ID
     * rn   行数, 明确指定为 0 则不返回 page, 为 -1 则 page.count=0 缺失 page.count=1 受限
     * 其他 rb,ar,nr 等同 search
     * </pre>
     *
     * @param rd
     * @return 结构: {info: {}, page: {}}
     * @throws CruxException
     */
    @Override
    public Map recite(Map rd) throws CruxException {
        /**
         * 未指定 id 返回空 map
         * 外部可能仅需选项数据等,
         * 此时无需查询和返回信息.
         * 由动作接口附加选项即可;
         * 例如添加数据之前调接口.
         */
        Object id = Synt.asSingle(rd.get(Cnst.ID_KEY));
        if (id == null || "".equals(id)) {
            return new HashMap(6);
        }

        Map info = getOne  ( rd );

        Map data = new HashMap(6);
        data.put( "info" , info );

        /**
         * 与 list 保持一致, 用 rn 控制 page
         * rn= 1 正常
         * rn= 0 不给 page
         * rn=-1 返回 page.count=0 缺失 page.count=1 受限
         */
        int rn = Synt.declare(rd.get(Cnst.RN_KEY), 1);
        if (rn == 0) {
            return data ;
        }

        Map page = new HashMap(2);
        data.put( "page" , page );

        /**
         * 查不到可能是不存在、已删除或受限
         * 需通过 id 再查一遍，区分不同错误
         */
        page.put(Cnst.RN_KEY, rn);
        if (null != info && ! info.isEmpty()) {
            page.put("state", 1 );
            page.put("count", 1 );
        } else
        if (rn >= 1 ) {
            page.put("state", 0 );
            page.put("count", 0 );
        } else
        if (null != getDoc( id.toString( ) )) {
            page.put("state", 0 );
            page.put("count", 1 );
        }  else
        {
            page.put("state", 0 );
            page.put("count", 0 );
        }

        return data;
    }

    /**
     * 创建记录
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public String create(Map rd) throws CruxException {
        /**
         * 外部用户不可指定 id
         * 想要指定只能使用 add
         */
        if (rd.containsKey(Cnst.ID_KEY)) {
            rd.remove     (Cnst.ID_KEY);
        }

        return add(rd);
    }

    /**
     * 更新记录
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public int update(Map rd) throws CruxException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit (rd, ids , 1096);
        int c = 0;
        for(String  id  : ids) {
            c+= put(id  , rd );
        }
        return  c;
    }

    /**
     * 删除记录
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public int delete(Map rd) throws CruxException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit (rd, ids , 1097);
        int c = 0;
        for(String  id  : ids) {
            c+= del(id  /**/ );
        }
        return  c;
    }

    /**
     * 确保操作合法
     * @param rd
     * @param ids
     * @param ern
     * @throws CruxException 错误码 1096 更新, 1097 删除, 1098 查询
     */
    protected void permit(Map rd, Set ids, int ern) throws CruxException {
        if (rd  == null) {
            throw new NullPointerException( "rd can not be null" );
        }
        if (ids == null || ids.isEmpty()) {
            throw new NullPointerException("ids can not be empty");
        }

        Map wh = new HashMap();
        if (rd.containsKey(Cnst.AR_KEY)) {
            wh.put(Cnst.AR_KEY, rd.get(Cnst.AR_KEY));
        }
        if (rd.containsKey(Cnst.OR_KEY)) {
            wh.put(Cnst.OR_KEY, rd.get(Cnst.OR_KEY));
        }
        if (wh.isEmpty()) {
            return;
        }

        // 组织查询
        wh.put(Cnst.ID_KEY, ids);
        wh.put(Cnst.RB_KEY, Cnst.ID_KEY);
        Set idz = new HashSet( );
        Loop rs = search(wh,0,0);
        while  (  rs.hasNext() ) {
            Map ro = rs.next();
            idz.add( ro.get(Cnst.ID_KEY).toString());
        }

        // 对比数量, 取出多余的部分作为错误消息抛出
        if (ids.size( ) != idz.size( ) ) {
            Set    zd = new HashSet(ids);
                   zd . removeAll  (idz);
            String er = zd.toString(  );
            if (ern == 1096) {
                throw new CruxException(ern, "Can not update by id: " + er);
            } else
            if (ern == 1097) {
                throw new CruxException(ern, "Can not delete by id: " + er);
            } else
            {
                throw new CruxException(ern, "Can not search by id: " + er);
            }
        }
    }

    /**
     * 唯一键值约束
     * @param nd
     * @throws CruxException 错误码 1088
     */
    protected void unique(Map nd, String id) throws CruxException {
        /**
         * 有 data-ut 表示其由外部进行检查
         * 多组唯一用 data-uk-xxx 进行定义
         * 至少有一组 data-uk
         */
        Map fp = getParams();
        if (fp.containsKey("data-ut")
        || !fp.containsKey("data-uk")) {
            return;
        }

        for(Object o : fp.entrySet( )) {
            Map.Entry e = (Map.Entry) o;
            String fn = e.getKey().toString();

            if (fn.startsWith("data-uk-")) {
                fn = "UK-"+fn.substring(8);
            } else
            if (fn.equals/**/("data-uk" )) {
                fn = "UK";
            } else {
                continue ;
            }

            // 组织查询
            Object uk = e.getValue( );
            Set us = Synt.toTerms(uk);
            Map rd = new HashMap (us.size());
            for(Object n : us) {
                Object v = nd.get(n );

                if (v == null) {
                    rd.put(n, Synt.mapOf(Cnst.IS_REL, "null"));
                } else {
                    rd.put(n, Synt.mapOf(Cnst.EQ_REL,   v   ));
                }
            }

            // 排除主键
            if (id != null) {
                rd.put(Cnst.ID_KEY, Synt.mapOf(Cnst.NE_REL, id));
            }

            if (search(rd, 0, 1).size() > 0 ) {
                throw new CruxException(1088, "UNIQUE KEY $0 ($1)", fn, Syno.concat(",", us));
            }
        }
    }

    //** 模型方法 **/

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws CruxException
     */
    public String add(Map rd) throws CruxException {
        String id = Synt.asString(rd.get(Cnst.ID_KEY));
        if (id == null || id.length() == 0) {
            id  =  Core.newIdentity();
            rd.put(Cnst.ID_KEY , id );
        }
        unique(rd, null);
        addDoc(id, padDoc(rd) );
        return id;
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @return 1
     * @throws CruxException
     */
    public int put(String id, Map rd) throws CruxException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in put");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            throw new NullPointerException("Doc#"+id+" not exists");
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            Map md = padDat(doc );
                md . putAll( rd );
                rd = md;
        }
        rd.put(Cnst.ID_KEY , id );
        unique( rd , id );
        setDoc( id , padDoc (rd)); // 总是新建 Document
        return  1;
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @return 1
     * @throws CruxException
     */
    public int set(String id, Map rd) throws CruxException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in set");
        }
        Document doc = getDoc(id);
        if (doc == null) {
//          throw new NullPointerException("Doc#"+id+" not exists");
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            Map md = padDat(doc );
                md . putAll( rd );
                rd = md;
        }
        rd.put(Cnst.ID_KEY , id );
        unique( rd , id );
        setDoc( id , padDoc (rd)); // 总是新建 Document
        return  1;
    }

    /**
     * 删除文档(delDoc 的别名)
     * @param id
     * @return 有 1, 无 0
     * @throws CruxException
     */
    public int del(String id) throws CruxException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in del");
        }
        Document doc = getDoc(id);
        if (doc == null) {
//          throw new NullPointerException("Doc#"+id+" not exists");
            return 0; // 删除为幂等, 只是无变化
        }
        delDoc(id);
        return 1;
    }

    /**
     * 获取文档信息
     * @param id
     * @return
     * @throws CruxException
     */
    public Map get(String id) throws CruxException {
        Document doc = getDoc(id);
        if (doc != null) {
            return padDat ( doc );
        } else {
            return new HashMap( );
        }
    }

    /**
     * 获取单个文档
     * @param rd
     * @return
     * @throws CruxException
     */
    public Map getOne(Map rd) throws CruxException {
        Loop roll = search(rd, 0, 1);
        if   (   roll.hasNext( )) {
            return  roll.next( );
        } else {
            return new HashMap();
        }
    }

    /**
     * 获取全部文档
     * @param rd
     * @return
     * @throws CruxException
     */
    public List getAll(Map rd) throws CruxException {
        Loop roll = search(rd, 0, 0);
        List list = new ArrayList(roll.size());
        while  (  roll.hasNext()) {
            list.add(roll.next());
        }
        return list;
    }

    /**
     * 搜索查询文档
     * @param rd
     * @param begin 起始位置
     * @param limit 获取限制
     * @return
     * @throws CruxException
     */
    public Loop search(Map rd, int begin, int limit) throws CruxException {
        Query q = padQry(rd);
        Sort  s = padSrt(rd);
        Set   r = Synt.toTerms (rd.get(Cnst.RB_KEY));

        // 清理字段列表
        // 限定查询范围
        if (r  != null ) {
        if (r.isEmpty()) {
            r   = null ;
        } else {
            Set doci = this.getFields().keySet();
            Set z = new HashSet(doci.size() + 2);
            z.addAll(doci);
            z.add("__docid__");
            z.add("__score__");
            z.retainAll(  r  );
            r = z;
        }}

        Loop  l = new Loop(this, q,s,r, begin,limit);

        if ( 4 == (4 & Core.DEBUG) ) {
            CoreLogger.debug("LuceneRecord.search: " + l.toString());
        }

        return l;
    }

    //** 组件方法 **/

    public void addDoc(String id, Document doc) throws CruxException {
        writes.put(id, doc);
        if (!REFLUX_MODE) {
            commit();
        }
        /* // 不再直接写入
        IndexWriter iw = getWriter();
        try {
            iw.addDocument (doc);
        } catch (IOException ex) {
            throw new CruxException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
        */
    }

    public void setDoc(String id, Document doc) throws CruxException {
        writes.put(id, doc);
        if (!REFLUX_MODE) {
            commit();
        }
        /* // 不再直接写入
        IndexWriter iw = getWriter();
        try {
            iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), doc);
        } catch (IOException ex) {
            throw new CruxException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
        */
    }

    public void delDoc(String id) throws CruxException {
        writes.put(id,null);
        if (!REFLUX_MODE) {
            commit();
        }
        /* // 不再直接写入
        IndexWriter iw = getWriter();
        try {
            iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)     );
        } catch (IOException ex) {
            throw new CruxException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
        */
    }

    public Document getDoc(String id) throws CruxException {
        // 从预备写入缓冲区中读取
        if (writes.containsKey(id)) {
            return  writes.get(id);
        }

        IndexSearcher  ff = getFinder( );
        try {
                 Term  tr = new Term ("@" + Cnst.ID_KEY, id);
                Query  qr = new TermQuery(tr);
              TopDocs  td = ff.search(qr, 1 );
            ScoreDoc[] sd = td.scoreDocs;
            if  (sd.length > 0 ) {
                return ff.storedFields().document(sd[0].doc);
            } else {
                return null;
            }
        } catch (IOException ex) {
            throw new Lost ( ex);
        } catch (AlreadyClosedException ex) {
            throw new Lost ( ex);
        }
    }

    public Map padDat(Document doc, Set rb) {
        int len;
        if (rb != null) {
        if ( ! rb.isEmpty()) {
            len = rb.size( );
        }  else {
            rb  = null;
            len = getFields( ).size(   );
        }} else {
            len = getFields( ).size(   );
        }
        Map map = new LinkedHashMap(len);
        padDat(doc, map, rb  );
        return map;
    }

    public Map padDat(Document doc) {
        int len = getFields( ).size(   );
        Map map = new LinkedHashMap(len);
        padDat(doc, map, null);
        return map;
    }

    public Document padDoc(Map map) {
        Document doc = new Document();
        padDoc(doc, map, null);
        return doc;
    }

    public Query padQry(Map rd) throws CruxException {
        BooleanQuery.Builder qb = new Queries();

        padQry(qb, rd);

        BooleanQuery qu = qb.build();
        if (! qu.clauses().isEmpty()) {
            return qu ;
        }

        return new MatchAllDocsQuery( );
    }

    public Sort  padSrt(Map rd) throws CruxException {
        List<SortField> sr = new ArrayList();

        padSrt(sr, rd);

        if (! sr.isEmpty()) {
            SortField[] sf;
            sf  =  new SortField [sr.size()];
            return new Sort (sr.toArray(sf));
        }

        return null;
    }

    //** 组件封装 **/

    /**
     * 填充返回数据(将 doc 填充到 map)
     * 可覆盖此方法补充额外数据
     *
     * @param doc
     * @param map
     * @param rep
     */
    protected void padDat(Document doc, Map map, Set rep) {
        if (rep != null && rep.isEmpty( )) {
            rep  = null;
        }

        Map<String, Map> fields = getFields();
        for(Map.Entry<String, Map> e : fields.entrySet()) {
            Map    m = e.getValue();
            String k = e.getKey  ();

            if (rep != null
            && !rep.contains(k)) {
                continue;
            }

            if (k == null
            ||  k.equals("@")
            ||  unstored( m )
            ||  inviable( m )
            ||  invisble( m )) {
                continue;
            }

            IValue  v ;
            String  t = datatype(m);
            boolean r = repeated(m);
            IndexableField[] fs = doc.getFields(k);

            if (t != null) switch (t) {
            case "search":
            case "sorted":
                continue; // 纯功能字段无可见值
            case "date":
                /**
                 * 有格式则返回时间字串
                 * 没格式则返回时间对象
                 * 时间戳类型则返回数值
                 */
                String y = Synt.declare( m.get( "type" ), "");
                if ("date".equals(y) || "datestamp".equals(y)) {
                String f = Synt.declare( m.get("format"), "");
                if (f != null && !f.isEmpty()) {
                    v  = new DatextValue(m);
                } else {
                    v  = new DatimeValue(m);
                }
                } else {
                    v  = new NumberValue( );
                }
                break;
            case "int":
            case "long":
            case "float":
            case "double":
            case "number":
                v = new NumberValue();
                break;
            case "object":
                v = new ObjectValue();
                break;
            default:
                v = new StringValue();
            } else {
                v = new StringValue();
            }

            if (r) {
                if (fs.length > 0) {
                    for(IndexableField f : fs ) {
                        Dict.put(map , v.get(f), k, null);
                    }
                } else {
                    map.put(k , new ArrayList());
                }
            } else {
                if (fs.length > 0) {
                    map.put(k , v.get ( fs[0] ));
                } else {
                    map.put(k , null);
                }
            }
        }
    }

    /**
     * 填充存储数据(将 map 填充到 doc)
     * 可覆盖此方法补充额外数据
     *
     * @param doc
     * @param map
     * @param rep
     */
    protected void padDoc(Document doc, Map map, Set rep) {
        if (rep != null && rep.isEmpty( )) {
            rep  = null;
        }

        Map<String, Map> fields = getFields();
        for(Map.Entry<String, Map> e : fields.entrySet()) {
            Map    m = e.getValue();
            String k = e.getKey  ();
            Object v = Dict.getParam(map , k);

        //  doc.removeFields(k); // 总是新文档, 不需要清理

            if (rep != null
            && !rep.contains(k)) {
                continue;
            }

            if (v == null
            ||  k == null
            ||  k.equals("@")
            ||  unstored( m )) {
                continue;
            }

            IStock  f ;
            String  t = datatype(m);
            boolean r = repeated(m);
            boolean s = sortable(m);
            boolean p = srchable(m);
            boolean q = findable(m);
            boolean g = ! inviable(m);

            if (Cnst.ID_KEY.equals(k)) {
                s  = true;
                q  = true;
                g  = true;
            }

            if (t != null) switch (t)  {
            case "int":
                if ("".equals(v)) continue;
                f = new IntStock();
                p = false;
                break;
            case "long":
            case "date":
                if ("".equals(v)) continue;
                f = new LongStock();
                p = false;
                break;
            case "float":
                if ("".equals(v)) continue;
                f = new FloatStock();
                p = false;
                break;
            case "double":
            case "number":
                if ("".equals(v)) continue;
                f = new DoubleStock();
                p = false;
                break;
            case "sorted":
                if ("".equals(v)) continue;
                f = new DoubleStock();
                s = true ;
                g = false; // 排序类型无需存储
                p = false; // 排序类型无法搜索
                q = false; // 排序类型无法筛选
                break;
            case "search":
                f = new StringStock();
                p = true ;
                g = false; // 搜索类型无需存储
                q = false; // 搜索类型无法筛选
                s = false; // 搜索类型无法排序
                break;
            case "stored":
                f = new StringStock();
                g = true ;
                p = false; // 存储类型无法搜索
                q = false; // 存储类型无法筛选
                s = false; // 存储类型无法排序
                break;
            case "object":
                if ("".equals(v)) continue;
                f = new ObjectStock();
                g = true ;
                p = false; // 对象类型无法搜索
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
                break;
            case "vector":
                if ("".equals(v)) continue;
                f = new VectorStock();
                q = true ; // 向量借用筛选
                p = false; // 向量类型无法搜索
                s = false; // 向量类型无法排序
                break;
            default:
                f = new StringStock();
            } else {
                f = new StringStock();
            }

            /**
             * 2021/04/18
             * 为在一个库里存多个表
             * 不再在开始预设分析器
             * 改为存字段时直接写入 TokenStream
             */
            if (p && f instanceof StringStock) {
                try {
                    ((StringStock) f).analyzer(getAnalyzer(m));
                } catch ( CruxException x) {
                    throw x.toExemption( );
                }
            }

            /**
             * 2024/11/11
             * 指定向量指定近似函数
             * 同时检查数据存储维数
             */
            if (q && f instanceof VectorStock) {
                try {
                    ((VectorStock) f).similar (getSimilar (m));
                } catch ( CruxException x) {
                    throw x.toExemption( );
                }

                float[] w;
                try {
                    w = VectorQuest.toVector(v);
                } catch ( ClassCastException x) {
                    throw new  CruxExemption(400, "Vector value for field `$0` is invalid, $1", k, x.getMessage());
                }
                int d = Synt.declare(m.get("vector-dimension"), 0);
                if (d > 0 && d != w.length) {
                    throw new  CruxExemption(400, "Vector dimension for field `$0` is $1, but $2", k, d, w.length);
                }
                v = w ;
            }

            if (r) {
                if (g) {
                    if (v instanceof Object [ ]) {
                        for (Object w: (Object [ ]) v) {
                            doc.add(f.get(k, w));
                        }
                    } else
                    if (v instanceof Collection) {
                        for (Object w: (Collection) v) {
                            doc.add(f.get(k, w));
                        }
                    } else
                    {
                        Set a = Synt.asSet ( v );
                        for (Object w: a ) {
                            doc.add(f.get(k, w));
                        }
                        v = a;
                    }
                }

                Set a  = Synt.asSet( v );
                if (a != null && ! a.isEmpty( )) {
                if (s) {
                    boolean   b = true ;
                    for (Object w: a) {
                    if  (b) { b = false;
                        doc.add(f.odr(k, w));
                    }   doc.add(f.ods(k, w));
                    }
                }
                if (q) {
                    for (Object w: a) {
                        doc.add(f.whr(k, w));
                    }
                }
                if (p) {
                    doc.add(f.wdr(k, getSrchText(m, a)));
                }
                if (g && !q) { // 仅存储的仍可判断空/非空/空串
                    doc.add(f.whr(k, v.equals("") ? "" : "0"));
                }}
            } else
            {
                if (g) {
                    doc.add(f.get(k, v));
                }
                if (s) {
                    doc.add(f.odr(k, v));
                }
                if (q) {
                    doc.add(f.whr(k, v));
                }
                if (p) {
                    doc.add(f.wdr(k, getSrchText(m, v)));
                }
                if (g && !q) { // 仅存储的仍可判断空/非空/空串
                    doc.add(f.whr(k, v.equals("") ? "" : "0"));
                }
            }
        }

        // 临时清理, 规避 Document.getField/removeField 等抛 NullPointerException
        Iterator<IndexableField>  it = doc.iterator();
        while (it.hasNext( )) {
           IndexableField field = it.next();
           if (field == null) {
               it.remove ( );
           }
        }
    }

    /**
     * 组织查询条件
     * 可覆盖此方法扩展顶层条件
     *
     * @param qr
     * @param rd
     * @throws CruxException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd) throws CruxException {
        try {
            padQry(qr, rd, 0);
        } catch (IndexSearcher.TooManyClauses | ClassCastException | NullPointerException ex) {
            throw new CruxException(ex, 400);
        }
    }

    /**
     * 组织查询条件
     * 可覆盖此方法扩展查询条件
     *
     * @param qr
     * @param rd
     * @param rl 递归层级
     * @throws CruxException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd, int rl) throws CruxException {
        Map<String, Map> fields = getFields();
        Set<String> ks = new LinkedHashSet(fields.keySet());
                    ks.retainAll(rd.keySet());

        for(String k : ks) {
            Object v = rd .get(k);
            if (v == null) {
                continue;
            }

            // 自定义条件
            if (padQry(qr, rd, k, v)) {
                continue;
            }

            Map m = fields.get(k);
            if (m == null) {
                continue;
            }

            /**
             * 注意: 此处并未像排序那样检查 findable,srchable,rankable.
             * 若字段不支持查询、搜索或区间,
             * 在调用查询类方法时自然会报错,
             * 是否可查完全由存储时分类处理.
             */

            IQuest qa ;
            String t  =  datatype (m);
            if (t != null) switch (t) {
            case "int":
                qa = new IntQuest();
                break;
            case "long":
            case "date":
                qa = new LongQuest();
                break;
            case "float":
                qa = new FloatQuest();
                break;
            case "double":
            case "number":
                qa = new DoubleQuest();
                break;
            case "object":
            case "stored":
                // 可以查询有无
                qa = new StringQuest();
                break;
            case "vector":
                qa = new VectorQuest();
                break;
            default:
                // 区分能否搜索
                if ( ! srchable(m)) {
                    qa = new StringQuest();
                } else {
                    SearchQuest qs;
                    Analyzer    a ;
                    qs = new SearchQuest();
                    a  = getAnalyser(m);
                    qs.analyser(a);
                    qs.settings(m);
                    qa = qs;
                }
            } else {
                // 区分能否搜索
                if ( ! srchable(m)) {
                    qa = new StringQuest();
                } else {
                    SearchQuest qs;
                    Analyzer    a ;
                    qs = new SearchQuest();
                    a  = getAnalyser(m);
                    qs.analyser(a);
                    qs.settings(m);
                    qa = qs;
                }
            }

            //** 向量查询 **/

            if (qa instanceof VectorQuest) {
                VectorQuest qv = (VectorQuest) qa;
                int   n = 0x0 ;
                float u = 0.1f;
                if (v instanceof Map) {
                    Map vd = (Map) v;
                    n = Synt.declare(vd.get(Cnst.RN_KEY), n);
                    u = Synt.declare(vd.get(Cnst.UP_REL), u);
                    v = vd.get(Cnst.AT_REL);
                }
                if (v.equals("")) continue ;

                float[] w;
                try {
                    w = VectorQuest.toVector(v);
                } catch ( ClassCastException x) {
                    throw new  CruxExemption(400, "Vector value for field `$0` is invalid, $1", k, x.getMessage());
                }
                int d = Synt.declare(m.get("vector-dimension"), 0);
                if (d > 0 && d != w.length) {
                    throw new  CruxExemption(400, "Vector dimension for field `$0` is $1, but $2", k, d, w.length);
                }

                try {
                    if (n > 0) {
                        qr.add(qv.vtr(k, w, n), BooleanClause.Occur.MUST);
                    } else {
                        qr.add(qv.vtr(k, w, u), BooleanClause.Occur.MUST);
                    }
                } catch (IllegalArgumentException e) { // 维数不符
                    throw new CruxException(e , 400);
                }
                continue;
            }

            //** 常规查询 **/

            Map vd ;
            if (v instanceof Map) {
                vd = (Map) v ;
            } else
            if (v instanceof Collection
            ||  v instanceof Object[ ]) {
                Set vs = Synt.asSet(v);
                    vs.remove("");
                if(!vs.isEmpty( )) {
                    BooleanQuery.Builder  qx = new BooleanQuery.Builder();
                    for(Object vv : vs) {
                        qx.add(qa.whr(k, vv), BooleanClause.Occur.SHOULD);
                    }
                        qr.add(qx.build (  ), BooleanClause.Occur.MUST  );
                }
                continue;
            } else {
                if (!v.equals("")) {
                    qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
                }
                continue;
            }

            //** 条件关系 **/

            /*
            // 因可更改符号含义, 可能造成严重漏洞, 故废弃
            v = vd.get(Cnst.OR_REL);
            if (Cnst.OR_KEY.equals(v )
            ||  Cnst.NR_KEY.equals(v)) {
                List a = new ArrayList ( vd.size() - 1 );
                for(Object ot : vd.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    Object r2 = et.getKey  ();
                    if (Cnst.OR_KEY.equals(r2)) continue;
                    Object v2 = et.getValue();
                    a.add(Synt.mapOf(k, Synt.mapOf(r2, v2)));
                }
                if (! a.isEmpty() ) {
                    padQry(qr , Synt.mapOf(v, a) , r);
                }
                continue;
            }
            */

            //** 空值查询 **/

            v = vd.get(Cnst.IS_REL);
            if ( v != null && !"".equals(v) ) {
                String a = Synt.asString(v).toUpperCase( );
                String b = inviable(m) && srchable(m) && ! findable(m) ? "$" : "@"; // 可搜素、不可见、不可查询，则用搜素字段
                Query  p ;
                // try {
                switch (a) {
                    case "NOT-NULL" :
                        p = new IsNotNull(b + k);
                        qr.add(p, BooleanClause.Occur.MUST);
                        break;
                    case "NULL" :
                        p = new IsNotNull(b + k);
                        qr.add(p, BooleanClause.Occur.MUST_NOT);
                        break;
                    case "NOT-NONE" :
                        p = new IsNotNone(b + k);
                        qr.add(p, BooleanClause.Occur.MUST);
                        break;
                    case "NONE" :
                        p = new IsNotNone(b + k);
                        qr.add(p, BooleanClause.Occur.MUST_NOT);
                        break;
                    case "EMPTY":
                        p = new IsEmpty(b + k);
                        qr.add(p, BooleanClause.Occur.MUST);
                        break;
                    case "NOT-EMPTY":
                        p = new IsEmpty(b + k);
                        qr.add(p, BooleanClause.Occur.MUST_NOT);
                        break;
                    default:
                        throw new CruxException(400, "Unsupported `is`: "+v);
                }
            }

            //** 精确匹配 **/

            v = vd.get(Cnst.EQ_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
            }
            v = vd.get(Cnst.NE_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST_NOT);
            }

            //** 模糊匹配 **/

            v = vd.get(Cnst.SE_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST);
            }
            v = vd.get(Cnst.NS_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST_NOT);
            }

            //** 集合查询 **/

            // IN/NI/MI 可以拆字符串, 如 fn.in=1,2 同 fn.in.=1&fn.in.=2
            v = vd.get(Cnst.ON_REL);
            if ( v != null ) {
                Set vs = Synt.toSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST);
                    }
                }
            }
            v = vd.get(Cnst.NO_REL);
            if ( v != null ) {
                Set vs = Synt.toSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST_NOT);
                    }
                }
            }
            v = vd.get(Cnst.IN_REL);
            if ( v != null ) {
                Set vs = Synt.toSet(v);
                if(!vs.isEmpty( )) {
                    BooleanQuery.Builder  qx = new BooleanQuery.Builder();
                    for(Object vv : vs) {
                        qx.add(qa.whr(k, vv), BooleanClause.Occur.SHOULD);
                    }
                        qr.add(qx.build (  ), BooleanClause.Occur.MUST  );
                }
            }

            //** 区间查询 **/

            Object  n, x;
            boolean l, g;

            n = vd.get(Cnst.GT_REL);
            if (n != null) {
                l  = false;
            } else {
            n = vd.get(Cnst.GE_REL);
            if (n != null) {
                l  = true ;
            } else {
                n  = null ;
                l  = true ;
            }}

            x = vd.get(Cnst.LT_REL);
            if (x != null) {
                g  = false;
            } else {
            x = vd.get(Cnst.LE_REL);
            if (x != null) {
                g  = true ;
            } else {
                x  = null ;
                g  = true ;
            }}

            if ((n != null && ! "".equals(n))
            ||  (x != null && ! "".equals(x))) {
                Query  qu = qa.whr( k, n, x, l, g );
                qr.add(qu,BooleanClause.Occur.MUST);
            }

            v = vd.get(Cnst.AT_REL);
            if (v != null) {
            Set s  = Synt.asSet (v);
            if (s != null && ! s.isEmpty()) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();

                for(Object o : s ) {
                    Object[] a = Synt.toRange(o);
                    if (a == null) {
                        continue ;
                    }

                    n = a[0]; l = (boolean) a[2];
                    x = a[1]; g = (boolean) a[3];
                    if (n == null
                    &&  x == null) {
                        continue ;
                    }

                       Query qu = qa.whr (k , n , x , l , g);
                    qx.add ( qu, BooleanClause.Occur.SHOULD);
                }

                BooleanQuery qz = qx.build();
                if (qz.clauses().size() > 0) {
                    qr.add ( qz, BooleanClause.Occur.MUST  );
                }
            }
            }
        }

        Object v;

        //** 全局搜索 **/

        v = rd.get(Cnst.WD_KEY);
        if ( v != null && ! "".equals(v) ) {
            Set<String> fs = getRschable();
            if (fs.size() > 0) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                 SearchQuest         qs = new  SearchQuest        ();

                for(String fn : fs) {
                    Map fc  = fields.get(fn);
                    if (fc == null) continue;
                    qs.settings( /*parser*/ fc );
                    qs.analyser(getAnalyser(fc));
                    qx.add(qs.wdr(fn, v),BooleanClause.Occur.SHOULD);
                }

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    qr.add(qa, BooleanClause.Occur.MUST  );
                }
            }
        }

        //** 递归条件 **/

        v = rd.get(Cnst.AR_KEY);
        if ( v != null ) {
            if (rl > QUERY_DEPTH) {
                throw new CruxException(400, "Key '" + Cnst.AR_KEY + "' can not exceed " + QUERY_DEPTH + " layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                if ( map == null) continue; // 规避 NullPointerException

                BooleanQuery.Builder qx = new Queries();
                padQry(qx, map, rl + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.UP_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST);
                }
            }
            }
        }

        v = rd.get(Cnst.NR_KEY);
        if ( v != null ) {
            if (rl > QUERY_DEPTH) {
                throw new CruxException(400, "Key '" + Cnst.NR_KEY + "' can not exceed " + QUERY_DEPTH + " layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                if ( map == null) continue; // 规避 NullPointerException

                BooleanQuery.Builder qx = new Queries();
                padQry(qx, map, rl + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.UP_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST_NOT);
                }
            }
            }
        }

        v = rd.get(Cnst.OR_KEY);
        if ( v != null ) {
            if (rl > QUERY_DEPTH) {
                throw new CruxException(400, "Key '" + Cnst.OR_KEY + "' can not exceed " + QUERY_DEPTH + " layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
                BooleanQuery.Builder qz = new BooleanQuery.Builder();
            for(Map  map : set) {
                if ( map == null) continue; // 规避 NullPointerException

                BooleanQuery.Builder qx = new Queries();
                padQry(qx, map, rl + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.UP_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qz.add(qb, BooleanClause.Occur.SHOULD);
                }
            }
                BooleanQuery qa = qz.build();
                if (! qa.clauses().isEmpty()) {
                    qr.add(qa, BooleanClause.Occur.MUST  );
                }
            }
        }
    }

    /**
     * 自定义查询条件
     * 可覆盖此方便补充特殊条件
     *
     * @param qr
     * @param rd
     * @param k 查询字段
     * @param v 字段取值
     * @return  自定义返 true
     */
    protected boolean padQry(BooleanQuery.Builder qr, Map rd, String k, Object v) {
        return false;
    }

    /**
     * 组织排序规则
     * 可覆盖此方法进行补充排序
     *
     * @param sr
     * @param rd
     * @throws CruxException
     */
    protected void padSrt(List<SortField> sr, Map rd) throws CruxException {
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (null != ob && !ob.isEmpty()) {
            padSrt( sr , rd , ob );
        }
    }

    /**
     * 组织排序规则
     * 可覆盖此方法进行补充排序
     *
     * @param sr
     * @param rd
     * @param ob 排序字段
     * @throws CruxException
     */
    protected void padSrt(List<SortField> sr, Map rd, Set ob) throws CruxException {
        Map<String, Map> fields = getFields();

        for(Object fx : ob) {
            String fn = Synt.asString(fx);

            // 相关
            if (fn.equals("-")) {
                sr.add(SortField.FIELD_SCORE);
                continue;
            }

            // 文档
            if (fn.equals("_")) {
                sr.add(SortField.FIELD_DOC);
                continue;
            }

            // 逆序
            boolean  rv;
            if (fn.  endsWith("!") ) { // 新的逆序后缀
                fn = fn.substring(0, fn.length() - 1);
                rv = true ;
            } else
            if (fn.startsWith("-") ) { // 旧的逆序前缀
                fn = fn.substring(1);
                rv = true ;
            } else {
                rv = false;
            }

            // 自定义排序
            if (padSrt(sr,rd, fn,rv)) {
                continue;
            }

            Map m  = fields.get( fn );
            if (m == null
            ||  ! sortable(m)) {
                continue;
            }

            SortField.Type st;
            String t  =  datatype (m);
            if (t != null) switch (t) {
            case "int":
                st = SortField.Type.INT;
                break;
            case "long":
            case "date":
                st = SortField.Type.LONG;
                break;
            case "float":
                st = SortField.Type.FLOAT;
                break;
            case "double":
            case "number":
                st = SortField.Type.DOUBLE;
                break;
            case "string":
            case "search":
                st = SortField.Type.STRING;
                break;
            case "sorted":
                st = SortField.Type.LONG; // 专用排序类型
                break;
            default:
                CoreLogger.warn("Field {} in {} can not be sorted", fn, dbname);
                continue;
            } else {
                CoreLogger.warn("Field {} in {} can not be sorted", fn, dbname);
                continue;
            }

            /**
             * 因为 Lucene 5 必须使用 DocValues 才能排序
             * 在更新数据时, 会加前缀 '#','%' 的排序字段
             * 2020/03/07 多个值的普通排序只能用其中一个
             */
        //  if (repeated(m)) {
        //      sr.add(new SortField("%" + fn, st, rv));
        //  } else {
                sr.add(new SortField("#" + fn, st, rv));
        //  }
        }
    }

    /**
     * 自定排序规则
     * 可覆盖此方法进行特殊排序
     *
     * @param sr
     * @param rd
     * @param k 排序字段
     * @param r 是否逆序
     * @return  自定义返 true
     */
    protected boolean padSrt(List<SortField> sr, Map rd, String k, boolean r) {
        return false;
    }

    //** 事务方法 **/

    /**
     * 销毁读写连接
     */
    @Override
    public void close() {
        // 提交变更
        if (REFLUX_MODE) {
            try {
            try {
                commit();
            } catch (Throwable e) {
                cancel();
                throw e ;
            }
            } catch (Throwable e) {
                CoreLogger.error(e);
            }
        }

        // 释放连接
        if (dbconn != null) {
            try {
                dbconn.close();
            } catch (Exception e) {
                CoreLogger.error(e);
            }
        }
    }

    /**
     * 事务开始
     */
    @Override
    public void begin() {
        if (REFLUX_MODE
        && !writes.isEmpty()) {
            throw new CruxExemption(1054, "Uncommitted changes");
        }
        REFLUX_MODE = true ;
    }

    /**
     * 提交更改
     */
    @Override
    public void commit() {
        REFLUX_MODE = false;
        if (writes.isEmpty()) {
            return;
        }
        try {
            getDbConn().write(writes);
        } catch (IOException ex) {
            throw new CruxExemption(ex, 1055);
        } finally {
            writes.clear();
        }
    }

    /**
     * 回滚操作
     */
    @Override
    public void cancel() {
        REFLUX_MODE = false;
        if (writes.isEmpty()) {
            return;
        }
    //  try {
    //      getDbConn().write(writes);
    //  } catch (IOException ex) {
    //      throw new CruxExemption(ex, 1055);
    //  } finally {
            writes.clear();
    //  }
    }

    //** 底层方法 **/

    protected final void setDbPath(String path) {
        dbpath = path;
    }

    protected final void setDbName(String name) {
        dbname = name;
    }

    public String getDbPath() {
        if (dbpath == null) {
            throw new NullPointerException("DB path is not set");
        }
        return dbpath;
    }

    public String getDbName() {
        if (dbname == null) {
            throw new NullPointerException("DB name is not set");
        }
        return dbname;
    }

    public Conn getDbConn() {
        if (dbconn == null) {
        DO: {
            String kn = Conn.class.getName() + ":" + getDbName();

            dbconn = (Conn) Core.getInstance().get(kn);
            if (dbconn != null) {
                break  DO;
            }

            dbconn = (Conn) Core.getInterior().get(kn);
            if (dbconn != null) {
                break  DO;
            }

            String cn, dn, dp;
            CoreConfig cc;
            ConnGetter cg;
            dn = getDbName();
            dp = getDbPath();
            cc = CoreConfig.getInstance();
            cn = DirectConn.Getter.class.getName();
            cn = cc.getProperty("core.lucene.conn.getter.class", cn);
            cg = (ConnGetter) Core.newInstance(cn);

            dbconn = cg.get(dp, dn);

            CoreLogger . trace ("Lucene conn to {} by {}" , dn , cn);
        }}
        return dbconn;
    }

    public IndexWriter getWriter() throws CruxException {
        try {
            return getDbConn().getWriter();
        }
        catch (IOException ex) {
            throw new CruxException(ex);
        }
    }

    public IndexReader getReader() throws CruxException {
        try {
            return getDbConn().getReader();
        }
        catch (IOException ex) {
            throw new CruxException(ex);
        }
    }

    public IndexSearcher getFinder() throws CruxException {
        try {
            return getDbConn().getFinder();
        }
        catch (IOException ex) {
            throw new CruxException(ex);
        }
    }

    //** 底层工具 **/

    /**
     * 向量近似函数
     * @param fc
     * @return
     * @throws CruxException
     */
    protected VectorSimilarityFunction getSimilar(Map fc) throws CruxException {
        switch(Synt.declare(fc.get("vector-similar"), "")) {
            case "MAXIMUM_INNER_PRODUCT":
                return VectorSimilarityFunction.MAXIMUM_INNER_PRODUCT;
            case "DOT_PRODUCT":
                return VectorSimilarityFunction.DOT_PRODUCT;
            case "COSINE":
                return VectorSimilarityFunction.COSINE;
            default:
                return VectorSimilarityFunction.EUCLIDEAN;
        }
    }

    /**
     * 存储分析器
     * @param fc 字段配置
     * @return
     * @throws CruxException
     */
    protected Analyzer getAnalyzer(Map fc) throws CruxException {
        try {
            CustomAnalyzer.Builder cb = CustomAnalyzer.builder();
            String kn, an, ac; Map oc;

            // 分词器
            an = Synt.declare(fc.get("lucene-tokenizer"), "");
            if (!"".equals(an)) {
                int p  = an.indexOf('{');
                if (p != -1) {
                    ac = an.substring(p);
                    an = an.substring(0, p - 1).trim();
                    oc = Synt.asMap(Dist.toObject(ac));
                    cb.withTokenizer(an, oc);
                } else {
                    cb.withTokenizer(an/**/);
                }
            } else {
                cb.withTokenizer("Standard");
            }

            // 过滤器
            for(Object ot2 : fc.entrySet()) {
                Map.Entry et2 = (Map.Entry) ot2;
                kn = (String) et2.getKey();

                // 存储参数为 char,token
                if (kn.startsWith("lucene-char-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dist.toObject(ac));
                        cb.addCharFilter(an, oc);
                    } else {
                        cb.addCharFilter(an/**/);
                    }
                } else
                if (kn.startsWith("lucene-token-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dist.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch (IOException ex) {
            throw new CruxException(ex);
        } catch (IllegalArgumentException ex) {
            throw new CruxException(ex);
        }
    }

    /**
     * 查询分析器
     * @param fc 字段配置
     * @return
     * @throws CruxException
     */
    protected Analyzer getAnalyser(Map fc) throws CruxException {
        try {
            CustomAnalyzer.Builder cb = CustomAnalyzer.builder();
            String kn, an, ac; Map oc;

            // 分词器
            an = Synt.declare(fc.get("lucene-tokenizer"), "");
            if (!"".equals(an)) {
                int p  = an.indexOf('{');
                if (p != -1) {
                    ac = an.substring(p);
                    an = an.substring(0, p - 1).trim();
                    oc = Synt.asMap(Dist.toObject(ac));
                    cb.withTokenizer(an, oc);
                } else {
                    cb.withTokenizer(an/**/);
                }
            } else {
                cb.withTokenizer("Standard");
            }

            // 过滤器
            for(Object ot2 : fc.entrySet()) {
                Map.Entry et2 = (Map.Entry) ot2;
                kn = (String) et2.getKey();

                // 查询参数为 find,query
                if (kn.startsWith("lucene-find-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dist.toObject(ac));
                        cb.addCharFilter(an, oc);
                    } else {
                        cb.addCharFilter(an/**/);
                    }
                } else
                if (kn.startsWith("lucene-query-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dist.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch (IOException ex) {
            throw new CruxException(ex);
        } catch (IllegalArgumentException ex) {
            throw new CruxException(ex);
        }
    }

    /**
     * 清理搜索文本
     * @param fc 字段配置
     * @param v  待存储值
     * @return
     */
    protected Object getSrchText(Map fc, Object v) {
        // 合并多组值
        if (v instanceof Collection) {
            StringBuilder s = new StringBuilder( );
            for ( Object  o : ( (Collection) v ) ) {
                s.append( o )
                 .append(" ");
            }
            v = s;
        }

        // 清理富文本
        if ("textview".equals(fc.get("__type__"))) {
            return Syno.stripEnds(Syno.stripTags(Syno.stripCros(v.toString())));
        }

        return v;
    }

    /**
     * 获取基准类型
     * 返回的类型有
     * int
     * long
     * float
     * double
     * date
     * string
     * search
     * sorted
     * stored
     * object
     * @param fc 字段配置
     * @return
     */
    protected String datatype(Map fc) {
        String t = (String) fc.get("__type__");
        if (t == null) {
            return t ;
        }

        // 常规类型
        switch (t) {
            case "string":
            case "search":
            case "sorted":
            case "stored":
            case "object":
            case "vector":
                return t ;
        }

        // 基准类型
        try {
            String k  = (String) FormSet
                  .getInstance ( /***/ )
                  .getEnum ("__types__")
                  .get (t);
            if (null != k) {
                   t  = k;
            }
        } catch ( CruxException e) {
            throw e.toExemption( );
        }

        // 扩展类型
        switch (t) {
            case "number":
                return Synt.declare(fc.get("type"), "double");
            case "hidden":
            case  "enum" :
            case  "fork" :
                return Synt.declare(fc.get("type"), "string");
            case  "file" :
                return "string";
            case  "form" :
                return "object";
            default:
                return t ;
        }
    }

    protected boolean findable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getFindable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean sortable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSortable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean srchable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSrchable().contains(name);
    }

    protected boolean rankable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getRankable().contains(name);
    }

    protected boolean repeated(Map fc) {
        return Synt.declare(fc.get("__repeated__"), false);
    }

    protected boolean unstored(Map fc) {
        // 兼容旧的 unstated
        return   fc.containsKey   (  "unstored"  )
             ? Synt.declare(fc.get(  "unstored"  ), false)
             : Synt.declare(fc.get(  "unstated"  ), false);
    }

    protected boolean inviable(Map fc) {
        return Synt.declare(fc.get(  "inviable"  ), false);
    }

    protected boolean invisble(Map fc) {
        return Synt.declare(fc.get(  "invisble"  ), false);
    }

    //** 辅助对象 **/

    /**
     * 查询迭代器
     */
    public static class Loop implements Iterable<Map>, Iterator<Map> {
        private final io.github.ihongs.dh.lucene.conn.Loop loop;
        private final LuceneRecord that;
        private final Set cols;
        private final boolean docid;
        private final boolean score;

        /**
         * 查询迭代器
         * @param d 记录实例
         * @param q 查询对象
         * @param s 排序对象
         * @param r 返回字段
         * @param b 起始偏移
         * @param l 查询限额
         */
        public Loop(LuceneRecord d, Query q, Sort s, Set r, int b, int l) {
            // 空取全部字段
            if (r!= null && r.isEmpty()) {
                r = null ;
            }

            // 获取搜索对象
            IndexSearcher p;
            try {
                p = d.getFinder( );
            } catch ( CruxException e ) {
                throw e.toExemption ( );
            }

            loop = new io.github.ihongs.dh.lucene.conn.Loop(p, q, s, r, b, l);
            that = d;
            cols = r;

            // 特殊标记字段
            docid = r != null && r.contains("__docid__");
            score = r != null && r.contains("__score__");
        }

        @Override
        public Iterator<Map> iterator() {
            return this;
        }

        @Override
        public boolean  hasNext( ) {
            try {
                return loop.hasNext();
            } catch (CruxExemption e) {
                throw new Lost (e);
            }
        }

        public ScoreDoc nextOne( ) {
            try {
                return loop.next();
            } catch (CruxExemption e) {
                throw new Lost (e);
            }
        }

        public Document currDoc( ) {
            try {
                return loop.curr();
            } catch (CruxExemption e) {
                throw new Lost (e);
            }
        }

        @Override
        public Map next() {
            ScoreDoc  sco = nextOne();
            Document  doc = currDoc();
            Map map = that. padDat (doc, cols);
            if (score) {
                map.put("__score__",sco.score);
            }
            if (docid) {
                map.put("__docid__",sco. doc );
            }
            return map;
        }

        /**
         * 获取单次数量
         * @return
         */
        public int size () {
            return loop.size();
        }

        /**
         * 获取全部数量
         * @deprecated 建议使用 count
         * @return
         */
        public int hits () {
            long   H = count();
            return H < Integer.MAX_VALUE
            ?(int) H : Integer.MAX_VALUE;
        }

        /**
         * 真实命中总数
         * @return
         */
        public long count() {
            return loop.count();
        }

        /**
         * 可能命中总数
         * @return
         */
        public long total() {
            return loop.total();
        }

        /**
         * 可信的 total
         * @return
         */
        public boolean truly() {
            return loop.truly();
        }

        public List<Map> toList() {
            List<Map> list = new ArrayList(size());
            while  (  hasNext() ) {
                list.add(next() );
            }
            return list;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                 .append(that.getDbName())
                 .append(" ")
                 .append(loop. toString())
                 .toString( );
        }

        @Override
        @Deprecated
        public void remove() {
            throw new UnsupportedOperationException("Not supported remove in search loop.");
        }
    }

    /**
     * 查询中断异常
     */
    public static class Lost extends CruxExemption {

        public Lost (AlreadyClosedException cause) {
            super(cause, "@fore.retries");
        }

        public Lost ( IOException cause ) {
            super(cause, "@fore.retries");
        }

        public Lost ( CruxCause ex ) {
            super(ex.getCause() , "@fore.retries");
        }

    }

}
