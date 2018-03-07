package app.hongs.dh.graphs;

import static app.hongs.Cnst.ID_KEY;
import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.dh.IEntity;
import app.hongs.dh.ITrnsct;
import app.hongs.dh.JoistBean;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.Tool;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;

/**
 * 图存储模型
 *
 * Neo4J 的一个简单逻辑封装.
 * 本想用嵌入方案但有些问题,
 * 如索引依赖的库方法冲突等,
 * 故仅支持连接外部查询服务.
 *
 * @author Hongs
 */
public class GraphsRecord extends JoistBean implements IEntity, ITrnsct, AutoCloseable {

    /**
     * 关系类型
     */
    public static final String RT_KEY = "type";
    /**
     * 关系方向, 0 出, 1 进
     */
    public static final String RD_KEY = "dirn";
    /**
     * 标签
     */
    public static final String LABELS = "_label_";
    /**
     * 关系
     */
    public static final String RELATS = "_relat_";
    /**
     * 清理标识, 1 清理属性, 2 清理标签, 4 清理出关系, 8 清理进关系
     */
    public static final String CLEANS = "_clean_";

    protected boolean TRNSCT_MODE = false;
    protected boolean OBJECT_MODE = false;

    private static final Pattern CQLEXP = Pattern.compile("\\$(\\w+)");
    private static final String[ ] RELS = new String[] {
            Cnst.EQ_REL, " = " , Cnst.NE_REL, " <> ",
            Cnst.LT_REL, " < " , Cnst.LE_REL, " <= ",
            Cnst.GT_REL, " > " , Cnst.GE_REL, " >= ",
            Cnst.IN_REL, " IN ", Cnst.NI_REL, " NOT IN "
    };

    private String      dn = null;
    private Driver      dd = null;
    private Session     db = null;
    private Transaction tx = null;

    public GraphsRecord(Map form) {
        super.setFields(form);

        // 是否要开启事务
        Object tr  = Core.getInstance().got(Cnst.TRNSCT_MODE);
        if ( ( tr != null  &&  Synt.declare( tr , false  )  )
        ||  CoreConfig.getInstance().getProperty("core.in.trnsct.mode", false)) {
            TRNSCT_MODE = true;
        }

        // 是否为对象模式
        Object ox  = Core.getInstance().got(Cnst.OBJECT_MODE);
        if ( ( ox != null  &&  Synt.declare( ox , false  )  )
        ||  CoreConfig.getInstance().getProperty("core.in.object.mode", false)) {
            OBJECT_MODE = true;
        }
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
    public static GraphsRecord getInstance(String conf, String form) throws HongsException {
        String code = GraphsRecord.class.getName() +":"+ conf +"."+ form;
        Core   core = Core.getInstance( );
        if ( ! core.containsKey( code ) ) {
            String path = conf +"/"+ form;
            String name = conf +"."+ form;
            String cxnf = FormSet.hasConfFile(path)? path : conf ;
            Map    fxrm = FormSet.getInstance(cxnf).getForm(form);

            GraphsRecord inst = new GraphsRecord(fxrm);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (GraphsRecord) core.got(code);
        }
    }

    /**
     * 连接数据库
     * 这与 open 不同在于会判断是否要开启事务
     * @return 
     */
    public Session conn() {
        if (TRNSCT_MODE ) {
            begin();
        } else {
            conn( );
        }
        return  db ;
    }

    /**
     * 连接数据库
     * 注意 open 方法不会根据环境自动开启事务
     * @return 
     */
    public Session open() {
        if (db == null) {
                Map    opts = getParams();
                String link = Synt.declare(opts.get("data-link"), String.class);
            if (link == null) {
                String href = Synt.declare(opts.get("data-href"), "bolt://localhost:7687");
                String user = Synt.declare(opts.get("data-username"), "neo4j" );
                String pswd = Synt.declare(opts.get("data-password"), "neo4j" );
                dd = GraphDatabase.driver (href, AuthTokens.basic(user, pswd) );
                db = this.dd.session();
                dn = href;
            } else {
                int    posi = link.lastIndexOf( "." );
                String conf = link.substring(0, posi);
                String form = link.substring(1+ posi);
                GraphsRecord  that;
                try {
                       that = getInstance(conf, form);
                       that.open();
                } catch ( HongsException e ) {
                       throw e.toExpedient();
                }
                db = that.dd.session();
                dd = null;
                dn = link;
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Connect to graph database "+dn);
            }
        }
        return  db ;
    }

    @Override
    public void close() throws Exception {
        if (db != null) {
            try {
                // 默认退出时提交
                if (tx != null) {
                    try {
                        commit();
                    } catch (Error er) {
                        revert();
                        throw er;
                    }
                }
            } finally {
                db.close( );
                db  = null ;
            if (dd != null) {
                dd.close( );
                dd  = null ;
            }}

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Disconnect graph database "+dn);
            }
        }
    }

    @Override
    public void begin( ) {
        if (tx == null ) {
            tx  = open().beginTransaction();
        }
    }

    @Override
    public void commit() {
        if (tx != null ) {
            tx.success();
            tx  = null;
        }
    }

    @Override
    public void revert() {
        if (tx != null ) {
            tx.failure();
            tx  = null;
        }
    }

    /**
     * 查询
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public Map search(Map rd) throws HongsException {
        Map<String, Map   > fds = getFields(  );
        Map<String, Object> pms = new HashMap();
        StringBuilder cql = new StringBuilder();
        StringBuilder whr = new StringBuilder();
        int fi = 0;

        cql.append("MATCH (n)");

        // 关联
        Set<String> rts = Synt.asSet(rd.get(RELATS));
        if (rts != null && !rts.isEmpty()) {
            cql.append(" MATCH (m)");
            whr.append(  "(n)--(m)");
            whr.append(" AND ");
            fi = _cqlIn(whr, pms, "m."+ID_KEY, " IN ", rts, fi);
            whr.append(" AND ");
        } else {
            rts = Synt.asSet(rd.get(RELATS+"0"));
        if (rts != null && !rts.isEmpty()) {
            cql.append(" MATCH (o)");
            whr.append( "(n)-->(o)");
            whr.append(" AND ");
            fi = _cqlIn(whr, pms, "o."+ID_KEY, " IN ", rts, fi);
            whr.append(" AND ");
        }
            rts = Synt.asSet(rd.get(RELATS+"1"));
        if (rts != null && !rts.isEmpty()) {
            cql.append(" MATCH (i)");
            whr.append( "(n)<--(i)");
            whr.append(" AND ");
            fi = _cqlIn(whr, pms, "i."+ID_KEY, " IN ", rts, fi);
            whr.append(" AND ");
        }
        }

        // 标签
        Set<String> lbs = Synt.asSet(rd.get(LABELS));
        if (lbs != null && !lbs.isEmpty()) {
            whr.append("(");
            for(String lb : lbs) {
                whr.append( "n:" )
                   .append(escaped(lb))
                   .append(" OR ");
            }
            whr.setLength (whr.length()-4);
            whr.append(") AND ");
        }

        // 条件
        for(Object ot : rd.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String fn = Synt.asString(et.getKey());
            Map    fc = fds .get( fn );

            if (RELATS.equals(fn)
            ||  LABELS.equals(fn)
            ||  CLEANS.equals(fn)) {
                continue;
            }

            /**
             * 明确存在的或符合命名的,
             * 才可以作为字段筛选条件.
             */
            if ((fc != null && ignored(fc) )
            || ( fc == null && ignored(fn))) {
                continue;
            }

            Object fv;
            fn = escaped(fn);
            fn = "n." + (fn);
            fv = et.getValue( );
            if (fv instanceof Map) {
                fi = _cqlRl(whr, pms, fn, ( Map ) fv, fi );
            } else
            if (fv instanceof Collection
            ||  fv instanceof Object [ ]) {
                fi = _cqlIn(whr, pms, fn, " IN ", fv, fi );
            } else {
                fi = _cqlEq(whr, pms, fn, " = " , fv, fi );
            }
            whr.append(" AND ");
        }
        if (whr.length() > 0) {
            whr.setLength(whr.length() - 5);
            cql.append(" WHERE ")
               .append( whr );
        }

        // 总数
        StringBuilder xql = new StringBuilder();
        xql.append( cql.toString ( ) )
           .append(" RETURN count(*)");

        // 排序
        StringBuilder obs = new StringBuilder();
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (null != ob ) {
        for(String  fn : ob ) {
            boolean de = fn.startsWith("-");
            if (de) {
                fn = fn.substring( 1 );
                fn = escaped(fn);
                fn = fn +" DESC";
            } else {
                fn = escaped(fn);
            }
            obs.append( "n.")
               .append( fn  )
               .append( ", ");
        }}
        if (obs.length() > 0) {
            obs.setLength(obs.length() - 2);
            cql.append(" ORDER BY ")
               .append( obs );
        }

        // 返回
        Set<String> rl = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set<String> rb = null;
        if (null != rl ) {
            rb =new HashSet();
        for(String  fn : rl ) {
            Map  fc = fds.get(fn);
            if ("*".equals( fn )) {
                 rb.add( fn );
            } else
            if ((fc != null && !ignored(fc) )
            || ( fc == null && !ignored(fn))) {
                 rb.add( fn );
            }
        }}
        cql.append( " RETURN n" );

        // 分页
        int rn = Synt.declare(rd.get(Cnst.RN_KEY), Cnst.RN_DEF);
        int pn = Synt.declare(rd.get(Cnst.PN_KEY), 1);
        int sn = rn * (pn - 1);
        if (rn > 0) {
        if (sn > 0) {
            cql.append(" SKIP " ).append(sn);
        }
            cql.append(" LIMIT ").append(rn);
        }

        // 获取结果和分页
        Map sd  = new HashMap();
        if (pn != 0) {
            String sql = cql.toString();
            StatementResult rst = run( sql, pms );
            sd.put("list", this.toList(rst, rb/**/));
        }
        if (rn != 0) {
            String sql = xql.toString();
            StatementResult rst = run( sql, pms );
            sd.put("page", this.toPage(rst, rn, pn));
        }
        return sd;
    }

    /**
     * 新建
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        String id = add(rd);
        rd.put(ID_KEY , id);
        return rd;
    }

    /**
     * 修改(可批量)
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int update(Map rd) throws HongsException {
        Map rd2 = new HashMap(rd);
        Set ids = Synt.asSet (rd2.remove(ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            try {
                put(id, rd2);
            }
            catch (NullPointerException ex) {
                throw new HongsException(0x1104, "Can not udpate for id: "+id);
            }
        }
        return ids.size();
    }

    /**
     * 删除(可批量)
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
        Set ids = Synt.asSet (rd.get (ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            try {
                del(id);
            }
            catch (NullPointerException ex) {
                throw new HongsException(0x1104, "Can not delete for id: "+id);
            }
        }
        return ids.size();
    }

    /**
     * 新建
     * @param info
     * @return
     */
    public String add(Map info) {
        String  id = Core.newIdentity();
        addNode(id);
        setNode(id, info, null);
        return  id;
    }

    /**
     * 修改
     * @param id
     * @param info
     */
    public void put(String id, Map info) {
       Node node = getNode(id);
        if (node == null) {
            throw new NullPointerException("Can not set node '"+id+"', it is not exists");
        }
        setNode(id, info, node);
    }

    /**
     * 删除
     * @param id
     */
    public void del(String id) {
       Node node = getNode(id);
        if (node == null) {
            throw new NullPointerException("Can not del node '"+id+"', it is not exists");
        }
        delNode(id);
    }

    /**
     * 执行查询
     * @param cql
     * @return
     */
    public StatementResult run(String cql) {
        if ( 0 != Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            // 调试用日志
            CoreLogger.debug("GraphsRecord.run: "+cql);
        }
        return conn().run(cql);
    }

    /**
     * 执行查询
     * @param cql
     * @param pms
     * @return
     */
    public StatementResult run(String cql, Map pms) {
        if ( 0 != Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            // 调试用日志
            CoreLogger.debug("GraphsRecord.run: "+_cqlBn(cql, pms));
        }
        return conn().run(cql, pms);
    }

    /**
     * 删除节点
     * @param id
     */
    public void delNode(String id) {
        run("MATCH (n {id:$id})-[r]-() DELETE r", Synt.mapOf("id", id));
        run("MATCH (n {id:$id}) "  +  "DELETE n", Synt.mapOf("id", id));
    }

    /**
     * 获取节点
     * @param id
     * @return
     */
    public Node getNode(String id) {
        StatementResult rs = run( "MATCH (n {id:$id}) RETURN n LIMIT 1", Synt.mapOf("id", id));
        return rs.hasNext()
             ? rs.next(   )
                 .get ("n")
                 .asNode( )
             : null;
    }

    /**
     * 创建节点
     * @param id
     * @return
     */
    public Node addNode(String id) {
        StatementResult rs = run("CREATE (n {id:$id}) RETURN n LIMIT 1", Synt.mapOf("id", id));
        return rs.hasNext()
             ? rs.next(   )
                 .get ("n")
                 .asNode( )
             : null;
    }

    /**
     * 设置节点
     * @param id
     * @param info
     * @param node
     */
    public void setNode(String id, Map info, Node node) {
        if (id == null) {
            throw new NullPointerException("GraphsRecord.setNode: id can not be null");
        }

        Map<String, Map> flds = getFields();
        Set<String>      keys = new HashSet();
        Set<String>      labs = new HashSet();
        Set<Map>         rels = new HashSet();
        Map              vals = new HashMap();
        StringBuilder    cqls = new StringBuilder();

        if (node  != null) {
            byte c = Synt.declare(info.get(CLEANS), (byte) 0);
            if (1 == (1 ^ c)) {
                for(String key : node.keys(  )) {
                    keys.add(key);
                }
            }
            if (2 == (2 ^ c)) {
                for(String lab : node.labels()) {
                    labs.add(lab);
                }
            }
            if (4 == (4 ^ c)) {
                run("MATCH (n {id:$id})-[r]->() DELETE r", Synt.mapOf("id", id));
            }
            if (8 == (8 ^ c)) {
                run("MATCH (n {id:$id})<-[r]-() DELETE r", Synt.mapOf("id", id));
            }
        }

        cqls.append("MATCH (n {id:$id})");

        // 写入新数据
        for(Object ot : info.entrySet( )) {
            Map.Entry et = (Map.Entry) ot;
            String fn = Synt.asString(
                        et.getKey( ) );
            Object fv = et.getValue( );
            Map    fc = flds.get(fn);

            if (CLEANS.equals(fn)) {
                // continue;
            } else
            if (RELATS.equals(fn)) {
                Set res = Synt.asSet(fv);
                if (res ==  null ) {
                    continue;
                }
                for(Object fv2 : res) {
                    rels.add(Synt.asMap(fv2));
                }
            } else
            if (LABELS.equals(fn)) {
                Set las = Synt.asSet(fv);
                if (las ==  null ) {
                    continue;
                }
                for(Object fv2 : las) {
                    labs.remove(Synt.asString(fv2));
                }
            } else
            if ((fc != null && !ignored(fc) )
            || ( fc == null && !ignored(fn))) {
                /**
                 * 满足条件:
                 * 名称必须符合命名规范
                 * 且字段未被标识为忽略
                 */
                if (fv != null) {
                    String k = (vals.size()) + "";
                    cqls.append("n." )
                        .append(escaped(fn))
                        .append("=$" )
                        .append(k    )
                        .append(", " );
                    vals.put   (k, fv);
                    keys.remove(   fn);
                } else {
                    keys.add   (   fn);
                }
            }
        }
        if (!vals.isEmpty()) {
            cqls.setLength(cqls.length() - 2);
        }

        // 删除多余的属性
        if (!keys.isEmpty()) {
            cqls.append(" REMOVE");
            for(String key : keys) {
                cqls.append("n." )
                    .append(escaped(key))
                    .append( ", ");
            }
            cqls.setLength(cqls.length() - 2);
        }

        // 删除多余的标签
        if (!labs.isEmpty()) {
            cqls.append(" REMOVE");
            for(String lab : labs) {
                cqls.append("n:" )
                    .append(escaped(lab))
                    .append( ", ");
            }
            cqls.setLength(cqls.length() - 2);
        }

        // 18 是 MATCH (n {id:$id}) 的长度
        if (cqls.length() > 18) {
            vals.put("id" , id);
            run(cqls.toString(), vals);
        }

        // 重设节点的关系
        if (!rels.isEmpty()) {
            for(Map rel : rels) {
                String arw0, arw1;
                String mid = Synt.asString(rel.get(ID_KEY));
                String mrt = Synt.asString(rel.get(RT_KEY));
                if (Synt.declare(rel.get(RD_KEY), 0) == 0 ) {
                    arw0 = "-" ;
                    arw1 = "->";
                } else {
                    arw0 = "<-";
                    arw1 =  "-";
                }

                // 添加关系
                cqls.setLength( 0 );
                cqls.append("MATCH (n {id:$nid}) ")
                    .append("MATCH (m {id:$mid}) ")
                    .append("CREATE ")
                    .append("(n)" )
                    .append( arw0 )
                    .append("[r:" )
                    .append(escaped(mrt))
                    .append(  "]" )
                    .append( arw1 )
                    .append("(m)" );
                vals.clear ( );
                vals.put("nid", id);
                vals.put("mid",mid);

                // 增加属性
                StringBuilder cqlz = new StringBuilder(" {");
                for(Object ot : rel.entrySet()) {
                    Map.Entry et = (Map.Entry)ot;
                    String fn = Synt.asString(et.getKey());
                    if (! ID_KEY.equals(fn)
                    &&  ! RT_KEY.equals(fn)
                    &&  ! RD_KEY.equals(fn)) {
                        String k = (vals.size()) + "";
                        Object v = et.getValue();
                        cqlz.append(escaped(fn))
                            .append(":$")
                            .append(k)
                            .append( ",");
                        vals.put(k, v);
                    }
                }
                if (cqlz.length() > 2) {
                    cqlz.append( "}" );
                    cqls.insert(cqls.length() - arw1.length() - 4, cqlz);
                }

                run(cqls.toString(), vals);
            }
        }
    }

    /**
     * 字段配置定为忽略
     * @param fc
     * @return
     */
    protected boolean ignored(Map fc) {
        return "".equals(fc.get("__name__"))
          ||  "@".equals(fc.get("__name__"))
          || "Ignore".equals(fc.get("rule"));
    }

    /**
     * 字段名不符合规范
     * @param fn
     * @return
     */
    protected boolean ignored(String fn) {
        try {
            return fn.getBytes("utf-8").length < 3;
        } catch (UnsupportedEncodingException ex ) {
            throw  new HongsExpedient.Common( ex );
        }
    }

    /**
     * 转义并包裹字段名
     * @param fn
     * @return
     */
    protected String  escaped(String fn) {
        return "`" + Tool.escape( fn ) + "`";
    }

    //** 查询辅助方法 **/

    private int _cqlEq(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
        pms.put(""+ i, v);
        xql.append( n )
           .append( r )
           .append("$")
           .append( i );
        return  1 + i  ;
    }

    private int _cqlIn(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
        v = Synt.asSet(v);
        pms.put(""+ i, v);
        xql.append( n )
           .append( r )
           .append("$")
           .append( i );
        return  1 + i  ;
    }

    private int _cqlRl(StringBuilder xql, Map pms, String n, Map m, int i) {
        Map w = new HashMap(m);
        for(int j = 0; j < RELS.length; j += 2) {
            Object v = w.remove(RELS[j]);
            if (v != null ) {
                String r = RELS[ j + 1 ];
                if (j < 12) {
                    i = _cqlEq(xql, pms, n, r, v, i);
                } else {
                    i = _cqlIn(xql, pms, n, r, v, i);
                }
            }
        }
        if (! w.isEmpty( )) {
            i = _cqlIn(xql, pms, n, " IN ", w, i);
        }
        return i;
    }

    private String _cqlBn(String cql, Map pms) {
        Matcher matcher = CQLEXP.matcher( cql);
        StringBuffer sb = new StringBuffer(  );
        String       st;
        Object       so;

        while (matcher.find()) {
            st = matcher.group(1);
            if (pms.containsKey(st)) {
                so = pms.get(st );
                if ( so != null )
                st = "'" + Tool.escape(so.toString()) + "'";
                else
                st = "";
            } else {
                st = "";
            }
            st = Matcher.quoteReplacement(st);
            matcher.appendReplacement(sb, st);
        }
        matcher.appendTail(sb);

        return sb.toString(  );
    }

    protected List<Map> toList(StatementResult rst, Set rb) {
        boolean withLabs = rb == null || rb.contains(LABELS);
        boolean withRel0 = rb == null || rb.contains(RELATS) || rb.contains(RELATS+"0");
        boolean withRel1 = rb == null || rb.contains(RELATS) || rb.contains(RELATS+"1");

        // 属性
        if (rb == null || rb.contains("*")) {
            rb =  null;
        } else {
            rb =  new HashSet( rb );
            rb.remove(RELATS + "0");
            rb.remove(RELATS + "1");
            rb.remove(RELATS);
            rb.remove(LABELS);
            rb.remove( "*"  );
        }

        List</*****/ Map> list = new LinkedList(  );
        Map <String, Map> maps = new   HashMap (  );

        while (rst.hasNext()) {
            Record rec = rst.next();
            Node   nod = rec.get(  "n" ).asNode(  );
            String nid = nod.get(ID_KEY).asString();
            Map    row = new HashMap(  );

            list.add(/**/ row);
            maps.put(nid, row);

            // 节点属性
            Iterable<String> ia = rb != null
                                ? rb : nod.keys(  );
            for( String fn : ia ) {
                row.put(fn , OBJECT_MODE
                           ? nod.get(fn).asObject( )
                           : nod.get(fn).asString());
            }

            // 节点标签
            if (withLabs) {
                Set labs = new HashSet();
                for( String  lab : nod.labels() ) {
                    labs.add(lab);
                }
                row.put( LABELS , labs );
            }
        }

        // 向外关系
        if (withRel0) {
            StatementResult rs = run("MATCH (n)-[r]->(m) WHERE n.id IN $ids RETURN r,n.id,m.id",
                Synt.mapOf("ids", maps.keySet())
            );
            while (rs.hasNext()) {
                Record ro  = rs.next();
                Relationship re = ro.get("r").asRelationship();
                String nid = ro.get("n.id").asString();
                String mid = ro.get("m.id").asString();
                Map ral = new HashMap();
                // 关系属性
                for(String  fn: re.keys()) {
                    ral.put(fn, OBJECT_MODE
                              ? re.get(fn).asObject( )
                              : re.get(fn).asString());
                }
                ral.put(RT_KEY, re.type());
                ral.put(RD_KEY,  0 );
                ral.put(ID_KEY, mid);
                Map row = maps.get(nid);
                Dict.put( row, ral, RELATS, null);
            }
        }

        // 向内关系
        if (withRel1) {
            StatementResult rs = run("MATCH (n)<-[r]-(m) WHERE n.id IN $ids RETURN r,n.id,m.id",
                Synt.mapOf("ids", maps.keySet())
            );
            while (rs.hasNext()) {
                Record ro  = rs.next();
                Relationship re = ro.get("r").asRelationship();
                String nid = ro.get("n.id").asString();
                String mid = ro.get("m.id").asString();
                Map ral = new HashMap();
                // 关系属性
                for(String  fn: re.keys()) {
                    ral.put(fn, OBJECT_MODE
                              ? re.get(fn).asObject( )
                              : re.get(fn).asString());
                }
                ral.put(RT_KEY, re.type());
                ral.put(RD_KEY,  1 );
                ral.put(ID_KEY, mid);
                Map row = maps.get(nid);
                Dict.put( row, ral, RELATS, null);
            }
        }

        return list;
    }

    protected Map toPage(StatementResult rst, int rn, int pn) {
        int tr = 0;
        int tp = 0;
        if (rst.hasNext()) {
            Record ro = rst.next( );
            tr = ro.get("count(*)").asInt();
            tp = (int) Math.ceil( (float) tr / rn );
        }

        Map page = new HashMap( );
        page.put("totalrows", tr);
        page.put("totlapage", tp);
        page.put("rows", rn);
        page.put("page", pn);
        page.put("ern" , rn > 0 ? 1 : 0);
        return page;
    }

}
