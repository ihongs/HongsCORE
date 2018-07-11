<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="foo.hongs.Core"%>
<%@page import="foo.hongs.action.ActionHelper"%>
<%@page import="foo.hongs.action.VerifyHelper"%>
<%@page import="java.util.Map" %>
<%@page extends="foo.hongs.jsp.Pagelet"%>
<%
    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    vh.addRulesByForm("upload", "image");
    vh.isPrompt(true);
    vh.isUpdate(true);
    ah.reply(vh.verify(ah.getRequestData()));
%>