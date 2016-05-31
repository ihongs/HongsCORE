<%@page import="app.hongs.HongsException"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    List<Map> getHotTags() throws HongsException {
        return null;
    }
%>
<%
    List<Map>  tags = getHotTags();
%>
<%if (tags != null && !tags.isEmpty()) {%>
<!-- 热门标签 -->
<div>
    <h4>热门标签</h4>
    <%for (Map info : tags) {%>
    <div><a href="<%=info.get("id")%>"><%=info.get("name")%></a></div>
    <%} /*End for*/%>
</div>
<%} /*End if*/%>