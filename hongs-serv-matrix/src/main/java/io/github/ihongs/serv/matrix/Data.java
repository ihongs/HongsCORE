package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
 * matrix.rev.unsupported=资源不支持回顾
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
     * 823  构建实例不成功
     * 910  配置文件不存在
     * 912  表单信息不存在
     * </pre>
     *
     * @param conf
     * @param form
     * @return
     * @throws CruxException 表单获取失败
     * @throws CruxExemption 实例构建失败
     * @throws ClassCastException 不是 Data 的子类
     */
    public static Data getInstance(String conf, String form) throws CruxException {
        String name = Data.class.getName()+":"+ conf +":"+ form;
        Core   core = Core. getInstance ();
        Data   inst =(Data) core.get(name);
        if (null != inst) {
            return  inst;
        }

        Map    dict = FormSet.getInstance(conf).getForm( form );
        String clsn = Dict.getValue(dict , "", "@", "db-class");
        if (clsn.isEmpty() || clsn.equals(Data.class.getName())) {
            inst  = new Data( conf, form );
                    core.set( name, inst );
            return  inst;
        }

        Class type ;
        try {
              type = Class.forName (clsn);
        } catch (ClassNotFoundException e) {
            throw new CruxExemption(821, "Can not find class by name '"+clsn+"'." );
        }
        try {
            Method func = type.getMethod("getInstance", new Class [] {String.class, String.class});
            int    modi = func.getModifiers();
            if (! Modifier.isPublic(modi)
            ||  ! Modifier.isStatic(modi)
            ||  type != func.getDeclaringClass()) {
                throw new NoSuchMethodException();
            }
            return (Data) func.invoke(null, new Object[] {conf, form});
        } catch (NoSuchMethodException ex) {
            return (Data) Core.getInstance (type);
        } catch (InvocationTargetException ex) {
            Throwable ta = ex.getCause(  );
            // 调用层级过多, 最好直接抛出
            if (ta instanceof StackOverflowError) {
                throw (StackOverflowError) ta;
            }
            throw new CruxExemption(ta, 824, "Can not call '"+clsn+".getInstance'");
        } catch ( IllegalArgumentException ex) {
            throw new CruxExemption(ex, 824, "Can not call '"+clsn+".getInstance'");
        } catch (   IllegalAccessException ex) {
            throw new CruxExemption(ex, 824, "Can not call '"+clsn+".getInstance'");
        }
    }

    public Table getTable() throws CruxException {
        String tn = Synt.declare(getParams().get("db-table"), "data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

    public Model getModel() throws CruxException {
        String tn = Synt.declare(getParams().get("db-table"), "data");
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
            } catch (CruxException ex) {
                if (ex.getErrno() != 910
                &&  ex.getErrno() != 912) { // 非表单缺失
                    throw ex.toExemption();
                }
                break;
            }

            if (fields  == null ) {
                break;
            }   cnf = getBgConf();
            if (cnf.equals(conf)) {
                break;
            }

            try {
                fieldx = FormSet.getInstance(cnf).getForm(form);
            } catch (CruxException ex) {
                if (ex.getErrno() != 910
                &&  ex.getErrno() != 912) { // 非表单缺失
                    throw ex.toExemption();
                }
                break;
            }

            if (fieldx  == null ) {
                break;
            }
            if (fieldx.isEmpty()) {
                break;
            }

            /**
             * 注意:
             * 1. 不可破坏原始配置
             * 2. 当前的覆盖后台的
             */
            fieldx = new LinkedHashMap(fieldx);
            for(Object ot : fields.entrySet()) {
                Map.Entry et = (Map.Entry) ot ;
                String fn = (String) et.getKey();
                Map    fc = (Map) et.getValue( );
                Map    fx = (Map) fieldx.get(fn);
                if (fx != null) {
                    fx  = new LinkedHashMap (fx);
                    fx.putAll (fc);
                    fieldx.put(fn, fx);
                } else {
                    fieldx.put(fn, fc);
                }
            }
            fields = fieldx ;
        }   while  ( false );

        if ( null == fields) {
            throw new CruxExemption(910, "@matrix:matrix.form.not.exists", conf, form);
        }

        setFields(fields);
        return    fields ;
    }

    public final String getForm() {
        return form;
    }

    public final String getConf() {
        return conf;
    }

    /**
     * 背景配置
     * 当前表单不在管理区之内时,
     * 会用当前表单覆盖管理表单,
     * 此可获取对内配置, 用于 getFields
     * @return
     */
    protected String getBgConf() {
        return conf.startsWith("centre/")
            ? "centra/"+conf.substring(7)
            :  conf;
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
                name = "matrix/"+ conf.substring(7) +":"+ form ;
            } else {
                name = conf +":"+ form;
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

    /**
     * 获取当前操作用户ID
     *
     * 有设置将返回设置的
     * 无设置将从会话读取
     * 都没有则返回访客ID
     *
     * @return
     */
    public String getUserId() {
        if ( null != userId ) {
            return   userId ;
        }
        return Synt.declare(ActionHelper.getInstance().getSessibute(Cnst.UID_SES), Cnst.GST_UID);
    }

    /**
     * 设置当前操作用户ID
     * @param uid
     */
    public void setUserId(String uid) {
        userId = uid;
    }

    /**
     * 更新记录
     * 调用 put(String, Map, long)
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public int update(Map rd) throws CruxException {
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
     * @throws CruxException
     */
    @Override
    public int delete(Map rd) throws CruxException {
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
     * @throws CruxException
     */
    public int revert(Map rd) throws CruxException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
    //  permit(rd , ids , 1096);
        int  c = 0;
        long t = System.currentTimeMillis() / 1000;
        for(String id : ids) {
            c += rev(id, rd, t);
        }
        return c;
    }

    /**
     * 查询历史记录
     * @param rd
     * @return
     * @throws CruxException
     */
    public Map reveal(Map rd) throws CruxException {
        Map rsp = getModel( ).search(rd, fetchCase());

        return rsp;
    }

    /**
     * 查询历史数据
     * @param rd
     * @return
     * @throws CruxException
     */
    public Map remind(Map rd) throws CruxException {
        Map rsp = getModel( ).recite(rd, fetchCase());

        if (rsp.containsKey("info")) {
            Map inf = (Map) rsp.get("info");
        if (inf.containsKey("data")) {
            // 解析详情数据
            Map dat = (Map) getData(
                   (String) inf.get("data")
            );
            rsp.put("info", dat);
            rsp.put("snap", inf);
            inf.remove( "data" );

            // 补充新旧标识
            Set ab  = Synt.toTerms(rd.get(Cnst.AB_KEY));
            if (ab != null
            && (ab.contains("older")
            ||  ab.contains("newer"))) {
                Object id = inf.get("id");
                long ctime = Synt.declare( inf.get("ctime"), 0L );
                if (ab.contains("older")) {
                    Map row = fetchCase()
                       .filter("id = ? AND ctime < ?", id, ctime)
                       .assort("ctime DESC")
                       .select("ctime")
                       .getOne();
                    inf.put("older", ! row.isEmpty() ? row.get("ctime") : null);
                }
                if (ab.contains("newer")) {
                    Map row = fetchCase()
                       .filter("id = ? AND ctime > ?", id, ctime)
                       .assort("ctime  ASC")
                       .select("ctime")
                       .getOne();
                    inf.put("newer", ! row.isEmpty() ? row.get("ctime") : null);
                }
            }
        }}

        return rsp;
    }

    /**
     * 添加记录
     * 调用 add(String, Map, long)
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public String add(Map rd) throws CruxException {
        String id = Core.newIdentity();
        add(id,rd , System.currentTimeMillis() / 1000);
        return id ;
    }

    /**
     * 更新记录
     * 调用 put(String, Map, long)
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    @Override
    public int put(String id, Map rd) throws CruxException {
        return put(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 保存记录
     * 调用 set(String, Map, long)
     * @param id
     * @param rd
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    @Override
    public int set(String id, Map rd) throws CruxException {
        return set(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 删除记录
     * 调用 del(String, Map, long)
     * @param id
     * @return
     * @throws CruxException
     */
    @Override
    public int del(String id) throws CruxException {
        Map rd = new HashMap( );
        return del(id, rd, System.currentTimeMillis() / 1000);
    }

    /**
     * 添加记录
     * @param id
     * @param rd
     * @param ctime
     * @return
     * @throws CruxException
     */
    public int add(String id, Map rd, long ctime) throws CruxException {
        Map dd = new HashMap();
        padDif(dd, rd);

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        addDoc(id, dc);

        FenceCase sc = fenceCase();
        if (sc == null) {
            return 1;
        }

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   1  );
        nd.put(  "id" ,  id  );

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", getData(dd));
        nd.put("name", getTval(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.insert(nd);

        return 1;
    }

    /**
     * 更新记录
     *
     * 注意:
     * 有则更新无则添加,
     * 每次都产生新节点.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    public int put(String id, Map rd, long ctime) throws CruxException {
        Map dd = get(id);
        int t  = dd.isEmpty()? 1: 2;
        int i  = padDif(dd , rd);
        // 无更新不存储
        if (i  ==  0) {
            return 0;
        }

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        FenceCase sc = fenceCase();
        if (sc == null) {
            return 1;
        }

        Object[] param = new String[] {id, "0"};
        String   where = "id = ? AND etime = ?";

        Map od = sc
            .filter( where,param )
            .select("ctime,state")
            .getOne( );
        if (! od.isEmpty()) {
            if (Synt.declare(od.get("state"), 0  ) ==  0   ) {
                throw new CruxException(404, "@matrix:matrix.item.is.removed", getFormId(), id);
            }
            if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
                throw new CruxException(400, "@matrix:matrix.wait.one.second", getFormId(), id);
            }
        } else {
            if (ctime == 0) {
                ctime = System.currentTimeMillis() / 1000; // 无则创建
            }
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   t  );
        nd.put(  "id" ,  id  );

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", getData(dd));
        nd.put("name", getTval(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.update(ud);
        sc.insert(nd);

        return 1;
    }

    /**
     * 保存记录
     *
     * 注意:
     * 有则更新无则添加,
     * ctime = 0 仅更新.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    public int set(String id, Map rd, long ctime) throws CruxException {
        Map dd = get(id);
        int t  = dd.isEmpty()? 1: 2;
        int i  = padDif(dd , rd);
        // 无更新不存储
        if (i  ==  0) {
            return 0;
        }

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        FenceCase sc = fenceCase();
        if (sc == null) {
            return 1;
        }

        Object[] param = new String[] {id, "0"};
        String   where = "id = ? AND etime = ?";

        Map od = sc
            .filter( where,param )
            .select("ctime,state")
            .getOne( );
        if (! od.isEmpty()) {
            if (Synt.declare(od.get("state"), 0  ) ==  0   ) {
                throw new CruxException(404, "@matrix:matrix.item.is.removed", getFormId(), id);
            }
            if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
            //  throw new CruxException(400, "@matrix:matrix.wait.one.second", getFormId(), id);
                ctime =  0;
            }
        } else {
            if (ctime == 0) {
                ctime = System.currentTimeMillis() / 1000; // 无则创建
            }
        }

        // 仅对最新节点作更新
        if (ctime == 0) {
            Map nd = new HashMap();

            // 数据快照和日志标题
            nd.put("__data__", dd);
            nd.put("data", getData(dd));
            nd.put("name", getTval(dd, "name"));

            /*
            // 改记日志, 以免冲掉
            // 操作备注和终端代码
            if (rd.containsKey("memo")) {
                nd.put("memo", getText(rd, "memo"));
            }
            if (rd.containsKey("meno")) {
                nd.put("meno", getText(rd, "meno"));
            }
            */

            if (sc.update(nd) > 0) {
                CoreLogger.info("Set data for {}, id: {}, ctime: {}. {} {}", getFormId(), id, od.get("ctime"), rd.get("meno"), rd.get("memo"));
                return 1;
            } else {
                return 0;
            }
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   t  );
        nd.put(  "id" ,  id  );

        // 数据快照和日志标题
        nd.put("__data__", dd);
        nd.put("data", getData(dd));
        nd.put("name", getTval(dd, "name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.update(ud);
        sc.insert(nd);

        return 1;
    }

    /**
     * 删除记录
     *
     * 注意:
     * 有则删除无则跳过,
     * 成功会产生新节点.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    public int del(String id, Map rd, long ctime) throws CruxException {
        delDoc(id);

        FenceCase sc = fenceCase();
        if (sc == null) {
            return 1;
        }

        Object[] param = new String[] {id, "0"};
        String   where = "id = ? AND etime = ?";

        Map od = sc
            .filter( where,param )
            .select("*")
            .getOne( );
        if (od.isEmpty()
        ||  Synt.declare(od.get("state"), 0  ) ==  0   ) {
            return 0; // 删除是幂等的可重复调用
        }
        if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
            throw new CruxException(400, "@matrix:matrix.wait.one.second", getFormId(), id);
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   0  );
        nd.put(  "id" ,  id  );

        // 拷贝快照和日志标题
        nd.put("data", od.get("data"));
        nd.put("name", od.get("name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.update(ud);
        sc.insert(nd);

        return 1;
    }

    /**
     * 终止记录
     *
     * 注意:
     * 有则删除无则跳过,
     * ctime = 0 仅更新.
     *
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    public int end(String id, Map rd, long ctime) throws CruxException {
        delDoc(id);

        FenceCase sc = fenceCase();
        if (sc == null) {
            return 1;
        }

        Object[] param = new String[] {id, "0"};
        String   where = "id = ? AND etime = ?";

        Map od = sc
            .filter( where,param )
            .select("*")
            .getOne( );
        if (od.isEmpty()
        ||  Synt.declare(od.get("state"), 0  ) ==  0   ) {
            return 0; // 删除是幂等的可重复调用
        }
        if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
        //  throw new CruxException(400, "@matrix:matrix.wait.one.second", getFormId(), id);
            ctime  = 0;
        }

        // 仅对最新节点作更新
        if (ctime == 0) {
            Map nd = new HashMap();
            nd.put("state",   0  );

            /*
            // 改记日志, 以免冲掉
            // 操作备注和终端代码
            if (rd.containsKey("memo")) {
                nd.put("memo", getText(rd, "memo"));
            }
            if (rd.containsKey("meno")) {
                nd.put("meno", getText(rd, "meno"));
            }
            */

            if (sc.update(nd) > 0) {
                CoreLogger.info("End data for {}, id: {}, ctime: {}. {} {}", getFormId(), id, od.get("ctime"), rd.get("meno"), rd.get("memo"));
                return 1;
            } else {
                return 0;
            }
        }

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("etime",   0  );
        nd.put("state",   0  );
        nd.put(  "id" ,  id  );

        // 拷贝快照和日志标题
        nd.put("data", od.get("data"));
        nd.put("name", od.get("name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.update(ud);
        sc.insert(nd);

        return 1;
    }

    /**
     * 恢复记录
     * @param id
     * @param rd
     * @param ctime
     * @return 有更新为 1, 无更新为 0
     * @throws CruxException
     */
    public int rev(String id, Map rd, long ctime) throws CruxException {
        FenceCase sc = fenceCase();
        if (sc == null) {
            throw new CruxException(405, "@matrix:matrix.rev.unsupported", getFormId());
        }
        FenceCase s2 = fenceCase();

        long     rtime = Synt.declare (rd.get("rtime"), 0L);
        Object[] para2 = new Object[] {id, rtime};
        String   wher2 = "id = ? AND ctime = ?";
        Object[] param = new String[] {id, "0"};
        String   where = "id = ? AND etime = ?";

        // 恢复最终数据
        if (rtime == 0L) {
            Map od = sc
                .filter( where, param)
            //  .assort("ctime  DESC")
                .select("state, data")
                .getOne( );
            if (Synt.declare(od.get("state"), 0) > 0) {
                Map dd = getData((String) od.get("data"));
                Map xd = new HashMap(00);
                dd.put(Cnst.ID_KEY , id);
                padDif(dd, xd);
                Document dc = padDoc(dd);
                setDoc(id, dc);
            } else {
                delDoc(id);
            }
            return 0;
        }

        // 获取当前数据
        Map od = sc
            .filter( where, param)
        //  .assort("ctime  DESC")
            .select("ctime")
            .getOne( );
        if (od.isEmpty()) {
        //  throw new CruxException(404, "@matrix:matrix.node.not.exists", getFormId(), id);
        } else
        if (Synt.declare(od.get("ctime"), 0L ) >= ctime) {
            throw new CruxException(400, "@matrix:matrix.wait.one.second", getFormId(), id);
        }

        // 获取快照数据
        Map sd = s2
            .filter( wher2, para2)
        //  .assort("ctime  DESC")
            .getOne( );
        if (sd.isEmpty()) {
            throw new CruxException(404, "@matrix:matrix.node.not.exists", getFormId(), id, ctime);
        }
        // 删除时保留的是删除前的快照, 即使为最终记录仍然可以恢复
        if (Synt.declare(sd.get("state"), 0  ) !=  0   ) {
        if (Synt.declare(sd.get("etime"), 0L ) ==  0L  ) {
            throw new CruxException(400, "@matrix:matrix.node.is.current", getFormId(), id, ctime);
        }}

        /*
        // 处理不可逆值
        Map xd , zd ;
        Map dd = getData((String) sd.get("data"));
        Set ur = getCaseNames("unrevtable");
        if (null != ur && ! ur.isEmpty( ) ) {
            zd = getData((String) od.get("data"));
            xd = new HashMap ( ur.size( ) );
            for( Object n : ur  ) {
            if ( zd . containsKey (n) ) {
                xd.put( n , zd.get(n) );
            }}
        } else {
            xd = new HashMap(00);
        }
        */

        // 从快照再构建
        Map dd = getData((String) sd.get("data"));
        Map xd = new HashMap(00);
        padDif(dd, xd);

        // 保存到文档库
        dd.put(Cnst.ID_KEY , id);
        Document dc = padDoc(dd);
        setDoc(id, dc);

        Map ud = new HashMap();
        ud.put("etime", ctime);

        Map nd = new HashMap();
        nd.put("ctime", ctime);
        nd.put("rtime", rtime);
        nd.put("etime",   0  );
        nd.put("state",   3  );
        nd.put(  "id" ,  id  );

        // 数据快照和日志标题
        nd.put("data", sd.get("data"));
        nd.put("name", sd.get("name"));

        // 操作备注和终端代码
        if (rd.containsKey("memo")) {
            nd.put("memo", getTval(rd, "memo"));
        }
        if (rd.containsKey("meno")) {
            nd.put("meno", getTval(rd, "meno"));
        }

        sc.update(ud);
        sc.insert(nd);

        return 1;
    }

    /**
     * 数据查询, 含表单参数
     * 不支持时抛异常
     * @return
     * @throws CruxException
     */
    public FetchCase fetchCase() throws CruxException {
        Model model = getModel();
        if (model == null) {
            throw new CruxException(405, "@matrix:matrix.rev.unsupported", getFormId());
        }
        return model.fetchCase().filter(DB.Q(model.table.name, "form_id")+" = ?", getFormId());
    }

    /**
     * 数据记录, 含表单参数
     * 不支持时返回空
     * @return
     * @throws CruxException
     */
    public FenceCase fenceCase() throws CruxException {
        Table table = getTable();
        if (table == null) {
            return   null;
        }
        return new FenceCase(this, table);
    }

    /**
     * 提交但不执行级联操作
     */
    public void submit() {
        super.commit();
        setIds.clear();
        delIds.clear();
    }

    @Override
    public void commit() {
        super.commit();
        cascades(setIds, delIds);
        setIds.clear();
        delIds.clear();
    }

    @Override
    public void cancel() {
        super.cancel();
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
    throws CruxException {
        super.setDoc(id , doc);
        setIds. add (id);
    }

    @Override
    public void delDoc(String id)
    throws CruxException {
        super.delDoc(id);
        delIds. add (id);
    }

    /**
     * 确认操作
     * 调用 deplets 判断是否被引用
     * @param rd
     * @param ds
     * @param ec
     * @throws CruxException
     */
    @Override
    protected void permit(Map rd, Set ds, int ec) throws CruxException {
        super.permit(rd, ds, ec);

        // 检查被引用状况
        if ( 1097 == ec) {
            depletes(ds);
        }
    }

    @Override
    protected void padQry(BooleanQuery.Builder qr, Map rd) throws CruxException {
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
     * 比对数据变更并填充
     * @param dd 旧数据
     * @param rd 新数据
     * @return 0 无更新
     * @throws CruxException
     */
    protected int padDif(Map dd, Map rd) throws CruxException {
        Map xd = new DataCascade.Mix(rd, dd);

        // 填充关联冗余
        includes(xd);

        // 扩充合并字段
        incloses(xd);

        // 合并数据, 检查变更
        int i = 0;
        Map<String,Map> fs = getFields();
        for(String fn : fs . keySet()) {
            if (! rd.containsKey (fn)) {
                continue;
            }

            Object fo = dd.get(fn);
            Object fr = rd.get(fn);
            dd.put(fn , fr);

            // 跳过环境字段, 比如修改时间
            if (!missable(fn, fo, fr)) {
                i ++;
            }
        }
        return  i;
    }

    /**
     * 保存中跳过的字段或取值
     * 当返回 true 时跳过检查,
     * 如都是 true 则不做更新.
     * @param fn
     * @param fo 旧值
     * @param fr 新值
     * @return
     */
    protected boolean missable(String fn, Object fo, Object fr) {
        if (Cnst. ID_KEY . equals (fn)) {
            return true ;
        }
        if (getMissable().contains(fn)) {
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
            fr = Dist.toString(fr, true);
            fo = Dist.toString(fo, true);
        }
        return fr.equals(fo);
    }

    /**
     * 获取记录文本
     * 会按记录字段容量进行切割
     * @param dd
     * @param fn
     * @return
     * @throws CruxException
     */
    public String getTval(Map dd, String fn)
    throws CruxException {
        String s;
        if (dd.containsKey(fn)) {
            s  = Synt.asString(dd.get (fn));
        } else
        if ("name".equals (fn)) {
            s  = getName(dd);
        } else
        if ("word".equals (fn)) {
            s  = getWord(dd);
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
    public String getName(Map dd) {
        String fl = Synt.asString(getParams().get("nameless"));
        if (fl != null && ! fl.isEmpty()) {
            return Syno.inject( fl , dd );
        }

        StringBuilder nn = new StringBuilder();
        Set<String>   ns = getNameable( );
        Map<String, Map> fs = getFields();
        for(String  fn : ns ) {
            Object  fv = dd.get(fn);
            Map     fc = fs.get(fn);
                    fv = getSrchText(fc, fv);
            if (fv != null && !"".equals(fv)) {
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
    public String getWord(Map dd) {
        String fl = Synt.asString(getParams().get("wordless"));
        if (fl != null && ! fl.isEmpty()) {
            return Syno.inject( fl , dd );
        }

        StringBuilder nn = new StringBuilder();
        Set<String>   ns = getWordable( );
        Map<String, Map> fs = getFields();
        for(String  fn : ns ) {
            Object  fv = dd.get(fn);
            Map     fc = fs.get(fn);
                    fv = getSrchText(fc, fv);
            if (fv != null && !"".equals(fv)) {
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
     * 获取数据序列
     * @param dd 原始数据
     * @return
     */
    public String getData(Map dd) {
        return Dist.toString(dd, true);
    }

    /**
     * 解析序列数据
     * @param ds 库存数据
     * @return
     */
    public Map getData(String ds) {
        return (Map) Dist.toObject(ds);
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
        if (fs != null && !Synt.declare(fs.get("disabled"), false)) {
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
        if (fs != null && !Synt.declare(fs.get("disabled"), false)) {
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

    public Set<String> getMissable() {
        if (null != skCols) {
            return  skCols;
        }
        skCols = getCaseNames("missable");
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

    /**
     * 获取级联动作配置
     * 返回格式 {"conf:form;fk": ["UPDATE","DELETE","DEPEND"]}
     * 值也可能是符合标准的字符串
     * @return
     */
    protected Map<String, Object> getCascades() {
        do {
            try {
                return FormSet.getInstance(conf).getEnum(form+".cascade");
            }
            catch (CruxException ex ) {
            if (910 != ex.getErrno( )
            &&  913 != ex.getErrno()) {
                throw  ex.toExemption();
            }}

            String canf = getBgConf();
            if (canf.equals(conf)) {
                break;
            }

            try {
                return FormSet.getInstance(canf).getEnum(form+".cascade");
            }
            catch (CruxException ex ) {
            if (910 != ex.getErrno( )
            &&  913 != ex.getErrno()) {
                throw  ex.toExemption();
            }}
        }
        while (false );
        return new HashMap(0);
    }

    /**
     * 获取级联包含配置
     * 返回格式 {"conf:form;fk": {"fn1":"fn_a", "fn2":"fn_x"}}
     * 值也可能是符合标准的字符串
     * @return
     */
    protected Map<String, Object> getIncludes() {
        do {
            try {
                return FormSet.getInstance(conf).getEnum(form+".include");
            }
            catch (CruxException ex ) {
            if (910 != ex.getErrno( )
            &&  913 != ex.getErrno()) {
                throw  ex.toExemption();
            }}

            String canf = getBgConf();
            if (canf.equals(conf)) {
                break;
            }

            try {
                return FormSet.getInstance(canf).getEnum(form+".include");
            }
            catch (CruxException ex ) {
            if (910 != ex.getErrno( )
            &&  913 != ex.getErrno()) {
                throw  ex.toExemption();
            }}
        }
        while (false);
        return new HashMap(0);
    }

    /**
     * 级联操作,
     * 异步更新或删除引用资源
     * @param us 已更新的
     * @param rs 已删除的
     */
    protected void cascades(Set us, Set rs) {
        Map<String, Object> aq = getCascades();
        if (aq == null || aq.isEmpty()) {
            return;
        }
        if (us == null || us.isEmpty()) {
        if (rs == null || rs.isEmpty()) {
            return;
        }}

        Set uq  =  new LinkedHashSet();
        Set rq  =  new LinkedHashSet();
        Set vq  =  new LinkedHashSet();

        /**
         * 若仅指定了级联更新
         * 则删除时会更新数据
         * 相关字段的值会置空
         */
        for(Map.Entry<String, Object> xt : aq.entrySet()) {
            String at = xt.getKey  ();
            Object av = xt.getValue();
            if (at == null || at.isEmpty()) {
                continue;
            }
            Set aa = Synt.toSet( av );
            if (aa.contains("DELETE")) {
                rq.add(at);
            if (aa.contains("UPDATE")) {
                uq.add(at);
            }} else
            if (aa.contains("UPDATE")) {
                uq.add(at);
                vq.add(at);
            }
        }

        // 放入队列, 异步处理
        if (rs != null && ! rs.isEmpty()) {
            for(Object id : rs) {
                DataCascade.delete(rq,id);
                DataCascade.update(vq,id);
            }
        }
        if (us != null && ! us.isEmpty()) {
            for(Object id : us) {
                DataCascade.update(uq,id);
            }
        }
    }

    /**
     * 引用检查
     * 检查被哪些资源关联引用
     * @param rs 将删除的
     * @throws CruxException
     */
    protected void depletes(Set rs) throws CruxException {
        Map<String, Object> aq = getCascades();
        if (aq == null || aq.isEmpty()) {
            return;
        }
        if (rs == null || rs.isEmpty()) {
            return;
        }

        StringBuilder nb = new StringBuilder();
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, Object> xt : aq.entrySet()) {
            String at = xt.getKey  ();
            Object av = xt.getValue();
            if (at == null || at.isEmpty()) {
                continue;
            }
            Set aa = Synt.toSet( av );
            if ( ! aa.contains ("DEPEND") ) {
                continue;
            }

            // 解析关联描述串, 格式: conf:form;fk
            int     p = at.indexOf  (";");
            String  k = at.substring(1+p);
            String  c = at.substring(0,p);
                    p =  c.indexOf  (":");
            String  f =  c.substring(1+p);
                    c =  c.substring(0,p);

            // 可能存在多个引用字段
            Map    ar = new HashMap();
            Set    or = new HashSet();
            ar.put( Cnst.OR_KEY, or );
            for(String fn : k.split(",")) {
                or.add(Synt.mapOf(fn,rs));
            }

            // 查询依赖当前表的资源
            Data inst = Data.getInstance(c, f);
            long hits = inst.search( ar, 0, 1).count();
            if ( hits > 0 ) {
                String  l = (String) inst.getParams( ).get("__text__");
                nb.append(l).append(" (").append(hits).append( "), " );
                sb.append(f).append(" (").append(hits).append( "), " );
            }
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
            nb.setLength(nb.length() - 2);

            // 抛出异常告知依赖情况
            throw new CruxException(1097, "@matrix:core.delete.depend.error", nb);
        }
    }

    /**
     * 内联包含
     * 将被引用的部分复制过来
     * @param rd 混合数据
     * @throws CruxException
     */
    protected void includes(Map rd) throws CruxException {
        Map<String, Object> aq = getIncludes();
        if (aq == null || aq.isEmpty()) {
            return;
        }

        for(Map.Entry<String, Object> xt : aq.entrySet()) {
            String at = xt.getKey  ();
            Object av = xt.getValue();
            if (at == null || at.isEmpty()) {
                continue;
            }

            // 解析关联描述串, 格式: conf:form;fk
            int     p = at.indexOf  (";");
            String  k = at.substring(1+p);
            String  c = at.substring(0,p);
                    p =  c.indexOf  (":");
            String  f =  c.substring(1+p);
                    c =  c.substring(0,p);

            // 解析字段映射表, 格式: f1:fa,f2:fb,fn
            Map fm = Synt.toMap(av);
            Set fs = new HashSet(fm.values());
            Map fc = (Map) getFields().get(k);

            // 获取关联外键值
            Object v = rd.get(k);

            // 处理未关联情况
            if (v == null || "".equals(v)) {
                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    rd.put(et.getKey(), null );
                }
                continue;
            } else
            if (v instanceof Collection && ((Collection)v).isEmpty()) {
                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    rd.put(et.getKey(), new ArrayList(0) );
                }
                continue;
            }

            // 写入当前信息表
            if (Synt.declare(fc.get("__repeated__"), false) == false) {
                /**/  Map  fd = Data.getInstance(c, f).getOne(Synt.mapOf(
                    Cnst.RB_KEY , fs,
                    Cnst.ID_KEY , v
                ));
                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    rd.put(et.getKey(), fd.get(et.getValue()));
                }
            } else {
                List <Map> fl = Data.getInstance(c, f).getAll(Synt.mapOf(
                    Cnst.RB_KEY , fs,
                    Cnst.ID_KEY , v
                ));

                // 多值汇总
                Map<Object, List> fd = new HashMap(fs.size());
                for(Object fn : fs) {
                    fd.put(fn , new ArrayList(fl.size()));
                }
                for(  Map  fb : fl) {
                for(Object fn : fs) {
                    List fv = Synt.asList(fb.get(fn));
                    if (fv != null) {
                        fd.get(fn).addAll(fv);
                    }
                }}

                for(Object ot : fm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    rd.put(et.getKey(), fd.get(et.getValue()));
                }
            }
        }
    }

    /**
     * 内部扩充
     * 处理多个字段合并的内容
     * @param rd 混合数据
     * @throws CruxException
     */
    protected void incloses(Map rd) throws CruxException {
        Map<String, Map> fields = getFields();
        if (fields.containsKey("name")) {
            Map m = fields.get("name");
            if (Synt.declare(m.get("disabled"), false)) {
                rd.put("name", getName(rd));
            }
        }
        if (fields.containsKey("word")) {
            Map m = fields.get("word");
            if (Synt.declare(m.get("disabled"), false)) {
                rd.put("word", getWord(rd));
            }
        }
    }

}
