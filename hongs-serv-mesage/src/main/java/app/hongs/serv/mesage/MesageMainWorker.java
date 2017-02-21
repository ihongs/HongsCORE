package app.hongs.serv.mesage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.serv.handle.MesageSocket;
import app.hongs.util.Async;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

/**
 * 消息主要管道
 *
 * 消息管道数据结构
 * ROOM_ID,USER_ID,MSG_ID,TIME|{MSG_DATA}
 * 前三项为固定数据, MSG_DATA 由 mesage.form 的 message 表单决定
 *
 * @author Hongs
 */
public class MesageMainWorker extends Async<Mesage> implements MesageWorker, Core.GlobalSingleton {

    private final MesageNoteWorker keepWorker;

    protected MesageMainWorker(int maxTasks, int maxServs) throws HongsException {
        super(MesageMainWorker.class.getName( ), maxTasks, maxServs);

        keepWorker = MesageNoteWorker.getInstance();
    }

    public static MesageMainWorker getInstance() throws HongsException {
        String name = MesageMainWorker.class.getName();
        MesageMainWorker inst = (MesageMainWorker) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("mesage");
            inst =  new MesageMainWorker(
                    conf.getProperty("core.mesage.chat.worker.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.mesage.chat.worker.max.servs", 1));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(Mesage info) {
        try {
            Set<Session> sess = MesageSocket.ROOM_CONNS.get(info.roomId);
            Set<String > uids = MesageSocket.ROOM_USERS.get(info.roomId);

            // 准备存储
            Mesage2 msg2 = new Mesage2(info, new HashSet(uids));
            keepWorker.add(msg2);

            // 发送消息
            String  msg  = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\",\"info\":" + info.data + "}";
            for(Session  ses  : sess) {
                try {
                    ses.getBasicRemote().sendText(msg);
                } catch (IOException ex) {
                    CoreLogger.error(ex);
                }
            }
        } catch (Exception | Error ex) {
            Logger.getLogger(MesageMainWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
