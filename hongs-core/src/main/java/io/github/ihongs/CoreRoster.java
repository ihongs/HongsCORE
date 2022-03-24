package io.github.ihongs;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    /**
     * 获取所有内置动作
     * @return
     */
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

    /**
     * 获取所有内置命令
     * @return
     */
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

    /**
     * 通过包名获取类名集合
     * @param pack 包名
     * @param recu 递归
     * @return
     * @throws IOException
     */
    public static Set<String> getClassNames(String pack, boolean recu)
           throws IOException {
        pack = pack.replace(".", "/");

        Enumeration<URL> links = Thread
                .currentThread (  /**/  )
                .getContextClassLoader( )
                .getResources  (  pack  );
        Set<String> names = new HashSet();

        while (links.hasMoreElements()) {
            URL    link = links.nextElement();
            String prot = link .getProtocol();
            String root = link .getPath().replaceFirst( "/$" , "")  // 去掉结尾的 /
                                         .replaceFirst("^.+:", ""); // 去掉开头的 file:
            root = root.substring(0 , root.length()-pack.length()); // 去掉目标包的路径

            if ( "jar".equals(prot)) {
                // 路径类似 file:/xxx/xxx.jar!/zzz/zzz
                root = root.substring(0 , root.lastIndexOf( "!" ));
                names.addAll(getClassNamesInJar(root, pack, recu));
            } else
            if ("file".equals(prot)){
                // 路径类似 /xxx/xxx/
                names.addAll(getClassNamesInDir(root, pack, recu));
            }
        }

        return  names;
    }

    private static Set<String> getClassNamesInJar(String root, String path, boolean recu)
            throws IOException {
        Set<String> names = new HashSet();
        int         pathl = 1 + path.length();
        try(JarFile filej = new JarFile(root)) {
            Enumeration<JarEntry> items  =  filej.entries();

            while ( items.hasMoreElements( )) {
                String name = items.nextElement().getName();
                if (!name.startsWith( path )) {
                    continue;
                }
                if (!name.endsWith(".class")) {
                    continue;
                }
                if (!recu && name.indexOf("/", pathl ) > 0) {
                    continue;
                }
                name = name.substring(0, name.length() - 6);
                name = name.replace("/", ".");
                names.add(name);
            }
        }

        return  names;
    }

    private static Set<String> getClassNamesInDir(String root, String path, boolean recu) {
        Set<String> names = new HashSet();
        File[]      files = new File(root + path).listFiles();

        for (File file : files) {
            if (! file.isDirectory()) {
                String name = file.getPath().substring(root.length());
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.lastIndexOf( '.' ));
                    name = name.replace(File.separator, "." );
                    names.add(name);
                }
            } else if (recu) {
                String name = path + File.separator + file.getName( );
                names.addAll( getClassNamesInDir (root, name, recu) );
            }
        }

        return  names;
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
            if (prms == null || prms.length != 1 || !ActionHelper.class.isAssignableFrom(prms[0])) {
                throw new HongsExemption(832, "Can not find action method '"+clsn+"."+mtdn+"(ActionHelper)'.");
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
            if (prms == null || prms.length != 1 || !String[].class.isAssignableFrom(prms[0])) {
                throw new HongsExemption(832, "Can not find cmdlet method '"+clsn+"."+mtdn+"(String[])'.");
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
                clss = getClassNames(pkgn, true );
            } catch (IOException ex) {
                throw new HongsExemption(830, "Can not load package '" + pkgn + "'.", ex);
            }
            if (clss == null) {
                throw new HongsExemption(830, "Can not find package '" + pkgn + "'.");
            }
        } else
        if (pkgn.endsWith(".*" )) {
            pkgn = pkgn.substring(0, pkgn.length() - 2);
            try {
                clss = getClassNames(pkgn, false);
            } catch (IOException ex) {
                throw new HongsExemption(830, "Can not load package '" + pkgn + "'.", ex);
            }
            if (clss == null) {
                throw new HongsExemption(830, "Can not find package '" + pkgn + "'.");
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
            throw new HongsExemption(831, "Can not find class '" + clsn + "'.", ex);
        }
        return clso;
    }

}
