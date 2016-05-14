<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Model"%>
<%@page import="app.hongs.serv.medium.ABaseModel"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    List<Map> getSubSecs(Model mod, String sid) throws HongsException {
        return mod.table.fetchCase()
                .where  ("pid = ? AND state > ?", sid, 0)
                .orderBy("seria DESC")
                .select ("id, name")
                .all    ();
    }
%>
<%
    ABaseModel  mod = (ABaseModel) DB.getInstance("medium").getModel("section");
                mod.setType("default");
    String      sid = request.getParameter("sid");
    String     root = request.getContextPath();
    List<Map>  secs = getSubSecs(mod, sid);
%>
<%if (secs != null && !secs.isEmpty()) {%>
<!-- 下级分类 -->
<div>
    <h4>下级分类</h4>
    <%for (Map info : secs) {%>
    <div><a href="<%=root%>/medium/section/<%=info.get("id")%>"><%=info.get("name")%></a></div>
    <%} /*End for*/%>
</div>
<%} /*End if*/%>