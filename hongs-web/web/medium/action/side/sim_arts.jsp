
<%@page import="app.hongs.HongsException"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
    List<Map> getSimArts() throws HongsException {
        return null;
    }
%>
<%
    List<Map>  arts = getSimArts();
%>
<%if (arts != null && !arts.isEmpty()) {%>
<!-- 类似文章 -->
<div>
    <%for (Map info : arts) {%>
    <div><a href="<%=info.get("id")%>"><%=info.get("name")%></a></div>
    <%} /*End for*/%>
</div>
<%} /*End if*/%>