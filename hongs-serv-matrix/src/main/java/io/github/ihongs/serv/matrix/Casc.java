package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.reflex.Async;
import java.util.Map;
import java.util.Set;

/**
 * 级联操作队列
 * @author Kevin
 */
public class Casc {

    private static final Async QUEUE = new Async<Casc> (
        "matrix.cascade" , Integer.MAX_VALUE , 1
    ) {
        @Override
        public void run(Casc casc) { casc.run(); }
    };

    public enum  ACTION {UPDATE, DELETE};
    public final     ACTION  ac;
    public final     Object  id;
    public final Set<String> aq;

    private Casc(Set<String> aq, Object id, ACTION ac) {
        this.aq = aq;
        this.id = id;
        this.ac = ac;
    }

    private void run () {
        Core core = Core.getInstance();
        long time = System.currentTimeMillis() / 1000;
        try {
            switch (ac) {
                case UPDATE: update(aq, id, time); break;
                case DELETE: delete(aq, id, time); break;
            }
        }
        catch (Exception|Error e) {
            CoreLogger.error ( e);
        }
        finally {
            core.reset();
        }
    }

    public static void update(Set<String> aq, Object id) {
        QUEUE.add(new Casc(aq, id, ACTION.UPDATE));
    }

    public static void delete(Set<String> aq, Object id) {
        QUEUE.add(new Casc(aq, id, ACTION.DELETE));
    }

    public static void update(Set<String> aq, Object id, long ct) throws HongsException {
        for(String at : aq) {
            if (at == null || at.isBlank()) {
                continue;
            }

            // 格式: conf.form?fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#UPDATE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  ("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("!");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            update(Data.getInstance(c, f), fk, id, ct);
        }
    }

    public static void delete(Set<String> aq, Object id, long ct) throws HongsException {
        for(String at : aq) {
            if (at == null || at.isBlank()) {
                continue;
            }

            // 格式: conf.form?fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#DELETE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  ("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("!");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            delete(Data.getInstance(c, f), fk, id, ct);
        }
    }

    public static void update(Data inst, String fk, Object fv, long ct) throws HongsException {
        Data.Loop loop = inst.search(Synt.mapOf(
            Cnst.RB_KEY, Synt.setOf(Cnst.ID_KEY),
            fk, fv
        ), 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            inst.set(id, Synt.mapOf(
                "__meno__" , "system.cascade",
                "__memo__" , "Update cascade " + fn + ":" + id
            ) , ct);
            CoreLogger.debug("Update cascade {} {}:{}", fn, fk, id);
        }
    }

    public static void delete(Data inst, String fk, Object fv, long ct) throws HongsException {
        Data.Loop loop = inst.search(Synt.mapOf(
            Cnst.RB_KEY, Synt.setOf(Cnst.ID_KEY),
            fk, fv
        ), 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            inst.cut(id, Synt.mapOf(
                "__meno__" , "system.cascade",
                "__memo__" , "Delete cascade " + fn + ":" + id
            ) , ct);
            CoreLogger.debug("Delete cascade {} {}:{}", fn, fk, id);
        }
    }

}
