package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
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
public class RoleSet extends CoreSerial implements CoreSerial.Mtimes, Set<String> {

    transient public final String userId;
    transient public final File   file  ;
    transient public final long   time  ;

    public Set <String> roles = null;

    private RoleSet(String userId) throws HongsException {
        String path = "master/role/" + Syno.splitPath(userId);
        this.userId = userId;
        this.file   = init  ( path );
        this.time   = fileModified();
    }

    @Override
    public long dataModified() {
        return time * 1000L ;
    }

    @Override
    public long fileModified() {
        return file.lastModified();
    }

    @Override
    protected byte expires(File f) throws HongsException {
        DB        db;
        Table     tb;
        Table     td;
        FetchCase fc;
        Map       rs;
        int       st;
        long      rt;
        long      ot;
        long      pt;

        db = DB.getInstance("master");

        tb = db.getTable("user");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .select(tb.name+".state, "+tb.name+".rtime, "+tb.name+".ptime")
                .filter(tb.name+".id = ?", userId);
        rs = db.fetchLess(fc);
        st = Synt.declare(rs.get("state"), 0 );
        rt = Synt.declare(rs.get("rtime"), 0L);
        pt = Synt.declare(rs.get("ptime"), 0L);
        if (st <= 0) {
            return -1; // 用户不存在或已锁定，则删除
        }

        /**
         * 使用密码登录
         * 当密码变更时(登录时间小于密码修改时间)
         * 需要重新登录
         */
        USK: {
            ActionHelper ah;
            try {
                ah = ActionHelper.getInstance();
            } catch (UnsupportedOperationException e) {
                break USK; // 不理会非动作环境
            }
            if ( ! "*".equals(ah.getSessibute(Cnst.USK_SES))) {
                break USK; // 不理会非密码登录
            }
            ot = Synt.declare(ah.getSessibute(Cnst.UST_SES) , 0L);
            if (ot < pt && 0 < ot && 0 < pt) {
                throw new HongsException(401, "@master:core.password.changed");
            }
        }
        
        tb = db.getTable("unit");
        td = db.getTable("unit_user");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .join  (td.tableName, td.name , td.name+".unit_id = "+tb.name+".id" /***/ )
                .select("MAX("+tb.name+".state) AS state, MAX("+tb.name+".rtime) AS rtime")
                .filter(td.name+".user_id = ?", userId)
                .gather(td.name+".user_id");
        rs = db.fetchLess(fc);
        st = Synt.declare(rs.get("state"), 1 );
        ot = Synt.declare(rs.get("rtime"), 0L);
        if (st <= 0) {
            return -1; // 所在的分组均已锁定，则删除
        }

        /**
         * 比较文件修改时间和权限变更时间
         * 还没有过期则从缓存文件载入即可
         */
        if (rt < ot) {
            rt = ot;
        }
        if (f.exists() && f.lastModified() >= rt * 1000L) {
            return  1;
        } else {
            return  0;
        }
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

        tb = db.getTable("unit_role");
        td = db.getTable("unit_user");
        tt = db.getTable("unit");
        fc = new FetchCase( FetchCase.STRICT )
                .from  (tb.tableName, tb.name)
                .join  (td.tableName, td.name , tb.name+".unit_id = "+td.name+".unit_id")
                .join  (tt.tableName, tt.name , td.name+".unit_id = "+tt.name+".id" /**/)
                .select(tb.name+".role")
                .filter(td.name+".user_id = ?", userId)
                .filter(tt.name+".state > 0" );
        rz = db.fetchMore(fc);
        for (Map rm : rz) {
            roles.add((String) rm.get("role"));
        }
    }

    @Override
    protected void load(Object data) {
        roles = (Set) data;
    }

    @Override
    protected Object save() {
        return roles;
    }

    //** 构造工厂方法 **/

    public static RoleSet getInstance(String userId)
    throws HongsException {
        String  k = RoleSet.class.getName() +":"+ userId;
        Core    c = Core.getInstance( );
        if ( c.exists( k ) ) { // 可以为空, 因此不用 isset
            return (RoleSet) c.get( k );
        }
        RoleSet s = new RoleSet(userId);
        if (s.roles == null) {
            s = null; // 状态不对
        }
        c.set(k , s); // 缓存对象
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
