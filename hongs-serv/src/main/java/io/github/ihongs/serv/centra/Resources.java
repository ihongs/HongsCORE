package io.github.ihongs.serv.centra;

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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 资源释放
 * @author Hongs
 */
@Combat("resources")
public class Resources {

    @Combat("list")
    public static void list(String[] args)
           throws IOException {
        if (args.length <= 0) {
        CombatHelper.println(
                "Usage: resources.list " +
                "resource/path1 resource/path2/* resource/path3/**");
            return;
        }

        for (int i = 0; i < args.length; i ++) {
            String path = args[i];

            if (path.endsWith("/**")) {
                saveResources(null, path.substring(0, path.length() - 3), true );
            } else
            if (path.endsWith("/*" )) {
                saveResources(null, path.substring(0, path.length() - 2), false);
            } else
            {
                saveResource (null, path);
            }
        }
    }

    @Combat("save")
    public static void save(String[] args)
           throws IOException {
        if (args.length <= 1) {
            CombatHelper.println(
                "Usage: resources.save save/path " +
                "resource/path1 resource/path2/* resource/path3/**");
            return;
        }

            String dist = args[0];
        for (int i = 1; i < args.length; i ++) {
            String path = args[i];

            if (path.endsWith("/**")) {
                saveResources(dist, path.substring(0, path.length() - 3), true );
            } else
            if (path.endsWith("/*" )) {
                saveResources(dist, path.substring(0, path.length() - 2), false);
            } else
            {
                saveResource (dist, path);
            }
        }
    }

    private static void saveResource (String dist, String path)
            throws IOException {
        try (
            InputStream inps = Thread.currentThread()
                           .getContextClassLoader(  )
                           .getResourceAsStream(path)
        ) {
            if (inps == null) {
                throw new IOException("Can not get resrouce for "+path);
            }
            if (dist != null) {
                Files.copy(inps , new File(dist + "/" + path).toPath());
            }
            CombatHelper.paintln(path);
        }
    }

    private static void saveResources(String dist, String path, boolean recu)
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
                    saveResourcesInJar(dist, root, path, recu);
                    break;
                case "file":
                    root = root.substring(0, root.length() - path.length() - 0); // [1]
                    saveResourcesInDir(dist, root, path, recu);
                    break;
                default:
                    throw new IOException("Can not get resrouces in "+ link.toString());
            }

        }
    }

    private static void saveResourcesInJar(String dist, String root, String path, boolean recu)
            throws IOException {
        try(JarFile jfile = new JarFile(root)) {
            Enumeration<JarEntry> items = jfile.entries();
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
                if (!recu && name.indexOf("/", leng) > 0) {
                    continue;
                }

                if (dist != null) {
                    File disf = new File(dist +"/"+ name);
                    File dirf = disf.getParentFile();
                    if (!dirf.exists()) {
                         dirf.mkdirs();
                    }

                    Path disp = disf.toPath();
                    InputStream fils = jfile.getInputStream(item);
                    Files.copy( fils , disp , StandardCopyOption.REPLACE_EXISTING );
                }

                CombatHelper.paintln ( name );
            }
        }
    }

    private static void saveResourcesInDir(String dist, String root, String path, boolean recu)
            throws IOException {
        File[] files = new File (root + path).listFiles( );
        for (File file : files) {
            if (! file.isDirectory()) {
                String name = path + "/" + file.getName( );
                if (name.endsWith(".class")) {
                    continue;
                }

                if (dist != null) {
                    File disf = new File(dist +"/"+  name);
                    File dirf = disf.getParentFile();
                    if (!dirf.exists()) {
                         dirf.mkdirs();
                    }

                    Path disp = disf.toPath();
                    Path filp = file.toPath();
                    Files.copy( filp , disp , StandardCopyOption.REPLACE_EXISTING );
                }

                CombatHelper.paintln ( name );
            } else if (recu) {
                String name = path + "/" + file.getName( );
                saveResourcesInDir(dist, root, name, recu);
            }
        }
    }

}
