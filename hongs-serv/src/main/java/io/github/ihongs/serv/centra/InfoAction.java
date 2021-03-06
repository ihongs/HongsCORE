package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Gate;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
/*
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
*/
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * 管理信息
 * @author Hongs
 */
@Action("centra/info")
public class InfoAction {

    @Action("search")
    public void search(ActionHelper helper) throws HongsException {
        Map  rsp = new HashMap();
        Map  req = helper.getRequestData();
        long now = System.currentTimeMillis();
        Set  rb = Synt.toTerms(req.get(Cnst.RB_KEY));

        // 当前时间
        rsp.put("now_msec", now);

        // 应用信息
        if ( rb == null || rb.contains("app_info") ) {
            Map  app = new HashMap();
            rsp.put("app_info", app);

            app.put("server_id"  , Core.SERVER_ID  );
            app.put("base_href"  , Core.SERV_HREF
                                 + Core.SERV_PATH  );
            app.put("core_path"  , Core.CORE_PATH  );
            app.put("starts_time", Core.STARTS_TIME);
        }

        // 磁盘情况
        if ( rb == null || rb.contains("dir_info") ) {
            Map  inf = new HashMap();
            rsp.put("dir_info", inf);

            inf.put("base_dir", getAllSize(new File(Core.BASE_PATH)));
            inf.put("data_dir", getAllSize(new File(Core.DATA_PATH)));
            inf.put("conf_dir", getAllSize(new File(Core.CONF_PATH)));
            inf.put("core_dir", getAllSize(new File(Core.CORE_PATH)));
        }

        // 系统信息
        if ( rb == null || rb.contains("sys_info") ) {
            Map  inf = new HashMap();
            rsp.put("sys_info", inf);

            Properties pps = System.getProperties( );
            inf.put("name", pps.getProperty("os.name"   )
                      +" "+ pps.getProperty("os.version"));
            inf.put("java", pps.getProperty("java.specification.name"   )
                      +" "+ pps.getProperty("java.specification.version"));
            inf.put("user", pps.getProperty("user.name" ));
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

            inf.put("load", new Object[] {avg, String.valueOf(avg), "负载"});
            inf.put("size", new Object[] {siz, Syno.humanSize(siz), "全部"});
            inf.put("free", new Object[] {fre, Syno.humanSize(fre), "空闲"});
            inf.put("dist", new Object[] {max, Syno.humanSize(max), "可用"});
            inf.put("used", new Object[] {use, Syno.humanSize(use), "已用"});
            inf.put("uses", new Object[] {stk, Syno.humanSize(stk), "非堆"});
        }

        /**
         * 公共核心情况和锁情况
         */
        if ( rb != null && rb.contains("core_info")) {
            rsp.put("core_info", new CoreToKeys(Core.GLOBAL_CORE).keySet());
        }
        if ( rb != null && rb.contains("lock_info")) {
            rsp.put("lock_info", Gate.counts());
        }
        if ( rb != null && rb.contains("task_info")) {
            rsp.put("task_info", getAllTasks());
        }

        helper.reply(Synt.mapOf( "info", rsp ));
    }

    private static Map  getAllSize(File d) {
        Map     map = new LinkedHashMap();
        long    tot ;
        long    one ;
        String  hum ;

        /*
        long    all = 0;
        long    oth = 0;

        for (File f : d.listFiles()) {
            if (f.getName().endsWith("-INF")) {
                continue;
            }
            if (f.isDirectory()) {
                one = getDirSize(f);
                if (one == 0) {
                    continue;
                }
                hum = Syno.humanSize(one);
                map.put(f.getName(), new Object[] {one, hum, "目录 "+f.getName()});
            } else {
                one = f.length();
                oth += one;
            }
            all += one;
        }

        hum = Syno.humanSize(all);
        map.put(".", new Object[] {all, hum, "当前目录"});

        hum = Syno.humanSize(oth);
        map.put("!", new Object[] {oth, hum, "其他文件"});
        */

        tot = d.getTotalSpace();
        hum = Syno.humanSize(tot);
        map.put("@", new Object[] {tot, hum, "磁盘大小"});

        one = d.getFreeSpace();
        hum = Syno.humanSize(one);
        map.put("#", new Object[] {one, hum, "磁盘剩余"});

        /*
        one = tot - one;
        hum = Syno.humanSize(one);
        map.put("$", new Object[] {one, hum, "磁盘已用"});
        */

        /*
        // 排序
        List<Map.Entry> a = new ArrayList(map.entrySet());
        Collections.sort(a, new SortBySize());
        map.clear();
        for (Map.Entry  n : a) {
            map.put(n.getKey(), n.getValue());
        }
        */

        return map;
    }

    /*
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

    private static class SortBySize implements Comparator<Map.Entry> {
        @Override
        public int compare(Map.Entry s1, Map.Entry s2) {
            Object[] a1 = (Object[]) s1.getValue();
            Object[] a2 = (Object[]) s2.getValue();
            long n1 = (Long) a1[0];
            long n2 = (Long) a2[0];
            return n1 < n2 ? 1 : -1;
        }
    }
    */

    public static Set<String> getAllTasks() {
        ThreadGroup subGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = subGroup;

        // 获取根线程组
        while (null != subGroup) {
            topGroup = subGroup;
            subGroup = subGroup.getParent();
        }

        // 获取所有线程
        int actCount = topGroup.activeCount( );
        Thread[   ] tasks = new Thread /***/ [actCount];
        Set<String> names = new LinkedHashSet(actCount);
        topGroup.enumerate(tasks);

        // 获取线程名称
        for (Thread task : tasks) {
            names.add(task.getName());
        }

        return names;
    }

    private static class CoreToKeys extends Core {
        public CoreToKeys(Core core) {
            super(core);
        }
        public Set<String> keySet() {
            return sup ( ).keySet();
        }
    }
}
