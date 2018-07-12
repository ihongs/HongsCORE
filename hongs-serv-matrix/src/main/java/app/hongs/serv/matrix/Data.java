package app.hongs.serv.matrix;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExemption;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.dh.search.SearchEntity;
import app.hongs.util.Synt;
import app.hongs.util.Tool;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;

/**
 * 数据存储模型
 * @author Hongs
 */
public class Data extends SearchEntity {

    protected final String comf;
    protected final String conf;
    protected final String form;
    protected long time = 0;

    /**
     * 数据实例基础构造方法
     * @param conf 当前配置文件
     * @param form 表单配置名称
     * @param comf 基础配置文件
     * @param path 数据存放路径
     * @param name 数据标识名称
     */
    protected Data(String conf, String form, String comf, String path, String name) {
        super(null, path, name);
        this.comf = comf;
        this.conf = conf;
        this.form = form;
    }

    /**
     * 数据实例快捷构造方法
     * @param conf 当前配置文件
     * @param form 表单配置名称
     */
    public Data(String conf, String form) {
        this( conf  ,  form
            , conf.replaceFirst("^(centre)/", "centra/")
            , conf.replaceFirst("^(centre|centra)/", "")
            , conf+"."+form );
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
     * 当表单不在管理区域时
     * 会用当前表单覆盖管理表单
     * 配置文件不存在则抛出异常 0x1104
     * @return
     */
    @Override
    public Map getFields() {
        try {
            return super.getFields();
        } catch (NullPointerException ex) {
            // Nothing todo
        }

        Map fields, fieldx;

        /**
         * 字段以 centra/data 的字段为基础
         * 但可在 centre/data 重设部分字段
         *
         * 配置文件不得放在资源包里面
         * 此处会校验表单文件是否存在
         */
        try {
            if (! new File(
                Core.CONF_PATH + "/"+ conf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExemption(0x1104, "Data form conf '" + conf + "' is not exists")
                    .setLocalizedOptions(conf);
            }

            fields = FormSet.getInstance(conf).getForm(form);

        if (! comf.equals(conf)) {
            if (! new File(
                Core.CONF_PATH + "/"+ comf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExemption(0x1104, "Data form conf '" + comf + "' is not exists")
                    .setLocalizedOptions(comf);
            }

            fieldx = FormSet.getInstance(comf).getForm(form);

            // 补充上额外的字段设置
            fieldx = new LinkedHashMap(fieldx);
            fieldx.putAll( fields );
            fields = fieldx;
        }
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }

        setFields(fields);

        return fields;
    }

    public String getFormId() {
        return Synt.declare(getParams().get("form_id"), form );
    }

    public Model getModel() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getModel(tn);
    }

    public Table getTable() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

    /**
     * 创建记录
     * @param rd
     * @return id,name等(由dispCols指定)
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        long ct = System.currentTimeMillis() / 1000;
             rd.put("ctime" , ct);
        String id = Core.newIdentity();
        save ( id , rd );
        call ( id , "create", ct);

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
        long ct = System.currentTimeMillis() / 1000;
             rd.put("ctime" , ct);
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1096);
        for(String  id  : ids) {
           save(id, rd  );
           call(id, "update", ct);
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
        long ct = System.currentTimeMillis() / 1000;
             rd.put("ctime" , ct);
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        permit (rd, ids , 0x1097);
        for(String  id  : ids) {
           drop(id, rd  );
           call(id, "delete", ct);
        }
        return ids.size();
    }

    public void save(String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";
        Object[] param = new  String [ ] { id , fid , "0"};
        long     ctime = Synt.declare(rd.get("ctime"), 0L);
        if (0 == ctime) ctime = System.currentTimeMillis()/1000;

        // 获取旧的数据
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
                    dd = (Map) app.hongs.util.Data.toObject(od.get("data").toString());
                }
            }
        }

        // 合并新旧数据
        int i = 0;
        Map<String,Map> fields = getFields();
        for(String fn : fields.keySet()) {
            if (  "id". equals(fn)) {
                dd.put(fn , id);
            } else
            if (rd.containsKey(fn)) {
                Object fr = Synt.defoult(rd.get(fn), "");
                Object fo = Synt.defoult(dd.get(fn), "");
                dd.put(fn , fr);

                if (!equals(fr,fo)
                &&  !fn.equals("muser" )
                &&  !fn.equals("mtime")) {
                    i ++; // 需要排除修改环境数据
                }
            }
        }

        // 无更新不存储
        if (i == 0) {
            return;
        }

        dd.put("name", getName(dd));
        dd.put("word", getWord(dd));

        //** 保存到数据库 **/

        if (table != null) {
            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            nd.put(/***/"id", id );
            nd.put("form_id", fid);
            nd.put("user_id", uid);
            nd.put("memo", rd.get("memo"));
            nd.put("name", dd.get("name"));
            nd.put("data", app.hongs.util.Data.toString(dd));

            table.update(ud, where, param);
            table.insert(nd);
        }

        //** 保存到索引库 **/

        Document doc = new Document();
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

    public void drop(String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";
        Object[] param = new  String [ ] { id , fid , "0"};
        long     ctime = Synt.declare(rd.get("ctime"), 0L);
        if (0 == ctime) ctime = System.currentTimeMillis()/1000;

        /** 记录到数据库 **/

        if (table != null) {
            Map dd = table.fetchCase()
                .filter( where, param)
                .select("ctime, state, data, name")
                .getOne( );
            if (dd.isEmpty()) {
                throw new HongsException(0x1104, "原始记录不存在");
            }
            if ( Synt.declare ( dd.get("ctime"), 0L )  >=  ctime ) {
                throw new HongsException(0x1100, "等会儿, 不要急");
            }

            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            ud.put("state",   0  );
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
    }

    public void redo(String id, Map rd) throws HongsException {
        Table    table = getTable( );
        String   fid   = getFormId();
        String   uid   = (String) rd.get( "user_id" );
        String   where = "`id`=? AND `form_id`=? AND `ctime`=?";
        long     rtime = Synt.declare(rd.get("rtime"), 0L);
        long     ctime = Synt.declare(rd.get("ctime"), 0L);
        Object[] param = new Object [ ] { id, fid, rtime };
        if (0 == ctime) ctime = System.currentTimeMillis()/1000;

        //** 获取旧的数据 **/

        if (table == null) {
            throw new HongsException(0x1100, "资源不支持恢复");
        }
        Map dd = table.fetchCase()
            .filter( where, param)
            .select("etime, state, data, name")
            .getOne( );
        if (dd.isEmpty()) {
            throw new HongsException(0x1100, "起源记录不存在");
        }
        if ( Synt.declare ( dd.get("etime"), 0L )  ==   0L   ) {
            throw new HongsException(0x1100, "已经是最新记录");
        }
        if ( Synt.declare ( dd.get("ctime"), 0L )  >=  ctime ) {
            throw new HongsException(0x1100, "等会儿, 不要急");
        }

        //** 保存到数据库 **/

        Map ud = new HashMap();
        ud.put("etime", ctime);

        rd.put("ctime", ctime);
        rd.put("rtime", rtime);
        rd.put("etime",   0  );
        rd.put("form_id", fid);
        rd.put("user_id", uid);
        rd.put("name" , dd.get("name"));
        rd.put("data" , dd.get("data"));

        where = "`id`=? AND `form_id`=? AND `etime`=?";
        param = new Object[]{id, fid,0};
        table.update(ud , where, param);
        table.insert(rd);

        //** 保存到索引库 **/

        dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());

        Document doc = new Document(  );
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

    public void call(String id, String act, long now) throws HongsException {
        String url = (String) getParams().get("callback");
        if (url == null || "".equals(url)) {
            return;
        }

        String fid = getFormId();
        url = Tool.inject(url,Synt.mapOf(
            "id"    , id ,
            "action", act,
            "entity", fid,
            "time"  , now
        ));
        DataCaller.getInstance().add(url);
    }

    protected boolean equals(Object fo, Object fr) {
        return fo.equals(fr);
    }

    private Set<String> wdCols = null;
    private Set<String> nmCols = null;

    @Override
    public Set<String> getSrchable() {
        if (null != wdCols) {
            return  wdCols;
        }
        Map fs = (Map) getFields().get("word");
        if (fs != null && !Synt.declare(fs.get("readonly"), false)) {
            wdCols =  Synt.setOf  (  "word"  );
        } else {
            wdCols =  getCaseNames("srchable");
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

    /**
     * 获取搜索串
     * @param dd
     * @return
     * @throws HongsException
     */
    protected String getWord(Map dd) throws HongsException {
        StringBuilder nn = new StringBuilder();
        Set < String> ns = getSrchable( );
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

    /**
     * 获取名称串
     * @param dd
     * @return
     * @throws HongsException
     */
    protected String getName(Map dd) throws HongsException {
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

}
