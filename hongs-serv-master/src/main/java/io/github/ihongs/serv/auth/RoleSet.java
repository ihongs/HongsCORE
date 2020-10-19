package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.Table;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
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
public class RoleSet extends CoreSerial implements CoreSerial.LastModified, Set<String> {

    public /**/String userId;
    public Set<String> roles;
    public long        rtime;

    private RoleSet(String userId) throws HongsException {
        this.userId = userId;
        init("master/role/" + Syno.splitPath(userId));
    }

    @Override
    protected void imports() throws HongsException {
        roles = new HashSet();

        DB        db;
        Table     tb;
        Table     td;
        Table     tt;
        FetchCase fc;
        List<Map> rz;

        db = DB.getInstance("master");

        //** 查询用户权限 **/

        tb = db.getTable("user_role");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .select(tb.name+".role")
                .filter(tb.name+".user_id = ?", userId);
        rz = db.fetchMore(fc);
        for (Map rm : rz) {
            roles.add((String) rm.get("role"));
        }

        //** 查询部门权限 **/

        tb = db.getTable("dept_role");
        td = db.getTable("dept_user");
        tt = db.getTable("dept");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .join  (td.tableName, td.name , tb.name+".dept_id = "+td.name+".dept_id")
                .join  (tt.tableName, tt.name , td.name+".dept_id = "+tt.name+".id" /**/)
                .select(tb.name+".role")
                .filter(td.name+".user_id = ?", userId)
                .filter(tt.name+".state > 0" );
        rz = db.fetchMore(fc);
        for (Map rm : rz) {
            roles.add((String) rm.get("role"));
        }

        //** 当前保存时间 **/

        rtime = System.currentTimeMillis() / 1000L;
    }

    @Override
    protected byte read(File f) throws HongsException {
        DB        db;
        Table     tb;
        Table     td;
        FetchCase fc;
        Map       rs;
        int       st;
        long      rt;
        long      ot;

        db = DB.getInstance("master");

        tb = db.getTable("user");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .select(tb.name+".state, "+tb.name+".rtime")
                .filter(tb.name+".id = ?", userId);
        rs = db.fetchLess(fc);
        st = Synt.declare(rs.get("state"), 0 );
        rt = Synt.declare(rs.get("rtime"), 0L);
        if (st <= 0) {
            return -1; // 用户不存在或已锁定，则删除
        }

        tb = db.getTable("dept");
        td = db.getTable("dept_user");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .join  (td.tableName, td.name , td.name+".dept_id = "+tb.name+".id" /***/ )
                .select("MAX("+tb.name+".state) AS state, MAX("+tb.name+".rtime) AS rtime")
                .filter(td.name+".user_id = ?", userId   )
                .gather(td.name+".user_id");
        rs = db.fetchLess(fc);
        st = Synt.declare(rs.get("state"), 1 );
        ot = Synt.declare(rs.get("rtime"), 0L);
        if (st <= 0) {
            return -1; // 所在的分组均已锁定，则删除
        }
        if (rt < ot) {
            rt = ot;
        }

        if (!f.exists()) {
            return  0;
        }
        load(f);
        if (rt > rtime ) {
            return  0;
        } else {
            return  1;
        }
    }

    @Override
    public long lastModified() {
        return rtime * 1000L;
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
        if (s . rtime == 0L ) {
            s = null; // 状态不对
        }
        c.put(k , s); // 缓存对象
        return s;
    }

    public static RoleSet getInstance()
    throws HongsException {
        Object id = Core.getInstance (ActionHelper.class)
                        .getSessibute(   Cnst.UID_SES   );
        if (id ==  null) {
            return null; // 未登录
        }
        return getInstance((String) id);
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
