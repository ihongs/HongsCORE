package io.github.ihongs.cmdlet.serv;

import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.CmdletRunner;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.CoreRoster.Mathod;
import io.github.ihongs.HongsExemption;
import java.io.File;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;

/**
 * 常规命令
 * @author Hongs
 */
@Cmdlet("common")
public class Common {

    @Cmdlet("drop-dir")
    public static void dropDir(String[] args) {
        new Dirs(args[0]).rmdirs();
    }

    @Cmdlet("make-dir")
    public static void makeDir(String[] args) {
        new Dirs(args[0]).mkdirs();
    }

    @Cmdlet("make-uid")
    public static void makeUID(String[] args) {
        String sid =  args.length > 0 ? args[0] : Core.SERVER_ID;
        String uid =  Core.newIdentity( sid );
        CmdletHelper.OUT.get().println( uid );
    }

    @Cmdlet("show-env")
    public static void showENV(String[] args) {
        Map<String, String> a = new TreeMap(new PropComparator());
        Map<String, String> m = new HashMap(    System.getenv ());
        int i = 0, j;

        for (Map.Entry<String, String> et : m.entrySet()) {
            String k = et.getKey(  );
            String v = et.getValue();
            a.put(k, v);
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        PrintStream out = CmdletHelper.OUT.get ();

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
                s.append(n.getValue());
            out.println ( s  );
        }
    }

    @Cmdlet("show-properties")
    public static void showProperties(String[] args) {
        Map<String, String> a = new TreeMap( new  PropComparator());
        Map<String, String> m = new HashMap(System.getProperties());
        int i = 0, j;

        for (Map.Entry<String, String> et : m.entrySet()) {
            String k = et.getKey(  );
            String v = et.getValue();
            a.put(k, v);
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        PrintStream out = CmdletHelper.OUT.get ();

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
                s.append(n.getValue());
            out.println ( s  );
        }
    }

    @Cmdlet("show-cmdlets")
    public static void showCmdlets(String[] args) {
        Map<String, String> a = new TreeMap(new PathComparator('.'));
        int i = 0, j;

        for (Map.Entry<String, Method> et : CmdletRunner.getCmdlets().entrySet()) {
            String k = et.getKey(  );
            Method v = et.getValue();
            a.put( k, v.getDeclaringClass().getName()+"."+v.getName() );
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        PrintStream out = CmdletHelper.OUT.get ();

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
                s.append(n.getValue());
            out.println ( s  );
        }
    }

    @Cmdlet("show-actions")
    public static void showActions(String[] args) {
        Map<String, String> a = new TreeMap(new PathComparator('/'));
        int i = 0, j;

        for (Map.Entry<String, Mathod> et : ActionRunner.getActions().entrySet()) {
            String k = et.getKey(  );
            Mathod v = et.getValue();
            a.put( k, v.toString() );
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        PrintStream out = CmdletHelper.OUT.get ();

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
                s.append(n.getValue());
            out.println ( s  );
        }
    }

    @Cmdlet("view-serial")
    public static void viewSerial(String[] args) throws HongsException {
        if (args.length == 0) {
          CmdletHelper.paintln(
                "Usage: common.view-serial serial/file/path\r\n\t"
              + "Just for CoreSerial or Collection object.");
          return;
        }

        File fio = new File(args[0]);
        try (
            FileInputStream fis = new   FileInputStream(fio);
          ObjectInputStream ois = new ObjectInputStream(fis);
        ) {
          CmdletHelper.preview(ois.readObject());
        }
        catch (ClassNotFoundException e)
        {
          throw new HongsException(0x10d8, e);
        }
        catch ( FileNotFoundException e)
        {
          throw new HongsException(0x10d6, e);
        }
        catch (           IOException e)
        {
          throw new HongsException(0x10d4, e);
        }
        catch (HongsExemption e) //0x1051
        {
          throw e.toException( );
        }
    }

    private static class Dirs {
        private final File dir;
        public Dirs(String dir) {
            this.dir = new File(dir);
        }
        public boolean mkdirs() {
            return this.dir.mkdirs();
        }
        public boolean rmdirs() {
            if (dir.exists ( )) {
                delete(dir);
                return true;
            }
            return false;
        }
        public void delete(File dir) {
            if (dir.isDirectory ( )) {
            for(File   one : dir.listFiles()) {
                delete(one);
            }}
            dir.delete(   );
        }
    }

    private static class PropComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo ( s2 );
        }
    }

    private static class PathComparator implements Comparator<String> {
        final char SP;
        public PathComparator(char sp) {
            SP  =  sp;
        }
        @Override
        public int compare(String s1, String s2) {
            int p1 = 0;
            int p2 = 0;
            while (true) {
                p1 = s1.indexOf(SP, p1);
                p2 = s2.indexOf(SP, p2);
                if (p1 == p2) {
                    if (p1 == -1) {
                        return s1.compareTo( s2 );
                    }
                  String s3 =  s1.substring(0,p1);
                  String s4 =  s2.substring(0,p2);
                    int cp  =  s3.compareTo( s4 );
                    if (cp !=  0) {
                        return cp;
                    }
                }
                if (p1 == -1) {
                   return -1;
                }
                if (p2 == -1) {
                   return  1;
                }
                p1 ++;
                p2 ++;
            }
        }
    }

}
