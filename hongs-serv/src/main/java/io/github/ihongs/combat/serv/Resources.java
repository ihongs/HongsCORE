package io.github.ihongs.combat.serv;

import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 资源释放
 * @author Hongs
 */
@Combat("resources")
public class Resources {

    @Combat("save")
    public static void save(String[] args)
           throws IOException {
        if (args.length <= 1 || args[0].isEmpty()) {
            CombatHelper.println(
                "Usage: resources.save save/path " +
                "resource/path1 resource/path2/* resource/path3/**");
            return;
        }

        saveResources(args[0], args, 1);
    }

    @Combat("list")
    public static void list(String[] args)
           throws IOException {
        if (args.length <= 0) {
        CombatHelper.println(
                "Usage: resources.list " +
                "resource/path1 resource/path2/* resource/path3/**");
            return;
        }

        saveResources(null, args, 0);
    }

    @Combat("find")
    public static void find(String[] args)
           throws IOException {
        if (args.length <= 0) {
        CombatHelper.println(
                "Usage: resources.find " +
                "resource/path1 resource/path2/* resource/path3/**");
            return;
        }

        saveResources( "" , args, 0);
    }

    private static void saveResources(String dist, String[] args, int b)
            throws IOException {
        Set ress = new HashSet();
        for (int i = b; i < args.length; i ++) {
            String path = args[i];

            if (path.endsWith("/**")) {
                saveResources(ress, dist, path.substring(0, path.length() - 3), true );
            } else
            if (path.endsWith("/*" )) {
                saveResources(ress, dist, path.substring(0, path.length() - 2), false);
            } else
            {
                saveResource (ress, dist, path);
            }
        }
    }

    private static void saveResources(Set ress, String dist, String path, boolean recu)
            throws IOException {
        Enumeration <URL> links = Thread
                .currentThread (  /**/  )
                .getContextClassLoader( )
                .getResources  (  path  );

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
                case "jar" :
                    root = root.substring(5, root.length() - path.length() - 2); // [2]
                    saveResourcesInJar(ress, dist, root, path, recu);
                    break;
                case "file":
                    root = root.substring(0, root.length() - path.length() - 0); // [1]
                    saveResourcesInDir(ress, dist, root, path, recu);
                    break;
                default:
                    throw new IOException("Can not get resrouces in "+ link.toString());
            }
        }
    }

    private static void saveResourcesInJar(Set ress, String dist, String root, String path, boolean recu)
            throws IOException {
        try(JarFile file = new JarFile(root)) {
            Enumeration<JarEntry> items = file.entries();
                path = path +  "/"  ;
            int leng = path.length();

            while ( items.hasMoreElements( )) {
                JarEntry item = items.nextElement();
                String   name = item . getName(   );
                if ( name.endsWith(   "/"   )
                ||   name.endsWith(".class")) {
                    continue;
                }
                if (!name.startsWith( path )) {
                    continue;
                }
                if (!recu && name.indexOf("/",leng) > 0) {
                    continue;
                }

                if (dist == null) {
                    CombatHelper.paintln(name);
                } else
                if (dist.isEmpty()) {
                    name = name +" in "+ root ;
                    CombatHelper.paintln(name);
                } else
                {
                    saveFile(ress, dist, name, file, item);
                    CombatHelper.paintln(name);
                }
            }
        }
    }

    private static void saveResourcesInDir(Set ress, String dist, String root, String path, boolean recu)
            throws IOException {
        File[] files = new File (root + path).listFiles();
        for (File file : files) {
            if (! file.isDirectory()) {
                String name = path + "/" + file.getName();
                if (name.endsWith(".class")) {
                    continue;
                }

                if (dist == null) {
                    CombatHelper.paintln(name);
                } else
                if (dist.isEmpty()) {
                    name = name +" in "+ root ;
                    CombatHelper.paintln(name);
                } else
                {
                    saveFile(ress, dist, name, file);
                    CombatHelper.paintln(name);
                }
            } else if (recu) {
                String name = path + "/" + file.getName( );
                saveResourcesInDir(ress, dist, root, name, recu);
            }
        }
    }

    private static void saveResource(Set ress, String dist, String path)
            throws IOException {
        Enumeration <URL> links = Thread
                .currentThread (  /**/  )
                .getContextClassLoader( )
                .getResources  (  path  );

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
                case "jar" :
                    root = root.substring(5, root.length() - path.length() - 2); // [2]
                    saveResourceInJar (ress, dist, root, path);
                    break;
                case "file":
                    root = root.substring(0, root.length() - path.length() - 0); // [1]
                    saveResourceInDir (ress, dist, root, path);
                    break;
                default:
                    throw new IOException("Can not get resrouce in " + link.toString());
            }
        }
    }

    private static void saveResourceInJar(Set ress, String dist, String root, String name)
            throws IOException {
        try(JarFile  file = new JarFile(root)) {
            JarEntry item = file.getJarEntry(name);
            if (item.isDirectory()) {
                throw new IOException("Can not save resrouce for dir "+ name);
            }

            if (dist == null) {
                CombatHelper.paintln(name);
            } else
            if (dist.isEmpty()) {
                name = name +" in "+ root ;
                CombatHelper.paintln(name);
            } else
            {
                saveFile(ress, dist, name, file, item);
                CombatHelper.paintln(name);
            }
        }
    }

    private static void saveResourceInDir(Set ress, String dist, String root, String name)
            throws IOException {
        File file = new File (root + name);
        if ( file.isDirectory() ) {
            throw new IOException("Can not save resrouce for dir "+ name);
        }

        if (dist == null) {
            CombatHelper.paintln(name);
        } else
        if (dist.isEmpty()) {
            name = name +" in "+ root ;
            CombatHelper.paintln(name);
        } else
        {
            saveFile(ress, dist, name, file);
            CombatHelper.paintln(name);
        }
    }

    private static void saveFile(Set ress, String dist, String name, File file)
            throws IOException {
        if (ress.contains(name) == false) {
            ress.add(name);
        } else {
            return;
        }

        File disf = new File (dist +"/"+ name);
        File dirf = disf.getParentFile();
        Path disp = disf.toPath();
        Path filp = file.toPath();
        if (!dirf.exists()) {
             dirf.mkdirs();
        }

        if (Files.isSameFile(disp, filp) == false) {
            Files.copy(filp, disp, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void saveFile(Set ress, String dist, String name, JarFile file, JarEntry item)
            throws IOException {
        if (ress.contains(name) == false) {
            ress.add(name);
        } else {
            return;
        }

        File disf = new File(dist +"/"+ name);
        File dirf = disf.getParentFile();
        Path disp = disf.toPath();
        if (!dirf.exists()) {
             dirf.mkdirs();
        }

        try (
            InputStream inps = file.getInputStream(item);
        ) {
            Files.copy(inps, disp, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
