package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Filter;
import app.hongs.action.anno.FilterInvoker;
import app.hongs.dh.IActing;
import app.hongs.util.Clses;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 动作执行器
 *
 * <h3>异常代码</h3>
 * <pre>
 * 区间: 0x1100~0x110f
 * 0x1100 错误请求
 * 0x1101 尚未登陆
 * 0x1102 区域错误
 * 0x1103 无权访问
 * 0x1104 无此动作
 * 0x1105 不支持此方法
 * 0x110e 无法执行, 禁止访问或参数错误
 * 0x110f 注解链溢出
 * </pre>
 *
 * @author Hong
 */
public class ActionRunner {
    private int idx = 0;
    private String action;
    private final Object   object;
    private final Method   method;
    private final Class<?> mclass;
    private final ActionHelper helper;
    private final Annotation[] annarr;
    private static Map<String, Mathod> ACTIONS = null;
    private static final ReadWriteLock ACTLOCK = new ReentrantReadWriteLock();

    public ActionRunner(String action, ActionHelper helper) throws HongsException {
        Mathod mt = getActions().get(action);
        if (mt == null ) {
            throw new HongsException(0x1104, "Can not find action '"+ action +"'");
        }

        this.action = action;
        this.helper = helper;
        this.mclass = mt.mclass;
        this.method = mt.method;
        this.object = Core.getInstance(mclass);
        this.annarr = method.getAnnotations( );
        helper.setAttribute(Cnst.RUNNER_ATTR , this);

        // Initialize action
        if (object instanceof IActing) {
            ((IActing)object).initiate(helper, this);
        }
    }

    public Object getObject() {
        return object;
    }

    public String getMthAnn() {
        return method.getAnnotation(Action.class).value();
    }

    public String getClsAnn() {
        return mclass.getAnnotation(Action.class).value();
    }

    /**
     * 执行动作方法
     * 会执行 action 方法上 annotation 指定的过滤器
     * @throws HongsException
     */
    public void doAction() throws HongsException {
        // 如果超出链长度, 则终止执行
        if ( idx  >  annarr.length) {
            throw new HongsException(0x110f, "Action annotation out of index: "
            +idx+">"+annarr.length);
        }

        // 如果已到达链尾, 则执行动作
        if ( idx ==  annarr.length) {
            doInvoke();
            return;
        }

        Filter actw;
        Annotation anno = annarr[idx ++];
        if (anno instanceof Filter) {
            actw = ( Filter ) anno;
        } else {
            actw = anno.annotationType().getAnnotation(Filter.class);
        }

        // 如果不是动作链, 则跳过注解
        if (actw == null) {
            doAction();
            return;
        }

        // 执行注解过滤器
        Class<? extends FilterInvoker> classo = actw.value();
        FilterInvoker filter = Core.getInstance(classo);
        filter.invoke(helper , this, anno);
    }

    /**
     * 执行动作方法
     * 不执行 action 方法上 annotation 指定的过滤器
     * @throws HongsException
     */
    public void doInvoke() throws HongsException {
        try {
            method.invoke(object, helper);
        } catch (   IllegalAccessException e) {
            throw new HongsException(0x110e, "Illegal access for method '"+mclass.getName()+"."+method.getName()+"(ActionHelper).");
        } catch ( IllegalArgumentException e) {
            throw new HongsException(0x110e, "Illegal params for method '"+mclass.getName()+"."+method.getName()+"(ActionHelper).");
        } catch (InvocationTargetException e) {
            Throwable ex = e.getCause();
            if (ex instanceof HongsError/**/) {
                throw (HongsError/**/) ex;
            } else
            if (ex instanceof HongsException) {
                throw (HongsException) ex;
            } else {
                throw new HongsException(0x110e, ex);
            }
        }
    }

    //** 更方便的获取模块、实体、操作的方式 **/

    private String mod = null;
    private String ent = null;
    private String ant = null;
    private String act = null;

    public void setModule(String name) {
        mod = name;
    }
    public void setEntity(String name) {
        ent = name;
    }
    public void setHandle(String name) {
        ant = name;
    }

    /**
     * 设置动作名
     * 即用于此动作的虚拟动作名,
     * getModule,getEntity,getHandle
     * 可从此动作名分解出各部分.
     * @param name
     */
    public void setAction(String name) {
        act = name;
    }

    public String getModule() throws HongsException {
        if (null != mod) {
            return  mod;
        }
        int pos;
        mod  = getAction();
        pos  = mod.lastIndexOf('/');
        mod  = mod.substring(0,pos); // 去掉动作
        pos  = mod.lastIndexOf('/');
        mod  = mod.substring(0,pos); // 去掉实体
        return mod;
    }
    public String getEntity() throws HongsException {
        if (null != ent) {
            return  ent;
        }
        int pos;
        ent  = getAction();
        pos  = ent.lastIndexOf('/');
        ent  = ent.substring(0,pos); // 去掉动作
        pos  = ent.lastIndexOf('/');
        ent  = ent.substring(1+pos); // 得到实体
        return ent;
    }
    public String getHandle() throws HongsException {
        if (null != ant) {
            return  ant;
        }
        int pos;
        ant  = getAction();
        pos  = ant.lastIndexOf('/');
        ant  = ant.substring(1+pos); // 得到动作
        return ant;
    }

    /**
     * 获取动作名
     * 外部有指定工作路径(ActionDriver.PATH)则返回工作动作名
     * 同时可使用 setAction 进行设置
     * @return
     * @throws HongsException
     */
    public String getAction() throws HongsException {
        if (null != act) {
            return  act;
        }
        act = (String) helper.getAttribute(Cnst.PATH_ATTR);
        if (act != null) {
            // 去除路径中的根目录和扩展名
            int pos = act.lastIndexOf('.');
            if (pos > 0) {
                act = act.substring(0,pos);
            }
            if (act.startsWith( "/" )) {
                act = act.substring(1);
            }
            return    act;
        } else {
            return action;
        }
    }

    //** 动作方法 **/

    public  static final class Mathod {
        Method   method;
        Class<?> mclass;
        @Override
        public String    toString() {
            return mclass.getName()+"."+method.getName();
        }
        public Method   getMethod() {
            return method;
        }
        public Class<?> getMclass() {
            return mclass;
        }
    }

    public  static Map<String, Mathod> getActions() {
        Lock rlock = ACTLOCK. readLock();
        rlock.lock();
        try {
            if (ACTIONS != null) {
                return  ACTIONS;
            }
        } finally {
            rlock.unlock();
        }

        Lock wlock = ACTLOCK.writeLock();
        wlock.lock();
        try {
            String[] pkgs = CoreConfig.getInstance("_begin_").getProperty("core.load.serv").split(";");
            ACTIONS = getActions( pkgs );
            return ACTIONS;
        } finally {
            wlock.unlock();
        }
    }

    private static Map<String, Mathod> getActions(String... pkgs) {
        Map<String, Mathod> acts = new HashMap();

        for(String pkgn : pkgs) {
            pkgn = pkgn.trim( );
            if (pkgn.length ( ) == 0) continue;
            Set< String > clss ;

            if (pkgn.endsWith(".*")) {
                pkgn = pkgn.substring(0, pkgn.length() - 2);
                try {
                    clss = Clses.getClassNames(pkgn, false);
                } catch (IOException ex) {
                    throw new HongsError( 0x4a , "Can not load package '" + pkgn + "'.", ex);
                }
                if (clss == null) {
                    throw new HongsError( 0x4a , "Can not find package '" + pkgn + "'.");
                }
            } else {
                clss = new HashSet();
                clss.add(pkgn);
            }

            for(String clsn : clss) {
                Class  clso;
                try {
                    clso = Class.forName(clsn);
                } catch (ClassNotFoundException ex) {
                    throw new HongsError(0x4a, "Can not find class '" + clsn + "'.");
                }

                // 从注解提取动作名
                Action anno = (Action) clso.getAnnotation(Action.class);
                if (anno == null) {
                    continue;
                }
                String actn = anno.value();
                if (actn == null || actn.length() == 0) {
                    actn =  clsn.replace('.','/');
                }

                Method[] mtds = clso.getMethods();
                for(Method mtdo : mtds) {
                    String mtdn = mtdo.getName( );

                    // 从注解提取动作名
                    Action annx = (Action) mtdo.getAnnotation(Action.class);
                    if (annx == null) {
                        continue;
                    }
                    String actx = annx.value();
                    if (actx == null || actx.length() == 0) {
                        actx =  mtdn;
                    }

                    // 检查方法是否合法
                    Class[] prms = mtdo.getParameterTypes();
                    if (prms == null || prms.length != 1 || !prms[0].isAssignableFrom(ActionHelper.class)) {
                        throw new HongsError(0x4a, "Can not find action method '"+clsn+"."+mtdn+"(ActionHelper)'.");
                    }

                    Mathod mtdx = new Mathod();
                    mtdx.method = mtdo;
                    mtdx.mclass = clso;

                    if ("__main__".equals(actx)) {
                        acts.put(actn /*__main__*/ , mtdx );
                    } else {
                        acts.put(actn + "/" + actx , mtdx );
                    }
                }
            }
        }

        return acts;
    }

}
