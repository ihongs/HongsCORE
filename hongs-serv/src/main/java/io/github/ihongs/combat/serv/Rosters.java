package io.github.ihongs.combat.serv;

import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.dh.Roster;

/**
 * 存储维护命令
 * @author Hongs
 */
@Combat("rosters")
public class Rosters {

    /**
     * 清除过期的数据
     * @param args
     */
    @Combat("clean")
    public static void clean(String[] args) {
        long exp = 0;
        if (args.length != 0) {
             exp = Integer.parseInt (args[0]);
        }
        Roster.del(System.currentTimeMillis() / 1000 - exp);
    }

    /**
     * 预览存储的数据
     * @param args
     */
    @Combat("check")
    public static void check(String[] args) {
        if (args.length == 0) {
          CombatHelper.println("Roster ID required!");
        }
          CombatHelper.preview( Roster.get(args[0]) );
    }

}
