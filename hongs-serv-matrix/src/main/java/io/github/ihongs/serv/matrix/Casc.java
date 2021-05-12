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

    private static Async QUEUE = new Async<Casc> (
        "matrix.cascade" , Integer.MAX_VALUE , 1
    ) {
        @Override
        public void run(Casc casc) { casc.run(); }
    };

    public  final  String conf;
    public  final  String form;
    public  final  Set<String> setIds;
    public  final  Set<String> delIds;

    private Casc (String conf, String form, Set<String> setIds, Set<String> delIds) {
        this.conf = conf;
        this.form = form;
        this.setIds = setIds;
        this.delIds = delIds;
    }
    
    private void run() {
        Core core = Core.getInstance();
        try {
            cascade(Data.getInstance(conf, form), setIds, delIds);
        }
        catch (Exception|Error e) {
            CoreLogger.error ( e);
        }
        finally {
            core.close();
        }
    }
    
    public static void add(String conf, String form, Set<String> setIds, Set<String> delIds) {
        QUEUE.add(new Casc( conf, form, setIds, delIds ) );
    }
    
    public static void cascade(Data inst, Set<String> setIds, Set<String> delIds) throws HongsException {
        Set<String> ats = Synt.toSet(inst.getParams().get("cascades"));
        if (ats == null || ats.isEmpty()) {
            return;
        }
        if (setIds.isEmpty( )
        &&  delIds.isEmpty()) {
            return;
        }
        long ct = System.currentTimeMillis() / 1000;
        for(String at : ats) {
            // 格式: conf.form?fk#DELETE#UPDATE
            int p ;
            p = at.indexOf("#");
            String tk = at.substring(0+p);
                   at = at.substring(0,p);
            p = at.indexOf("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
            p = at.lastIndexOf(".");
            String c  = at.substring(0,p);
            String f  = at.substring(1+p);

            // 获取实例, 逐一处理
            Data   md = Data.getInstance( c , f );
            if (!delIds.isEmpty()
            &&  tk.contains( "#DELETE" )) {
                deleteCascade(md, fk, delIds, ct);
            }
            if (!setIds.isEmpty()
            &&  tk.contains( "#UPDATE" )) {
                updateCascade(md, fk, setIds, ct);
            }
        }
    }

    public static void updateCascade(Data inst, String fk, Set<String> fv, long ct) throws HongsException {
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

    public static void deleteCascade(Data inst, String fk, Set<String> fv, long ct) throws HongsException {
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
