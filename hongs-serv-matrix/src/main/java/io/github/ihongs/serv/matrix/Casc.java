package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Async;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 级联操作队列
 *
 * <pre>
 * 配置格式:
 *  &lt;enum name="form.cascade"&gt;
 *    &lt;value code="conf:form;thatFk"&gt;UPDATE,DELETE,DEPEND&lt/value&gt;
 *  &lt;/enum&gt;
 *  &lt;enum name="form.include"&gt;
 *    &lt;value name="conf:form;thisFk"&gt;thisFn:thatFn,sameFn&lt/value&gt;
 *  &lt;/enum&gt;
 * cascade 中可用逗号分隔相同表单的多个外键字段, 如 ;fk1,fk2
 * </pre>
 * <pre>
 * 级联模式:
 *  UPDATE 级联更新对应记录
 *  DELETE 级联删除对应级联
 *  DEPEND 删除时做关联检查, 有记录则报错中止
 * 如果只有 UPDATE 没有 DELETE 则将关联字段设为空值
 * </pre>
 *
 * @author Hongs
 */
public class Casc {

    private enum ACTION {UPDATE, DELETE};

    private static final Async QUEUE = new Queue();

    private static final class Queue extends Async <Group> {

        public Queue () {
            super ("matrix.cascade", Integer.MAX_VALUE, 1);
        }

        @Override
        public void run (Group group) {
            group.run();
        }
    }

    private static final class Group implements  Runnable  {

        public final     ACTION  ac;
        public final     Object  id;
        public final Set<String> aq;

        public Group(Set<String> aq, Object id, ACTION ac) {
            this.aq = aq;
            this.id = id;
            this.ac = ac;
        }

        @Override
        public void run () {
            Core core = Core.getInstance();
            long time = System.currentTimeMillis() / 1000;
            try {
                // 将当前用户设为管理员
                ActionHelper hlpr = ActionHelper.getInstance();
                hlpr.setSessibute (Cnst.UID_SES, Cnst.ADM_UID);

                switch (ac) {
                    case UPDATE: update (aq, id, time); break;
                    case DELETE: delete (aq, id, time); break;
                }
            }
            catch (Exception|Error e) {
                CoreLogger.error ( e);
            }
            finally {
                core.reset();
            }
        }

    }

    /**
     * 混合读取, 写入目标
     * 
     * 仅能 get/put, 专供 Data 的 includes/incloses
     */
    public static final class Mixes implements Map {

        public final Map ND;
        public final Map OD;

        /**
         * @param nd 目标数据
         * @param od 默认数据
         */
        public Mixes(Map nd, Map od) {
            this.ND = nd;
            this.OD = od;
        }

        @Override
        public Object get (Object key) {
            return ND.containsKey(key)
                 ? ND.get (key)
                 : OD.get (key);
        }

        @Override
        public Object put (Object key , Object value) {
            return ND.put (key, value);
        }

        @Override
        @Deprecated
        public Object remove(Object key) {
            return ND.remove(key);
        }

        @Override
        @Deprecated
        public void putAll(Map map) {
            ND.putAll(map);
        }

        @Override
        @Deprecated
        public void clear() {
            ND.clear();
        }

        @Override
        @Deprecated
        public boolean isEmpty() {
            return  ND.isEmpty();
        }

        @Override
        @Deprecated
        public boolean containsKey(Object key) {
            return  ND.containsKey(key);
        }

        @Override
        @Deprecated
        public boolean containsValue(Object value) {
            return  ND.containsKey(value);
        }

        @Override
        @Deprecated
        public int size() {
            return ND.size();
        }

        @Override
        @Deprecated
        public Collection values() {
            return ND.values();
        }

        @Override
        @Deprecated
        public Set keySet() {
            return ND.keySet();
        }

        @Override
        @Deprecated
        public Set entrySet() {
            return ND.entrySet();
        }

    }

    /**
     * 级联更新(仅放入队列,待异步执行)
     * @param aq 关联代码
     * @param id 主键取值
     */
    public static void update(Set<String> aq, Object id) {
        if (id != null && aq != null && ! aq.isEmpty( )) {
            QUEUE.add (new Group(aq, id, ACTION.UPDATE));
        }
    }

    /**
     * 级联删除(仅放入队列,待异步执行)
     * @param aq 关联代码
     * @param id 主键取值
     */
    public static void delete(Set<String> aq, Object id) {
        if (id != null && aq != null && ! aq.isEmpty( )) {
            QUEUE.add (new Group(aq, id, ACTION.DELETE));
        }
    }

    /**
     * 级联更新
     * @param aq 关联代码
     * @param id 主键取值
     * @param ct 操作时间
     * @throws CruxException
     */
    public static void update(Set<String> aq, Object id, long ct) throws CruxException {
        for(String at : aq) {
            if (at == null || at.isEmpty()) {
                continue;
            }

            // 格式: conf:form;fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#UPDATE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  (";");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  (":");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            update(Data.getInstance(c, f), fk, id, ct);
        }
    }

    /**
     * 级联删除
     * @param aq 关联代码
     * @param id 主键取值
     * @param ct 操作时间
     * @throws CruxException
     */
    public static void delete(Set<String> aq, Object id, long ct) throws CruxException {
        for(String at : aq) {
            if (at == null || at.isEmpty()) {
                continue;
            }

            // 格式: conf:form;fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#DELETE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  (";");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  (":");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            delete(Data.getInstance(c, f), fk, id, ct);
        }
    }

    /**
     * 级联更新
     * @param inst
     * @param fk 关联字段, 多个用分号分隔
     * @param fv 关联取值
     * @param ct 操作时间
     * @throws CruxException
     */
    public static void update(Data inst, String fk, Object fv, long ct) throws CruxException {
        // 可能多个关联指向同一资源
        Map  ar = new HashMap();
        Set  or = new HashSet();
        Set  rb = new HashSet();
        ar.put(Cnst.OR_KEY, or);
        ar.put(Cnst.RB_KEY, rb);
        rb.add(Cnst.ID_KEY    );
        for (String fn : fk.split(";")) {
            or.add(Synt.mapOf(fn , fv));
        }

        Data.Loop loop = inst.search(ar, 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            CoreLogger.debug("Update cascade {} {}:{}", fn, fk, id);
            inst.set(id, Synt.mapOf(
                "__memo__" , "Update cascade " + fn + ":" + id,
                "__meno__" , "system.cascade"
            ) , ct);
        }
    }

    /**
     * 级联删除
     * @param inst
     * @param fk 关联字段, 多个用分号分隔
     * @param fv 关联取值
     * @param ct 操作时间
     * @throws CruxException
     */
    public static void delete(Data inst, String fk, Object fv, long ct) throws CruxException {
        // 可能多个关联指向同一资源
        Map  ar = new HashMap();
        Set  or = new HashSet();
        Set  rb = new HashSet();
        ar.put(Cnst.OR_KEY, or);
        ar.put(Cnst.RB_KEY, rb);
        rb.add(Cnst.ID_KEY    );
        for (String fn : fk.split(";")) {
            or.add(Synt.mapOf(fn , fv));
        }

        Data.Loop loop = inst.search(ar, 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            CoreLogger.debug("Delete cascade {} {}:{}", fn, fk, id);
            inst.end(id, Synt.mapOf(
                "__memo__" , "Delete cascade " + fn + ":" + id,
                "__meno__" , "system.cascade"
            ) , ct);
        }
    }

}
