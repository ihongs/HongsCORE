package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.dh.lucene.field.*;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;

import org.apache.lucene.document.Document;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据存储模型
 * @author Hongs
 */
public class Data extends SearchEntity {

    protected final String conf;
    protected final String form;
    private   final Set<String> dcUrls = new LinkedHashSet();
    private         Set<String> nmCols = null;
    private         Set<String> wdCols = null;
    private         Set<String> skCols = null;

    /**
     * 数据实例基础构造方法
     * @param conf 当前配置文件
     * @param form 当前表单名称
     */
    protected Data(String conf, String form) {
        super(null, null, null);
        this.conf = conf;
        this.form = form;
    }

    /**
     * 获取实例
     * 生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     */
    public static Data getInstance(String conf, String form) {
        Data   inst;
        Core   core = Core.getInstance();
        String name = Data.class.getName() +":"+ conf +":"+ form;
        if (core.containsKey(name)) {
            inst = (Data) core.got(name);
        } else {
            inst = new Data( conf, form);
            core.put ( name, inst);
        }
        return inst;
    }

    /**
     * 获取字段
     * 另一方法非常可能需要覆盖,
     * 故提供此方法以便从基类调,
     * 未设时抛出 NullPointerException
     * @return
     */
    protected final Map gotFields() {
        return  super . getFields();
    }

    /**
     * 获取字段
     * 当前表单不在管理区之内时,
     * 会用当前表单覆盖管理表单,
     * 配置文件不存在则抛出异常 0x1104
     * @return
     */
    @Override
    public Map getFields() {
        try {
            return gotFields();
        }
        catch (NullPointerException ex) {
            // 使用超类来管理字段集合
            // 拿不到就走下面流程填充
        }

        /**
         * 字段以 centra/data 的字段为基础
         * 但可在 centre/data 重设部分字段
         *
         * 配置文件不得放在资源包里面
         * 此处会校验表单文件是否存在
         */
        Map fields = null;
        Map fieldx = null;
        String cnf = conf;
        do {
            try {
                fields = FormSet.getInstance(cnf).getForm(form);
            } catch (HongsException ex) {
                if (ex.getErrno() != 0x10e8
                &&  ex.getErrno() != 0x10ea) {
                    throw ex.toExemption();
                }
                break;
            }

            if (fields != null && cnf.startsWith("centre/")) {
                cnf = "centra/" + cnf.substring (7);
            } else {
                break;
            }

            try {
                fieldx = FormSet.getInstance(cnf).getForm(form);
            } catch (HongsException ex) {
                if (ex.getErrno() != 0x10e8
                &&  ex.getErrno() != 0x10ea) {
                    throw ex.toExemption();
                }
                break;
            }

            // 注意:
            // 1. 不可破坏原始配置
            // 2. 当前的覆盖后台的
            fieldx = new LinkedHashMap(fieldx);
            fieldx.putAll(fields);
            fields = fieldx ;
        }   while  ( false );

        if ( null == fields) {
            throw new HongsExemption(0x1104, "Data form conf '" + conf + "' is not exists")
                .setLocalizedOptions(conf);
        }

        setFields(fields);
        return    fields ;
    }

    @Override
    public String getDbPath() {
        String path = conf.replaceFirst("^(centre|centra)/" , "") +"/"+ form;
        path = Synt.declare(getParams().get("db-path"),path);

        // 进一步处理路径
        Map m = new HashMap();
        m.put("SERVER_ID", Core.SERVER_ID);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Tool.inject(path, m);
        if ( ! new File(path).isAbsolute())
        path = Core.DATA_PATH + "/lucene/" + path;

        return path;
    }

    @Override
    public String getDbName() {
        String name = conf.replaceFirst("^(centre|centra)/" , "") +"/"+ form;
        return Synt.declare(getParams().get("db-name"),name);
    }

    public String getFormId() {
        return Synt.declare(getParams().get("form_id"),form);
    }

    public Model getModel() throws HongsException {
        String tn = Synt.declare(getParams().get("db-table"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getModel(tn);
    }

    public Table getTable() throws HongsException {
        String tn = Synt.declare(getParams().get("db-table"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

    /**
     * 创建记录
     * @param rd
     * @return id,name等(可由listable指定)
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        long ct = System.currentTimeMillis() / 1000 ;
        String id = Core.newIdentity();
        save ( ct , id , rd );
        call ( ct , id , "create" );

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
        int  cn = 0;
        long ct = System.currentTimeMillis() / 1000 ;
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1096);
        for(String  id  : ids) {
           cn += save(ct, id, rd);
           call(ct, id, "update");
        }
        return  cn;
    }

    /**
     * 删除记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
        int  cn = 0;
        long ct = System.currentTimeMillis() / 1000 ;
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1097);
        for(String  id  : ids) {
           cn += drop(ct, id, rd);
           call(ct, id, "delete");
        }
        return  cn;
    }

    /**
     * 恢复记录
     * @param rd
     * @return
     * @throws HongsException
     */
    public int revert(Map rd) throws HongsException {
        long ct = System.currentTimeMillis() / 1000 ;
        String id = ( String ) rd.get( Cnst.ID_KEY );
        permit(rd , Synt.setOf(id), 0x1096 );
        int  cn = redo(ct, id, rd);
        call(ct, id, "revert");
        return cn;
    }

    /**
     * 保存记录
     * @param ctime
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int save(long ctime, String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        Object[] param = new String[] { id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        if (uid == null) {
            throw new NullPointerException("user_id required!");
        }

        // 获取旧的数据
        int st = 1;
        Map dd = get( id );
        if (! dd.isEmpty()) {
            if (table != null) {
                Map od = table.fetchCase()
                    .filter( where, param)
                    .select("ctime")
                    .getOne( );
                if (! od.isEmpty( )) {
                    if ( Synt.declare ( od.get("ctime"), 0L )  >=  ctime ) {
                        throw new HongsException(0x1100, "等会儿, 不要急");
                    }
                }
            }
        } else {
            if (table != null) {
                Map od = table.fetchCase()
                    .filter( where, param)
                    .select("ctime, data")
                    .getOne();
                if (! od.isEmpty( )) {
                    if ( Synt.declare ( od.get("ctime"), 0L )  >=  ctime ) {
                        throw new HongsException(0x1100, "等会儿, 不要急");
                    }
                    dd = (Map) io.github.ihongs.util.Data.toObject(od.get("data").toString());
                } else {
                    st = 2; // 新增
                }
            } else {
                    st = 2; // 新增
            }
        }

        // 合并新旧数据
        int i  = 0;
        Map<String,Map> fields = getFields();
        for(String fn : fields . keySet( ) ) {
            if (  "id". equals(fn)) {
                dd.put(fn , id);
            } else
            if (rd.containsKey(fn)) {
                Object fr = rd.get(fn);
                Object fo = dd.get(fn);
                dd.put(fn , fr);
                // 跳过环境字段, 比如修改时间
                if (! canSkip (fn, fr, fo) ) {
                    i ++;
                }
            }
        }
        // 无更新不存储
        if (i == 0) {
          return 0;
        }

        //** 保存到数据库 **/

        if (table != null) {
            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            nd.put("state",   st );
            nd.put(/***/"id", id );
            nd.put("form_id", fid);
            nd.put("user_id", uid);
            nd.put("memo", rd.get("memo"));
            nd.put("name", dd.get("name"));
            nd.put("data", io.github.ihongs.util.Data.toString(dd, true));

            table.update(ud, where, param);
            table.insert(nd);
        }

        //** 保存到索引库 **/

        Document doc = new Document();
        dd.put(Cnst.ID_KEY, id);
        padDoc(doc, dd , null );
        setDoc(id, doc);

        return 1;
    }

    /**
     * 删除记录
     * @param ctime
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int drop(long ctime, String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        Object[] param = new String[] { id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        if (uid == null) {
            throw new NullPointerException("user_id required!");
        }

        /** 记录到数据库 **/

        if (table != null) {
            Map dd = table.fetchCase()
                .filter( where, param)
                .select("ctime, state, name, data")
                .getOne( );
            if (dd.isEmpty()) {
                 delDoc( id ); return 0; // 规避关系库无而搜索库有
//              throw new HongsException(0x1104, "找不到原始记录");
            }
            if ( Synt.declare ( dd.get("state"), 0  )  ==   0    ) {
                 delDoc( id ); return 0; // 删除是幂等的可重复调用
//              throw new HongsException(0x1100, "禁操作删除记录");
            }
            if ( Synt.declare ( dd.get("ctime"), 0L )  >=  ctime ) {
                throw new HongsException(0x1100, "等会儿, 不要急");
            }

            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            nd.put("state",   0  );
            nd.put(/***/"id", id );
            nd.put("form_id", fid);
            nd.put("user_id", uid);
            nd.put("memo", rd.get("memo"));
            nd.put("name", dd.get("name"));
            nd.put("data", dd.get("data"));

            table.update(ud, where, param);
            table.insert(nd);
        }

        //** 从索引库删除 **/

        delDoc(id);

        return 1;
    }

    /**
     * 恢复记录
     * @param ctime
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int redo(long ctime, String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        Object[] param = new String[] { id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";
        long     rtime = Synt.declare(rd.get("rtime"), 0L);
        Object[] para2 = new String[] { id, fid, ""+rtime};
        String   wher2 = "`id`=? AND `form_id`=? AND `ctime`=?";

        if (uid == null) {
            throw new NullPointerException("user_id required!");
        }
        if (rtime == 0L) {
            throw new NullPointerException( "rtime required!" );
        }

        //** 获取旧的数据 **/

        if (table == null) {
            throw new HongsException(0x1100, "资源不支持恢复");
        }
        Map ld = table.fetchCase()
            .filter( where, param)
            .select("ctime")
            .getOne( );
        if ( Synt.declare ( ld.get("ctime"), 0L )  >=  ctime ) {
            throw new HongsException(0x1100, "等会儿, 不要急");
        }
        Map dd = table.fetchCase()
            .filter( wher2, para2)
            .getOne( );
        if (dd.isEmpty()) {
            throw new HongsException(0x1100, "找不到恢复起源");
        }
        // 删除时保留的是删除前的快照, 即使为最终记录仍然可以恢复
        if ( Synt.declare ( dd.get("state"), 0  )  !=   0    ) {
        if ( Synt.declare ( dd.get("etime"), 0L )  ==   0L   ) {
            throw new HongsException(0x1100, "这已是最终记录");
        }}

        //** 保存到数据库 **/

        Map ud = new HashMap();
        ud.put("etime", ctime);

        dd.put("ctime", ctime);
        dd.put("rtime", rtime);
        dd.put("etime",   0  );
        dd.put("state",   3  );
        dd.put("form_id", fid);
        dd.put("user_id", uid);
        dd.put("memo" , rd.get("memo"));

        table.update(ud , where, param);
        table.insert(dd);

        //** 保存到索引库 **/

        dd = (Map) io.github.ihongs.util.Data.toObject(dd.get("data").toString());

        Document doc = new Document(  );
        dd.put(Cnst.ID_KEY, id);
        padDoc(doc, dd , null );
        setDoc(id, doc);

        return 1;
    }

    /**
     * 外部回调
     * @param xtime
     * @param id
     * @param on
     * @return 有回调为 1, 无回调为 0
     * @throws HongsException
     */
    public int call(long xtime, String id, String on) throws HongsException {
        String url = (String) getParams().get("callback");
        if (url == null || "".equals(url)) {
            return 0;
        }

        String fid = getFormId();
        url = Tool.inject(url,Synt.mapOf(
            "form_id", fid,
            "id"     , id ,
            "type"   , on ,
            "time"   , xtime
        ));

        dcUrls.add(url);

        return 1;
    }

    @Override
    public void close() {
        super . close();

        // 离开时通知第三方
        try {
            DataCaller dc = DataCaller.getInstance();
            for(String du : dcUrls) {
                dc.add(du);
            }
        } catch (HongsException e ) {
            throw e.toExemption(  );
        } finally {
            dcUrls.clear();
        }
    }

    @Override
    protected void padDoc(Document doc, Map map, Set rep) {
        if (rep != null && rep.isEmpty( )) {
            rep  = null;
        }

        Map<String, Map> fields = getFields();
        for(Map.Entry<String, Map> e : fields.entrySet()) {
            Map    m = e.getValue();
            String k = e.getKey  ();
            Object v = Dict.getParam(map , k);

            doc.removeFields(k);

            if (rep != null
            && !rep.contains(k)) {
                continue;
            }

            /**
             * 补充:
             * 需写入名称和关键词
             * 2019/03/23
             * 存在外部只读才拼接
             */
            if (k != null) switch (k) {
            case "name":
                if (Synt.declare(m.get("readonly"), false)) {
                    v = getName(map);
                }
                break;
            case "word":
                if (Synt.declare(m.get("readonly"), false)) {
                    v = getWord(map);
                }
                break;
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

            if (t != null) switch (t) {
            case "int":
                if ("".equals(v)) continue;
                f = new IntField();
                p = false;
                break;
            case "long":
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
                if ("".equals(v)) continue;
                f = new DoubleField();
                p = false;
                break;
            case "date":
                if ("".equals(v)) continue;
                f = new LongField();
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
            case "stored":
                f = new StringFiald();
                g = true ;
                p = false; // 存储类型无法搜索
                q = false; // 存储类型无法筛选
                s = false; // 存储类型无法排序
                break;
            case "object":
                if ("".equals(v)) continue;
                f = new ObjectFiald();
                g = true ;
                p = false; // 对象类型无法搜索
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
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
             * 补充:
             * 找出那些 textview 类的字段
             * 索引前需将标签代码清理干净
             */
            Object x = v;
            if (p && "textview".equals(m.get("__type__"))) {
                x = Tool.stripEnds(
                    Tool.stripTags(
                    Tool.stripCros(
                    Synt.asString ( x )
                    )) );
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

                // 条件类可去重
                Set a = Synt.asSet(v);

                if (s && a != null && !a.isEmpty()) {
                    Object  w = a.toArray( )[0]; // 排序值不能存多个
                        doc.add(f.odr(k, w));
                }
                if (q && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.whr(k, w));
                    }
                }
                if (p && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.wdr(k, w));
                    }
                }
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
                    doc.add(f.wdr(k, x));
                }
            }
        }
    }

    /**
     * 保存中跳过的字段或取值
     * 当返回 true 时跳过检查,
     * 如都是 true 则不做更新.
     * @param fn
     * @param fr 新值
     * @param fo 旧值
     * @return
     */
    protected boolean canSkip(String fn, Object fr, Object fo) {
        if (getSkipable().contains(fn)) {
            return true ;
        }
        if (fr == null && fo == null) {
            return true ;
        }
        if (fr == null || fo == null) {
            return false;
        }
        // 复杂对象用 JSON 串进行对比
        if (fr instanceof Map
        ||  fr instanceof Collection
        ||  fr instanceof Object [ ]) {
            fr = io.github.ihongs.util.Data.toString(fr, true);
            fo = io.github.ihongs.util.Data.toString(fo, true);
        }
        return fr.equals(fo);
    }

    /**
     * 获取名称串
     * @param dd
     * @return
     */
    protected String getName(Map dd) {
        StringBuilder nn = new StringBuilder();
        Set < String> ns = getNameable( );
        for ( String  fn : ns  ) {
              Object  fv = dd.get(fn);
            if (fv == null) continue ;
            if (fv instanceof Collection)
            for (Object fw : (Collection) fv ) {
                nn.append(fw).append(' ');
            } else {
                nn.append(fv).append(' ');
            }
        }
        String nm = nn.toString().trim( );

        if (! ns.contains("name")
        &&    99 < nm.length( ) ) {
            return nm.substring(0, 99) + "...";
        } else {
            return nm;
        }
    }

    /**
     * 获取关键词
     * @param dd
     * @return
     */
    protected String getWord(Map dd) {
        StringBuilder nn = new StringBuilder();
        Set < String> ns = getWordable( );
        for ( String  fn : ns ) {
              Object  fv = dd.get(fn);
            if (fv == null) continue ;
            if (fv instanceof Collection)
            for (Object fw : (Collection) fv ) {
                nn.append(fw).append(' ');
            } else {
                nn.append(fv).append(' ');
            }
        }
        String nm = nn.toString().trim( );

        if (! ns.contains("word")
        &&  ! ns.contains("id") ) {
            return dd.get("id") +" "+ nm ;
        } else {
            return nm;
        }
    }

    @Override
    public Set<String> getCaseTypes(String t) {
        if ("nameable".equals(t)) {
            return Synt.setOf("string", "search", "text");
        } else
        if ("wordable".equals(t)) {
            return super.getCaseTypes("srchable");
        } else
        {
            return super.getCaseTypes(t);
        }
    }

    @Override
    public Set<String> getWordable() {
        if (null != wdCols) {
            return  wdCols;
        }
        Map fs = (Map) getFields().get("word");
        if (fs != null && !Synt.declare(fs.get("readonly"), false)) {
            wdCols =  Synt.setOf  (  "word"  );
        } else {
            wdCols =  getCaseNames("wordable");
            wdCols.remove("word");
        }
        return wdCols;
    }

    public Set<String> getNameable() {
        if (null != nmCols) {
            return  nmCols;
        }
        Map fs = (Map) getFields().get("name");
        if (fs != null && !Synt.declare(fs.get("readonly"), false)) {
            nmCols =  Synt.setOf  (  "name"  );
        } else {
            nmCols =  getCaseNames("nameable");
            nmCols.remove("name");
        }
        return nmCols;
    }

    public Set<String> getSkipable() {
        if (null != skCols) {
            return  skCols;
        }
        skCols = new HashSet(getCaseNames("skipable"));
        skCols.add("mtime");
        skCols.add("muser");
        skCols.add("memo" );
        return skCols;
    }

}
