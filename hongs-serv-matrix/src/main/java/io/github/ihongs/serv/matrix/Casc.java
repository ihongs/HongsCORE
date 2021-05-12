package io.github.ihongs.serv.matrix;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.reflex.Async;
import java.util.Set;

/**
 * 级联操作队列
 * @author Kevin
 */
public class Casc {

    private static Queue  QUEUE = new Queue ( );
    private static enum   PROC {UPDATE, DELETE};
    public  final  PROC   proc ;
    public  final  String conf ;
    public  final  String form ;
    public  final  String fk   ;
    public  final  Set<String> fv;

    private  Casc ( PROC proc, String conf, String form, String fk, Set<String> fv) {
        this.proc = proc ;
        this.conf = conf ;
        this.form = form ;
        this.fk   = fk   ;
        this.fv   = fv   ;
    }
    
    public static void update (String conf, String form, String fk, Set<String> fv) {
        QUEUE.add(new Casc(PROC.UPDATE,  conf, form, fk, fv));
    }
    
    public static void delete (String conf, String form, String fk, Set<String> fv) {
        QUEUE.add(new Casc(PROC.DELETE,  conf, form, fk, fv));
    }
    
    private static class Queue extends Async<Casc> {

        public Queue () {
            super("matrix.cascade", Integer.MAX_VALUE, 1);
        }
        
        @Override
        public void run(Casc data) {
            Core core = Core.getInstance();
            long time = System.currentTimeMillis() / 1000;
            try {
                switch (data.proc) {
                    case UPDATE:
                        for (String fv : data.fv ) {
                            Data.getInstance(data.conf, data.form).updateCascade(data.fk, fv, time);
                        }
                        break;
                    case DELETE:
                        for (String fv : data.fv ) {
                            Data.getInstance(data.conf, data.form).deleteCascade(data.fk, fv, time);
                        }
                        break;
                }
            }
            catch (HongsException ex) {
                CoreLogger.error (ex);
            }
            finally {
                core.close( );
            }
        }
        
    }
    
}
