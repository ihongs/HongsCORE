package app.hongs.serv.mesage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.util.Async;

/**
 * 消息推送管道
 * @author Hongs
 */
public class MesagePushWorker extends Async<Mesage2> implements Core.GlobalSingleton {

    protected MesagePushWorker(int maxTasks, int maxServs) throws HongsException {
        super(MesagePushWorker.class.getName( ), maxTasks, maxServs);
    }

    public static MesagePushWorker getInstance() throws HongsException {
        String name = MesagePushWorker.class.getName();
        MesagePushWorker inst = (MesagePushWorker) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("mesage");
            inst =  new MesagePushWorker(
                    conf.getProperty("core.mesage.push.worker.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.mesage.push.worker.max.servs", 1));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(Mesage2 data) {
        // TODO: 按终端推送
    }
    
}
