package app.hongs.serv.mesage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.link.Loop;
import app.hongs.util.Async;
import app.hongs.util.Synt;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 消息存储管道
 * @author Hongs
 */
public class MesageNoteWorker extends Async<Mesage2> implements Core.GlobalSingleton {

    private final MesagePushWorker pushWorker;

    private final String addNoteSql;
    private final String addStatSql;
    private final String setStatSql;
    private final String getStatSql;
    private final String getMateSql;

    protected MesageNoteWorker(int maxTasks, int maxServs) throws HongsException {
        super(MesageNoteWorker.class.getName( ), maxTasks, maxServs);

        pushWorker = MesagePushWorker.getInstance();

        DB db = DB.getInstance("mesage");
        String noteTableName = db.getTable("note").tableName;
        String statTableName = db.getTable("note_stat").tableName;
        String mateTableName = db.getTable("room_mate").tableName;
        addNoteSql = "INSERT INTO `"+noteTableName+"` (`id`, `user_id`, `room_id`, `data`, `stime`, `ctime`) VALUES (?, ?, ?, ?, ?, ?)";
        addStatSql = "INSERT INTO `"+statTableName+"` (`stati`, `mtime`, `data`, `room_id`, `user_id`, `mate_id`) VALUES (?, ?, ?, ?, ?, ?)";
        setStatSql = "UPDATE `"+statTableName+"` SET `stati` = `stati` + ?, `mtime` = ?, `data` = ?, `room_id` = ?, `user_id` = ? WHERE `mate_id` = ?";
        getStatSql = "SELECT user_id FROM `"+statTableName+"` WHERE user_id = ?";
        getMateSql = "SELECT user_id FROM `"+mateTableName+"` WHERE room_id = ? AND user_id NOT IN (?)";
    }

    public static MesageNoteWorker getInstance() throws HongsException {
        String name = MesageNoteWorker.class.getName();
        MesageNoteWorker inst = (MesageNoteWorker) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("mesage");
            inst =  new MesageNoteWorker(
                conf.getProperty("core.mesage.keep.worker.max.tasks", Integer.MAX_VALUE),
                conf.getProperty("core.mesage.keep.worker.max.servs", 1));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(Mesage2 msg2) {
        try {
            Mesage info = msg2.message;
            DB     db   = DB.getInstance( "mesage" );
            long   ct   = System.currentTimeMillis() / 1000;

            // 存储消息
            db.execute(addNoteSql, info.id, info.userId, info.roomId, info.data, info.stime, ct);

            // 登记未读
            Loop loop = db.query(getMateSql, 0, 0, info.roomId, msg2.userIds);
            for(Map row : loop) {
                String uid = ( String ) row.get( "user_id" );
                List   lst = db.fetch(getStatSql, 0, 1, uid);
                        ct = System.currentTimeMillis()/1000;
                if (lst == null || lst.isEmpty()) {
                    db.execute(addStatSql, 1, ct, info.data, info.roomId, info.userId, uid);
                } else {
                    db.execute(setStatSql, 1, ct, info.data, info.roomId, info.userId, uid);
                }

                // 转入推送队列
                pushWorker.add(new Mesage2(info, Synt.asSet(uid)));
            }
        } catch (HongsException ex) {
            Logger.getLogger(MesageNoteWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
