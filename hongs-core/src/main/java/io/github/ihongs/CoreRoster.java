package io.github.ihongs;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.util.reflex.Classes;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务加载工具
 * @author Hongs
 */
public class CoreRoster {

    private static final ReadWriteLock RWLOCKS = new ReentrantReadWriteLock();
    private static Map<String, Method> CMDLETS = null;
    private static Map<String, Mathod> ACTIONS = null;

    public  static final class Mathod {
        private Method   method;
        private Class<?> mclass;
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

    public static Map<String, Mathod> getActions() {
        Lock rlock = RWLOCKS.readLock();
        rlock.lock();
        try {
            if (ACTIONS != null) {
                return  ACTIONS;
            }
        } finally {
            rlock.unlock();
        }

        addServ();
        return ACTIONS;
    }

    public static Map<String, Method> getCmdlets() {
        Lock rlock = RWLOCKS.readLock();
        rlock.lock();
        try {
            if (null != CMDLETS) {
                return  CMDLETS;
            }
        } finally {
            rlock.unlock();
        }

        addServ();
        return CMDLETS;
    }

    private static void addServ() {
        Lock wlock = RWLOCKS.writeLock();
        wlock.lock();
        try {
            ACTIONS = new HashMap();
            CMDLETS = new HashMap();
            String[] pkgs = CoreConfig
                    .getInstance("defines"   )
                    .getProperty("mount.serv")
                    .split(";");
            addServ(ACTIONS , CMDLETS , pkgs );
        } finally {
            wlock.unlock();
        }
    }

    private static void addServ(Map<String, Mathod> acts, Map<String, Method> cmds, String... pkgs) {
        for(String pkgn : pkgs) {
            pkgn = pkgn.trim( );
            if (pkgn.length ( ) == 0) {
                continue;
            }

            Set<String> clss = getClss(pkgn);
            for(String  clsn : clss) {
                Class   clso = getClso(clsn);

                // 从注解提取动作名
                Action acto = (Action) clso.getAnnotation(Action.class);
                if (acto != null) {
                    addActs(acts, acto, clsn, clso);
                    continue;
                }

                Cmdlet cmdo = (Cmdlet) clso.getAnnotation(Cmdlet.class);
                if (cmdo != null) {
                    addCmds(cmds, cmdo, clsn, clso);
                }
            }
        }
    }

    private static void addActs(Map<String, Mathod> acts, Action anno, String clsn, Class clso) {
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
                throw new HongsError(0x3c, "Can not find action method '"+clsn+"."+mtdn+"(ActionHelper)'.");
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

    private static void addCmds(Map<String, Method> acts, Cmdlet anno, String clsn, Class clso) {
        String actn = anno.value();
        if (actn == null || actn.length() == 0) {
            actn =  clsn;
        }

        Method[] mtds = clso.getMethods();
        for(Method mtdo : mtds) {
            String mtdn = mtdo.getName( );

            // 从注解提取动作名
            Cmdlet annx = (Cmdlet) mtdo.getAnnotation(Cmdlet.class);
            if (annx == null) {
                continue;
            }
            String actx = annx.value();
            if (actx == null || actx.length() == 0) {
                actx =  mtdn;
            }

            // 检查方法是否合法
            Class[] prms = mtdo.getParameterTypes();
            if (prms == null || prms.length != 1 || !prms[0].isAssignableFrom(String[].class)) {
                throw new HongsError(0x3c, "Can not find cmdlet method '"+clsn+"."+mtdn+"(String[])'.");
            }

            if ("__main__".equals(actx)) {
                acts.put(actn /*__main__*/ , mtdo );
            } else {
                acts.put(actn + "." + actx , mtdo );
            }
        }
    }

    private static Set<String> getClss(String pkgn) {
        Set<String> clss;

        if (pkgn.endsWith(".**")) {
            pkgn = pkgn.substring(0, pkgn.length() - 3);
            try {
                clss = Classes.getClassNames(pkgn, true );
            } catch (IOException ex) {
                throw new HongsError( 0x3a , "Can not load package '" + pkgn + "'.", ex);
            }
            if (clss == null) {
                throw new HongsError( 0x3a , "Can not find package '" + pkgn + "'.");
            }
        } else
        if (pkgn.endsWith(".*" )) {
            pkgn = pkgn.substring(0, pkgn.length() - 2);
            try {
                clss = Classes.getClassNames(pkgn, false);
            } catch (IOException ex) {
                throw new HongsError( 0x3a , "Can not load package '" + pkgn + "'.", ex);
            }
            if (clss == null) {
                throw new HongsError( 0x3a , "Can not find package '" + pkgn + "'.");
            }
        } else {
            clss = new HashSet();
            clss.add  (  pkgn  );
        }

        return clss;
    }

    private static Class getClso(String clsn) {
        Class  clso;
        try {
            clso = Class.forName(clsn);
        } catch (ClassNotFoundException ex) {
            throw new HongsError(0x3b, "Can not find class '" + clsn + "'.", ex);
        }
        return clso;
    }

}
