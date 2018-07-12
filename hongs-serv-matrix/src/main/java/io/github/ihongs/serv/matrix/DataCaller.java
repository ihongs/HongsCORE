package io.github.ihongs.serv.matrix;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Tool;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.thread.Async;

/**
 * 数据更新通知队列
 * @author Hongs
 */
public class DataCaller extends Async<String> {

    private static final String SPACE = "hongs.log.matrix.data.call" ;
    private static final String ENVIR =    "INSIDE matrix.data.call ";

    protected DataCaller(int maxTasks, int maxServs) throws HongsException {
        super(DataCaller.class.getName( ), maxTasks, maxServs);
    }

    public static DataCaller getInstance() throws HongsException {
        String name =  DataCaller.class.getName();
        DataCaller inst = (DataCaller) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("matrix");
            inst  = new DataCaller(
                conf.getProperty("core.matrix.data.caller.max.tasks", Integer.MAX_VALUE),
                conf.getProperty("core.matrix.data.caller.max.servs", 1));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(String url) {
        try {
            String rsp = Remote.get(url);
            String log = "GET: "+ url +" RSP: "+ Tool.indent(rsp.trim( ));
            CoreLogger.getLogger (SPACE).info(ENVIR + log);
        } catch ( Exception | Error ex ) {
            String log = ex.getMessage();
            CoreLogger.getLogger (SPACE).warn(ENVIR + log);
        }
    }

}
