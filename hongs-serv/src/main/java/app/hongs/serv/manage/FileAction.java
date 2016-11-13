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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
        if ("".equals(path)) {
            helper.reply(Synt.asMap(
                "list", Synt.asList(
                    Synt.asMap(
                        "path", "/CONF",
                        "name", "配置",
                        "size", "1"
                    ),
                    Synt.asMap(
                        "path", "/DATA",
                        "name", "数据",
                        "size", "1"
                    ),
                    Synt.asMap(
                        "path", "/BASE",
                        "name", "网站",
                        "size", "1"
                    )
                )
            ));
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
            File[] files = sortByName(file.listFiles());
            List<Map> filez = new ArrayList();
            for (File item  : files) {
                if (t == 1) {
                    if (!item.isDirectory()) {
                        continue;
                    }
                } else
                if (t == 2) {
                    if (!item.isFile()) {
                        continue;
                    }
                }
                Map xxxx = new HashMap();
                xxxx.put("path" , pxth + "/" + item.getName());
                xxxx.put("name" , item.getName());
                xxxx.put("size" , item.isDirectory() ? item.list().length : item.length());
                xxxx.put("ftype", item.isDirectory() ? 0 : ( isTextFile( item ) ? 1 : 2 ));
                xxxx.put("mtime", item.lastModified());
            }
            Map rsp = new HashMap();
            rsp.put("list", filez );
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
            return  Core.BASE_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/CONF/")) {
            return  Core.CONF_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/DATA/")) {
            return  Core.DATA_PATH+"/"+path.substring(6);
        } else {
            return  null;
        }
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
