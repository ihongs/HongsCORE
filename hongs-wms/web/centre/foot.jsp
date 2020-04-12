<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<blockquote><p>
    <span>&copy; <%=CoreLocale.getInstance().translate("copy.right")%></span>
    <small class="pull-right">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></small>
</p></blockquote>
