package foo.hongs.action.anno;

import foo.hongs.Cnst;
import foo.hongs.Core;
import foo.hongs.HongsException;
import foo.hongs.HongsExemption;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.ActionRunner;
import foo.hongs.dh.ITrnsct;
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

                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex ;
                } else
                if (ex instanceof HongsException) {
                    throw (HongsException) ex ;
                }
                throw new  HongsException (0x1138, ex);
            }
        } finally {
            core.remove(Cnst.TRNSCT_MODE);
        }
    }
}
