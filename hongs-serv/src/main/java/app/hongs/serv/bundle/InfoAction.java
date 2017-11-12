package app.hongs.serv.bundle;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理信息
 * @author Hongs
 */
@Action("bundle/info")
public class InfoAction {

    @Action("search")
    public void search(ActionHelper helper) throws HongsException {
        Map  rsp = new HashMap();
        Map  req = helper.getRequestData();
        long now = System.currentTimeMillis( );
        Set  rb  = Synt.toTerms(req.get("rb"));

        // 当前时间
        rsp.put("now_msec", now);

        // 应用信息
        if ( rb == null || rb.contains("app_info") ) {
            Map  app = new HashMap();
            rsp.put("app_info", app);
            long tim = Core.STARTS_TIME;
            app.put("server_id",Core.SERVER_ID);
            app.put("base_href",Core.BASE_HREF);
            app.put("base_path",Core.BASE_PATH);
            app.put("open_time",tim);
            app.put("live_time",Tool.humanTime(now - tim));
        }

        // 系统信息
        if ( rb == null || rb.contains("sys_info") ) {
            Map  inf = new HashMap();
            rsp.put("sys_info", inf);

            Properties pps = System.getProperties( );
            inf.put("name", pps.getProperty("os.name"));
            inf.put("arch", pps.getProperty("os.arch"));
            inf.put("vers", pps.getProperty("os.version"));
            inf.put("user", pps.getProperty("user.name" ));
            inf.put("java", pps.getProperty("java.version"));

            try {
                InetAddress hst = InetAddress.getLocalHost();
                inf.put("addr", hst.getHostAddress());
                inf.put("host", hst.getHostName());
            }  catch (UnknownHostException e) {
                // Nothing todo.
            }
        }

        // 运行信息
        if ( rb == null || rb.contains("run_info") ) {
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean mm = ManagementFactory.getMemoryMXBean();
            MemoryUsage  nm = mm.getNonHeapMemoryUsage();
            MemoryUsage  hm = mm.getHeapMemoryUsage();
            Runtime      rt = Runtime.getRuntime();

            double avg = os.getSystemLoadAverage();
            long siz = rt.totalMemory();
            long fre = rt.freeMemory();
            long max = rt.maxMemory();
            long stk = nm.getUsed( );
            long use = hm.getUsed( );

            Map  inf = new HashMap();
            rsp.put("run_info", inf);

            inf.put("load" , new Object[] {avg, String.valueOf(avg), "负载"});
            inf.put("size" , new Object[] {siz, Tool.humanSize(max), "全部"});
            inf.put("free" , new Object[] {fre, Tool.humanSize(fre), "空闲"});
            inf.put("dist" , new Object[] {max, Tool.humanSize(fre), "可用"});
            inf.put("used" , new Object[] {use, Tool.humanSize(use), "已用"});
            inf.put("uses" , new Object[] {stk, Tool.humanSize(stk), "非堆"});
        }

        // 磁盘情况
        if ( rb == null || rb.contains("dir_info") ) {
            rsp.put("base_dir", getAllSize(new File(Core.BASE_PATH)));
            rsp.put("data_dir", getAllSize(new File(Core.DATA_PATH)));
            rsp.put("conf_dir", getAllSize(new File(Core.CONF_PATH)));
            rsp.put("core_dir", getAllSize(new File(Core.CORE_PATH)));
        }

        /**
         * 公共核心情况和锁情况
         */
        if ( rb != null && rb.contains("core_info")) {
            rsp.put("core_set", Core.GLOBAL_CORE.keySet());
        }
        if ( rb != null && rb.contains("lock_info")) {
            rsp.put("lock_map", app.hongs.util.Block.counts());
        }

        helper.reply("", rsp);
    }

    @Action("note/update")
    public void noteUpdate(ActionHelper helper) {

    }

    private static Map  getAllSize(File d) {
        Map     map = new LinkedHashMap();
        long    all = 0;
        long    oth = 0;
        long    one ;
        String  hum ;

        for (File f : d.listFiles()) {
            if (f.getName().endsWith("-INF")) {
                continue;
            }
            if (f.isDirectory()) {
                one = getDirSize(f);
                if (one == 0) {
                    continue;
                }
                hum = Tool.humanSize(one);
                map.put(f.getName(), new Object[] {one, hum, "目录 "+f.getName()});
            } else {
                one = f.length();
                oth += one;
            }
            all += one;
        }

        hum = Tool.humanSize(all);
        map.put(".", new Object[] {all, hum, "当前目录"});

        hum = Tool.humanSize(oth);
        map.put("!", new Object[] {oth, hum, "其他文件"});

        long tot;

        tot = d.getTotalSpace();
        hum = Tool.humanSize(tot);
        map.put("@", new Object[] {tot, hum, "磁盘大小"});

        one = d.getFreeSpace();
        hum = Tool.humanSize(one);
        map.put("#", new Object[] {one, hum, "磁盘剩余"});

        one = tot - one;
        hum = Tool.humanSize(one);
        map.put("$", new Object[] {one, hum, "磁盘已用"});

        // 排序
        List<Map.Entry> a = new ArrayList(map.entrySet());
        Collections.sort(a, new sortBySize());
        map.clear();
        for (Map.Entry  n : a) {
            map.put(n.getKey(), n.getValue());
        }

        return map;
    }

    private static long getDirSize(File d) {
        long s  = 0 ;
        for (File f : d.listFiles()) {
            if (f.isDirectory()) {
                s += getDirSize( f );
            } else {
                s += f.length( );
            }
        }
        return s;
    }

    private static class sortBySize implements Comparator<Map.Entry> {
        @Override
        public int compare(Map.Entry s1, Map.Entry s2) {
            Object[] a1 = (Object[]) s1.getValue();
            Object[] a2 = (Object[]) s2.getValue();
            long n1 = (Long) a1[0];
            long n2 = (Long) a2[0];
            return n1 < n2 ? 1 : -1;
        }
    }
}
