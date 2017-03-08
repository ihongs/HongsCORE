package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsError;
import app.hongs.HongsException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * 会话状态记录
 * @author Hongs
 */
public class Sesion implements HttpSession, AutoCloseable, Serializable {

    private transient boolean isNew = false;
    private transient boolean isMod = false;
    private transient    SessFiller  ctz;
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
    }

    public Sesion() {
        this(Core.newIdentity());
    }

    public void setRequestContext(   SessFiller  ctz) {
        this.ctz = ctz;
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
        return sid;
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
    public void setMaxInactiveInterval(int sec) {
        this.xtime = sec;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new Attrs(dat.keySet());
    }

    @Override
    public Object getAttribute(String s) {
        return dat.get(s);
    }

    @Override
    public void setAttribute(String s, Object v) {
        dat.put(s, v);
        isMod = true ;
    }

    @Override
    public void removeAttribute(String s) {
        dat.remove(s);
        isMod = true ;
    }

    @Override
    public void invalidate() {
        // 删除旧的会话
        try {
            getRecord( ).del( "$." + sid );
        } catch (HongsException e) {
            throw new HongsError.Common(e);
        }

        // 重建新的会话
        dat.clear(  );
        sid   = Core.newIdentity();
        ctime = System.currentTimeMillis();
        atime = ctime;
        isMod = false;
        isNew = true ;

        // 延期会话 Cookie
        if (  ctz != null  ) {
            ctz.setCookies();
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public boolean isMod() {
        return isMod;
    }

    /**
     * 退出时将会话数据存起来
     * @throws HongsException
     */
    @Override
    public void close() throws HongsException {
        long   exp = getMaxInactiveInterval();
        String key = "$." + sid ;
        if (exp == 0) {
            getRecord().del(key);
            return;
        }

        if (exp <  0) {
            exp =  0; // 永不过期
        } else {
            exp = exp + System.currentTimeMillis() / 1000;
        }

        IRecord rec = getRecord( );

        if (isMod( )) {
            atime   =   System.currentTimeMillis() / 1000;
            rec.set(key, this, exp);
        } else {
            rec.set(key, /***/ exp);
        }

        // 如果对应的数据记录对象可关闭, 则关闭之
        if (rec instanceof AutoCloseable) {
            try {
                (   (AutoCloseable) rec ).close(   );
            } catch (Exception err) {
                throw new HongsException.Common(err);
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
     * 用会话 ID 获取会话对象
     * @param id
     * @return
     */
    public static Sesion getSesion(String id) {
        try {
            return getRecord().get("$." + id);
        }
        catch (HongsException ex) {
            throw new HongsError.Common( ex );
        }
    }

    private static IRecord<Sesion> getRecord() {
        CoreConfig  conf = CoreConfig.getInstance();
        String clsn = conf.getProperty("core.sesion.record.model");
        if (clsn == null || clsn.length() < 1) {
               clsn = conf.getProperty("core.common.record.model");
        if (clsn == null || clsn.length() < 1) {
               clsn = MRecord.class.getName( );
        }}
        return (IRecord<Sesion>) Core.getInstance(clsn);
    }

    private static class Attrs implements Enumeration<String> {
        private Iterable<String> iterable;
        private Iterator<String> iterator;

        public Attrs(Iterable<String> iterable) {
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
