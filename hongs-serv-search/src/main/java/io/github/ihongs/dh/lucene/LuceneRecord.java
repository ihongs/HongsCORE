package io.github.ihongs.dh.lucene;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.ITrnsct;
import io.github.ihongs.dh.ModelCase;
import io.github.ihongs.dh.lucene.field.*;
import io.github.ihongs.dh.lucene.value.*;
import io.github.ihongs.dh.lucene.query.*;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
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

/**
 * Lucene 记录模型
 *
 * 可选字段配置参数:
 *  lucene-type         Lucene 字段类型(string,search,stored,sorted,int,long,float,double)
 *  lucene-tokenizer    Lucene 分词器类
 *  lucene-char-filter  存储时使用的 CharFilter  类
 *  lucene-token-filter 存储时使用的 TokenFilter 类
 *  lucene-find-filter  查询时使用的 CharFilter  类
 *  lucene-query-filter 查询时使用的 TokenFilter 类
 *
 * @author Hongs
 */
public class LuceneRecord extends ModelCase implements IEntity, ITrnsct, AutoCloseable {

    protected boolean OBJECT_MODE = false;
    protected boolean TRNSCT_MODE = false;
    protected final  boolean TRNSCT_BASE ;

    private IndexSearcher finder  = null ;
    private IndexReader   reader  = null ;
    private IndexWriter   writer  = null ;
    private String        dbpath  = null ;
    private String        dbname  = null ;
    private Set<String>  replies  = null ;

    /**
     * 构造方法
     * @param form 字段配置, 可覆盖 getFields
     * @param path 存储路径, 可覆盖 getDbPath
     * @param name 存储名称, 可覆盖 getDbName
     */
    public LuceneRecord(Map form, String path, String name) {
        super.setFields(form);
        this.dbpath  =  path ;
        this.dbname  =  name ;

        // 环境模式
        CoreConfig  conf = CoreConfig.getInstance( );
        this.OBJECT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.OBJECT_MODE),
            conf.getProperty("core.in.object.mode", false));
        this.TRNSCT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.TRNSCT_MODE),
            conf.getProperty("core.in.trnsct.mode", false));
        this.TRNSCT_BASE = this.TRNSCT_MODE;
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
        String code = LuceneRecord.class.getName() +":"+ conf +"."+ form;
        Core   core = Core.getInstance( );
        if ( ! core.containsKey( code ) ) {
            String path = conf +"/"+ form;
            String name = conf +"."+ form;
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
            path = Tool.inject(path, m);
            if ( ! new File(path).isAbsolute())
            path = Core.DATA_PATH + "/lucene/" + path;

            LuceneRecord inst = new LuceneRecord(fxrm, path, name);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (LuceneRecord) core.got(code);
        }
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
     * ar   串联多组关系条件
     * sr   附加多组"或"关系, LuceneRecord 特有
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
        if (id != null && !(id instanceof Collection) && !(id instanceof Map)) {
            if ( "".equals( id ) ) {
                return  new HashMap(); // id 为空则不获取
            }
            Map  data = new HashMap();
            Map  info = getOne(rd);
            data.put("info", info);
            return data;
        }

        // 获取页码
        int pn = 1;
        if (rd.containsKey(Cnst.PN_KEY)) {
            pn = Synt.declare(rd.get(Cnst.PN_KEY), 1); if ( pn < 0 ) pn = Math.abs( pn );
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
            gn = CoreConfig.getInstance().getProperty("fore.pags.for.page", Cnst.GN_DEF);
        }

        // 指定行数 0, 则走 getAll
        if (rn == 0) {
            Map  data = new HashMap();
            List list = getAll(rd);
            data.put("list", list);
            return data;
        }

        // 指定页码 0, 仅获取分页
        if (gn == 0) {
            gn =  1;
        }
        if (pn == 0) {
            return getPage(rd, rn, gn, 1 );
        } else {
            return getList(rd, rn, gn, pn);
        }
    }

    /**
     * 创建记录
     * @param rd
     * @return id,name等(由dispCols指定)
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        String  id = add( rd);
        Set<String> fs = getListable();
        if (null != fs && ! fs.isEmpty( )) {
            Map sd = new LinkedHashMap( );
            for(String fn : fs) {
                if ( ! fn.contains( "." )) {
                    sd.put( fn, rd.get(fn) );
                }
            }
            sd.put(Cnst.ID_KEY, id);
            return sd;
        } else {
            rd.put(Cnst.ID_KEY, id);
            return rd;
        }
    }

    /**
     * 更新记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int update(Map rd) throws HongsException {
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1096);
        for(String  id  : ids) {
            put(id, rd  );
        }
        return ids.size();
    }

    /**
     * 删除记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1097);
        for(String  id  : ids) {
            del(id /**/ );
        }
        return ids.size();
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
            if (ern == 0x1096) {
                throw new HongsException(ern, "Can not update by id: " + er);
            } else
            if (ern == 0x1097) {
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
        if (id != null && id.length() != 0) {
            throw new HongsException.Common("Id can not set in add");
        }
        id = Core.newIdentity();
        rd.put(Cnst.ID_KEY, id);
        addDoc(map2Doc(rd));
        return id;
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @throws HongsException
     */
    public void set(String id, Map rd) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in set");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            doc =  new Document();
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            setReps(null);
            Map md = doc2Map(doc);
            md.putAll(rd);rd = md;
            doc =  new Document();
        }
        rd.put(Cnst.ID_KEY, id);
        docAdd(doc, rd);
        setDoc(id, doc);
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @throws HongsException
     */
    public void put(String id, Map rd) throws HongsException {
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
            setReps(null);
            Map md = doc2Map(doc);
            md.putAll(rd);rd = md;
        }
        rd.put(Cnst.ID_KEY, id);
        docAdd(doc, rd);
        setDoc(id, doc);
    }

    /**
     * 删除文档(delDoc 的别名)
     * @param id
     * @throws HongsException
     */
    public void del(String id) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in del");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            throw new NullPointerException("Doc#"+id+" not exists");
        }
        delDoc(id);
    }

    /**
     * 获取文档信息
     * @param id
     * @return
     * @throws HongsException
     */
    public Map get(String id) throws HongsException {
        Document doc = getDoc(id);
        if ( doc != null) {
            setReps(null);
            return doc2Map( doc );
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
        List list = new LinkedList();
        while  (  roll.hasNext()) {
            list.add(roll.next());
        }
        return list;
    }

    private Map getPage(Map rd, int rn, int gn, int pn) throws HongsException {
        // 获取页码, 计算查询区间
        int minPn = pn - (gn / 2);
        if (minPn < 1)   minPn=1 ;
        int maxPn = gn + minPn-1 ;
        int totRn = rn * maxPn+1 ;
        int minRn = rn * (pn - 1);
//      int maxRn = rn + minRn   ;
        // 数量太少的话没必要估算
        if (totRn < gn * rn * 10 + 1) {
            totRn = gn * rn * 10 + 1;
        }

        Loop roll = search( rd, minRn , totRn - minRn );
        int  rc   = roll . size();
        int  pc   = ( int ) Math.ceil((double) rc / rn);
        int  st   ;

        if (rc == 0) {
            st =  1;
        } else
//      if (rc <  minRn) {
//          st =  2;
//      } else
        if (rc <  totRn) {
            st =  0;
        } else
        {
            st = -1;
            rc -= 1;
        }

        Map  resp = new HashMap();
        Map  page = new HashMap();
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.GN_KEY, gn);
        page.put(Cnst.PN_KEY, pn);
        page.put("count", rc );
        page.put("pages", pc );
        page.put("state", st );
        resp.put("page", page);

        return resp;
    }

    private Map getList(Map rd, int rn, int gn, int pn) throws HongsException {
        // 获取页码, 计算查询区间
        int minPn = pn - (gn / 2);
        if (minPn < 1)   minPn=1 ;
        int maxPn = gn + minPn-1 ;
        int totRn = rn * maxPn+1 ;
        int minRn = rn * (pn - 1);
        int maxRn = rn + minRn   ;
        // 数量太少的话没必要估算
        if (totRn < gn * rn * 10 + 1) {
            totRn = gn * rn * 10 + 1;
        }

        Loop roll = search( rd, minRn , totRn - minRn );
        int  rc   = roll . size();
        int  pc   = ( int ) Math.ceil((double) rc / rn);
        int  st   ;

        if (rc == 0) {
            st =  1;
        } else
        if (rc <  minRn) {
            st =  2;
        } else
        if (rc < totRn) {
            st =  0;
        } else
        {
            st = -1;
            rc -= 1;
        }

        // 提取分页片段
        List list = new LinkedList();
        while ( maxRn > minRn ++
          && roll . hasNext (/**/) ) {
             list . add(roll.next());
        }

        Map  resp = new HashMap();
        Map  page = new HashMap();
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.GN_KEY, gn);
        page.put(Cnst.PN_KEY, pn);
        page.put("count", rc );
        page.put("pages", pc );
        page.put("state", st );
        resp.put("page", page);
        resp.put("list", list);

        return  resp;
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
        Query q = getQuery(rd);
        Sort  s = getSort (rd);
                  setReps (rd);
        Loop  r = new Loop(this, q, s, begin, limit);

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            CoreLogger.debug("LuceneRecord.search: " + r.toString());
        }

        return r ;
    }

    //** 组件方法 **/

    public void addDoc(Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.addDocument (doc);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public void setDoc(String id, Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.updateDocument (new Term(Cnst.ID_KEY, id), doc);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public void delDoc(String id) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.deleteDocuments(new Term(Cnst.ID_KEY, id) /**/);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public Document getDoc(String id) throws HongsException {
        IndexSearcher  ff = getFinder( );
        try {
                Query  qq = new TermQuery(new Term(Cnst.ID_KEY, id));
              TopDocs  tt = ff.search(qq,  1  );
            ScoreDoc[] hh = tt.scoreDocs;
            if  ( 0 != hh.length ) {
                return ff.doc(hh[0].doc);
            } else {
                return null;
            }
        } catch ( IOException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    public Document map2Doc(Map map) {
        Document doc = new Document();
        docAdd(doc, map);
        return doc;
    }

    public Map doc2Map(Document doc) {
        Map map = new LinkedHashMap();
        mapAdd(map, doc);
        return map;
    }

    //** 事务方法 **/

    @Override
    protected void finalize() throws Throwable {
        try {
           this.close(   );
        } finally {
          super.finalize();
        }
    }

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

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene reader for " + getDbName());
            }
        }

        if (writer != null ) {
        if (writer.isOpen()) {
            // 默认退出时提交
            if (TRNSCT_MODE) {
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

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
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
        TRNSCT_MODE = true;
    }

    /**
     * 提交更改
     */
    @Override
    public void commit() {
        TRNSCT_MODE = TRNSCT_BASE;
        if (writer == null) {
            return;
        }
        try {
            writer.commit ( );
        } catch (IOException ex) {
            throw new HongsExemption(0x102c, ex);
        }
    }

    /**
     * 回滚操作
     */
    @Override
    public void revert() {
        TRNSCT_MODE = TRNSCT_BASE;
        if (writer == null) {
            return;
        }
        try {
            writer.rollback();
        } catch (IOException ex) {
            throw new HongsExemption(0x102d, ex);
        }
    }

    //** 底层方法 **/

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

    public IndexSearcher getFinder() throws HongsException {
        if (finder == null) {
            finder  = new IndexSearcher(getReader());
        }
        return finder;
    }

    public IndexReader getReader() throws HongsException {
        if (reader == null) {
            String path = getDbPath();

            try {
                // 目录不存在需开写并提交从而建立索引
                // 否则会抛出: IndexNotFoundException
                if (! new File(path).exists())
                    getWriter (    ).commit();

                Directory dir = FSDirectory.open(Paths.get(path));

                reader = DirectoryReader.open(dir);
            } catch (IOException x) {
                throw new HongsException.Common(x);
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene reader for "+getDbName());
            }
        }
        return reader;
    }

    public IndexWriter getWriter() throws HongsException {
        if (writer == null || writer.isOpen() == false ) {
            String path = getDbPath();

            try {
                IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

                Directory dir = FSDirectory.open(Paths.get(path));

                writer = new IndexWriter(dir, iwc);
            } catch (IOException x) {
                throw new HongsException.Common(x);
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene writer for "+getDbName());
            }
        }
        return writer;
    }

    /**
     * 查询分析
     * @param rd
     * @return
     * @throws HongsException
     */
    public Query getQuery(Map rd) throws HongsException {
        Map<String, Map> fields = getFields();
        BooleanQuery.Builder qr = new BooleanQuery.Builder();

        for (Object o : rd.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object fv = e.getValue( );
            String fn = (String) e.getKey();

            // 自定义查询
            if (queried ( rd, fn, fv, qr )) {
                continue;
            }

            Map m = (Map ) fields.get( fn );
            if (m == null) {
                continue;
            }
            if (findable(m) == false ) {
                continue;
            }

            IQuery aq;
            String t = datatype(m);
            if (   "int".equals(t)) {
                aq = new IntQuery();
            } else
            if (  "long".equals(t)) {
                aq = new LongQuery();
            } else
            if ( "float".equals(t)) {
                aq = new FloatQuery();
            } else
            if ("double".equals(t)) {
                aq = new DoubleQuery();
            } else
            if (  "date".equals(t)) {
                aq = new LongQuery();
            } else
            if ("string".equals(t)) {
                aq = new StringQuery();
            } else
            if ("search".equals(t)) {
                aq = new SearchQuery();
            } else
            {
                continue;
            }

            qryAdd(qr, fn, fv, aq);
        }

        // 关键词
        if (rd.containsKey(Cnst.WD_KEY)) {
            Object fv = rd.get (Cnst.WD_KEY);
                   fv = Synt.declare(fv, "");
            Set<String> fs = getSrchable(  );

            if (fv != null && !"".equals(fv)) {
                if (fs.size() > 1) {
                    // 当设置了多个搜索字段时
                    // 将条件整理为: +(fn1:xxx fn2:xxx)

                    Map fw = new HashMap(  );
                    fw.put(Cnst.SE_REL , fv);
                    BooleanQuery.Builder qx = new BooleanQuery.Builder();

                    for(String fk : fs) {
                        qryAdd(qx , fk , fw , new SearchQuery());
                    }

                    qr.add(qx.build(), BooleanClause.Occur.MUST);
                } else {
                    for(String fk : fs) {
                        qryAdd(qr , fk , fv , new SearchQuery());
                    }
                }
            }
        }

        // 附条件
        if (rd.containsKey(Cnst.SR_KEY)) {
            Set<Map> set = Synt.asSet(rd.get(Cnst.SR_KEY));
            if (set != null && ! set.isEmpty())
            for(Map  map : set) {
                qr.add(getQuery(map), BooleanClause.Occur.SHOULD);
            }
        }

        // 并条件
        if (rd.containsKey(Cnst.AR_KEY)) {
            Set<Map> set = Synt.asSet(rd.get(Cnst.AR_KEY));
            if (set != null && ! set.isEmpty())
            for(Map  map : set) {
                qr.add(getQuery(map), BooleanClause.Occur.MUST  );
            }
        }

        // 或条件
        if (rd.containsKey(Cnst.OR_KEY)) {
            Set<Map> set = Synt.asSet(rd.get(Cnst.OR_KEY));
            if (set != null && ! set.isEmpty()) {
            BooleanQuery.Builder qx = new BooleanQuery.Builder( );
            for(Map  map : set) {
                qx.add(getQuery(map), BooleanClause.Occur.SHOULD);
            }
                qr.add(qx.build(   ), BooleanClause.Occur.MUST  );
            }
        }

        // 没有条件则查询全部
        BooleanQuery query = qr.build(   );
        if ( query.clauses( ).isEmpty( ) ) {
            return new MatchAllDocsQuery();
        }

        return query;
    }

    /**
     * 排序分析
     * @param rd
     * @return
     * @throws HongsException
     */
    public Sort getSort(Map rd) throws HongsException {
        Map<String, Map> fields = getFields();
        List<SortField> of = new LinkedList();
        Object xb = rd.get(Cnst.OB_KEY);
        Set<String> ob  =  xb  !=  null
                  ? Synt.toTerms ( xb )
                  : new LinkedHashSet();

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
            if (sorted ( rd, fn, rv, of ) ) {
                continue;
            }

            Map m = (Map ) fields.get ( fn);
            if (m == null) {
                continue;
            }
            if (sortable(m)==false) {
                continue;
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
            if ("string".equals(t)) {
                st = SortField.Type.STRING;
            } else
            if ("search".equals(t)) {
                st = SortField.Type.STRING;
            } else
            {
                continue;
            }

            /**
             * 因为 Lucene 5 必须使用 DocValues 才能排序
             * 在更新数据时, 默认有加 '.' 打头的排序字段
             */
            of.add(new SortField("." + fn , st , rv));
        }

        // 未指定则按文档顺序
        if (of.isEmpty()) {
            of.add(SortField.FIELD_DOC);
        }

        return new Sort(of.toArray(new SortField[0]));
    }

    /**
     * 返回字段
     * @param rd
     */
    public void setReps(Map rd) {
        if ( rd  == null) {
          replies = null; // 默认情况返回全部设定的字段.
        } else {
          replies = Synt.toTerms(rd.get(Cnst.RB_KEY));
        }
    }

    //** 底层工具 **/

    /**
     * 存储分析器
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer() throws HongsException {
        Map<String, Analyzer> az = new HashMap();
        Map<String, Map>  fields = getFields(  );
        Analyzer ad = new StandardAnalyzer();
        for(Object ot : fields.entrySet( ) ) {
            Map.Entry et = (Map.Entry) ot;
            Map    m = (Map ) et.getValue( );
            String n = (String) et.getKey( );
            String t = datatype(m);
            if ("search".equals(t)) {
                az.put(n, getAnalyzer(m));
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
                    oc = Synt.asMap(Data.toObject(ac));
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
                        oc = Synt.asMap(Data.toObject(ac));
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
                        oc = Synt.asMap(Data.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch ( IOException ex) {
            throw new HongsException.Common(ex);
        } catch ( IllegalArgumentException  ex) {
            throw new HongsException.Common(ex);
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
                    oc = Synt.asMap(Data.toObject(ac));
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
                        oc = Synt.asMap(Data.toObject(ac));
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
                        oc = Synt.asMap(Data.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch ( IOException ex) {
            throw new HongsException.Common(ex);
        } catch ( IllegalArgumentException  ex) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 设置搜索项
     * @param fc 字段配置
     * @param sq 搜索对象
     * @throws HongsException
     */
    protected void setParser(Map fc, SearchQuery sq) throws HongsException {
        sq.analyzer   (getAnalyser (fc));
        sq.lightMatch (Synt.asBool (fc.get("lucene-light-match")));
        sq.smartParse (Synt.asBool (fc.get("lucene-smart-parse")));
        sq.phraseSlop (Synt.asInt  (fc.get("lucene-phrase-slop")));
        sq.fuzzyPreLen(Synt.asInt  (fc.get("lucene-fuzzy-pre-len")));
        sq.fuzzyMinSim(Synt.asFloat(fc.get("lucene-fuzzy-min-sim")));
        sq.analyzeRangeTerms(Synt.asBool(fc.get("lucene-parser-analyze-range-terms")));
        sq.allowLeadingWildcard(Synt.asBool(fc.get("lucene-parser-allow-leading-wildcard")));
        sq.lowercaseExpandedTerms(Synt.asBool(fc.get("lucene-parser-lowercase-expanded-terms")));
        sq.enablePositionIncrements(Synt.asBool(fc.get("lucene-parser-enable-position-increments")));
        sq.autoGeneratePhraseQueries(Synt.asBool(fc.get("lucene-parser-auto-generate-phrase-queries")));
    }

    /**
     * 获取类型变体
     * 返回的类型有
     * int
     * long
     * float
     * double
     * search
     * string
     * object
     * date
     * @param fc 字段配置
     * @return
     */
    protected String datatype(Map fc) {
        String t = Synt.asString(fc.get("lucene-type"));
        if (null != t) {
            if (t.equals("text")) {
                return "search"; // Lucene 的 text 在这里叫 search
            }
            return  t;
        }

        t = (String) fc.get("__type__");
        Set <String> ks;

        //** 先查特有的 **/

        if (srchable(fc)) {
            return "search";
        }

        ks = getSaveTypes("search");
        if (ks != null && ks.contains(t) ) {
            return "search";
        }

        ks = getSaveTypes("sorted");
        if (ks != null && ks.contains(t) ) {
            return "sorted";
        }

        ks = getSaveTypes("stored");
        if (ks != null && ks.contains(t) ) {
            return "stored";
        }

        ks = getSaveTypes("object");
        if (ks != null && ks.contains(t) ) {
            return "object";
        }

        //** 再查一般的 **/

        ks = getSaveTypes("number");
        if (ks != null && ks.contains(t) ) {
            return Synt.declare(fc.get("type"), "double");
        }

        ks = getSaveTypes("string");
        if (ks != null && ks.contains(t) ) {
            return "string";
        }

        ks = getSaveTypes( "date" );
        if (ks != null && ks.contains(t) ) {
            return  "date" ;
        }

        return t;
    }

    protected boolean sortable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSortable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean findable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getFindable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean srchable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSrchable().contains(name);
    }

    protected boolean repeated(Map fc) {
        return Synt.declare(fc.get("__repeated__"), false);
    }

    protected boolean unstored(Map fc) {
        return Synt.declare(fc.get(  "unstored"  ), false);
    }

    protected boolean ignored (Map fc) {
        return "@".equals(fc.get("__name__"))
            || "" .equals(fc.get("__name__"))
            || "" .equals(fc.get("__type__"))
            || "Ignore".equals (fc.get(  "rule"  ));
    }

    protected boolean replied (Map fc) {
        return replies == null
            || replies.isEmpty (/* ignore empty */)
            || replies.contains(fc.get("__name__"));
    }

    protected boolean queried (Map rd, String fn, Object fv, BooleanQuery.Builder qb) {
        return false;
    }

    protected boolean sorted  (Map rd, String fn, boolean r, List <  SortField  > sf) {
        return false;
    }

    protected void mapAdd(Map map, Document doc) {
        Map<String, Map> fields = getFields( );
        for(Object o : fields.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Map    m = (Map) e.getValue();
            String k = (String)e.getKey();

            if (unstored(m)
            ||  ignored (m)
            || !replied (m)) {
                continue;
            }

            IValue  v ;
            String  t = datatype(m);
            boolean r = repeated(m);
            IndexableField[] fs = doc.getFields(k);

            if ("sorted".equals(t)) {
                continue; // 排序字段没有可见值
            } else
            if (  "date".equals(t)) {
                // 时间戳转 Date 对象时需要乘以 1000
                String  y = Synt.declare(m.get("type"), "");
                if (OBJECT_MODE) {
                    if ("time".equals(y) || "timestamp".equals(y)) {
                        v = new NumberValue( );
                    } else {
                        v = new DatimeValue(m);
                    }
                } else {
                    if ("time".equals(y) || "timestamp".equals(y)) {
                        v = new NumeraValue( );
                    } else {
                        v = new DatextValue(m);
                    }
                }
            } else
            if (   "int".equals(t)
            ||    "long".equals(t)
            ||   "float".equals(t)
            ||  "double".equals(t)
            ||  "number".equals(t)) {
                if (OBJECT_MODE) {
                    v = new NumberValue();
                } else {
                    v = new NumeraValue();
                }
            } else
            if ("object".equals(t)) {
                v = new ObjectValue();
            } else
            {
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
                    map.put(k , v.get( fs[0] ) );
                } else {
                    map.put(k , null);
                }
            }
        }
    }

    protected void docAdd(Document doc, Map map) {
        Map<String, Map> fields = getFields();
        for(Object o : fields.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Map    m = (Map) e.getValue();
            String k = (String)e.getKey();
            Object v = Dict.getParam(map , k);

            doc.removeFields(k);

            if (null == v
            ||  ignored(m)) {
                continue;
            }

            IField  f ;
            String  t = datatype(m);
            boolean s = sortable(m);
            boolean q = findable(m);
            boolean u = unstored(m);
            boolean r = repeated(m);
            boolean g = true; // 是否要存储, 数值类型过滤与存储是分离的, 故其等同于 !unstored

            /**
             * 日期和排序均是长整型
             * 排序字段仅排序不存储
             */
            if (  "date".equals (t)) {
                t = "long";
            }
            if ("sorted".equals (t)) {
                t = "long";
                s =  true ;
                u =  true ;
            }

            /**
             * 数值和搜索及对象类型
             * 空串没有意义不作储存
             */
            if (   "int".equals (t)) { if ("".equals(v)) continue;
                f = new IntField( );
                g = !u;
            } else
            if (  "long".equals (t)) { if ("".equals(v)) continue;
                f = new LongField();
                g = !u;
            } else
            if ( "float".equals (t)) { if ("".equals(v)) continue;
                f = new FloatField();
                g = !u;
            } else
            if ("double".equals (t)) { if ("".equals(v)) continue;
                f = new DoubleField();
                g = !u;
            } else
            if ("string".equals (t)) {
                f = new StringFiald();
                q = false; // 字符类型自带筛选
            } else
            if ("search".equals (t)) {
                f = new SearchFiald();
                q = false; // 搜索类型自带筛选
            } else
            if ("object".equals (t)) { if ("".equals(v)) continue;
                f = new ObjectFiald();
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
            } else
            {
                f = new StoredFiald();
                q = false; // 存储类型无法筛选
                s = false; // 存储类型无法排序
            }

            if (r) {
                if (g) {
                    if (v instanceof Object[ ] ) {
                        for (Object w: (Object[ ] ) v) {
                            doc.add(f.get(k, w, u));
                        }
                    } else
                    if (v instanceof Collection) {
                        for (Object w: (Collection) v) {
                            doc.add(f.get(k, w, u));
                        }
                    } else
                    {
                        Set a = Synt.asSet(v);
                        for (Object w: a) {
                            doc.add(f.get(k, w, u));
                        }
                        v = a;
                    }
                }

                // 筛选和排序前去重
                Set a = Synt.asSet(v);
                if (q && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.whr(k, w));
                    }
                }
                // 排序值不能存多个
                if (s && a != null && !a.isEmpty()) {
//                  for (Object w: a) {
                        v = a.toArray()[ 0 ];
                        doc.add(f.odr(k, v));
//                  }
                }
            } else
            {
                doc.add(f.get( k, v, u));

                if (q) {
                    doc.add(f.whr(k, v));
                }
                if (s) {
                    doc.add(f.odr(k, v));
                }
            }
        }
    }

    /**
     * 组织查询条件
     *
     * 操作符:
     *  eq 等于
     *  ne 不等于
     *  lt 小于
     *  le 小于或等于
     *  gt 大于
     *  ge 大于或等于
     *  on 区间
     *  in 包含
     *  ni 不包含
     * 以下为 Lucene 特有的操作符:
     *  wt 优先度, 设定查询的权重
     *  ai 全包含, 此为目标真子集
     *  si 可包含, 有则优先
     *  se 可等于, 有则优先
     * 注意: 默认情况下查询参数不给值则忽略, 如果指定了操作符则匹配空串
     *
     * @param qry
     * @param k
     * @param v
     * @param q
     * @throws HongsException
     */
    protected void qryAdd(BooleanQuery.Builder qry, String k, Object v, IQuery q)
    throws HongsException {
        Map m;
        if (v instanceof Map) {
            m = new HashMap();
            m.putAll((Map) v);
        } else {
            if (null== v || "".equals(v)) {
                return ;
            }
            m = new HashMap();
            if (v instanceof Collection ) {
                Collection c = (Collection) v;
                    c.remove("");
                if (c.isEmpty()) {
                    return;
                }
                m.put(Cnst.IN_REL, c);
            } else
            if (q instanceof SearchQuery) {
                m.put(Cnst.CQ_REL, v);
            } else
            {
                m.put(Cnst.EQ_REL, v);
            }
        }

        // 对 text 类型指定分词器
        if (q instanceof SearchQuery) {
            Map<String, Map> fields = getFields( );
            Map         fc = ( Map ) fields.get(k);
            setParser(  fc , ( SearchQuery )  q  );
        }

        float                bst = 1.0F;
        BooleanQuery.Builder src = null;
        if (m.containsKey(Cnst.WT_REL)) {
            src = qry;
            qry = new BooleanQuery.Builder();
            bst = Synt.declare(m.remove(Cnst.WT_REL), bst);
        }

        if (m.containsKey(Cnst.EQ_REL)) {
            Object n = m.remove(Cnst.EQ_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST);
        } else
        if (m.containsKey(Cnst.CQ_REL)) {
            Object n = m.remove(Cnst.CQ_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST);
        }

        if (m.containsKey(Cnst.NE_REL)) {
            Object n = m.remove(Cnst.NE_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST_NOT);
        } else
        if (m.containsKey(Cnst.NC_REL)) {
            Object n = m.remove(Cnst.NC_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST_NOT);
        }

        if (m.containsKey(Cnst.SE_REL)) {
            Object n = m.remove(Cnst.SE_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.SHOULD);
        }

        if (m.containsKey(Cnst.AI_REL)) { // All In
            Set a = Synt.declare(m.remove(Cnst.AI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.MUST);
            }
        }

        if (m.containsKey(Cnst.NI_REL)) { // Not In
            Set a = Synt.declare(m.remove(Cnst.NI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.MUST_NOT);
            }
        }

        if (m.containsKey(Cnst.SI_REL)) { // May In
            Set a = Synt.declare(m.remove(Cnst.SI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.SHOULD);
            }
        }

        if (m.containsKey(Cnst.IN_REL)) { // In
            BooleanQuery.Builder qay = new BooleanQuery.Builder();
            Set a = Synt.declare(m.remove(Cnst.IN_REL), new HashSet());
            for(Object x : a) {
                qay.add(q.get(k, x), BooleanClause.Occur.SHOULD);
            }
            qry.add(qay.build(), BooleanClause.Occur.MUST );
        }

        if (m.containsKey(Cnst.IS_REL)) { // Is
            String a = Synt.asString(m.remove(Cnst.IS_REL));
            if ("FILL".equalsIgnoreCase(a)) {
                qry.add(new SearchQuery().get(k, null), BooleanClause.Occur.MUST);
            } else
            if ("NULL".equalsIgnoreCase(a)) {
                qry.add(new SearchQuery().get(k, null), BooleanClause.Occur.MUST_NOT);
            } else
            if ("FINE".equalsIgnoreCase(a)) {
                qry.add(new SearchQuery().get(k, null), BooleanClause.Occur.SHOULD);
            }
        }

        //** 区间查询 **/

        Object  n, x;
        boolean l, g;
        Set s = null;

        if (m.containsKey(Cnst.RN_REL)) {
            s = Synt.setOf(m.remove(Cnst.RN_REL));
        } else
        if (m.containsKey(Cnst.ON_REL)) {
            s = Synt.asSet(m.remove(Cnst.ON_REL));
        }

        if (s != null && !s.isEmpty( )) {
            BooleanQuery.Builder qay = new BooleanQuery.Builder();

            for(Object   o :  s) {
                Object[] a = Synt.toRange(  o  );
                if (null  !=  a) {
                    n = a[0]; l = (boolean) a[2];
                    x = a[1]; g = (boolean) a[3];

                    if (n != null || x != null ) {
                        qay.add(q.get(k, n, x, l, g), BooleanClause.Occur.SHOULD);
                    }
                }
            }

            BooleanQuery qxy = qay.build();
            if (qxy.clauses().size() > 0 ) {
                qry.add(qxy, BooleanClause.Occur.MUST);
            }
        } else {
            if (m.containsKey(Cnst.GE_REL)) {
                n = m.remove (Cnst.GE_REL); l = true ;
            } else
            if (m.containsKey(Cnst.GT_REL)) {
                n = m.remove (Cnst.GT_REL); l = false;
            } else
            {
                n = null; l = false;
            }

            if (m.containsKey(Cnst.LE_REL)) {
                x = m.remove (Cnst.LE_REL); g = true ;
            } else
            if (m.containsKey(Cnst.LT_REL)) {
                x = m.remove (Cnst.LT_REL); g = false;
            } else
            {
                x = null; g = false;
            }

            if ((n != null && ! "".equals(n))
            ||  (x != null && ! "".equals(x)) ) {
                qry.add(q.get(k, n, x, l, g), BooleanClause.Occur.MUST);
            }
        }

        //** 其他包含 **/

        m.remove(""); // 想 IN ('') 必须明确指定 xxx.in=''
        if (!m.isEmpty()) {
            s =  new  HashSet( );
            s.addAll(m.values());
            qryAdd(qry, k, s, q);
        }

        //** 权重设置 **/

        if (src != null ) {
            BooleanQuery qay = qry.build();
            if (qay.clauses().size() > 0 ) {
                src.add(new BoostQuery(qay, bst), BooleanClause.Occur.MUST);
            }
        }
    }

    //** 辅助对象 **/

    /**
     * 查询迭代器
     */
    public static class Loop implements Iterable<Map>, Iterator<Map> {
        private final IndexSearcher finder;
        private final IndexReader   reader;
        private final LuceneRecord  that;
        private       ScoreDoc[]    docs;
        private       ScoreDoc      doc ;
        private final Query   q;
        private final Sort    s;
        private final int     b; // 起始位置
        private final int     l; // 单次限制
        private       int     L; // 起始限制
        private       int     h; // 单次总数
        private       int     H; // 全局总数
        private       int     i; // 提取游标
        private       boolean A; // 无限查询

        /**
         * 查询迭代器
         * @param that 记录实例
         * @param q 查询对象
         * @param s 排序对象
         * @param b 起始偏移
         * @param l 查询限额
         */
        public Loop(LuceneRecord that, Query q, Sort s, int b, int l) {
            this.that = that;
            this.docs = null;
            this.doc  = null;
            this.q    = q;
            this.s    = s;
            this.b    = b;

            // 是否获取全部
            if ( l  ==  0) {
                 l = 1000;
                 A = true;
            }

            this.l    = l;
            this.L    = l;

            // 起始位置偏移
            if ( b  !=  0) {
                 L  +=  b;
            }

            // 获取查读对象
            try {
                finder = that.getFinder();
                reader = that.getReader();
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
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.search/***/(/***/q, L, s);
                    } else {
                        tops = finder.search/***/(/***/q, L);
                    }
                    docs = tops.scoreDocs;
                    h    = docs.length;
                    i    = b;
                    H    = h;
                    L    = l;
                } else
                if ( A && L <= i ) {
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.searchAfter(doc, q, l, s);
                    } else {
                        tops = finder.searchAfter(doc, q, l);
                    }
                    docs = tops.scoreDocs;
                    h    = docs.length;
                    i    = 0;
                    H   += h;
                }
                return i < h;
            } catch (IOException ex) {
                throw new HongsExemption.Common(ex);
            }
        }

        @Override
        public Map next() {
            if ( i >= h ) {
                throw new NullPointerException("hasNext not run?");
            }
            try {
                /*Read*/ doc = docs[i++];
                Document dox = reader.document( doc.doc );
                return that.doc2Map(dox);
            } catch (IOException ex) {
                throw new HongsExemption.Common(ex);
            }
        }

        /**
         * 获取命中总数
         * 注意:
         * 初始化时 y 参数为 0 (即获取全部命中)
         * 则在全部循环完后获取到的数值才是对的
         * 但其实此时完全可以直接计算循环的次数
         * 此方法主要用于分页时获取查询命中总量
         * @return
         */
        public int size() {
            hasNext();
            return H ;
        }

        /**
         * @deprecated
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported remove in lucene loop.");
        }

        @Override
        public String toString() {
            hasNext();
            StringBuilder sb = new StringBuilder(q.toString());
            if ( s != null ) {
                sb.append( " Sort: ");
                sb.append( s );
            }
            if ( l != 0 || b != 0 ) {
                sb.append(" Limit: ");
                sb.append( b );
                sb.append(",");
                sb.append( l );
            }
            return sb.toString();
        }
    }

}
