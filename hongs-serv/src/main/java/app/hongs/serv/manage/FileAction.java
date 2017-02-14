package app.hongs.serv.manage;

import app.hongs.Core;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.dh.IAction;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件管理
 * @author Hongs
 */
@Action("manage/file")
public class FileAction implements IAction {

    @Override
    @Action("retrieve")
    public void retrieve(ActionHelper helper) throws HongsException {
        CoreLocale lang = CoreLocale.getInstance("manage_serv");
        String type = helper.getParameter("type");
        String pxth = helper.getParameter("path");
        String path = pxth;
        File   file;

        // 根目录
        if ("".equals(path) || "/".equals(path)) {
            if ("file".equals(type)) {
                helper.reply(Synt.asMap (
                    "page" , Synt.asMap (
                        "ern", 1
                    ),
                    "list" , Synt.asList(
                    )
                ));
            } else {
                helper.reply(Synt.asMap (
                    "page" , Synt.asMap (
                        "ern", 0
                    ),
                    "list" , Synt.asList(
                        Synt.asMap(
                            "path", "/BASE/",
                            "name", "网站",
                            "size", "1"
                        ),
                        Synt.asMap(
                            "path", "/CONF/",
                            "name", "配置",
                            "size", "1"
                        ),
                        Synt.asMap(
                            "path", "/DATA/",
                            "name", "数据",
                            "size", "1"
                        )
                    )
                ));
            }
            return;
        }

        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.required"));
            return;
        }
        path = realPath(path);
        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.error"));
            return;
        }
        file = new File(path);
        if (!file.exists()) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exists"));
            return;
        }

        byte t = 0;
        if ( "dir".equals(type)) {
             t = 1;
        } else
        if ("file".equals(type)) {
             t = 2;
        }

        if ( file.isDirectory()) {
            File[ ]  files = file.listFiles();
            Set<Map> filez;

            // 排序规则, 默认按名称排序
            final boolean ds;
            String ob = helper.getParameter("ob");
            if (ob != null && ob.startsWith("-")) {
                ob  = ob.substring(1);
                ds  = true  ;
            } else {
                ds  = false ;
            }
            if ("size".equals(ob)) {
                filez = new TreeSet(new Comparator<Map>() {
                    @Override
                    public int compare(Map f1, Map f2) {
                        long s1 = Synt.declare(f1.get("size") , 0L);
                        long s2 = Synt.declare(f2.get("size") , 0L);
                        if (s1 != s2)
                        if (ds)
                            return s1 < s2 ? 1 : -1;
                        else
                            return s1 > s2 ? 1 : -1;

                        String n1 = (String) f1.get("name");
                        String n2 = (String) f2.get("name");
                        if (ds)
                            return n2.compareTo(n1);
                        else
                            return n1.compareTo(n2);
                    };
                });
            } else
            if ("type".equals(ob)) {
                filez = new TreeSet(new Comparator<Map>() {
                    @Override
                    public int compare(Map f1, Map f2) {
                        String t1 = (String) f1.get("type");
                        String t2 = (String) f2.get("type");
                        if ( ! t1.equals(t2))
                        if (ds)
                            return t2.compareTo(t1);
                        else
                            return t1.compareTo(t2);

                        String n1 = (String) f1.get("name");
                        String n2 = (String) f2.get("name");
                        if (ds)
                            return n2.compareTo(n1);
                        else
                            return n1.compareTo(n2);
                    };
                });
            } else {
                filez = new TreeSet(new Comparator<Map>() {
                    @Override
                    public int compare(Map f1, Map f2) {
                        String n1 = (String) f1.get("name");
                        String n2 = (String) f2.get("name");
                        if (ds)
                            return n2.compareTo(n1);
                        else
                            return n1.compareTo(n2);
                    };
                });
            }

            for (File item  : files) {
                if (t == 1) {
                    if ( ! item.isDirectory()) {
                        continue;
                    }
                } else
                if (t == 2) {
                    if ( ! item.isFile()) {
                        continue;
                    }
                }
                Map xxxx = new HashMap();
                xxxx.put("path" , pxth + "/" + item.getName());
                xxxx.put("name" , item.getName());
                xxxx.put("size" , item.isDirectory() ? item.list().length : item.length());
                xxxx.put("type" , item.isDirectory() ? "dir" : (isTextFile(item) ? "txt" : "bin"));
                xxxx.put("mtime", item.lastModified());
                filez.add(xxxx );
            }

            Map rsp = new HashMap();
            rsp.put("list", filez );
            rsp.put("page", Synt.asMap(
                "ern", filez.size() > 0 ? 0 : 1
            ));
            helper.reply(rsp);
        } else
        if (isTextFile(file)) {
            Map xxxx = new HashMap();
            xxxx.put("path" , pxth + "/" + file.getName());
            xxxx.put("name" , file.getName());
            xxxx.put("size" , file.length ());
            xxxx.put("text" , readFile(file));
            xxxx.put("ftype", 1);
            xxxx.put("mtime", file.lastModified());
            helper.reply( "", xxxx );
        } else {
            helper.fault(lang.translate("core.serv.manage.file.unsupported"));
        }
    }

    @Override
    @Action("create")
    public void create(ActionHelper helper) throws HongsException {
        CoreLocale lang = CoreLocale.getInstance("manage_serv");
        String path = helper.getParameter("path");
        String type = helper.getParameter("type");
        String text = helper.getParameter("text");
        File   file;

        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.required"));
            return;
        }
        path = realPath(path);
        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.error"));
            return;
        }
        file = new File(path);
        if ( file.exists()) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.exists"));
            return;
        }

        // 创建目录
        if ("dir".equals(type)) {
            new File(path).mkdirs();
            helper.reply("");
            return;
        }

        Tool.storeFile(path, text, false);
        helper.reply("");
    }

    @Override
    @Action("update")
    public void update(ActionHelper helper) throws HongsException {
        CoreLocale lang = CoreLocale.getInstance("manage_serv");
        String path = helper.getParameter("path");
        String dist = helper.getParameter("dist");
        String text = helper.getParameter("text");
        File   file;
        File   dizt;

        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.required"));
            return;
        }
        path = realPath(path);
        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.error"));
            return;
        }
        file = new File(path);
        if (!file.exists()) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exists"));
            return;
        }

        // 改名移动
        if ( dist != null ) {
            dist = realPath(dist);
            if ( dist == null ) {
                helper.fault(lang.translate("core.serv.manage.file.path.is.error"));
                return;
            }
            dizt = new File(dist);
            if ( dizt.exists()) {
                helper.fault(lang.translate("core.serv.manage.file.dist.is.exists"));
                return;
            }
            if (!file.renameTo(dizt)) {
                helper.fault(lang.translate("core.serv.manage.file.rename.failed"));
            } else {
                helper.reply("");
            }
            return;
        }

        Tool.storeFile(path, text, false);
        helper.reply("");
    }

    @Override
    @Action("delete")
    public void delete(ActionHelper helper) throws HongsException {
        CoreLocale lang = CoreLocale.getInstance("manage_serv");
        String path = helper.getParameter("path");
        File   file;

        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.required"));
            return;
        }
        path = realPath(path);
        if ( path == null ) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.error"));
            return;
        }
        file = new File(path);
        if (!file.exists()) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exists"));
            return;
        }
        if (file.isDirectory() && file.list().length > 0) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.blanks"));
            return;
        }

        if (!file.delete()) {
            helper.fault(lang.translate("core.serv.manage.file.delete.failed"));
        } else {
            helper.reply("");
        }
    }

    private String realPath(String path) {
        if (path == null) {
            return  null;
        }
        if (path.matches(".*/\\.{1,2}/.*")) {
            return  null;
        }
        if (path.startsWith("/BASE/")) {
            path =  Core.BASE_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/CONF/")) {
            path =  Core.CONF_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/DATA/")) {
            path =  Core.DATA_PATH+"/"+path.substring(6);
        } else {
            return  null;
        }
        if (path.endsWith("/")) {
            path = path.substring(0 , path.length() - 1);
        }
        return path;
    }

    private File[] sortByName(File[] files) {
        TreeSet<File> ts = new TreeSet(new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        ts.addAll(Arrays.asList(files));
        return ts.toArray(new File[]{});
    }

    private File[] sortBySize(File[] files) {
        TreeSet<File> ts = new TreeSet(new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        ts.addAll(Arrays.asList(files));
        return ts.toArray(new File[]{});
    }

    private File[] sortByTime(File[] files) {
        TreeSet<File> ts = new TreeSet(new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        ts.addAll(Arrays.asList(files));
        return ts.toArray(new File[]{});
    }

    private String readFile(File file) {
      BufferedReader br = null;
      try {
          br = new BufferedReader(new FileReader(file));
          StringBuilder sb = new StringBuilder();
          char[ ]       bs ;
          while ( true ) {
              bs = new char[ 1024 ];
              if( -1 == br.read(bs)) {
                  break;
              }
              sb.append(bs);
          }
          return sb.toString();
      } catch (FileNotFoundException ex) {
          throw new RuntimeException("Can not find " + file.getAbsolutePath(), ex);
      } catch (IOException ex) {
          throw new RuntimeException("Can not read " + file.getAbsolutePath(), ex);
      } finally {
      if (br != null) {
      try {
          br.close( );
      } catch (IOException ex) {
          throw new RuntimeException("Can not close "+ file.getAbsolutePath(), ex);
      }
      }
      }
    }

    private boolean isTextFile(File file) {
        boolean isBinary = false;
        try {
            FileInputStream fin = new FileInputStream(file);
            long len = file.length();
            for (int j = 0; j < (int) len; j++) {
                int t = fin.read();
                if (t < 32 && t != 9 && t != 10 && t != 13) {
                    isBinary = true;
                    break;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Can not close "+ file.getAbsolutePath(), ex);
        }
        return !isBinary;
    }

}
