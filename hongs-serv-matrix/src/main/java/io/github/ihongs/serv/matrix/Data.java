package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;

/**
 * 数据存储模型
 *
 * <pre>
 * 错误代码:
 * matrix.form.not.exists=表单配置文件不存在
 * matrix.wait.one.second=等会儿, 不要急
 * matrix.item.is.removed=记录已被删除了
 * matrix.node.not.exists=找不到恢复起源
 * matrix.node.is.current=这已是最终记录
 * matrix.rev.unsupported=资源不支持恢复
 * </pre>
 *
 * @author Hongs
 */
public class Data extends SearchEntity {

    /**
     * 分区字段名
     */
    public static final String  PART_ID_KEY  =  "pd";

    protected     final String  conf   ;
    protected     final String  form   ;
    private             String  userId = null;
    private         Set<String> nmCols = null;
    private         Set<String> wdCols = null;
    private         Set<String> skCols = null;
    private  final  Set<String> setIds = new LinkedHashSet();
    private  final  Set<String> delIds = new LinkedHashSet();

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
     *
     * <pre>
     * 配置如指定 db-class 则调用类的:
     * getInstance(conf, form)
     * getInstance()
     * new  Xxxx  ()
     * 此类必须是 Data 的子类.
     * 存在以上任一方法即返回,
     * 首个需自行维护生命周期,
     * 后两个交由 Core 作存取.
     * </pre>
     *
     * <pre>
     * 错误代码:
     * 821  找不到对应的类
     * 822  构建方法不可用
     * 823  构建实例不成功
     * 910  配置文件不存在
     * 912  表单信息不存在
     * </pre>
     *
     * @param conf
     * @param form
     * @return
     * @throws HongsException 表单获取失败
     * @throws HongsExemption 实例构建失败
     * @throws ClassCastException 不是 Data 的子类
     */
    public static Data getInstance(String conf , String form) throws HongsException {
        // 外部指定
        Map    dict = FormSet.getInstance(conf).getForm(form);
        String name = ( String ) Dict.get(dict, null, "@", "db-class");
        if (name != null && !name.isEmpty() && !name.equals(Data.class.getName( ))) {
            Class klass ;
            try {
                  klass = Class.forName (name);
            } catch (ClassNotFoundException e) {
                throw new HongsExemption(821, "Can not find class by name '"+name+"'." );
            }

            try {
                return (Data) klass
                    .getMethod("getInstance", new Class [] {String.class, String.class})
                    .invoke   (    null     , new Object[] {  conf      ,   form      });
            } catch (NoSuchMethodException ex) {
                return (Data) Core.getInstance(klass);
            } catch (InvocationTargetException ex) {
                Throwable ta = ex.getCause(  );
                // 调用层级过多, 最好直接抛出
                if (ta instanceof StackOverflowError) {
                    throw (StackOverflowError) ta;
                }
                throw new HongsExemption(823, "Can not call '"+name+".getInstance'", ta);
            } catch ( IllegalArgumentException ex) {
                throw new HongsExemption(823, "Can not call '"+name+".getInstance'", ex);
            } catch (   IllegalAccessException ex) {
                throw new HongsExemption(823, "Can not call '"+name+".getInstance'", ex);
            } catch (        SecurityException se) {
                throw new HongsExemption(822, "Can not call '"+name+".getInstance'", se);
            }
        }

        // 默认构造
        name = Data.class.getName() +":"+ conf +"."+ form;
        Core core =  Core.getInstance(  );
        Data inst = (Data) core.get(name);
        if (inst == null) {
            inst = new Data (conf , form);
            core.set ( name, inst );
        }
        return inst;
    }

    public Table getTable() throws HongsException {
        String tn = Synt.declare(getParams().get("db-table"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

    public Model getModel() throws HongsException {
        String tn = Synt.declare(getParams().get("db-model"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getModel(tn);
    }

    /**
     * 获取参数
     * 另一方法非常可能需要覆盖,
     * 故提供此方法以便从基类调,
     * 未设时抛出 NullPointerException
     * @return
     */
    protected final Map gotParams() {
        return  super . getParams();
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
     * 获取参数
     * 取不到则会尝试先获取字段.
     * @return
     */
    @Override
    public Map getParams() {
        try {
            return gotParams();
        }
        catch (NullPointerException ex) {
                   getFields();
            return gotParams();
        }
    }

    /**
     * 获取字段
     * 当前表单不在管理区之内时,
     * 会用当前表单覆盖管理表单,
     * 配置文件不存在则抛出异常 404
     * @return
     */
    @Override
    public Map getFields() {
        try {
            return gotFields();
        }
        catch (NullPointerException ex) {
            // 使用超类管理字段
            // 拿不到就进行填充
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
                if (ex.getErrno() != 910
                &&  ex.getErrno() != 912) { // 非表单缺失
                    throw ex.toExemption();
                }
                break;
            }

            if (fields == null) {
                break;
            }
            cnf = getBgConf(  );
            if (  cnf  == null) {
                break;
            }

            try {
                fieldx = FormSet.getInstance(cnf).getForm(form);
            } catch (HongsException ex) {
                if (ex.getErrno() != 910
                &&  ex.getErrno() != 912) { // 非表单缺失
                    throw ex.toExemption();
                }
                break;
            }

            /**
             * 注意:
             * 1. 不可破坏原始配置
             * 2. 当前的覆盖后台的
             */
            Map fieldz = new LinkedHashMap(fieldx);
                fieldz.putAll (fields);
                fields = /**/  fieldz ;

            // 3. 表单参数可被重写, 2019/08/10
            Map params = (Map) fields .get( "@"  );
            Map paramx = (Map) fieldx .get( "@"  );
            if ( null != params && null != paramx) {
                paramx = new LinkedHashMap(paramx);
                paramx.putAll (params);
                fields.put("@",paramx);
            }
        }   while  ( false );

        if ( null == fields) {
            throw new HongsExemption(910, "Data form '"+conf+"."+form+"' is not exists")
                .setLocalizedContent("matrix.form.not.exists")
                .setLocalizedContext("matrix");
        }

        setFields(fields);
        return    fields ;
    }

    /**
     * 获取背景
     * 当前表单不在管理区之内时,
     * 会用当前表单覆盖管理表单,
     * 此可获取对内配置, 用于 getFields
     * @return
     */
    protected String getBgConf() {
        if ( ! conf.startsWith("centre/") ) {
            return null;
        }
        return "centra/"+ conf.substring(7);
    }

    @Override
    public String getDbPath() {
        try {
            return super.getDbPath();
        }
        catch (NullPointerException ex) {
            // Will build the path.
        }

        String path = Synt.asString(getParams().get("db-path"));

        // 按配置构建路径
        if (path == null || path.isEmpty()) {
            if (conf.startsWith("centra/")
            ||  conf.startsWith("centre/")) {
                path = "matrix/"+ conf.substring(7) +"/"+ form ;
            } else {
                path = conf +"/"+ form;
            }
        }

        // 进一步处理路径
        Map m = new HashMap();
        m.put("SERVER_ID", Core.SERVER_ID);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Syno.inject(path, m);
        if ( ! new File(path).isAbsolute())
        path = Core.DATA_PATH +"/lucene/"+ path;

        setDbPath(path);

        return path;
    }

    @Override
    public String getDbName() {
        try {
            return super.getDbName();
        }
        catch (NullPointerException ex) {
            // Will build the name.
        }

        String name = Synt.asString(getParams().get("db-name"));

        // 按配置构建路径
        if (name == null || name.isEmpty()) {
            if (conf.startsWith("centra/")
            ||  conf.startsWith("centre/")) {
                name = "matrix/"+ conf.substring(7) +"."+ form ;
            } else {
                name = conf +"."+ form;
            }
        }

        setDbName(name);

        return name;
    }

    public String getFormId() {
        String code = Synt.asString(getParams().get("form_id"));

        // 默认同表单名称
        if (code == null || code.isEmpty()) {
            code  = form;
        }

        return code;
    }

    public String getPartId() {
        String code = Synt.asString(getParams().get("part_id"));

        return code;
    }

    public String getUserId() {
        if ( null == userId ) {
            try {
                userId = (String) ActionHelper.getInstance()
                                . getSessibute(Cnst.UID_SES);
            } catch (UnsupportedOperationException e ) {
                throw new NullPointerException("Call setUserId first");
            }
            if ( null == userId ) {
                throw new NullPointerException("Call setUserId first");
            }
        }
        return userId;
    }

    public void setUserId(String cuId) {
        userId = cuId;
    }

    /**
     * 更新记录
     * 调用 put(String, Map, long)
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int update(Map rd) throws HongsException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit (rd, ids , 1096);
        int  c = 0;
        long t = System.currentTimeMillis() / 1000;
        for(String id : ids) {
            c += put(id, rd, t);
        }
        return c;
    }

    /**
     * 删除记录
     * 调用 del(String, Map, long)
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit(rd , ids , 1097);
        int  c = 0;
        long t = System.currentTimeMillis() / 1000;
        for(String id : ids) {
            c += del(id, rd, t);
        }
        return c;
    }

    /**
     * 恢复记录
     * 调用 rev(String, Map, long)
     * @param rd
     * @return
     * @throws HongsException
     */
    public int revert(Map rd) throws HongsException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
    //  permit(rd , ids , 1096);
        int  c = 0;
        long t = System.currentTimeMillis() / 1000;
        for(String id : ids) {
            c += rev(id, rd, t);
        }
        return  c;
    }

    /**
     * 添加记录
     * 调用 add(String, Map, long)
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public String add(Map rd) throws HongsException {
        String id = Core.newIdentity();
        add(id,rd , System.currentTimeMillis() / 1000);
        return id ;
    }

    /**
     * 保存记录
     * 调用 set(String, Map, long)
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    @Override
    public int set(String id, Map rd) throws HongsException {
        return set(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 更新记录
     * 调用 put(String, Map, long)
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    @Override
    public int put(String id, Map rd) throws HongsException {
        return put(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 删除记录
     * 调用 del(String, Map, long)
     * @param id
     * @return
     * @throws HongsException
     */
    @Override
    public int del(String id) throws HongsException {
        Map rd = new HashMap( );
        return del(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 添加记录
     * @param id
     * @param rd
     * @param ctime
     * @return
     * @throws HongsException
     */
    public int add(String id, Map rd, long ctime) throws HongsException {
        Map dd = new HashMap();
        padInf(dd, rd);

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        addDoc(id, dc);

        Table table = getTable();
        if (table == null) {
            return 1;
        }

        String   uid   = getUserId();
        String   fid   = getFormId();

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   1  );
        nd.put(     "id", id );
        nd.put("form_id", fid);
        nd.put("user_id", uid);

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", Dawn.toString(dd, true));
        nd.put("name",     getText(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        table.insert(nd);
        return 1;
    }

    /**
     * 保存记录
     *
     * 注意:
     * 更新不产生新节点,
     * 仅供内部持续补充.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int set(String id, Map rd, long ctime) throws HongsException {
        Map dd = get(id);
        int t  = dd.isEmpty()? 1: 2;
        int i  = padInf(dd , rd);
        // 无更新不存储
        if (i  ==  0) {
            return 0;
        }

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        Table table = getTable();
        if (table == null) {
            return 1;
        }

        String   uid   = getUserId();
        String   fid   = getFormId();
        Object[] param = new String[] {id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        Map nd = table.fetchCase()
            .filter( where,param )
            .select("ctime,state")
            .getOne( );
        if (! nd.isEmpty()) {
            if (Synt.declare(nd.get("state"), 0 ) ==  0   ) {
                throw new HongsException(404, "Data item '"+id+"' is removed in "+getDbName())
                    .setLocalizedContent("matrix.item.is.removed")
                    .setLocalizedContext("matrix");
            } /* 没有新增, 不必限时
            if (Synt.declare(nd.get("ctime"), 0L ) >= ctime) {
                throw new HongsException(400, "Wait 1 second to put '"+id+"' in "+getDbName())
                    .setLocalizedContent("matrix.wait.one.second")
                    .setLocalizedContext("matrix");
            } */
        } else {
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            nd.put("state",   t  );
            nd.put(     "id", id );
            nd.put("form_id", fid);
            nd.put("user_id", uid);
        }

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", Dawn.toString(dd, true));
        nd.put("name",     getText(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        if (nd.containsKey("etime") == false ) {
            table.update(nd, where, param);
        } else {
            table.insert(nd);
        }

        return 1 ;
    }

    /**
     * 更新记录
     *
     * 注意:
     * 每次都产生新节点,
     * 有则更新无则添加.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int put(String id, Map rd, long ctime) throws HongsException {
        Map dd = get(id);
        int t  = dd.isEmpty()? 1: 2;
        int i  = padInf(dd , rd);
        // 无更新不存储
        if (i  ==  0) {
            return 0;
        }

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        Table table = getTable();
        if (table == null) {
            return 1;
        }

        String   uid   = getUserId();
        String   fid   = getFormId();
        Object[] param = new String[] {id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        Map od = table.fetchCase()
            .filter( where,param )
            .select("ctime,state")
            .getOne( );
        if (! od.isEmpty()) {
            if (Synt.declare(od.get("state"), 0  ) ==  0   ) {
                throw new HongsException(404, "Data item '"+id+"' is removed in "+getDbName())
                    .setLocalizedContent("matrix.item.is.removed")
                    .setLocalizedContext("matrix");
            }
            if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
                throw new HongsException(400, "Wait 1 second to put '"+id+"' in "+getDbName())
                    .setLocalizedContent("matrix.wait.one.second")
                    .setLocalizedContext("matrix");
            }
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   t  );
        nd.put(     "id", id );
        nd.put("form_id", fid);
        nd.put("user_id", uid);

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", Dawn.toString(dd, true));
        nd.put("name",     getText(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        table.update(ud, where, param);
        table.insert(nd);

        return 1 ;
    }

    /**
     * 删除记录
     *
     * 注意:
     * 用于终止当前节点,
     * 仅供内部持续补充.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int cut(String id, Map rd, long ctime) throws HongsException {
        delDoc(id);

        Table table = getTable();
        if (table == null) {
            return 1;
        }

    //  String   uid   = getUserId();
        String   fid   = getFormId();
        Object[] param = new String[] {id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        Map nd = table.fetchCase()
            .filter( where,param )
            .select("ctime,state,data,name")
            .getOne( );
        if (nd.isEmpty()
        ||  Synt.declare(nd.get("state"), 0  ) ==  0   ) { // 删除是幂等的可重复调用
            return 0;
        }
    //  if (Synt.declare(nd.get("ctime"), 0L ) >= ctime) { // 总是更新最终的记录状态
    //      throw new HongsException(400, "Wait 1 second to del '"+id+"' in "+getDbName())
    //          .setLocalizedContent("matrix.wait.one.second")
    //          .setLocalizedContext("matrix");
    //  }

        nd.put("state" ,  0  );

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        table.update(nd, where, param);

        return 1;
    }

    /**
     * 删除记录
     *
     * 注意:
     * 删除时产生新节点,
     * 重复调用不会多增.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int del(String id, Map rd, long ctime) throws HongsException {
        delDoc(id);

        Table table = getTable();
        if (table == null) {
            return 1;
        }

        String   uid   = getUserId();
        String   fid   = getFormId();
        Object[] param = new String[] {id, fid, "0"};
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";

        Map od = table.fetchCase()
            .filter( where,param )
            .select("ctime,state,data,name")
            .getOne( );
        if (od.isEmpty()
        ||  Synt.declare(od.get("state"), 0  ) ==  0   ) { // 删除是幂等的可重复调用
            return 0;
        }
        if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
            throw new HongsException(400, "Wait 1 second to del '"+id+"' in "+getDbName())
                .setLocalizedContent("matrix.wait.one.second")
                .setLocalizedContext("matrix");
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   0  );
        nd.put(     "id", id );
        nd.put("form_id", fid);
        nd.put("user_id", uid);

        // 拷贝快照和日志标题
        nd.put("data", od.get("data"));
        nd.put("name", od.get("name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        table.update(ud, where, param);
        table.insert(nd);

        return 1;
    }

    /**
     * 恢复记录
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws HongsException
     */
    public int rev(String id, Map rd, long ctime) throws HongsException {
        Table table = getTable();
        if (table == null) {
            throw new HongsException(405, "Data table for '"+getDbName()+"' is not exists")
                .setLocalizedContent("matrix.rev.unsupported")
                .setLocalizedContext("matrix");
        }

        String   uid   = getUserId();
        String   fid   = getFormId();
        long     rtime = Synt.declare (rd.get("rtime"), 0L);
        Object[] param = new String[] {id, fid, "0" };
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";
        Object[] para2 = new Object[] {id, fid,rtime};
        String   wher2 = "`id`=? AND `form_id`=? AND `ctime`=?";

        // 获取当前数据
        Map od = table.fetchCase()
            .filter( where, param)
        //  .assort("etime  DESC")
            .select("ctime")
            .getOne( );
        if (od.isEmpty()) {
        //  throw new HongsException(404, "Can not find current '"+id+"' in "+getDbName())
        //      .setLocalizedContent("matrix.node.not.exists")
        //      .setLocalizedContext("matrix");
        } else
        if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
            throw new HongsException(400, "Wait 1 second to del '"+id+"' in "+getDbName())
                .setLocalizedContent("matrix.wait.one.second")
                .setLocalizedContext("matrix");
        }

        // 获取快照数据
        Map nd = table.fetchCase()
            .filter( wher2, para2)
        //  .assort("ctime  DESC")
            .getOne( );
        if (nd.isEmpty()) {
            throw new HongsException(404, "Empty '"+id+"' at '"+ctime+"' in "+getDbName())
                .setLocalizedContent("matrix.node.not.exists")
                .setLocalizedContext("matrix");
        }
        // 删除时保留的是删除前的快照, 即使为最终记录仍然可以恢复
        if (Synt.declare(nd.get("state"), 0  ) !=  0   ) {
        if (Synt.declare(nd.get("etime"), 0L ) ==  0L  ) {
            throw new HongsException(400, "Alive '"+id+"' at '"+ctime+"' in "+getDbName())
                .setLocalizedContent("matrix.node.is.current")
                .setLocalizedContext("matrix");
        }}

        Map ud = new HashMap();
        ud.put("etime", ctime);
        nd.put("ctime", ctime);
        nd.put("rtime", rtime);
        nd.put("etime",   0  );
        nd.put("state",   3  );
        nd.put("form_id", fid);
        nd.put("user_id", uid);

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getText(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getText(rd, "meno"));
        }

        table.update(ud, where, param);
        table.insert(nd);

        // 保存到索引库
        Map dd = (Map) Dawn.toObject((String) nd.get("data"));
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        return 1 ;
    }

    @Override
    public void commit() {
        super.commit();
        cascades(setIds, delIds);
        setIds.clear();
        delIds.clear();
    }

    @Override
    public void revert() {
        super.revert();
        setIds.clear();
        delIds.clear();
    }

    @Override
    public void close () {
        super. close();
        setIds.clear();
        delIds.clear();
    }

    @Override
    public void setDoc(String id, Document doc)
    throws HongsException {
        super.setDoc(id , doc);
        setIds. add (id);
    }

    @Override
    public void delDoc(String id)
    throws HongsException {
        super.delDoc(id);
        delIds. add (id);
    }

    @Override
    protected void padQry(BooleanQuery.Builder qr, Map rd) throws HongsException {
        // 限定分区范围
        String pd = getPartId();
        if (null != pd && ! pd.isEmpty( )) {
            qr.add(new TermQuery(new Term("@"+PART_ID_KEY, pd)), BooleanClause.Occur.MUST);
        }

        super.padQry( qr , rd );
    }

    @Override
    protected void padDoc(Document doc, Map map, Set rep) {
        // 写入分区标识
        String pd = getPartId();
        if (null != pd && ! pd.isEmpty( )) {
            doc.add(new StringField("@"+PART_ID_KEY, pd, Field.Store.NO));
            doc.add(new StoredField(/**/PART_ID_KEY, pd));
        }

        super.padDoc(doc, map, rep);
    }

    /**
     * 填充准备保存的信息
     * @param dd 旧数据
     * @param rd 新数据
     * @return 0 无更新
     */
    protected int padInf(Map dd, Map rd) {
        // 填充关联冗余
        try {
            includes(rd);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }

        int i = 0;
        Map<String,Map> fs = getFields();
        for(String fn : fs . keySet()) {
            if (! rd.containsKey (fn)) {
                continue;
            }

            Object fr = rd.get(fn);
            Object fo = dd.get(fn);
            dd.put(fn , fr);

            // 跳过环境字段, 比如修改时间
            if (! canSkip(fn, fr, fo)) {
                i ++;
            }
        }

        // 填充自述字段, 如名称关键词
        if (i > 0) {
            Map<String, Map> fields = getFields();
            if (fields.containsKey("name")) {
                Map m = fields.get("name");
                if (Synt.declare(m.get("disabled"), false)) {
                    dd.put("name", getName(dd));
                }
            }
            if (fields.containsKey("word")) {
                Map m = fields.get("word");
                if (Synt.declare(m.get("disabled"), false)) {
                    dd.put("word", getWord(dd));
                }
            }
        }

        return i;
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
        if (Cnst. ID_KEY . equals (fn)) {
            return true ;
        }
        if (getSkipable().contains(fn)) {
            return true ;
        }
        if (fr == null && fo == null) {
            return true ;
        }
        if (fr == null || fo == null) {
            return false;
        }
        // 数字类则转为字符串进行对比
        if (fr instanceof Number
        ||  fo instanceof Number ) {
            fr = Synt.asString(fr);
            fo = Synt.asString(fo);
        } else
        // 复杂对象用 JSON 串进行对比
        if (fr instanceof Map
        ||  fr instanceof Collection
        ||  fr instanceof Object [ ]) {
            fr = Dawn.toString(fr, true);
            fo = Dawn.toString(fo, true);
        }
        return fr.equals(fo);
    }

    /**
     * 获取记录文本
     * 会按记录字段容量进行切割
     * @param dd
     * @param fn
     * @return
     * @throws HongsException
     */
    protected String getText(Map dd, String fn)
    throws HongsException {
        String s;
        if (dd.containsKey(fn)) {
            s  = Synt.asString(dd.get (fn));
        } else
        if ("name".equals (fn)) {
            s  = Synt.asString(getName(dd));
        } else {
            s  = null;
        }

        if (s == null) return null;
        Map m  = (Map) getTable().getFields().get(fn);
        if (m == null) return null;
        int k  = Synt.defxult((Integer) m.get("size"), 255);
        if (k >= s.length()) return s;

        // 宽字符按长度为 2 进行切割
        int l  = 0, i , c;
        for(i  = 0; i < s.length( ); i ++ )
        {
            c  = Character.codePointAt(s,i);
            if (c >= 0 && c <= 255) {
                l += 1;
            } else {
                l += 2;
            }
            if (l >  k) {
                s  = s.substring(0 , i - 1) + "…";
                break ;
            }
        }

        return  s ;
    }

    /**
     * 获取名称串
     * @param dd
     * @return
     */
    protected String getName(Map dd) {
        String fl = Synt.asString(getParams().get("nameless"));
        if (fl != null && ! fl.isEmpty()) {
            return Syno.inject( fl , dd );
        }

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
        &&  255 <  nm.length( ) ) {
            return nm.substring(0, 254) + "…" ;
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
        String fl = Synt.asString(getParams().get("wordless"));
        if (fl != null && ! fl.isEmpty()) {
            return Syno.inject( fl , dd );
        }

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

    /**
     * 增加搜索和命名未明确指定时的后备类型
     * @param t
     * @return
     */
    @Override
    public Set<String> getCaseTypes(String t) {
        if ("wordable".equals(t)) {
            return Synt.setOf("string", "search", "text", "textarea", "textview");
        } else
        if ("nameable".equals(t)) {
            return Synt.setOf("string", "search", "text");
        } else {
            return super.getCaseTypes(t);
        }
    }

    /**
     * 存在 word 字段则为 word, 否则调 getWordable
     * @return
     */
    @Override
    public Set<String> getRschable() {
        Map fs  = (Map) getFields().get("word");
        if (fs != null) {
            return Synt.setOf ("word");
        } else {
            return getWordable( /**/ );
        }
    }

    public Set<String> getWordable() {
        if (null != wdCols) {
            return  wdCols;
        }
        Map fs  = (Map) getFields().get("word");
        if (fs != null && !Synt.declare(fs.get("readonly"), false)) {
            wdCols = Synt.setOf("word");
        } else {
            wdCols = getCaseNames("wordable");
            if (wdCols == null
            ||  wdCols.isEmpty()) {
                wdCols = getSrchable( );
            }
            wdCols.remove("word");
        }
        return wdCols;
    }

    public Set<String> getNameable() {
        if (null != nmCols) {
            return  nmCols;
        }
        Map fs  = (Map) getFields().get("name");
        if (fs != null && !Synt.declare(fs.get("readonly"), false)) {
            nmCols = Synt.setOf("name");
        } else {
            nmCols = getCaseNames("nameable");
        //  if (nmCols == null
        //  ||  nmCols.isEmpty()) {
        //      nmCols = getListable( );
        //  }
            nmCols.remove("name");
        }
        return nmCols;
    }

    public Set<String> getSkipable() {
        if (null != skCols) {
            return  skCols;
        }
        skCols = getCaseNames("skipable");
        if (skCols == null
        ||  skCols.isEmpty( ) ) {
            skCols =  new  HashSet();
            skCols.add("mtime");
            skCols.add("muser");
            skCols.add("memo" );
            skCols.add("meno" );
        }
        return skCols;
    }

    //** 级联操作 **/

    protected void cascades(Set sd, Set rd) {
        Set<String> aq = Synt.toSet(getParams().get("cascades"));
        if (aq == null || aq.isEmpty()) {
            return;
        }
        if (sd == null || sd.isEmpty()) {
        if (rd == null || rd.isEmpty()) {
            return;
        }}

        Set sq  =  new LinkedHashSet();
        Set rq  =  new LinkedHashSet();

        for(String at : aq) {
        if (at == null || at.isBlank()) {
            continue;
        }
            if (at.contains("#UPDATE")) {
                sq.add(at);
            }
            if (at.contains("#DELETE")) {
                rq.add(at);
            }
        }

        // 放入队列, 异步处理
        if (rd != null && ! rd.isEmpty() && ! rq.isEmpty()) {
            for(Object id : rd) {
                Casc.delete(rq, id);
            }
        }
        if (sd != null && ! sd.isEmpty() && ! sq.isEmpty()) {
            for(Object id : sd) {
                Casc.update(sq, id);
            }
        }
    }

    protected void includes(Map dd) throws HongsException {
        Set<String> aq = Synt.toSet(getParams().get("includes"));
        if (aq == null || aq.isEmpty()) {
            return;
        }

        for(String at : aq) {
        if (at == null || at.isBlank()) {
            continue;
        }

            // 格式: conf.form?fk#f1=fa;f2=fb
            int     p = at.indexOf  ("#");
            String tk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("!");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);
            String  k = fk.trim ();
            String  s = tk.trim ();

            // 解析字段映射表
            Map fm = new HashMap();
                String[] x = s.split("\\s*;\\s*", 0);
            for(String   y : x) {
                String[] z = y.split("\\s*=\\s*", 2);
                if (z.length == 2) {
                    fm.put(z[0].trim(), z[1].trim());
                } else {
                    fm.put(z[0].trim(), z[0].trim());
                }
            }
            Set fs = new HashSet(fm.values());
            Map fc = (Map) getFields().get(k);

            // 写入当前信息表
            Object v = dd.get(k);
            if (Synt.declare(fc.get("__repeated__"), false) == false) {
                /**/  Map  fd = Data.getInstance(c, f).getOne(Synt.mapOf(
                    Cnst.RB_KEY , fs,
                    Cnst.ID_KEY , v
                ));
                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    dd.put(et.getKey(), fd.get(et.getValue()));
                }
            } else {
                Map<Object, List> fd = new HashMap();
                List <Map> fl = Data.getInstance(c, f).getAll(Synt.mapOf(
                    Cnst.RB_KEY , fs,
                    Cnst.ID_KEY , v
                ));
                for(Object fn : fs) {
                    fd.put(fn , new ArrayList (fl.size(/**/)));
                }
                for(  Map  fb : fl) {
                for(Object fn : fs) {
                    fd.get(fn).add(fb.get(fn));
                }}
                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    dd.put(et.getKey(), fd.get(et.getValue()));
                }
            }
        }
    }

}
