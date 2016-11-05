package app.hongs.serv.manage;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.dh.IAction;
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
 * 基础配置管理
 * @author Hongs
 */
@Action("manage/file")
public class FileAction implements IAction {
    
    @Override
    @Action("retrieve")
    public void retrieve(ActionHelper helper) throws HongsException {
        String path = getRealPath(helper.getParameter("path"));
        if (path == null) {
            helper.fault("File or dir is not exists");
            return;
        }
        
        String type = helper.getParameter("type");
        byte   t = 0;
        if ( "dir".equals(type)) {
            t = 1;
        } else
        if ("file".equals(type)) {
            t = 2;
        }
        
        File file = new File(path);
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
                xxxx.put("name", item.getName());
                xxxx.put("size", item.length( ));
            }
            Map rsp = new HashMap();
            rsp.put("list", filez );
            helper.reply(rsp);
        } else
        if (isTextFile(file)) {
            Map xxxx = new HashMap();
            xxxx.put("name", file.getName());
            xxxx.put("size", file.length( ));
            xxxx.put("text", readFile(file));
        } else {
            helper.fault("Wrong file");
        }
    }

    @Override
    @Action("create")
    public void create(ActionHelper helper) throws HongsException {
        String path = getRealPath(helper.getParameter("path"));
        if (path == null) {
            helper.fault("Dir is not exists");
            return;
        }
        
        
    }

    @Override
    @Action("update")
    public void update(ActionHelper helper) throws HongsException {
        String path = getRealPath(helper.getParameter("path"));
        if (path == null) {
            helper.fault("File or dir is not exists");
            return;
        }
    }

    @Override
    @Action("delete")
    public void delete(ActionHelper helper) throws HongsException {
        String path = getRealPath(helper.getParameter("path"));
        if (path == null) {
            helper.fault("File or dir is not exists");
            return;
        }
    }
    
    private String getRealPath(String path) {
        if (path.matches(".*/\\.{1,2}/.*")) {
            return null;
        }
        if (path.startsWith("/BASE/")) {
            return Core.BASE_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/CONF/")) {
            return Core.CONF_PATH+"/"+path.substring(6);
        } else
        if (path.startsWith("/DATA/")) {
            return Core.DATA_PATH+"/"+path.substring(6);
        } else {
            return null;
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
