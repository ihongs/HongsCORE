package io.github.ihongs.util.reflex;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 通过包名获取类名集合
 * <p>
 * 此类被用于处理 defines.properties 中 mount.serv 下 .* 后缀的包下的所有类名
 * </p>
 * @author Hongs
 */
public final class Classes {

    /**
     * 通过包名获取类名集合
     * @param pkgn 包名
     * @param recu 递归
     * @return
     * @throws IOException
     */
    public static Set<String> getClassNames(String pkgn, boolean recu) throws IOException {
        ClassLoader      pload = Thread.currentThread().getContextClassLoader();
        String           ppath = pkgn.replace( "." , "/" );
        Enumeration<URL> links = pload.getResources(ppath);
        Set<String>      names = new  HashSet();
//      boolean          gotit = false;

        while ( links.hasMoreElements(  )  ) {
            URL plink = links.nextElement( );

            String  proto = plink.getProtocol();
            String  proot = plink.getPath( ).replaceFirst( "/$" , "")  // 去掉结尾的 /
                                            .replaceFirst("^.+:", ""); // 去掉开头的 file:
            proot = proot.substring(0, proot.length()-ppath.length()); // 去掉目标包的路径

            if ( "jar".equals(proto)) {
                // 路径类似 file:/xxx/xxx.jar!/zzz/zzz
                // 上面已删 zzz/zzz 还需删掉 !/
                proot = proot.substring(0 , proot.lastIndexOf( "!" ));
                names.addAll(getClassNamesByJar( proot, ppath, recu));
            } else
            if ("file".equals(proto)){
                // 路径类似 /xxx/xxx/ 有后缀 /
                names.addAll(getClassNamesByDir( proot, ppath, recu));
            }

//          gotit = true;
        }

        // 上面找不到就找不到了, 没必要再用 URLClassLoader
        /*
        if (gotit) {
            URL[] paurl = ((URLClassLoader) pload).getURLs();

            if (  paurl != null  ) for ( URL pourl : paurl ) {
                String proot = pourl.getPath( );

                if (proot.endsWith(".jar")) {
                    names.addAll(getClassNamesByJar( proot, ppath, recu));
                } else
                if (proot.endsWith(  "/" )) {
                    names.addAll(getClassNamesByDir( proot, ppath, recu));
                }
            }
        }
        */

        return  names;
    }

    private static Set<String> getClassNamesByDir(String root, String path, boolean recu) {
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
                names.addAll(getClassNamesByDir(root, name, recu));
            }
        }

        return  names;
    }

    private static Set<String> getClassNamesByJar(String root, String path, boolean recu)
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
                name = name.substring(0, name.length() - 6);
                if (!recu && name.indexOf("/", pathl ) > 0) {
                    continue;
                }
                name = name.replace("/", ".");
                names.add(name);
            }
        }

        return  names;
    }

    /**
     * 测试
     * @param args 包名1 包名2... [-r(包含下级)]
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        URL[] paurl = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        for (URL pourl : paurl) {
            System.out.println(pourl.getPath());
        }

        boolean wcp  =  false;
        Set<String> pkgs = new HashSet();
        for (String pkg  :  args) {
            if ("-r".equals(pkg)) {
                wcp  =  true ;
            } else {
                pkgs.add(pkg);
            }
        }
        for (String pkg  :  pkgs) {
            Set<String> clss = getClassNames(pkg, wcp);
            for(String  cls  : clss) {
                System.out.println(cls);
            }
        }
    }

}
