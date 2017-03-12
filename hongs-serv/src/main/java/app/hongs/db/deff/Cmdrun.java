package app.hongs.db.deff;

import app.hongs.HongsException;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.db.DB;
import app.hongs.db.Table;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库结构对比命令
 * 此工具仅适合 MySQL
 * @author Hongs
 */
@Cmdlet("common.db.deff")
public class Cmdrun {

    @Cmdlet("__main__")
    public static void exec(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args,
            "from=s", "to=s", "sync:b", "del-tables:b", "del-fields", "!U", "!A"
        );

        String  fr = (String ) opts.get("from");
        String  to = (String ) opts.get( "to" );
        boolean sc = (boolean) opts.get("sync");
        boolean dt = (boolean) opts.get("del-tables");
        boolean df = (boolean) opts.get("del-fields");

        Pattern patt = Pattern.compile("\\.\\w+$");
        Matcher frMt = patt.matcher(fr);
        Matcher toMt = patt.matcher(to);

        if (frMt.find() && toMt.find()) {
            DB    frDb = DB.getInstance(fr.substring(0 , frMt.start()));
            Table frTb = frDb.getTable (fr.substring(frMt.start() + 1));
            DB    toDb = DB.getInstance(to.substring(0 , toMt.start()));
            Table toTb = toDb.getTable (to.substring(toMt.start() + 1));

            if (sc) {
                new TableDeff(frTb).syncSlaver(toTb, df);
            } else {
                new TableDeff(frTb).deffSlaver(toTb, df);
            }
        } else {
            DB    frDb = DB.getInstance(fr);
            DB    toDb = DB.getInstance(fr);

            if (sc) {
                new DBDeff(frDb).syncSlaver(toDb, null, null, dt, df);
            } else {
                new DBDeff(frDb).deffSlaver(toDb, null, null, dt, df);
            }
        }
    }

}
