package app.hongs.action.anno;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.NaviMap;
import java.lang.annotation.Annotation;

/**
 * 权限过滤处理器
 * @author Hongs
 */
public class PermitInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno) throws HongsException {
        Permit   ann  = (Permit) anno;
        String   conf = ann.conf();
        String[] role = ann.role();

        // 识别路径
        if (conf.length() == 0) {
            String form;
            form = chains.getEntity();
            conf = chains.getModule();
            try {
                new NaviMap(conf);
            } catch (HongsException ex) {
                if (ex.getCode() == 0x10e0) {
                    conf = conf +"/"+ form;
                }
            }
        }

        NaviMap map = NaviMap.getInstance(conf);
        boolean was = map.getAuthSet() != null ;
        boolean has = false;

        if (! was) {
            throw  new  HongsException (0x1101);
        }

        if (  null == role || role.length == 0  ) {
            has = map.chkAuth(chains.getAction());
        } else {
            for ( String rale : role ) {
                if (map.chkRole(rale)) {
                    has = true;
                    break;
                }
            }
        }

        if (! has) {
            throw new HongsException(0x1103);
        }

        chains.doAction();
    }

}
