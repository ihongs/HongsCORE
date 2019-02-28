package io.github.ihongs.serv.auth;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.NaviMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将权限菜单名称补上路径
 * @author Hongs
 */
public class RoleMap {

    private final NaviMap navi;
    private final CoreLocale lang;

    public  RoleMap(NaviMap navi) {
        this.navi = navi;
        this.lang = navi.getCurrTranslator();
    }

    /**
     * 按当前权限获取权限列表
     * @return
     * @throws HongsException
     */
    public  List<Map> getRoleTranslated() throws HongsException {
        return getRoleTranslated(navi.getRoleSet());
    }

    /**
     * 按特定权限获取权限列表
     * @param rolez
     * @return 
     */
    public  List<Map> getRoleTranslated(Set<String> rolez) {
        Set  <String> namez  =  new HashSet(      );
        StringBuilder path   =  new StringBuilder();
        return getRoleTranslated(navi.menus, rolez, namez, lang, path, 0, 0);
    }

    private List<Map> getRoleTranslated(Map<String, Map> menus, Set<String> rolez, Set<String> namez, CoreLocale lang, StringBuilder path, int j, int i) {
      List<Map> list = new ArrayList( );

      if (null == menus||(j != 0 && j <= i)) {
          return  list;
      }

      for(Map.Entry item : menus.entrySet()) {
          Map v = (Map) item.getValue();
          Map m = (Map) v.get("menus" );
          Set r = (Set) v.get("roles" );

          // 没有指定 text 则用 href 获取
          StringBuilder b;
          String h = (String) item.getKey();
          String t = (String) v.get("text");
          if (t == null || t.length() == 0) {
              t = "core.menu."+name2Prop(h);
          }
          t = lang.translate(t);
          b = new StringBuilder(path)
                .append (" / ")
                .append (  t  );

          if (r != null) {
          List<Map> rolz = new ArrayList( );

          for(String n : ( Set<String> ) r) {
              if (rolez != null
              && !rolez.contains(n)) {
                  continue; // 无权
              }
              if (namez.contains(n)) {
                  continue; // 重名
              }

              Map    o = navi.getRole(n);
              Set    x = (Set) o.get("depends");
              String s = (String) o.get("text");

              // 没有指定 text 则用 name 获取
              if (s == null || s.length() == 0) {
//                s = "core.role."+name2Prop(n);
                  continue; // 没名字干脆隐藏
              }
              namez.add(n);

              Map role = new HashMap();
              role.put("name", n);
              role.put("text", s);
              role.put("rels", x);
              rolz.add(role);
          }

          if (! rolz.isEmpty()) {
              String p = (String) v.get("hrel");
              String d = (String) v.get("icon");

              Map menu = new HashMap();
              menu.put("roles", rolz );
              menu.put("text", b.substring(3) );
              menu.put("href", h);
              menu.put("hrel", p);
              menu.put("icon", d);
              list.add(menu);
          }
          }

          // 获取下级并拉平
          List<Map> subz = getRoleTranslated(m, rolez, namez, lang, b, j, i + 1);
          if (! subz.isEmpty()) {
              list.addAll(subz);
          }
      }

      return  list;
  }

  /**
   * 链接和名称转换为属性键
   * <pre>
   * 将一些特殊符号转换为点, 例如:
   * "abc/def#xyz" 转为 "abc.def.xyz"
   * "abc.def?x=z" 转为 "abc.def.x.z"
   * </pre>
   * @param n
   * @return
   */
  protected static final String name2Prop(String n) {
      n = CONV_DOT.matcher(n).replaceAll("."); // URL 符号换成点
      n = TRIM_DOT.matcher(n).replaceAll("" ); // 去前后多余的点
      return n;
  }
  private static final java.util.regex.Pattern CONV_DOT = java.util.regex.Pattern.compile("[/#?&=!]+");
  private static final java.util.regex.Pattern TRIM_DOT = java.util.regex.Pattern.compile("^\\.|\\.$");

}
