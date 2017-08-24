package app.hongs.serv.manage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.dh.IAction;
import app.hongs.util.Synt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.MimetypesFileTypeMap;

/**
 * 文件管理
 * @author Hongs
 */
@Action("manage/file")
public class FileAction implements IAction {

    private static final Map<String, Byte> TYPE_SORT = new HashMap();
    static {
        TYPE_SORT.put("dir", (byte) 0);
        TYPE_SORT.put("txt", (byte) 1);
        TYPE_SORT.put("bin", (byte) 2);
    }
    private static final List ROOT_LIST = new ArrayList();
    static {
        ROOT_LIST.add(Synt.mapOf(
            "path", "/BASE",
            "name", "网站",
            "type", "dir",
            "size", "1"
        ));
        ROOT_LIST.add(Synt.mapOf(
            "path", "/CONF",
            "name", "配置",
            "type", "dir",
            "size", "1"
        ));
        ROOT_LIST.add(Synt.mapOf(
            "path", "/DATA",
            "name", "数据",
            "type", "dir",
            "size", "1"
        ));
    }

    @Override
    @Action("search")
    public void search(ActionHelper helper) throws HongsException {
        MimetypesFileTypeMap nmap = new MimetypesFileTypeMap( );
        CoreLocale lang = CoreLocale.getInstance("manage_serv");
        String type = helper.getParameter("type");
        String path = helper.getParameter("path");
        String pxth = path;
        File   file;

        // 根目录
        if ("".equals(path) || "/".equals(path)) {
            if ("file".equals(type)) {
                helper.reply(Synt.mapOf("page" , Synt.mapOf(
                        "ern" , 1
                    ),
                    "list" , new ArrayList()
                ));
            } else {
                helper.reply(Synt.mapOf("page" , Synt.mapOf(
                        "ern" , 0
                    ),
                    "list" , ROOT_LIST
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
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exist"));
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

            if ("type".equals(ob)) {
                filez = new TreeSet(new Comparator<Map>() {
                    @Override
                    public int compare(Map f1, Map f2) {
                        String t1 = (String) f1.get("type");
                        String t2 = (String) f2.get("type");
                        byte s1 = Synt.declare(TYPE_SORT.get(t1), (byte) 0);
                        byte s2 = Synt.declare(TYPE_SORT.get(t2), (byte) 0);
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

                String name = item.getName();
                String extn = name.replaceFirst("^.*/","");
                String mime = nmap.getContentType ( extn );

                Map xxxx = new HashMap();
                xxxx.put("path" , pxth + "/" + name);
                xxxx.put("name" , name );
                xxxx.put("mime" , mime );
                xxxx.put("type" , item.isDirectory() ? "dir" : (isTextFile(item) ? "txt" : "bin"));
                xxxx.put("size" , item.isDirectory() ? item.list(  ).length  :  item.length(  )  );
                xxxx.put("mtime", item.lastModified());
                filez.add(xxxx );
            }

            Map rsp = new HashMap();
            rsp.put("list", filez );
            rsp.put("page", Synt.mapOf(
                "ern", filez.size() > 0 ? 0 : 1
            ));
            helper.reply(rsp);
        } else
        if (isTextFile(file)) {
            String name = file.getName();
            String extn = name.replaceFirst("^.*/","");
            String mime = nmap.getContentType ( extn );

            Map xxxx = new HashMap();
            xxxx.put("path" , pxth );
            xxxx.put("name" , name );
            xxxx.put("mime" , mime );
            xxxx.put("type" , "txt");
            xxxx.put("size" , file.length( ));
            xxxx.put("text" , readFile(file));
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
            helper.fault(lang.translate("core.serv.manage.file.path.is.exist"));
            return;
        }
        if (isDenyFile(file)) {
            helper.fault(lang.translate("core.serv.manage.file.interdicted"  ));
            return;
        }

        // 创建目录
        if ("dir".equals(type)) {
            new File(path).mkdirs();
            helper.reply("");
            return;
        }

        // 写入文件
        try {
            saveFile(file, text);
        } catch ( Exception ex ) {
            CoreLogger.error(ex);
            helper.fault(lang.translate("core.serv.manage.file.create.failed"));
            return;
        }

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

        if ( dist != null && dist.equals ( path )) {
             dist  = null ;
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
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exist"));
            return;
        }
        if (isDenyFile(file)) {
            helper.fault(lang.translate("core.serv.manage.file.interdicted"  ));
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
            helper.fault(lang.translate("core.serv.manage.file.dist.is.exist"));
            return;
        }
        if (isDenyFile(file)) {
            helper.fault(lang.translate("core.serv.manage.file.interdicted"  ));
            return;
        }
        if (!file.renameTo(dizt)) {
            helper.fault(lang.translate("core.serv.manage.file.rename.failed"));
            return;
        }
        if ( text == null ) {
            return;
        }
        file = dizt;
        }

        // 写入文件
        try {
            saveFile(file, text);
        } catch ( Exception ex ) {
            CoreLogger.error(ex);
            helper.fault(lang.translate("core.serv.manage.file.update.failed"));
            return;
        }

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
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.exist"));
            return;
        }
        if (isDenyDire(file)) {
            helper.fault(lang.translate("core.serv.manage.file.path.is.not.empty"));
            return;
        }
        if (isDenyFile(file)) {
            helper.fault(lang.translate("core.serv.manage.file.interdicted"  ));
            return;
        }
        if (!file.delete()) {
            helper.fault(lang.translate("core.serv.manage.file.delete.failed"));
            return;
        }

        helper.reply("");
    }

    private String realPath(String path) {
        if (path == null) {
            return  null;
        }
        if (path.matches(".*/\\.{1,2}/.*")) {
            return  null;
        }
        if (path.startsWith("/BASE")) {
            path =  Core.BASE_PATH+"/"+path.substring(5);
        } else
        if (path.startsWith("/CONF")) {
            path =  Core.CONF_PATH+"/"+path.substring(5);
        } else
        if (path.startsWith("/DATA")) {
            path =  Core.DATA_PATH+"/"+path.substring(5);
        } else {
            return  null;
        }
        if (path.endsWith("/")) {
            path = path.substring(0 , path.length() - 1);
        }
        return path;
    }

    private String readFile(File file) {
        try (
            BufferedReader br = new BufferedReader(new FileReader(file));
        ) {
            StringBuilder  sb = new StringBuilder ( );
            char[]         bs;
            int            bn;
            while (true) {
                bs = new char [1024];
                if((bn = br.read(bs)) < 0) {
                    break;
                }
                sb.append(bs, 0, bn);
            }
            return sb.toString();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Can not find " + file.getAbsolutePath(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Can not read " + file.getAbsolutePath(), ex);
        }
    }

    private void saveFile(File file, String text) {
        try (
            FileWriter fw = new FileWriter(file,false);
            BufferedWriter bw = new BufferedWriter(fw);
        ) {
            bw.write(text);
        } catch (IOException ex) {
            throw new RuntimeException("Can not save " + file.getAbsolutePath(), ex);
        }
    }

    private boolean isDenyDire(File file) {
        return file.isDirectory (   )
            && file.list().length > 0;
    }

    private boolean isDenyFile(File file) {
        if (file.isDirectory()) {
            return true ;
        }

        String serv = System.getProperty("manage.serv.file");
        if ( "no".equals(serv)) {
            return true ;
        }
        if ("all".equals(serv)) {
            return false;
        }

        String name = file.getName( );
        String extn = name.replaceFirst("^.*\\.", "");
        CoreConfig  conf = CoreConfig.getInstance(  );
        Set<String> extz = Synt.asTerms(conf.getProperty("fore.upload.deny.extns" ));
        Set<String> exts = Synt.asTerms(conf.getProperty("fore.upload.allow.extns"));
        return (null != extz &&  extz.contains(extn))
            || (null != exts && !exts.contains(extn));
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
