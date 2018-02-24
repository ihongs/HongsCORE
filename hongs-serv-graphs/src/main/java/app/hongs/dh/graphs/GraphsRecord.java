package app.hongs.dh.graphs;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.dh.IEntity;
import app.hongs.dh.ITrnsct;
import app.hongs.dh.JoistBean;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import static app.hongs.Cnst.ID_KEY;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * 图存储模型
 * @author Hongs
 */
public class GraphsRecord extends JoistBean implements IEntity, ITrnsct, Cloneable, AutoCloseable {

    public static final String RT_KEY = "type";
    public static final String RD_KEY = "dirn";
    public static final String LABELS = "_label_";
    public static final String RELATS = "_relat_";
    public static final String CLEANS = "_clean_";

    private static final Pattern KEYEXP = Pattern.compile("^[a-z]{2}$"       , Pattern.CASE_INSENSITIVE);
    private static final Pattern FLDEXP = Pattern.compile("^[a-z][a-z0-9_]+$", Pattern.CASE_INSENSITIVE);
    private static final String[ ] RELS = new String[] {
            Cnst.EQ_REL, " = " , Cnst.NE_REL, " <> ",
            Cnst.LT_REL, " < " , Cnst.LE_REL, " <= ",
            Cnst.GT_REL, " > " , Cnst.GE_REL, " >= ",
            Cnst.IN_REL, " IN ", Cnst.NI_REL, " NOT IN "
    };

    private GraphDatabaseService db = null;
    private Transaction tx = null;
    private String  dbpath = null;
    private String  dbname = null;

    public GraphsRecord(Map form, String path, String name) {
        super.setFields(form);

        // 数据路径
        if (path != null) {
            Map m = new HashMap();
            m.put("SERVER_ID", Core.SERVER_ID);
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);
            path = Tool.inject(path, m);
            String root = Core.DATA_PATH + "/";
            if (name != null) {
                if ( !  new  File(path).isAbsolute( ) ) {
                    path = "graphs/" + path;
                    path = root + path;
                }
            } else
            {
                if ( !  new  File(path).isAbsolute( ) ) {
                    name = "graphs/" + path;
                    path = root + name;
                } else
                if (path.startsWith(root)) {
                    name = path.substring(root.length());
                } else
                {
                    name = path;
                }
            }
        }
        this.dbpath = path;
        this.dbname = name;
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

            // 表单配置中可指定数据路径
            Map c = (Map) fxrm.get("@");
            if (c!= null) {
                String p;
                p = (String) c.get("data-path");
                if (null != p && 0 < p.length()) {
                    path  = p;
                }
                p = (String) c.get("data-name");
                if (null != p && 0 < p.length()) {
                    name  = p;
                }
            }

            GraphsRecord inst = new GraphsRecord(fxrm, path,name);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (GraphsRecord) core.got(code);
        }
    }

    public String getDataPath() {
        if (null != dbpath) {
            return  dbpath;
        }
        throw new NullPointerException("Data path is not set");
    }

    public String getDataName() {
        if (null != dbname) {
            return  dbname;
        }
        throw new NullPointerException("Data name is not set");
    }

    public GraphDatabaseService getDB() {
        if (db == null) {
            db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbpath));
        }
        return db;
    }

    @Override
    public void close() throws Exception {
        if (db != null) {
            db.shutdown();
            db  = null;
        }
    }

    @Override
    public void begin() {
        if (tx == null) {
            tx  = getDB().beginTx();
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

    @Override
    public Map search(Map rd) throws HongsException {
        Map<String, Map   > fls = getFields(  );
        Map<String, Object> pms = new HashMap();
        StringBuilder cql = new StringBuilder();
        StringBuilder whr = new StringBuilder();
        int fi = 0;

        cql.append( "MATCH ");

        // 标签
        Set<String> lbs = Synt.asSet(rd.get(LABELS));
        if (lbs != null && !lbs.isEmpty()) {
            cql.append("(n" );
            for(String lb : lbs) {
                cql.append( ":")
                   .append( lb );
            }
            cql.append(  ")");
        } else {
            cql.append("(n)");
        }

        // 关联
        Set<String> rts = Synt.asSet(rd.get(RELATS));
        if (rts != null && !rts.isEmpty()) {
            cql.append("<--(m)");
            fi = _cqlIn(whr, pms, "m."+ID_KEY, " IN ", rts, fi);
        }

        // 条件
        for(Map.Entry et : pms.entrySet()) {
            String fn = Synt.asString(et.getKey());
            Object fv = et.getValue();
            if (2 >= fn .length()
            &&!fls.containsKey(fn)) {
                continue;
            }
            if (LABELS.equals (fn)
            ||  RELATS.equals (fn)
            ||  CLEANS.equals (fn)
            || !FLDEXP.matcher(fn)
                      .matches(  )) {
                continue;
            }
            fn = "n."+fn;
            if (fv instanceof Map ) {
                fi = _cqlRl(whr, pms, fn, ( Map ) fv, fi );
            } else
            if (fv instanceof Collection
            ||  fv instanceof Object [ ]) {
                fi = _cqlIn(whr, pms, fn, " IN ", fv, fi );
            } else {
                fi = _cqlEq(whr, pms, fn, " = " , fv, fi );
            }
            whr.append(", ");
        }
        if (whr.length() > 0) {
            whr.delete(-2 + whr.length( ), whr.length( ) );
            cql.append(" WHERE ")
               .append(whr);
        }

        // 排序
        StringBuilder obs = new StringBuilder();
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        for(String  fn : ob ) {
            boolean de = fn.startsWith("-");
            if (de) fn = fn.substring ( 1 )
                  + " DESC";
            fn = "n." + fn ;
            obs.append( fn  )
               .append( ", ");
        }
        if (obs.length() > 0) {
            obs.delete(-2 + obs.length( ), obs.length( ) );
            cql.append(" ORDER BY ")
               .append( obs );
        }

        // 返回
        StringBuilder rbs = new StringBuilder();
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        for(String  fn : rb ) {
            fn = "n." + fn ;
            rbs.append( fn  )
               .append( ", ");
        }
        if (rbs.length() > 0) {
            rbs.delete(-2 + rbs.length( ), rbs.length( ) );
            cql.append(" RETURN n,")
               .append( rbs );
        } else {
            cql.append(" RETURN n,")
               .append("n.*");
        }

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

        // 总数
        StringBuilder xql = new StringBuilder();
        xql.append("MATCH (n) WHERE ")
           .append(whr)
           .append(" RETURN count(*)");

        // 获取结果和分页
        GraphDatabaseService db = getDB();
        Map sd  = new HashMap();
        if (pn != 0) {
            Result rst = db.execute(cql.toString(), pms);
            sd.put("list", _toList(rst, rb/**/));
        }
        if (rn != 0) {
            Result rst = db.execute(xql.toString(), pms);
            sd.put("page", _toPage(rst, rn, pn));
        }
        return  sd;
    }

    @Override
    public Map create(Map rd) throws HongsException {
        String  id = Core.newIdentity();
        rd.put (ID_KEY, id);
        setNode(id, rd);
        return  rd;
    }

    @Override
    public int update(Map rd) throws HongsException {
        Map rd2 = new HashMap(rd);
        Set ids = Synt.asSet (rd.remove(ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            Node node = getNode(id);
            if ( node == null) {
                throw new HongsException(0x1104, "Can not udpate for id: "+id);
            }
            setNode(node, rd2);
        }
        return ids.size();
    }

    @Override
    public int delete(Map rd) throws HongsException {
        Set ids = Synt.asSet (rd.get (ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            Node node = getNode(id);
            if ( node == null) {
                throw new HongsException(0x1104, "Can not delete for id: "+id);
            }
            delNode(node);
        }
        return ids.size();
    }

    protected Node getNode(String id) {
        return null;
    }

    protected void delNode(String id) {
       Node node = getNode(id);
        if (node != null) {
            delNode(node);
        }
        throw new NullPointerException("Can not del node '"+id+"', it is not exists");
    }

    protected void delNode(Node node) {
        Iterable <Relationship> rels = node.getRelationships(Direction.INCOMING);
        if (rels != null)
        for(Relationship relo : rels) {
            relo.delete();
        }
            node.delete();
    }

    protected void setNode(String id, Map info) {
        Node node = db.createNode();
        info.put(ID_KEY, id);
        setNode (node, info);
    }

    protected void setNode(Node node, Map info) {
        Map<String, Map> fields = getFields(  );

        // 清除旧属性
        byte cl = Synt.declare(info.get(CLEANS), (byte) 0);
        if (1 == (1 ^ cl))
        for(String fn : node.getPropertyKeys()) {
            node.removeProperty(fn);
        }
        if (2 == (2 ^ cl))
        for(Label  la : node.getLabels()) {
            node.removeLabel(la);
        }
        if (4 == (4 ^ cl))
        for(Relationship ra : node.getRelationships(Direction.OUTGOING)) {
            ra.delete(  );
        }

        // 写入新数据
        for(Object ot : info.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String fn = Synt.asString(
                        et.getKey( ) );
            Object fv = et.getValue( );
            Map    fc = fields.get(fn);

            if (RELATS.equals(fn)) {
                Set res = Synt.asSet(fv);
                if (res ==  null ) {
                    continue;
                }
                for(Object fv2 : res) {
                    Map    fv3 = Synt.asMap(fv2);
                    String fid = Synt.asString (fv3.get(ID_KEY));
                    String ftp = Synt.asString (fv3.get(RT_KEY));
                    Node   nod =    getNode(fid);
                    RelationshipType rtp = DynamicRelationshipType.withName(ftp);
                    Relationship     rel = node.createRelationshipTo ( nod, rtp);
                    // 关系属性
                    for(Object ot3 : fv3.entrySet()) {
                        Map.Entry et3 = (Map.Entry)ot3;
                        String fn3 = Synt.asString(et3.getKey());
                        if (! ID_KEY.equals(fn3)
                        &&  ! RT_KEY.equals(fn3)
                        &&  ! RD_KEY.equals(fn3)) {
                            rel.setProperty(fn3, et3.getValue());
                        }
                    }
                }
            } else
            if (LABELS.equals(fn)) {
                Set las = Synt.asSet(fv);
                if (las ==  null ) {
                    continue;
                }
                for(Object fv2 : las) {
                    String fn2 = Synt.asString(/**/ fv2);
                    Label  lab = DynamicLabel.label(fn2);
                    node.addLabel( lab );
                }
            } else
            if (CLEANS.equals(fn)) {
                // continue;
            } else
            if (fields.containsKey(fn)
            || (fn != null && 0 != fn.length()
            && (fc == null || ! ignored(fc) ))
            && !KEYEXP.matcher(fn).matches( ))
            {
                /**
                 * 满足条件:
                 * 名称必须符合命名规范
                 * 且字段未被标识为忽略
                 */
                if (fv != null) {
                    node.setProperty(fn,fv);
                } else {
                    node.removeProperty(fn);
                }
            }
        }
    }

    protected boolean ignored(Map fc) {
        return "".equals (fc.get("__name__"))
          ||  "@".equals (fc.get("__name__"))
          || "Ignore".equals (fc.get("rule"));
    }

    //** 辅助工具方法 **/

    private int _cqlEq(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
        pms.put(""+ i, v);
        xql.append( n )
           .append( r )
           .append("$")
           .append( i );
        return i ++;
    }

    private int _cqlIn(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
        Set a = Synt.asSet(v);
        xql.append( n )
           .append( r )
           .append("[");
        for(Object o : a) {
            pms.put(""+ i, o);
            xql.append("$")
               .append( i )
               .append(",");
            i ++;
        }
        xql.delete(-1 + xql.length(), xql.length());
        xql.append("]");
        return i;
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

    private Map _toPage(Result rst, int rn, int pn) {
        int tr = 0;
        int tp = 0;
        if (rst.hasNext() ) {
            Map  ro = rst.next( );
            tr = Synt.asInt(ro.get("count(*)"));
            tp = (int) Math.ceil((float) tr/rn);
        }

        Map page = new HashMap( );
        page.put("totalrows", tr);
        page.put("totlapage", tp);
        page.put("rows", rn);
        page.put("page", pn);
        page.put("ern" , rn > 0 ? 1 : 0);
        return page;
    }

    private List<Map> _toList(Result rst, Set rb) {
        boolean withLabs = rb == null || rb.contains(LABELS);
        boolean withRels = rb == null || rb.contains(RELATS);
        List list = new LinkedList();

        while (rst.hasNext()) {
            Map row  =  rst.next(  );
           Node nod  = (Node)row.get("n");
            if (nod !=  null) {
                row  =  new  HashMap(row);

                // 标签
                if (withLabs) {
                   List labs = new LinkedList();
                    for(Label lab : nod.getLabels()) {
                        labs.add( lab.name( ) );
                    }
                    row.put(LABELS, labs);
                }

                // 关系
                if (withRels) {
                   List rels = new LinkedList();
                    for(Relationship rel : nod.getRelationships(Direction.OUTGOING)) {
                        Map ral = new HashMap(rel.getAllProperties());
                        ral.put(RD_KEY, 0);
                        ral.put(RT_KEY, rel.getType().name());
                        ral.put(ID_KEY, rel.getEndNode(  ).getProperty(ID_KEY));
                    }
                    for(Relationship rel : nod.getRelationships(Direction.INCOMING)) {
                        Map ral = new HashMap(rel.getAllProperties());
                        ral.put(RD_KEY, 1);
                        ral.put(RT_KEY, rel.getType().name());
                        ral.put(ID_KEY, rel.getStartNode().getProperty(ID_KEY));
                    }
                    row.put(RELATS, rels);
                }
            }
            list.add(row);
        }

        return list;
    }

}
