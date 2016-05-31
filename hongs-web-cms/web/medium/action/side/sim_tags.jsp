<%@page import="app.hongs.HongsException"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    List<Map> getSimTags() throws HongsException {
        return null;
    }
%>
<%
    List<Map>  tags = getSimTags();
%>
<%if (tags != null && !tags.isEmpty()) {%>
<!-- 类似标签 -->
<div>
    <%for (Map info : tags) {%>
    <div><a href="<%=info.get("id")%>"><%=info.get("name")%></a></div>
    <%} /*End for*/%>
</div>
<%} /*End if*/%>