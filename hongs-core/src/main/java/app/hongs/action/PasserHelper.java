package app.hongs.action;

import java.util.HashSet;
import java.util.Set;

/**
 * 路径过滤助手
 * 用于在过滤器中通过初始化参数
 * 排除或选择部分需要处理的路径
 * @author Hongs
 */
public class PasserHelper {

    protected Set<String>[] ignoreUrls;
    protected Set<String>[] attendUrls;

    /**
     * 构建过滤实例
     * @param ignoreUrls 待忽略的路径
     * @param attendUrls 需处理的路径
     */
    public PasserHelper(String[] ignoreUrls, String[] attendUrls) {
        this.ignoreUrls = check( ignoreUrls );
        this.attendUrls = check( attendUrls );
    }

    /**
     * 构建过滤实例
     * 路径分号分隔, 之间允许有空字符
     * @param ignoreUrls 待忽略的路径
     * @param attendUrls 需处理的路径
     */
    public PasserHelper(String   ignoreUrls, String   attendUrls) {
        this.ignoreUrls = check( ignoreUrls );
        this.attendUrls = check( attendUrls );
    }

    /**
     * 检查是否要忽略
     * 优先 uri 匹配 attendUrls 返回 false
     * @param uri
     * @return
     */
    public boolean ignore(String uri) {
        return !check(uri, attendUrls) && check(uri, ignoreUrls);
    }

    /**
     * 检查是否要继续
     * 优先 uri 匹配 ignoreUrls 返回 false
     * @param uri
     * @return
     */
    public boolean attend(String uri) {
        return !check(uri, ignoreUrls) && check(uri, attendUrls);
    }

    private boolean check(String uri, Set<String>[] uris) {
        if (uris[0].contains(uri)) {
            return true;
        }

        int poz = uri.lastIndexOf( ".");
        int pos = uri.lastIndexOf( "/");

        // 扩展名
        if ( ! uris[1].isEmpty() ) {
            if (poz != -1 && poz > pos) {
                String suf = uri.substring(1 + pos);
                if (uris[1].contains (suf)) {
                    return true;
                }
            }
        }

        // 目录名
        if ( ! uris[2].isEmpty() ) {
            while  ( pos  !=  -1 ) {
                String pre = uri.substring(0 , pos);
                if (uris[2].contains (pre)) {
                    return true;
                }
                pos = pre.lastIndexOf("/");
                uri = pre;
            }
        }

        return false;
    }

    private Set<String>[] check(String   urls) {
        return check( urls == null ? null :
                      urls.split ( ";" ) );
    }

    private Set<String>[] check(String[] urls) {
        if (urls == null) {
            return  new Set[] {
                new HashSet(),
                new HashSet(),
                new HashSet()
            };
        }

        Set<String> cu = new HashSet();
        Set<String> eu = new HashSet();
        Set<String> su = new HashSet();
        for ( String u : urls  ) {
            u = u.trim(  );
            if (u.length() == 0) {
                // ignore
            } else if (u.endsWith  ("/*")) {
                su.add(u.substring ( 0, u.length() - 2));
            } else if (u.startsWith("*.")) {
                eu.add(u.substring ( 2  ));
            } else {
                cu.add(u);
            }
        }
        return new Set[ ] { cu , eu , su };
    }

}
