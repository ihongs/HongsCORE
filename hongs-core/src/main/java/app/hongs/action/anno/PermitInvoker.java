package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.NaviMap;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限过滤处理器
 * conf 为 $ 时仅查会话状态
 * 此时 role 解释为登录区域
 * 空串 role 表示可在匿名区
 * @author Hongs
 */
public class PermitInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno) throws HongsException {
        Permit   ann  = (Permit) anno;
        String   conf = ann.conf();
        String[] role = ann.role();

        /**
         * 很多对外动作并不需要做复杂的权限校验
         * 仅需判断用户是否登录即可
         * conf 为 $ 时仅查会话状态
         * 此时 role 解释为登录区域
         * 空串 role 表示可在匿名区
         */
        if ("$".equals( conf ) ) {
            Object uid = helper.getSessibute(Cnst.UID_SES);
            if ( null == uid || "".equals( uid ) ) {
                throw  new  HongsException  (   0x1101   );
            }
            if (role.length > 0) {
                Set usp = (Set) helper.getSessibute(Cnst.USL_SES);
                Set rol =  new  HashSet ( Arrays.asList( role ) );
                if (usp == null || !usp.isEmpty()) {
                    if (!rol.contains ( "")) {
                        throw new HongsException( 0x1102 );
                    }
                } else {
                    if (!rol.retainAll(usp)) {
                        throw new HongsException( 0x1102 );
                    }
                }
            }
            chains.doAction();
            return;
        }

        // 识别路径
        if (conf.length() == 0) {
            String form;
            form = chains.getEntity();
            conf = chains.getModule();
            // 照顾 Module Action 的配置规则
            if (NaviMap.hasConfFile(conf+"/"+form)) {
                conf = conf+"/"+form ;
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
