package io.github.ihongs;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.combat.anno.Combat;
import java.io.IOException;
import java.io.InputStream;
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
    private static Map<String, Method> COMBATS = null;
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

        regHandles();
        return ACTIONS;
    }

    /**
     * 获取所有内置命令
     * @return
     */
    public static Map<String, Method> getCombats() {
        Lock rlock = RWLOCKS.readLock();
        rlock.lock();
        try {
            if (null != COMBATS) {
                return  COMBATS;
            }
        } finally {
            rlock.unlock();
        }

        regHandles();
        return COMBATS;
    }

    private static void regHandles() {
        Lock wlock = RWLOCKS.writeLock();
        wlock.lock();
        try {
            ACTIONS = new HashMap();
            COMBATS = new HashMap();
            String[] pkgs = CoreConfig
                    .getInstance("defines"   )
                    .getProperty("mount.serv")
                    .split(";");
            addHandles(ACTIONS , COMBATS , pkgs );
        } finally {
            wlock.unlock();
        }
    }

    private static void addHandles(Map<String, Mathod> acts, Map<String, Method> cmds, String... pkgs) {
        for(String pkgn : pkgs) {
            pkgn = pkgn.trim( );
            if (pkgn.length ( ) == 0) {
                continue;
            }

            Set<String> clss = getClasses(pkgn);
            for(String  clsn : clss) {
                Class   clso ;
                try {
                    clso = Class.forName (clsn);
                }
                catch (ClassNotFoundException ex) {
                    throw  new  CruxExemption(ex, 831, "Can not find class '" + clsn + "'.");
                }

                // 从注解提取动作名
                Action acto = (Action) clso.getAnnotation(Action.class);
                if (acto != null) {
                    addActions(acts, acto, clsn, clso);
                    continue;
                }

                Combat cmdo = (Combat) clso.getAnnotation(Combat.class);
                if (cmdo != null) {
                    addCombats(cmds, cmdo, clsn, clso);
                }
            }
        }
    }

    private static void addActions(Map<String, Mathod> acts, Action anno, String clsn, Class clso) {
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
                throw new CruxExemption(832, "Can not find action method '"+clsn+"."+mtdn+"(ActionHelper)'.");
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

    private static void addCombats(Map<String, Method> acts, Combat anno, String clsn, Class clso) {
        String actn = anno.value();
        if (actn == null || actn.length() == 0) {
            actn =  clsn;
        }

        Method[] mtds = clso.getMethods();
        for(Method mtdo : mtds) {
            String mtdn = mtdo.getName( );

            // 从注解提取动作名
            Combat annx = (Combat) mtdo.getAnnotation(Combat.class);
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
                throw new CruxExemption(832, "Can not find combat method '"+clsn+"."+mtdn+"(String[])'.");
            }

            if ("__main__".equals(actx)) {
                acts.put(actn /*__main__*/ , mtdo );
            } else {
                acts.put(actn + "." + actx , mtdo );
            }
        }
    }

    private static Set<String> getClasses(String pkgn) {
        Set<String> clss;

        if (pkgn.endsWith(".**")) {
            pkgn = pkgn.substring(0, pkgn.length() - 3);
            try {
                clss = getClassNames(pkgn, true );
            } catch (IOException ex) {
                throw new CruxExemption(ex, 830, "Can not load package '" + pkgn + "'.");
            }
            if (clss == null) {
                throw new CruxExemption(830, "Can not find package '" + pkgn + "'.");
            }
        } else
        if (pkgn.endsWith(".*" )) {
            pkgn = pkgn.substring(0, pkgn.length() - 2);
            try {
                clss = getClassNames(pkgn, false);
            } catch (IOException ex) {
                throw new CruxExemption(ex, 830, "Can not load package '" + pkgn + "'.");
            }
            if (clss == null) {
                throw new CruxExemption(830, "Can not find package '" + pkgn + "'.");
            }
        } else {
            clss = new HashSet();
            clss.add  (  pkgn  );
        }

        return clss;
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

        Enumeration <URL> links = Thread
                .currentThread (  /**/  )
                .getContextClassLoader( )
                .getResources  (  pack  );
        Set<String> names = new HashSet();

        while (links.hasMoreElements()) {
            URL    link = links.nextElement();
            String prot = link .getProtocol();
            String root = link .getPath  (  );

            /**
             * jar 格式 file:/xxxx/xxxx.jar!/zzzz/zzzz
             * dir 格式 /xxxx/xxxx/zzzz/zzzz
             * [1] 需要去掉后面类库包名路径
             * [2] jar 还要去掉 file: 和 !/
             */
            switch (prot) {
                case "file":
                    root = root.substring(0, root.length() - pack.length() - 0); // [1]
                    names.addAll(getClassNamesInDir(root, pack, recu));
                    break;
                case "jar" :
                    root = root.substring(5, root.length() - pack.length() - 2); // [2]
                    names.addAll(getClassNamesInJar(root, pack, recu));
                    break;
                default:
                    throw new IOException("Can not get class names in "+ link.toString());
            }
        }

        return  names;
    }

    private static Set<String> getClassNamesInJar(String root, String path, boolean recu)
            throws IOException {
        Set<String> names = new HashSet();
        try(JarFile jfile = new JarFile(root)) {
            Enumeration<JarEntry> items  =  jfile.entries();
                path = path +  "/"  ;
            int leng = path.length();

            while ( items.hasMoreElements( )) {
                String name = items.nextElement().getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                if (!name.startsWith( path )) {
                    continue;
                }
                if (!recu && name.indexOf( "/", leng ) > 0) {
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
        File[]      files = new File(root+path).listFiles();

        for (File file : files) {
            if (! file.isDirectory()) {
                String name = path +"/"+ file.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                name = name.substring(0, name.length() - 6);
                name = name.replace("/", ".");
                names.add(name);
            } else if (recu) {
                String name = path +"/"+ file.getName();
                names.addAll (
                    getClassNamesInDir ( root, name, recu )
                );
            }
        }

        return  names;
    }

    /**
     * 获取资源的输入流
     * @param name
     * @return
     */
    public static InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    /**
     * 获取资源修改时间
     * jar 内为打包时间
     * @param name
     * @return
     */
    public static long getResourceModified(String name) {
        URL link = Thread.currentThread().getContextClassLoader().getResource(name);
        if (link == null) {
            return 0;
        }
        switch (link.getProtocol()) {
            case "file":
                String  h = link.getPath();
                return new File(h).lastModified();
            case "jar" :
                String  l = link.getPath();
                int p = l.indexOf(":");
                if (p > -1) l = l.substring(1+ p); // 去掉开头的 xxxx:
                p = l.lastIndexOf("!");
                if (p > -1) l = l.substring(0, p); // 去掉结尾的 !xxxx
                return new File(l).lastModified();
            default :
                try {
                    return link.openConnection ()
                               .getLastModified();
                }
                catch (IOException e) {
                    throw new CruxExemption ( e );
                }
        }
    }

}
