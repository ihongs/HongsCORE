package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.dh.ITrnsct;
import java.lang.annotation.Annotation;

/**
 * 操作成功才提交数据更改
 *
 * 由Action.doAction自动调用
 *
 * @author Hongs
 */
public class CommitInvoker implements FilterInvoker {
    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Core core = Core.getInstance();

        try {
            core.put(Cnst.TRNSCT_MODE, true);

            // 开启
            for(Object o : core.values()) {
                if (o instanceof ITrnsct) {
                    ((ITrnsct) o).begin();
                }
            }

            try {
                chains.doAction();

                // 提交
                for(Object o : core.values()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : core.values()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).revert();
                    }
                }

                if (ex instanceof HongsExpedient) {
                    throw (HongsExpedient) ex ;
                } else
                if (ex instanceof HongsException) {
                    throw (HongsException) ex ;
                }
                throw new  HongsException (0x1120, ex);
            }
        } finally {
            core.remove(Cnst.TRNSCT_MODE);
        }
    }
}
