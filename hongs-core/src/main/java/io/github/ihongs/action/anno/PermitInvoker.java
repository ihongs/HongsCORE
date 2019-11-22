package io.github.ihongs.action.anno;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.NaviMap;
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
//          String form;
//          form = chains.getEntity();
            conf = chains.getModule();
            // 照顾 Module Action 的配置规则. 2018/7/7 改为完全由外部预判
//          if (NaviMap.hasConfFile(conf+"/"+form)) {
//              conf = conf+"/"+form ;
//          }
        }

        NaviMap map = NaviMap.getInstance(conf);
        boolean was = map.getAuthSet() != null ;
        boolean has = false;

        if (! was) {
            throw new HongsException(0x401);
        }

        if (role == null || role.length == 0) {
            has = map.chkAuth(chains.getAction( ) );
        } else {
            for ( String rale : role ) {
                if ( rale.startsWith( "@" ) ) {
                if (map.chkAuth(rale.substring(1))) {
                    has = true;
                    break;
                }
                } else
                if (map.chkRole(rale)) {
                    has = true;
                    break;
                }
            }
        }

        if (! has) {
            throw new HongsException(0x403);
        }

        chains.doAction();
    }

}
