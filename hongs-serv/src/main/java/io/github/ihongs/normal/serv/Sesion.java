package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.DB;
import io.github.ihongs.util.Digest;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * 会话状态记录
 * @author Hongs
 * @deprecated 改用 Jetty 的 SessionManager
 */
@Cmdlet("normal.sesion")
public class Sesion implements HttpSession, AutoCloseable, Serializable {

    private transient boolean isNew = false;
    private transient boolean isMod = false;
    private transient boolean isSav = false;
    private transient ServletRequest req;
    private transient ServletContext ctx;
    private final Map<String,Object> dat;
    private String sid;
    private long ctime = -1;
    private long atime = -1;
    private int  xtime = 86400;

    public Sesion(String nid) {
        sid   = nid;
        dat   = new HashMap();
        ctime = System.currentTimeMillis();
        atime = ctime;
        isNew = true ;
        isMod = true ;
        isSav = false;
    }

    public Sesion() {
        this(null );
    }

    public void setServletRequest(ServletRequest req) {
        this.req = req;
    }

    public void setServletContext(ServletContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public ServletContext getServletContext() {
        return ctx;
    }

    @Override
    public String getId() {
        if (sid == null ) {
            sid = newId();
        }
        return sid;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public long getCreationTime() {
        return ctime;
    }

    @Override
    public long getLastAccessedTime() {
        return atime;
    }

    @Override
    public int  getMaxInactiveInterval() {
        return xtime;
    }

    @Override
    public void setMaxInactiveInterval( int time ) {
        xtime = time ;
        isSav = false;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new Keys(dat.keySet());
    }

    @Override
    public Object getAttribute(String s) {
        return dat.get(s);
    }

    @Override
    public void setAttribute(String s, Object v) {
        dat.put(s, v);
        isMod = true ;
        isSav = false;
    }

    @Override
    public void removeAttribute(String s) {
        dat.remove(s);
        isMod = true ;
        isSav = false;
    }

    @Override
    public void invalidate() {
        dat.clear ( );
        sid = newId();
        isNew = true ;
        isMod = true ;
        isSav = true ;
        ctime = System.currentTimeMillis();

        // 删除会话 Cookie
        if (req != null && req instanceof SessAccess) {
           ((SessAccess) req ).delCookie();
        }
    }

    public void revalidate() {
        sid = newId();
        isNew = true ;
        isMod = true ;
        isSav = false;
        ctime = System.currentTimeMillis();

        // 更新会话 Cookie
        if (req != null && req instanceof SessAccess) {
           ((SessAccess) req ).setCookie();
        }
    }

    private String newId() {
        // 删除旧的数据
        if (sid != null) {
            try {
                getRecord().del( sid );
            } catch (HongsException e) {
                throw e.toExemption( );
            }
        }

        // 重新构建的ID
        String id = Core.newIdentity();
        if (req != null) {
        String ip = ActionDriver.getClientAddr((HttpServletRequest) req);
        if (ip  != null) {
            id  +=  "@" + ip ;
        }}

        return Digest.md5(id);
    }

    public void store() throws HongsException {
        if (isSav) {
            return;
        }
        boolean beMod = isMod;
        isMod = false;
        isSav = true ;

        IRecord rec = getRecord();
        String  key = getId(    );
        long    exp = getMaxInactiveInterval();

        if (exp == 0) {
            rec.del(key); return ;
        } else
        if (exp  < 0) {
            exp  = 0; // 永不过期;
        } else {
            exp  += System.currentTimeMillis() / 1000;
        }

        if (beMod) {
            atime = System.currentTimeMillis() / 1000;
            rec.set(key, this, exp);
        } else {
            rec.set(key,       exp);
        }
    }

    @Override
    public void close() throws HongsException {
        store();

        IRecord rec = getRecord();

        // 如果数据记录对象可关闭, 关闭之
        if (rec instanceof AutoCloseable) {
            try {
                ((AutoCloseable) rec).close();
            } catch ( Exception  err) {
                throw new HongsException(err);
            }
        }
    }

    /**
     * @deprecated
     * @return
     */
    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    /**
     * @deprecated
     * @return
     */
    @Override
    public String[] getValueNames() {
        return dat.keySet().toArray(new String[]{});
    }

    /**
     * @deprecated
     * @param s
     * @return
     */
    @Override
    public Object getValue(String s) {
        return dat.get(s);
    }

    /**
     * @deprecated
     * @param s
     * @param o
     */
    @Override
    public void putValue(String s, Object o) {
        dat.put(s, o);
    }

    /**
     * @deprecated
     * @param s
     */
    @Override
    public void removeValue(String s) {
        dat.remove(s);
    }

    /**
     * 清除过期的数据
     * @param args
     * @throws io.github.ihongs.HongsException
     */
    @Cmdlet("clean")
    public static void clean(String[] args) throws HongsException {
        long exp = 0;
        if (args.length != 0) {
             exp = Integer.parseInt ( args [ 0 ] );
        }
        getRecord().del(System.currentTimeMillis() / 1000 - exp);
    }

    /**
     * 预览存储的数据
     * @param args
     * @throws io.github.ihongs.HongsException
     */
    @Cmdlet("check")
    public static void check(String[] args) throws HongsException {
        if (args.length == 0) {
            CmdletHelper.println("Record ID required");
        }
        CmdletHelper.preview(getRecord().get(args[0]));
    }

    /**
     * 用会话 ID 获取会话对象
     * @param id
     * @return
     */
    public static Sesion getInstance(String id) {
        try {
            return getRecord().get(id);
        } catch (HongsException e) {
            throw e.toExemption( );
        }
    }

    private static IRecord<Sesion> getRecord() throws HongsException {
        String cls = CoreConfig.getInstance( ).getProperty("core.normal.sesion.model");
        if (null == cls || 0 == cls.length() ) {
               cls = Recs.class.getName( );

            // 缺失则用私有类构造一个
            Core core = Core.getInstance();
            if (!core.containsKey(cls)) {
                Recs rec = new Recs();
                core.put ( cls, rec );
                return rec;
            }
        }
        return (IRecord<Sesion>) Core.getInstance(cls);
    }

    private static class Recs extends JRecord<Sesion> implements IRecord<Sesion> {

        public Recs() throws HongsException {
            super(DB.getInstance("normal").getTable("sesion"));
        }

    }

    private static class Keys implements Enumeration<String> {
        private Iterable<String> iterable;
        private Iterator<String> iterator;

        public Keys(Iterable<String> iterable) {
            this.iterable = iterable;
        }

        @Override
        public boolean hasMoreElements() {
            if (iterator == null) {
                iterator = iterable.iterator();
            }

            return iterator.hasNext();
        }

        @Override
        public String nextElement() {
            if (iterator == null) {
                iterator = iterable.iterator();
            }

            return iterator.next();
        }
    }

}
