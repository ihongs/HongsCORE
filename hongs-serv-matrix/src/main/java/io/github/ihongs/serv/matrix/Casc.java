package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.reflex.Async;
import java.util.Map;

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

    public enum  ACTS  {UPDATE, DELETE};
    public final ACTS   type;
    public final String conf;
    public final String form;
    public final String fk;
    public final Object fv;

    private Casc(ACTS type, String conf, String form, String fk, Object fv) {
        this.type = type;
        this.conf = conf;
        this.form = form;
        this.fk = fk;
        this.fv = fv;
    }
    
    private void run() {
        Core core = Core.getInstance();
        long time = System.currentTimeMillis()/ 1000 ;
        try {
            Data inst = Data.getInstance(conf , form);
            switch (type) {
                case UPDATE:
                    update(inst, fk, fv, time);
                    break;
                case DELETE:
                    delete(inst, fk, fv, time);
                    break;
            }
        }
        catch (Exception|Error e) {
            CoreLogger.error ( e);
        }
        finally {
            core.reset( );
        }
    }
    
    public static void update(String conf, String form, String fk, Object fv) {
        QUEUE.add(new Casc(ACTS.UPDATE, conf, form, fk, fv));
    }
    
    public static void delete(String conf, String form, String fk, Object fv) {
        QUEUE.add(new Casc(ACTS.DELETE, conf, form, fk, fv));
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
