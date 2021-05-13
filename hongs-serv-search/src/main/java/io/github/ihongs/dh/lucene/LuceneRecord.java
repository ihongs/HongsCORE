package io.github.ihongs.dh.lucene;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.JFigure;
import io.github.ihongs.dh.IReflux;
import io.github.ihongs.dh.lucene.field.*;
import io.github.ihongs.dh.lucene.value.*;
import io.github.ihongs.dh.lucene.query.*;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * Lucene 记录模型
 *
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
 * @author Hongs
 */
public class LuceneRecord extends JFigure implements IEntity, IReflux, AutoCloseable {

    protected boolean REFLUX_MODE = false;

    private IndexSearcher finder  = null ;
    private IndexReader   reader  = null ;
    private IndexWriter   writer  = null ;
    private String        dbpath  = null ;
    private String        dbname  = null ;

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
     * @throws HongsException
     */
    public static LuceneRecord getInstance(String conf, String form) throws HongsException {
        String code = LuceneRecord.class.getName() +":"+ conf +"!"+ form;
        Core   core = Core.getInstance( );
        LuceneRecord  inst = (LuceneRecord) core.get(code);
        if (inst == null) {
            String path = conf +"/"+ form;
            String name = conf +"!"+ form;
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

            // 进一步处理路径中的变量等
            Map m = new HashMap();
            m.put("SERVER_ID", Core.SERVER_ID);
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);
            path = Syno.inject(path, m);
            if ( ! new File(path).isAbsolute())
            path = Core.DATA_PATH+ "/lucene/" + path ;

            inst = new LuceneRecord(fxrm, path, name);
            core.set(code , inst);
        }
        return inst;
    }

    //** 实体方法 **/

    /**
     * 获取数据
     *
     * 以下参数为特殊参数, 可在 default.properties 中配置:
     * id   ID, 仅指定单个 id 时则返回详情(info)
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
     *
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public Map search(Map rd) throws HongsException {
        // 指定单个 id 则走 getOne
        Object id = rd.get (Cnst.ID_KEY);
        if (id instanceof String || id instanceof Number) {
            if ("".equals(id)) {
                return new HashMap(); // id 为空则不获取
            }
            Map info = getOne ( rd );

            Map data = new HashMap();
            data.put( "info", info );

            /**
             * 与 list 保持一致, 用 rn 控制 page
             * rn= 1 正常
             * rn= 0 不给 page
             * rn=-1 返回 page.state=-1 (缺失), page.state=-2 (受限)
             */
            int rn = Synt.declare(rd.get(Cnst.RN_KEY), 1);
            if (rn == 0) {
                return data ;
            }

            Map page = new HashMap();
            data.put( "page", page );

            /**
             * 查不到可能是不存在、已删除或受限
             * 需通过 id 再查一遍，区分不同错误
             */
            page.put(Cnst.RN_KEY,rn);
            if (null != info && ! info.isEmpty()) {
                page.put("state", 1);
                page.put("count", 1);
            } else
            if (rn >= 1 ) {
                page.put("state", 0);
                page.put("count", 0);
            } else
            if (null != getDoc( id.toString( ) )) {
                page.put("state", 0);
                page.put("count", 1);
            }  else
            {
                page.put("state", 0);
                page.put("count", 0);
            }

            return data;
        }

        // 默认仅返回可以列举的字段
        Set rb = Synt.declare(rd.get(Cnst.RB_KEY), Set.class);
        if (rb == null || rb.isEmpty( )) {
            rd =  new LinkedHashMap (rd);
            rd.put( Cnst.RB_KEY , getListable() );
        }

        // 获取行数, 默认依从配置
        int rn;
        if (rd.containsKey(Cnst.RN_KEY)) {
            rn = Synt.declare(rd.get(Cnst.RN_KEY), 0); if ( rn < 0 ) rn = Math.abs( rn );
        } else {
            rn = CoreConfig.getInstance().getProperty("fore.rows.per.page", Cnst.RN_DEF);
        }

        // 获取链数, 默认依从配置
        int gn;
        if (rd.containsKey(Cnst.GN_KEY)) {
            gn = Synt.declare(rd.get(Cnst.GN_KEY), 1); if ( gn < 0 ) gn = Math.abs( gn );
        } else {
            gn = CoreConfig.getInstance().getProperty("fore.pugs.for.page", Cnst.GN_DEF);
        }

        // 获取页码
        int pn = 1;
        if (rd.containsKey(Cnst.PN_KEY)) {
            pn = Synt.declare(rd.get(Cnst.PN_KEY), 1); if ( pn < 0 ) pn = Math.abs( pn );
        }

        if (rn < 0 || pn < 0 || gn < 1 ) {
            throw new HongsException(400 , "Wrong page parameter." );
        }

        // 指定行数 0, 则获取全部
        if (rn == 0) {
            Map  data = new HashMap();
            List list = getAll(rd);
            data.put("list", list);
            return data;
        }

        // 指定页码 0, 仅获取分页
        boolean  nl = pn == 0 ;
        if (nl)  pn = /***/ 1 ;
        int bn = rn * (pn - 1);

        Loop roll = search(rd, bn, rn);

        int rc = (int) roll.hits(/* total hits */);
        int pc = (int) Math.ceil((double) rc / rn);
        int st = rc > bn ? 1 : 0 ;

        Map  resp = new HashMap();
        Map  page = new HashMap();
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.GN_KEY, gn);
        page.put(Cnst.PN_KEY, pn);
        page.put("count", rc);
        page.put("pages", pc);
        page.put("state", st);

        if (! nl) {
            List list = roll.toList( );
            resp.put("list", list);
        }   resp.put("page", page);

        return resp;
    }

    /**
     * 创建记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public String create(Map rd) throws HongsException {
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
     * @throws HongsException
     */
    @Override
    public int update(Map rd) throws HongsException {
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
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
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
     * @throws HongsException
     */
    protected void permit(Map rd, Set ids, int ern) throws HongsException {
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
                throw new HongsException(ern, "Can not update by id: " + er);
            } else
            if (ern == 1097) {
                throw new HongsException(ern, "Can not delete by id: " + er);
            } else
            {
                throw new HongsException(ern, "Can not search by id: " + er);
            }
        }
    }

    //** 模型方法 **/

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    public String add(Map rd) throws HongsException {
        String id = Synt.asString(rd.get(Cnst.ID_KEY));
        if (id == null || id.length() == 0) {
            id  =  Core.newIdentity();
            rd.put(Cnst.ID_KEY , id );
        }
        addDoc(id, padDoc(rd) );
        return id;
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @return 1
     * @throws HongsException
     */
    public int put(String id, Map rd) throws HongsException {
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
        setDoc( id , padDoc (rd)); // 总是新建 Document
        return  1;
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @return 1
     * @throws HongsException
     */
    public int set(String id, Map rd) throws HongsException {
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
        setDoc( id , padDoc (rd)); // 总是新建 Document
        return  1;
    }

    /**
     * 删除文档(delDoc 的别名)
     * @param id
     * @return 有 1, 无 0
     * @throws HongsException
     */
    public int del(String id) throws HongsException {
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
     * @throws HongsException
     */
    public Map get(String id) throws HongsException {
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
     * @throws HongsException
     */
    public Map getOne(Map rd) throws HongsException {
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
     * @throws HongsException
     */
    public List getAll(Map rd) throws HongsException {
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
     * @throws HongsException
     */
    public Loop search(Map rd, int begin, int limit) throws HongsException {
        Query q = padQry(rd);
        Sort  s = padSrt(rd);
        Set   r = Synt.toTerms (rd.get(Cnst.RB_KEY));
        Loop  l = new Loop(this, q,s,r, begin,limit);

        if ( 4 == (4 & Core.DEBUG) ) {
            CoreLogger.debug("LuceneRecord.search: " + l.toString());
        }

        return l;
    }

    //** 组件方法 **/

    public void addDoc(String id, Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.addDocument (doc);
        } catch (IOException ex) {
            throw new HongsException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
    }

    public void setDoc(String id, Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), doc);
        } catch (IOException ex) {
            throw new HongsException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
    }

    public void delDoc(String id) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id) /**/);
        } catch (IOException ex) {
            throw new HongsException(ex);
        }
        if (!REFLUX_MODE) {
            commit();
        }
    }

    public Document getDoc(String id) throws HongsException {
        /* Maybe changed */ getReader( );
        IndexSearcher  ff = getFinder( );
        try {
                Query  qq = new TermQuery(new Term("@"+Cnst.ID_KEY, id));
              TopDocs  tt = ff.search(qq,  1  );
            ScoreDoc[] hh = tt.scoreDocs;
            if  ( 0 != hh.length ) {
                return ff.doc(hh[0].doc);
            } else {
                return null;
            }
        } catch ( IOException ex ) {
            throw new HongsException(ex);
        }
    }

    public Document padDoc(Map map) {
        Document doc = new Document();
        padDoc(doc, map, null);
        return doc;
    }

    public Document padDoc(Map map, Set rep) {
        Document doc = new Document();
        padDoc(doc, map, rep);
        return doc;
    }

    public Map padDat(Document doc) {
        Map map = new LinkedHashMap();
        padDat(doc, map, null);
        return map;
    }

    public Map padDat(Document doc, Set rep) {
        Map map = new LinkedHashMap();
        padDat(doc, map, rep );
        return map;
    }

    public Query padQry(Map rd) throws HongsException {
        BooleanQuery.Builder qr = new BooleanQuery.Builder();

        padQry(qr, rd);

        BooleanQuery qu = qr.build();
        if (! qu.clauses().isEmpty()) {
            return qu ;
        }

        return new MatchAllDocsQuery( );
    }

    public Sort  padSrt(Map rd) throws HongsException {
        List<SortField> of = new ArrayList();

        padSrt(of, rd);

        if (of.isEmpty()) {
            of.add(SortField.FIELD_DOC);
        }

        return new Sort(of.toArray(new SortField[0]));
    }

    //** 组件封装 **/

    /**
     * 遍历暂存文档(供 Loop.next 专用)
     * 可实现此方法用于中间缓存
     *
     * @param doc
     */
    protected void preDoc(Document doc) {
        // Nothing to do.
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
            ||  unstated( m )) {
                continue;
            }

            IField  f ;
            String  t = datatype(m);
            boolean r = repeated(m);
            boolean s = sortable(m);
            boolean p = srchable(m);
            boolean q = findable(m);
            boolean g =!unstored(m);

            if (Cnst.ID_KEY.equals(k)) {
                q  = true;
                g  = true;
            }

            if (t != null) switch (t)  {
            case "int":
                if ("".equals(v)) continue;
                f = new IntField();
                p = false;
                break;
            case "long":
            case "date":
                if ("".equals(v)) continue;
                f = new LongField();
                p = false;
                break;
            case "float":
                if ("".equals(v)) continue;
                f = new FloatField();
                p = false;
                break;
            case "double":
            case "number":
                if ("".equals(v)) continue;
                f = new DoubleField();
                p = false;
                break;
            case "sorted":
                if ("".equals(v)) continue;
                f = new LongField();
                s = true ;
                g = false; // 排序类型无需存储
                p = false; // 排序类型无法搜索
                q = false; // 排序类型无法筛选
                break;
            case "object":
                if ("".equals(v)) continue;
                f = new ObjectFiald();
                g = true ;
                p = false; // 对象类型无法搜索
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
                break;
            case "stored":
                f = new StringFiald();
                g = true ;
                p = false; // 存储类型无法搜索
                q = false; // 存储类型无法筛选
                s = false; // 存储类型无法排序
                break;
            case "search":
                f = new StringFiald();
                p = true ;
                g = false; // 搜索类型无需存储
                q = false; // 搜索类型无法筛选
                s = false; // 搜索类型无法排序
                break;
            default:
                f = new StringFiald();
            } else {
                f = new StringFiald();
            }

            /**
             * 2021/04/18
             * 为在一个库里存多个表
             * 不再在开始预设分析器
             * 改为存字段时直接写入 TokenStream
             */
            if (p && f instanceof StringFiald) {
                try {
                    ( ( StringFiald ) f).analyser(getAnalyzer(m));
                }
                catch (HongsException x) {
                    throw x.toExemption( );
                }
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
                if (!q && !p) { // 不可过滤时仍可判断空/非空/空串
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
                if (!q && !p) { // 不可过滤时仍可判断空/非空/空串
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
            ||  unstated( m )
            ||  unstored( m )) {
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
     * 组织查询条件
     * 可覆盖此方法扩展顶层条件
     *
     * @param qr
     * @param rd
     * @throws HongsException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd) throws HongsException {
        try {
            padQry(qr, rd, 0);
        } catch (BooleanQuery.TooManyClauses ex) {
            throw  new  HongsException(400 , ex);
        }
    }

    /**
     * 组织查询条件
     * 可覆盖此方法扩展查询条件
     *
     * @param qr
     * @param rd
     * @param r 递归层级
     * @throws HongsException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd, int r) throws HongsException {
        int i = 0, j = 0; // 条件数量, 否定数量, 计数规避全否定时查不到数据

        Map<String, Map> fields = getFields();
        Set<String> ks = new LinkedHashSet(fields.keySet());
                    ks.retainAll(rd.keySet());

        for(String k : ks) {
            Object v = rd.get(k);
            Map m =fields.get(k);

            if (m == null
            ||  v == null) {
                continue;
            }

            // 自定义条件
            if (!padQry(qr, rd, k,v)) {
                continue;
            }

            IQuery qa ;
            String t  =  datatype (m);
            if (t != null) switch (t) {
            case "int":
                qa = new IntQuery();
                break;
            case "long":
            case "date":
                qa = new LongQuery();
                break;
            case "float":
                qa = new FloatQuery();
                break;
            case "double":
            case "number":
                qa = new DoubleQuery();
                break;
            case "string":
            case "search":
            if (!srchable(m)) {
                qa = new StringQuery();
            } else {
                SearchQuery qs;
                qa = qs = new SearchQuery();
                qs.analyser(getAnalyser(m));
                qs.settings(m);
            }
                break;
            default:
                continue;
            } else {
                continue;
            }

            //** 常规查询 **/

            Map vd;
            if (v instanceof Map) {
                vd = (Map) v ;
            } else
            if (v instanceof Collection
            ||  v instanceof Object[ ]) {
                Set  a = Synt.asSet(v);
                     a.remove("");
                if (!a.isEmpty( )) {
                    BooleanQuery.Builder qx = new BooleanQuery.Builder();
                    for( Object b : a ) {
                        qx.add(qa.whr(k, b), BooleanClause.Occur.SHOULD);
                    }
                    qr.add(qx.build(  ), BooleanClause.Occur.MUST);
                    i ++;
                }
                continue;
            } else {
                if (!v.equals("")) {
                    qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
                    i ++;
                }
                continue;
            }

            //** 条件关系 **/

            v = vd.get(Cnst.OR_REL);
            if (Cnst.OR_KEY.equals(v )
            ||  Cnst.NR_KEY.equals(v)) {
                List r2 = new ArrayList(vd.size() - 1);
                for(Object ot : vd.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    Object k2 = et.getKey  ();
                    if (! Cnst.OR_KEY.equals(k2)) {
                    Object v2 = et.getValue();
                        r2.add(Synt.mapOf(k, Synt.mapOf(k2, v2)));
                    }
                }
                if ( !  r2.isEmpty( )) {
                    padQry(qr, Synt.mapOf(v, r2), r-1);
                }
                continue;
            }

            //** 空值查询 **/

            v = vd.get(Cnst.IS_REL);
            if ( v != null ) {
                String b = qa instanceof SearchQuery ? "$" : "@";
                String a = Synt.asString(v);
                Query  p ;
                try {
                    p = new QueryParser(b + k, new StandardAnalyzer()).parse("[* TO *]");
                } catch ( ParseException e) {
                    throw new HongsExemption(e);
                }
                if ("WELL".equalsIgnoreCase(a)) {
                    qr.add(p, BooleanClause.Occur.MUST);
                    i ++;
                } else
                if ("NULL".equalsIgnoreCase(a)) {
                    qr.add(p, BooleanClause.Occur.MUST_NOT);
                    i ++;  j ++;
                }
            }

            //** 精确匹配 **/

            v = vd.get(Cnst.EQ_REL);
            if ( v != null ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
                i ++;
            }
            v = vd.get(Cnst.NE_REL);
            if ( v != null ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST_NOT);
                i ++;  j ++;
            }

            //** 模糊匹配 **/

            v = vd.get(Cnst.CQ_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST);
                i ++;
            }
            v = vd.get(Cnst.NC_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST_NOT);
                i ++;  j ++;
            }

            //** 集合查询 **/

            v = vd.get(Cnst.AI_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST);
                        i ++;
                    }
                }
            }
            v = vd.get(Cnst.NI_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST_NOT);
                        i ++;  j ++;
                    }
                }
            }
            v = vd.get(Cnst.IN_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    BooleanQuery.Builder  qx = new BooleanQuery.Builder();
                    for(Object vv : vs) {
                        qx.add(qa.whr(k, vv), BooleanClause.Occur.SHOULD);
                    }
                        qr.add(qx.build (  ), BooleanClause.Occur.MUST  );
                        i ++;
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
                i ++;
            }

            Set s  = null;
            v = vd.get(Cnst.RG_REL);
            if (v != null) {
                s  = Synt.asSet (v);
            }

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
                    i ++;
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
                 SearchQuery         qs = new  SearchQuery        ();

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
                    i ++;
                }
            }
        }

        //** 递归条件 **/

        v = rd.get(Cnst.AR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException(400, "Key '" + Cnst.AR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST);
                    i ++;
                }
            }
            }
        }

        v = rd.get(Cnst.NR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException(400, "Key '" + Cnst.NR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST_NOT);
                    i ++;  j ++;
                }
            }
            }
        }

        v = rd.get(Cnst.OR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException(400, "Key '" + Cnst.OR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
                BooleanQuery.Builder qz = new BooleanQuery.Builder();
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qz.add(qb, BooleanClause.Occur.SHOULD);
                }
            }
                BooleanQuery qa = qz.build();
                if (! qa.clauses().isEmpty()) {
                    qr.add(qa, BooleanClause.Occur.MUST  );
                    i ++;
                }
            }
        }

        /**
         * 如果全部条件都是 MUST_NOT 会导致取不到数据
         * 故有必要增加一个 MUST all 从而规避这个问题
         */
        if (i > 0 & i == j) {
            qr.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
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
     * @return 返回 false 阻断
     */
    protected boolean padQry(BooleanQuery.Builder qr, Map rd, String k, Object v) {
        return true;
    }

    /**
     * 组织排序规则
     * 可覆盖此方法进行补充排序
     *
     * @param of
     * @param rd
     * @throws HongsException
     */
    protected void padSrt(List<SortField> of, Map rd) throws HongsException {
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (ob == null) {
            return;
        }

        Map<String, Map> fields = getFields();
        for (String fn: ob) {
            // 相关
            if (fn.equals("-")) {
                of.add(SortField.FIELD_SCORE);
                continue;
            }

            // 文档
            if (fn.equals("_")) {
                of.add(SortField.FIELD_DOC);
                continue;
            }

            // 逆序
            boolean rv = fn.startsWith("-");
            if (rv) fn = fn.substring ( 1 );

            // 自定义排序
            if (! padSrt (of, rd, fn, rv) ) {
                continue;
            }

            Map m  = (Map) fields.get(fn);
            if (m == null) {
                continue ;
            }
            if (! sortable(m)) {
                continue ;
            }

            SortField.Type st;
            String t = datatype(m);
            if (   "int".equals(t)) {
                st = SortField.Type.INT;
            } else
            if (  "long".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ( "float".equals(t)) {
                st = SortField.Type.FLOAT;
            } else
            if ("double".equals(t)) {
                st = SortField.Type.DOUBLE;
            } else
            if (  "date".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ("sorted".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ("search".equals(t)) {
                st = SortField.Type.STRING;
            } else
            if ("string".equals(t)) {
                st = SortField.Type.STRING;
            } else
            {
                continue;
            }

            /**
             * 因为 Lucene 5 必须使用 DocValues 才能排序
             * 在更新数据时, 会加前缀 '#','%' 的排序字段
             * 2020/03/07 多个值的普通排序只能用其中一个
             */
        //  if (repeated(m)) {
        //      of.add(new SortField("%" + fn, st, rv));
        //  } else {
                of.add(new SortField("#" + fn, st, rv));
        //  }
        }
    }

    /**
     * 自定排序规则
     * 可覆盖此方法进行特殊排序
     *
     * @param of
     * @param rd
     * @param k 排序字段
     * @param r 是否逆序
     * @return 返回 false 阻断
     */
    protected boolean padSrt(List<SortField> of, Map rd, String k, boolean r) {
        return true;
    }

    //** 事务方法 **/

    /**
     * 销毁读写连接
     */
    @Override
    public void close() {
        if (reader != null ) {
            try {
                reader.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            } finally {
                reader = null ;
                finder = null ;
            }

            if (4 == (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene reader for " + getDbName());
            }
        }

        if (writer != null ) {
        if (writer.isOpen()) {
            // 默认退出时提交
            if (REFLUX_MODE) {
                try {
                try {
                    commit();
                } catch (Throwable e) {
                    revert();
                    throw e ;
                }
                } catch (Throwable e) {
                    CoreLogger.error(e);
                }
            }

            // 退出时合并索引
            try {
                writer.maybeMerge();
            } catch (IOException x) {
                CoreLogger.error(x);
            }

            try {
                writer.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            } finally {
                writer = null ;
            }

            if (4 == (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene writer for " + getDbName());
            }
        } else {
            /**/writer = null ;
        }
        }
    }

    /**
     * 事务开始
     */
    @Override
    public void begin() {
        if (REFLUX_MODE
        &&  writer != null
        &&  writer.hasUncommittedChanges()) {
            throw new HongsExemption(1054, "Uncommitted changes");
        }
        REFLUX_MODE = true;
    }

    /**
     * 提交更改
     */
    @Override
    public void commit() {
        try {
            if (writer !=  null ) {
                writer.commit ( );
            }
        } catch (IOException ex ) {
            throw new HongsExemption(1055, ex);
        }
        REFLUX_MODE = false;
    }

    /**
     * 回滚操作
     */
    @Override
    public void revert() {
        try {
            if (writer !=  null ) {
                writer.rollback();
            }
        } catch (IOException ex ) {
            throw new HongsExemption(1056, ex);
        }
        REFLUX_MODE = false;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
           this.close(   );
        } finally {
          super.finalize();
        }
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

    /**
     * 获取查询器
     * 由于 getReader 中发现数据变化会重开
     * 如需 getReader 务必先于当前方法执行
     * @return
     * @throws HongsException
     */
    public IndexSearcher getFinder() throws HongsException {
        if (finder == null) {
        if (reader == null) {
            getReader(/**/);
        }
            finder  = new IndexSearcher(reader);
        }
        return finder;
    }

    public IndexReader getReader() throws HongsException {
        if (reader != null) {
            try {
                // 如果有更新数据则会重新打开查询接口
                // 这可以规避提交更新后却查不到的问题
                IndexReader  nred = DirectoryReader.openIfChanged((DirectoryReader) reader);
                if ( null != nred) {
                    reader.close();
                    reader = nred ;
                    finder = null ;
                }
            } catch (IOException x) {
                throw new HongsException(x);
            }
        } else {
            String path = getDbPath();

            try {
                // 目录不存在需开写并提交从而建立索引
                // 否则会抛出: IndexNotFoundException
                if (! new File(path).exists())
                    getWriter (    ).commit();

                Directory dir = FSDirectory.open(Paths.get(path));

                reader = DirectoryReader.open(dir);
            } catch (IOException x) {
                throw new HongsException(x);
            }

            if (4 == (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene reader for "+getDbName());
            }
        }
        return reader;
    }

    public IndexWriter getWriter() throws HongsException {
        if (writer == null || writer.isOpen() == false ) {
            String path = getDbPath();

            try {
                /**
                 * 2021/04/18
                 * 为在一个库里存多个表
                 * 不再在开始预设分析器
                 * 改为存字段时直接写入 TokenStream
                 */
                CoreConfig cc = CoreConfig.getInstance();
                IndexWriterConfig iwc = new IndexWriterConfig();
            //  IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                iwc.setMaxBufferedDocs(cc.getProperty("core.lucene.max.buf.docs", -1 ));
                iwc.setRAMBufferSizeMB(cc.getProperty("core.lucene.ram.buf.size", 16D));

                Directory dir = FSDirectory.open(Paths.get(path));

                writer = new IndexWriter(dir, iwc);
            } catch (IOException x) {
                throw new HongsException(x);
            }

            if (4 == (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene writer for "+getDbName());
            }
        }
        return writer;
    }

    //** 底层工具 **/

    /**
     * 存储分析器
     * @return
     * @throws HongsException
     * @deprecated 不再需要提前预设, 改为写入值时构建 TokenStream
     */
    protected Analyzer getAnalyzer() throws HongsException {
        /*Default*/ Analyzer  ad = new StandardAnalyzer();
        Map<String, Analyzer> az = new HashMap();
        Map<String, Map     > fs = getFields(  );
        for(Map.Entry<String, Map> et : fs.entrySet()) {
            String fn = et.getKey(  );
            Map    fc = et.getValue();
            if (srchable(fc)) {
                // 注意: 搜索对应的字段名开头为 $
                az.put("$" + fn, getAnalyzer(fc) );
            }
        }
        return new PerFieldAnalyzerWrapper(ad, az);
    }

    /**
     * 存储分析器
     * @param fc 字段配置
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer(Map fc) throws HongsException {
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
                    oc = Synt.asMap(Dawn.toObject(ac));
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
                        oc = Synt.asMap(Dawn.toObject(ac));
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
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch (IOException ex) {
            throw new HongsException(ex);
        } catch (IllegalArgumentException ex) {
            throw new HongsException(ex);
        }
    }

    /**
     * 查询分析器
     * @param fc 字段配置
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyser(Map fc) throws HongsException {
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
                    oc = Synt.asMap(Dawn.toObject(ac));
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
                        oc = Synt.asMap(Dawn.toObject(ac));
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
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch (IOException ex) {
            throw new HongsException(ex);
        } catch (IllegalArgumentException ex) {
            throw new HongsException(ex);
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

        switch (t) {
            case "string":
            case "search":
            case "sorted":
            case "stored":
            case "object":
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
        } catch (HongsException e) {
            throw e.toExemption( );
        }

        switch (t) {
            case "number":
                return Synt.declare(fc.get("type"), "double");
            case "hidden":
            case  "enum" :
                return Synt.declare(fc.get("type"), "string");
            case  "file" :
            case  "fork" :
                return "string";
            case  "form" :
            case  "json" :
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
        return Synt.declare(fc.get(  "unstored"  ), false);
    }

    protected boolean unstated(Map fc) {
        return Synt.declare(fc.get(  "unstated"  ), false);
    }

    //** 辅助对象 **/

    /**
     * 查询迭代器
     */
    public static class Loop implements Iterable<Map>, Iterator<Map> {
        private final IndexSearcher finder;
        private final LuceneRecord  that;
        private       ScoreDoc[]    docs;
        private       ScoreDoc      doc ;
        private final boolean t; // 有限查询
        private final Query   q;
        private final Sort    s;
        private final Set     r;
        private final int     b; // 起始位置
        private final int     l; // 数量限制
        private       int     i; // 提取游标
        private       int     h; // 单次总数
        private       long    H; // 全局总数

        /**
         * 查询迭代器
         * @param that 记录实例
         * @param q 查询对象
         * @param s 排序对象
         * @param r 返回字段
         * @param b 起始偏移
         * @param l 查询限额
         */
        public Loop(LuceneRecord that, Query q, Sort s, Set r, int b, int l) {
            // 是否获取全部
            if (l == 0 ) {
                l = 65536;
                t = false;
            } else {
                t = true ;
                // 最多 2G
                if (b > Integer.MAX_VALUE - l) {
                    b = Integer.MAX_VALUE - l;
                }
            }

            this.that = that;
            this.docs = null;
            this.doc  = null;
            this.q =  q;
            this.s =  s;
            this.r =  r;
            this.b =  b;
            this.l =  l;

            // 获取查读对象
            try {
                finder = that.getFinder();
            } catch ( HongsException ex ) {
                throw ex.toExemption(   );
            }
        }

        @Override
        public Iterator<Map> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            try {
                if ( docs == null) {
                    int L  = l+b ;
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.searchAfter(doc, q, L, s);
                    } else {
                        tops = finder.searchAfter(doc, q, L);
                    }
                    docs = tops.scoreDocs;
                    H    = tops.totalHits;
                    h    = docs.length;
                    i    = b;
                } else
                if (! t && i >= h) {
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.searchAfter(doc, q, l, s);
                    } else {
                        tops = finder.searchAfter(doc, q, l);
                    }
                    docs = tops.scoreDocs;
                    h    = docs.length;
                    i    = 0;
                }
                return i < h;
            } catch (IOException e) {
                throw new HongsExemption(e);
            }
        }

        @Override
        public Map next() {
            if ( i >= h ) {
                throw new NullPointerException("hasNext not run?");
            }
            try {
                Document dox;
                doc = docs[i ++];
                dox = finder.doc( doc.doc );
                        that.preDoc(dox   );
                return  that.padDat(dox, r);
            } catch (IOException e) {
                throw new HongsExemption(e);
            }
        }

        /**
         * 获取单次数量
         * @return
         */
        public int size() {
            if (docs == null) {
                hasNext();
            }
            int L;
            if (t) {
                L  = (int) (h - b);
            } else {
                L  = (int) (H - b);
            }
            return L > 0 ?  L : 0 ;
        }

        /**
         * 获取命中总数
         * @return
         */
        public int hits() {
            if (docs == null) {
                hasNext();
            }
            // 最多 2G
            return H < Integer.MAX_VALUE
            ?(int) H : Integer.MAX_VALUE;
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
            StringBuilder sb = new StringBuilder(that.getDbName());
            if ( q != null ) {
                sb.append(" QUERY: ");
                sb.append( q );
            }
            if ( s != null ) {
                sb.append( " SORT: ");
                sb.append( s );
            }
            if ( r != null && !r.isEmpty()) {
                sb.append(" REPLY: ");
            for(Object x : r ) {
                sb.append( x );
                sb.append(",");
            }   sb.setLength(sb.length()-1);
            }
            if ( l != 0 || b != 0 ) {
                sb.append(" LIMIT: ");
                sb.append( b );
                sb.append(",");
                sb.append( l );
            }
            return sb.toString();
        }

        /**
         * @deprecated
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported remove in lucene loop.");
        }
    }

}
