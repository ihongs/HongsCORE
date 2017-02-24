package app.hongs.serv.auth;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreSerial;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Table;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户组记录
 * @author Hongs
 */
public class RoleSet extends CoreSerial implements Set<String> {

    public String       userId;
    public Set<String>  roles;
    public int          rtime;

    private RoleSet(String userId) throws HongsException {
        this.userId = userId;

        String  n;
        File    f;

        n = Core.DATA_PATH
          + File.separator + "serial"
          + File.separator + "member"
          + File.separator + "role"
          + File.separator + Tool.splitPath(userId);
        f = new  File(n + ".ser");

        if (f.exists()) {
            this.load(f, 0); // 从文件加载
        } else {
            this.load(f, 1); // 从库表加载
            return;
        }

        DB        db;
        Table     tb;
        Table     td;
        FetchCase fc;
        Map       rs;
        int       rt;
        int       st;

        db = DB.getInstance("member");

        tb = db.getTable("user");
        fc = new FetchCase( )
                .from   (tb.tableName, tb.name)
                .select (tb.name+".rtime, "+tb.name+".state")
                .filter (tb.name+".id = ?" , userId);
        rs = db.fetchLess(fc);
        rt = Synt.declare(rs.get( "rtime" ), 0);
        st = Synt.declare(rs.get( "state" ), 0);
        if (st <=   0 ) { // 删除或锁定
            rtime = 0 ;
            return;
        }
        if (rt > rtime) { // 从库表加载
            load(f, 1);
            return;
        }

        tb = db.getTable("dept");
        td = db.getTable("user_dept");
        fc = new FetchCase( )
                .from   (tb.tableName, tb.name)
                .select (tb.name+".rtime, "+tb.name+".state")
                .orderBy(tb.name+".rtime DESC");
              fc.join   (td.tableName, td.name)
                .on     (td.name+".dept_id = "+tb.name+".id")
                .filter (td.name+".user_id = ?", userId);
        rs = db.fetchLess(fc);
        rt = Synt.declare(rs.get( "rtime" ), 0);
        st = Synt.declare(rs.get( "state" ), 0);
        if (rs.isEmpty()) { // 没有部门
            return;
        }
        if (st <=   0 ) { // 删除或锁定
            rtime = 0 ;
            return;
        }
        if (rt > rtime) { // 从库表加载
            load(f, 1 );
        }
    }

    @Override
    protected void imports() throws HongsException {
        roles = new HashSet();

        DB        db;
        Table     tb;
        Table     td;
        FetchCase fc;
        List<Map> rz;

        db = DB.getInstance("member");

        //** 查询用户权限 **/

        tb = db.getTable("user_role");
        fc = new FetchCase( )
                .from   (tb.tableName, tb.name)
                .select (tb.name+".role")
                .filter (tb.name+".user_id = ?",userId);
        rz = db.fetchMore(fc);
        for (Map rm : rz) {
            roles.add((String) rm.get("role"));
        }

        //** 查询部门权限 **/

        tb = db.getTable("dept_role");
        td = db.getTable("user_dept");
        fc = new FetchCase( )
                .from   (tb.tableName, tb.name)
                .select (tb.name+".role");
              fc.join   (td.tableName, td.name)
                .on     (td.name+".dept_id = "+tb.name+".dept_id")
                .filter (td.name+".user_id = ?", userId);
        rz = db.fetchMore(fc);
        for (Map rm : rz) {
            roles.add((String) rm.get( "role" ));
        }

        //** 当前保存时间 **/

        rtime = (int) (System.currentTimeMillis() / 1000);
    }

    //** 构造工厂方法 **/

    public static RoleSet getInstance(String userId)
    throws HongsException {
        String  k = RoleSet.class.getName() +":"+ userId ;
        Core    c = Core.getInstance( );
        if (c.containsKey(k)) {
            return (RoleSet) c.get( k );
        }
        RoleSet s = new RoleSet(userId);
        if (s.rtime ==   0  ) {
            s = null;   // 状态不对
        }
        c.put(k,s);     // 缓存对象
        return  s ;
    }

    public static RoleSet getInstance()
    throws HongsException {
        ActionHelper ah = Core.getInstance (ActionHelper.class);
        String id = (String)ah.getSessibute(Cnst.UID_SES);
        if  (  id == null  ) {
            return   null; // 未登录
        }
        return getInstance(id);
    }

    //** Set 相关操作 **/

    @Override
    public int size() {
        return roles.size();
    }

    @Override
    public boolean isEmpty() {
        return roles.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return roles.contains(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        return roles.containsAll(c);
    }

    @Override
    public boolean add(String e) {
        return roles.add(e);
    }

    @Override
    public boolean addAll(Collection c) {
        return roles.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return roles.remove(o);
    }

    @Override
    public boolean removeAll(Collection c) {
        return roles.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
        return roles.retainAll(c);
    }

    @Override
    public void clear() {
        roles.clear();
    }

    @Override
    public Iterator iterator() {
        return roles.iterator();
    }

    @Override
    public Object[] toArray( ) {
        return roles.toArray( );
    }

    @Override
    public Object[] toArray(Object[] a) {
        return roles.toArray(a);
    }

}
