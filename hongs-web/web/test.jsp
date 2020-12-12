<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    ActionHelper helper = ActionHelper.getInstance();
    helper.redirect("/", "lsdfsf", 302);
%>
