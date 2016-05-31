<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Table"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%!
    List<String> getMenuSids() throws HongsException {
        List<Map>    list = NaviMap.getInstance("handle").getMenuTranslates();
        List<String> sids = new ArrayList();
        for (Map menu : list ) {
            String href = (String) menu.get("href");
            if (href.startsWith("medium/section/")) {
                String[] a = href.split("\\/" , 4 );
                sids.add(a[2]);
            }
        }
        return sids;
    }

    List<Map> getSectPath(String sid) throws HongsException {
        Table sec = DB.getInstance("medium").getTable("section");
        List<String> sids = getMenuSids(  );
        List<Map>    rows = new ArrayList();
        while (sid != null && !"0".equals(sid))  {
            Map row = sec.fetchCase()
                    .where  ("type = ? AND id = ?" , "default", sid)
                    .select ("id, pid, name")
                    .one    ();
            if (!row.isEmpty()) {
                rows.add(0, row);
                if (sids.contains( sid )) {
                    break;
                }
            }
            sid = (String)row.get("pid");
        }
        return rows;
    }

    Set<String> getSectIds(String sid) throws HongsException {
        if (sid == null || "0".equals(sid)) {
            return null;
        }
        Table sec = DB.getInstance("medium").getTable("section");
        Set<String> ids = new HashSet();
        while (sid != null)  {
            Map row = sec.fetchCase()
                    .where  ("type = ? AND pid = ?", "default", sid)
                    .select ("id")
                    .one    ();
            if (!row.isEmpty()) {
                ids.add((String) row.get("id"));
            }
            sid = (String)row.get("pid");
        }
        return ids;
    }
    
    String getSectPid(String sid) throws HongsException {
        Table sec = DB.getInstance("medium").getTable("section");
        Map one = sec.fetchCase()
                .where  ("type = ? AND id = ?" , "default", sid)
                .select ("pid")
                .one    ();
        if (!one.isEmpty()) {
            return (String) one.get("pid");
        }
        return "0";
    }

    String getArtiSid(String aid) throws HongsException {
        Table seg = DB.getInstance("medium").getTable("segment");
        Map   one = seg.fetchCase()
                .where  ("link = ? AND link_id = ?", "article", aid)
                .select ("sect_id")
                .one    ();
        if (!one.isEmpty()) {
            return (String) one.get("sect_id");
        }
        return "0";
    }
%>