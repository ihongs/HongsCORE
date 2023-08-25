package io.github.ihongs.combat.serv;

import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.PrivTable;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Crypto;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 变更加密
 * @author Hongs
 */
@Combat("cryptos")
public class Cryptos {

    /**
     * 加密
     * @param args
     */
    @Combat("encrypt")
    public static void encrypt(String[] args) {
        Map opts = CombatHelper.getOpts(
            args,
            "type=s",
            "sk=s",
            "iv:s",
            "!Anonymous",
            "?Usage: encrypt"
                + " --type CRYPTO_TYPE"
                + " --sk CRYPTO_SK"
                + " --iv CRYPTO_IV"
                + " TEXT..."
        );
        String   type = Synt.asString(opts.get("type"));
        String    sk  = Synt.asString(opts.get( "sk" ));
        String    iv  = Synt.asString(opts.get( "iv" ));
        String[] argz = (String[]) opts.get( "" );
        Crypto crypto = new Crypto(type, sk, iv );
        for (String arg : argz) {
            CombatHelper.paintln(crypto.encrypt().apply(arg));
        }
    }

    /**
     * 加密
     * @param args
     */
    @Combat("decrypt")
    public static void decrypt(String[] args) {
        Map opts = CombatHelper.getOpts(
            args,
            "type=s",
            "sk=s",
            "iv:s",
            "!Anonymous",
            "?Usage: decrypt"
                + " --type CRYPTO_TYPE"
                + " --sk CRYPTO_SK"
                + " --iv CRYPTO_IV"
                + " TEXT..."
        );
        String   type = Synt.asString(opts.get("type"));
        String    sk  = Synt.asString(opts.get( "sk" ));
        String    iv  = Synt.asString(opts.get( "iv" ));
        String[] argz = (String[]) opts.get( "" );
        Crypto crypto = new Crypto(type, sk, iv );
        for (String arg : argz) {
            CombatHelper.paintln(crypto.decrypt().apply(arg));
        }
    }

    @Combat("becrypt")
    public static void becrypt(String[] args) throws HongsException {
        Map opts = CombatHelper.getOpts(
            args,
            "table=s",
            "table-name:s",
            "type:s",
            "sk:s",
            "iv:s",
            "sql:s",
            "start:i",
            "limit:i",
            "?Usage: check"
                + " --table DB.TABLE*"
                + " --table-name REAL_TABLE_NAME"
                + " --type CRYPTO_TYPE"
                + " --sk CRYPTO_SK"
                + " --iv CRYPTO_IV"
        );
        String tab = (String) opts.get("table");
        String tbn = (String) opts.get("table-name");
        String mod = (String) opts.get("type");
        String sk  = (String) opts.get("sk");
        String iv  = (String) opts.get("iv");
        String sql = (String) opts.get("sql");
        int  start = Synt.declare(opts.get("start"),  0  );
        int  limit = Synt.declare(opts.get("limit"), 1000);

        Table  tb  = DB.getInstance( ).getTable(tab);
        DB     db  = tb.db;
        if (! (tb instanceof PrivTable)) {
            throw new HongsException("Table not crypto");
        }

        // 按参数改表名和密钥, 获取验证方法
        if (tbn != null && ! tbn.isEmpty()) {
            tb.tableName = tbn;
        }
        if (mod != null && ! mod.isEmpty()) {
            ((PrivTable) tb).setCrypto(new Crypto(mod, sk, iv));
        }
        Consumer <Map> becrypt = ((PrivTable) tb).becrypt();

        // 获取总数以计算进度
        Map row = db.fetchOne ("SELECT COUNT(*) AS `cnt` FROM `"+tb.tableName+"`");
        int cnt = Synt.declare(row.get("cnt"), 0) - start;
        int fin = 0;
        int ok  = 0;
        int er  = 0;
        List<String> ers = new LinkedList();

        CombatHelper.progres((float) fin / cnt, fin+"/"+cnt);
        while(true) {
            try (Loop lp = query(db, tb, sql, fin + start, limit)) {
                if (! lp.hasNext()) {
                    break;
                }
                while(lp.hasNext()) {
                    row = lp.next();
                    fin ++;

                    // 检测
                    try {
                        becrypt.accept(row);
                        ok ++;
                    } catch (Exception ex ) {
                        er ++;
                        ers.add(fin+"\t"+ex.getMessage());
                    }

                    CombatHelper.progres((float) fin / cnt, fin+"/"+cnt+" ok:"+ok+" er:"+er);
                }
                if (limit == 0) {
                    break;
                }
            }
        }
        CombatHelper.progres();

        if (!ers.isEmpty()) {
            for (String es : ers) {
                CombatHelper.println(es);
            }
            CombatHelper.paintln("total:"+cnt+" ok:"+ok+" er:"+er);
        }
    }

    /**
     * 更换数据库表密钥
     * @param args
     * @throws HongsException
     */
    @Combat("recrypt")
    public static void recrypt(String[] args) throws HongsException {
        Map opts = CombatHelper.getOpts(
            args,
            "table=s",
            "old-table-name:s",
            "old-type:s",
            "old-sk:s",
            "old-iv:s",
            "new-table-name:s",
            "new-type:s",
            "new-sk:s",
            "new-iv:s",
            "sql:s",
            "start:i",
            "limit:i",
            "?Usage: convert"
                + " --table DB.TABLE*"
                + " --old-table-name OLD_TABLE_NAME"
                + " --old-type OLD_CRYPTO_TYPE"
                + " --old-sk OLD_CRYPTO_SK"
                + " --old-iv OLD_CRYPTO_IV"
                + " --old-table-name NEW_TABLE_NAME"
                + " --new-type NEW_CRYPTO_TYPE"
                + " --new-sk NEW_CRYPTO_SK"
                + " --new-iv NEW_CRYPTO_IV"
        );
        String tab    = (String) opts.get("table");
        String oldTbn = (String) opts.get("old-table-name");
        String oldMod = (String) opts.get("old-type");
        String oldSk  = (String) opts.get("old-sk");
        String oldIv  = (String) opts.get("old-iv");
        String newTbn = (String) opts.get("new-table-name");
        String newMod = (String) opts.get("new-type");
        String newSk  = (String) opts.get("new-sk");
        String newIv  = (String) opts.get("new-iv");
        String sql    = (String) opts.get("sql");
        int    start  = Synt.declare(opts.get("start"),  0  );
        int    limit  = Synt.declare(opts.get("limit"), 1000);

        Table  tb = DB.getInstance( ).getTable(tab);
        DB     db = tb.db;
        if (! (tb instanceof PrivTable)) {
            throw new HongsException("Table not crypto");
        }

        PrivTable oldTab = ((PrivTable) tb).clone();
        PrivTable newTab = ((PrivTable) tb).clone();

        if (oldTbn != null && !oldTbn.isEmpty()) {
            oldTab.tableName = oldTbn;
        }
        if (newTbn != null && !newTbn.isEmpty()) {
            newTab.tableName = newTbn;
        } else {
            newTab.tableName+= "_new";
        }

        // 加解密参数, 不指定则使用默认配置
        if (oldMod != null && ! oldMod.isEmpty()) {
            oldTab.setCrypto(new Crypto(oldMod, oldSk, oldIv));
        }
        if (newMod != null && ! newMod.isEmpty()) {
            newTab.setCrypto(new Crypto(newMod, newSk, newIv));
        }
        Consumer<Map> decrypt = oldTab.decrypt();
        Consumer<Map> encrypt = newTab.encrypt();

        // 新建目标表
        try {
            try (ResultSet rs = db.open().getMetaData().getTables(null, null, newTab.tableName, new String[] {"TABLE"})) {
                if ( ! rs.next( ) ) {
                    db.execute("CREATE TABLE `"+newTab.tableName+"` LIKE `"+oldTab.tableName+"`");
                }
            }
        } catch (HongsException ex) {
            throw new HongsException("Create new table failed, please create manually." , ex);
        } catch (  SQLException ex) {
            throw new HongsException(ex);
        }

        // 检查目标表是否为空
        Map one = db.fetchOne ("SELECT COUNT(1) AS `cnt` FROM `"+newTab.tableName+"`");
        int cxt = Synt.declare(one.get("cnt"), 0) - start;
        if (cxt > 0) {
            throw new HongsException("Verify new table failed, please truncate table first.");
        }

        // 获取总数以计算进度
        Map row = db.fetchOne ("SELECT COUNT(1) AS `cnt` FROM `"+oldTab.tableName+"`");
        int cnt = Synt.declare(row.get("cnt"), 0) - start;
        int fin = 0;
        CombatHelper.progres(fin, cnt);

        while(true) {
            try (Loop lp = query(db, oldTab, sql, fin + start, limit)) {
                if (! lp.hasNext()) {
                    break;
                }
                while(lp.hasNext()) {
                    row = lp.next();

                    decrypt.accept(row); // 解密
                    encrypt.accept(row); // 加密

                    db.insert(newTab.tableName , row);

                    CombatHelper.progres(++ fin, cnt);
                }
                if (limit == 0) {
                    break;
                }
            }
        }

        CombatHelper.progres();
    }

    private static Loop query(DB db, Table tb, String sql, int start, int limit) throws HongsException {
        if (sql != null && !sql.isEmpty()) {
            if (limit == 0) {
                return db.query(Syno.inject(sql, start), 0, 0);
            } else {
                return db.query(Syno.inject(sql, start, limit), 0, 0);
            }
        } else {
            return db.query("SELECT * FROM `"+tb.tableName+"`", start, limit);
        }
    }

}
