package app.hongs.serv.record;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.util.Synt;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * 会话状态
 * @author Hongs
 */
public class Status implements HttpSession, Serializable {
    private transient boolean isNew = false;
    private transient boolean isMod = false;
    private transient ServletContext  ctx;
    private final Map<String, Object> map;
    private final String sid;
    private long  ctime = -1;
    private long  atime = -1;
    private int   etime = -1;

    public Status() {
        map   = new HashMap();
        sid   = Core.newIdentity();
        ctime = System.currentTimeMillis();
        atime = ctime;
        isNew = true;
        isMod = true;
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
        return etime;
    }

    @Override
    public void setMaxInactiveInterval(int sec) {
        this.etime = sec;
    }

    @Override
    public ServletContext getServletContext(  ) {
        return  this.ctx;
    }

    public void setServletContext(ServletContext ctx) {
        this.ctx  =  ctx;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new It2Em(map.keySet());
    }

    @Override
    public Object getAttribute(String s) {
        return map.get(s);
    }

    @Override
    public void setAttribute(String s, Object v) {
        isMod = true;
        map.put(s, v);
    }

    @Override
    public void removeAttribute(String s) {
        isMod = true;
        map.remove(s);
    }

    @Override
    public void invalidate() {
        setMaxInactiveInterval(0);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public boolean isMod() {
        return isMod;
    }

    /**
     * 将会话数据持久存储
     * @throws HongsException
     */
    public void store() throws HongsException {
        long   exp = getMaxInactiveInterval();
        String key = "$." + sid ;
        if (exp == 0) {
            getRecord().del(key);
            return;
        }

        if (exp <  0) {
            exp  = 0; // 永不过期
        } else {
            exp += System.currentTimeMillis() / 1000;
        }

        if (isMod( )) {
            getRecord().set(key, this, exp);
        } else {
            getRecord().set(key, /***/ exp);
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
        return map.keySet().toArray(new String[]{});
    }

    /**
     * @deprecated
     * @param s
     * @return
     */
    @Override
    public Object getValue(String s) {
        return map.get(s);
    }

    /**
     * @deprecated
     * @param s
     * @param o
     */
    @Override
    public void putValue(String s, Object o) {
        map.put(s, o);
    }

    /**
     * @deprecated
     * @param s
     */
    @Override
    public void removeValue(String s) {
        map.remove(s);
    }

    /**
     * 通过会话 ID 获取会话对象
     * @param ssid
     * @return
     */
    public  static  Status getStatus(String ssid) {
        try {
            return Synt.declare(getRecord().get(ssid), Status.class);
        }
        catch (HongsException ex) {
            throw new HongsError.Common(ex);
        }
    }

    private static IRecord getRecord() {
        String clsn = CoreConfig.getInstance().getProperty("core.common.status.class", MRecord.class.getName());
        return Synt.declare(Core.getInstance( clsn ), MRecord.class);
    }

    private class It2Em implements Enumeration<String> {
        private Iterable<String> iterable;
        private Iterator<String> iterator;

        public It2Em(Iterable<String> iterable) {
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
