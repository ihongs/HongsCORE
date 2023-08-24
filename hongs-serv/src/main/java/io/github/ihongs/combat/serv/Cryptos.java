package io.github.ihongs.combat.serv;

import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.PrivTable;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Crypto;
import io.github.ihongs.util.Synt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 变更加密
 * @author Hongs
 */
@Combat("cryptos")
public class Cryptos {

    /**
     * 更换数据库表密钥
     * @param args
     * @throws HongsException
     */
    @Combat("alter")
    public static void recrypt(String[] args) throws HongsException {
        Map opts = CombatHelper.getOpts(
            args,
            "table=s",
            "old-type:s",
            "old-sk:s",
            "old-iv:s",
            "new-type:s",
            "new-sk:s",
            "new-iv:s",
            "?Usage: crypto"
                + " --table TABLE"
                + " --old-type OLD_CRYPTO_TYPE"
                + " --old-sk OLD_CRYPTO_SK"
                + " --old-iv OLD_CRYPTO_IV"
                + " --new-type NEW_CRYPTO_TYPE"
                + " --new-sk NEW_CRYPTO_SK"
                + " --new-iv NEW_CRYPTO_IV"
        );
        String tab    = (String) opts.get("table");
        String oldMod = (String) opts.get("old-type");
        String oldSk  = (String) opts.get("old-sk");
        String oldIv  = (String) opts.get("old-iv");
        String newMod = (String) opts.get("new-type");
        String newSk  = (String) opts.get("new-sk");
        String newIv  = (String) opts.get("new-iv");

        Table  tb = DB.getInstance( ).getTable(tab);
        DB     db = tb.db;
        if (! (tb instanceof PrivTable)) {
            throw new HongsException("Table not crypto");
        }

        PrivTable oldTab = ((PrivTable) tb).clone();
        PrivTable newTab = ((PrivTable) tb).clone();
        newTab.tableName += "_new";

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
        Map one = db.fetchOne ("SELECT COUNT(*) AS `cnt` FROM `"+newTab.tableName+"`");
        int cxt = Synt.declare(one.get("cnt"), 0);
        if (cxt > 0) {
            throw new HongsException("Verify new table failed, please truncate table first.");
        }

        // 获取总数以计算进度
        Map row = db.fetchOne ("SELECT COUNT(*) AS `cnt` FROM `"+oldTab.tableName+"`");
        int cnt = Synt.declare(row.get("cnt"), 0);
        int fin = 0;
        CombatHelper.progres((float) fin / cnt, String.format("%d/%d", fin, cnt));

        try (Loop lp = db.query("SELECT * FROM `"+oldTab.tableName+"`", 0, 0)) {
        while (lp.hasNext()) {
            row = lp.next();

            decrypt.accept(row); // 解密
            encrypt.accept(row); // 加密

            db.insert(newTab.tableName , row);

            fin ++;
            CombatHelper.progres((float) fin / cnt, String.format("%d/%d", fin, cnt));
        } }

        CombatHelper.progres();
    }

}
