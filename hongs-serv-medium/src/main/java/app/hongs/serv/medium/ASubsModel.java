package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.FetchCase;
import app.hongs.db.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 子表模型
 * 类似 species,statics
 * @author Hongs
 */
public class ASubsModel extends ALinkModel {

    private final Map topMap = new HashMap();

    public ASubsModel(Table table) throws HongsException {
        super(table);
    }

    /**
     * 与 setAll(linkId,names) 配合使用
     * 用于同步记录一些与上层一致的数据
     * 如 user_id
     * 通常 fs 取自 assoc 配置的 convey
     * @param rd
     * @param fs 
     */
    public void setSubs(Map rd, String... fs) {
        topMap.clear();
        for(String fn : fs) {
            Object fv = rd.get(fn);
            if (null != fv) {
                topMap.put(fn, fv);
            }
        }
    }

    /**
     * 采用最简单的方式添加、更新数据
     * @param linkId
     * @param names
     * @return
     * @throws HongsException 
     */
    public Set<String> setSubs(String linkId, String... names) throws HongsException {
        Map<String, Map> map = new HashMap();
        Set<String>      ids = new HashSet();
        Pattern          pat = Pattern.compile("^(.*?)(?:\\^(\\d+))?(?:\\|(.*))?$");

        FetchCase caze = table
                .filter("link = ? AND link_id = ?" , link , linkId)
                .select("id, name");
        List<Map> rows = caze.all();

        for (String name  : names) {
            name = name.trim( );
            if ("".equals(name)) {
                continue;
            }
            Matcher mat = pat.matcher(name);
            Map m = new HashMap(topMap);
            if (mat.matches()) {
                String temp;
                temp = mat.group(2);
                if (temp != null) {
                    m.put("score", Integer.parseInt(temp));
                }
                temp = mat.group(3);
                if (temp != null) {
                    m.put("note", temp);
                }
                temp = mat.group(1);
                m.put("name", temp);
            } else {
                m.put("name", name);
            }
            map.put(name, m);
        }

        if (map.isEmpty()) {
            if (rows != null && !rows.isEmpty()) {
                caze.delete( );
            }
            return new HashSet();
        }

        for (Map row : rows) {
            String  id  = (String) row.get( "id" );
            String name = (String) row.get("name");
            try {
            if (map.containsKey(name)) {
                Map md = map.remove(name);
                md.put("id", id);
                ids.add(id);
                put(id, md); // 修改的
            } else {
                del(id);     // 删除的
            }
            } catch (HongsException e) {
                // 无法删除、修改(已逻辑删除)则跳过
                if (e.getCode() != 0x1096
                &&  e.getCode() != 0x1097) {
                    throw e;
                }
            }
        }

        for (Map row : map.values()) {
            row.put("link_id", linkId);
            row.put("link"   , link  );
            String id = add(row);
            /**/ids.add(id); // 新增的
        }

        return  ids;
    }

}
