package io.github.ihongs.serv.centra;

import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.normal.serv.Record;
import io.github.ihongs.normal.serv.Sesion;

/**
 * 存储维护命令
 * @author Hongs
 */
@Combat("records")
public class Records {

    /**
     * 清除过期的数据
     * @param args
     */
    @Combat("clean")
    public static void clean(String[] args) {
        long exp = 0;
        if (args.length != 0) {
             exp = Integer.parseInt ( args[0] );
        }
        Record.del(System.currentTimeMillis() / 1000 - exp);
    }

    /**
     * 预览存储的数据
     * @param args
     */
    @Combat("check")
    public static void check(String[] args) {
        if (args.length == 0) {
          CombatHelper.println("Record ID required!");
        }
          CombatHelper.preview( Record.get(args[0]) );
    }

    /**
     * 清除过期的数据
     * @param args
     * @throws io.github.ihongs.HongsException
     */
    @Combat("clean.sess")
    public static void cleanSess(String[] args) throws HongsException {
        long exp = 0;
        if (args.length != 0) {
             exp = Integer.parseInt ( args[0] );
        }
        Sesion.getRecord().del(System.currentTimeMillis() / 1000 - exp);
    }

    /**
     * 预览存储的数据
     * @param args
     * @throws io.github.ihongs.HongsException
     */
    @Combat("check.sess")
    public static void checkSess(String[] args) throws HongsException {
        if (args.length == 0) {
          CombatHelper.println("Record ID required!");
        }
          CombatHelper.preview( Sesion.getRecord().get(args[0]) );
    }

}
