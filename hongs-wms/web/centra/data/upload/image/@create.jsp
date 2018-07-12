<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page import="java.util.Map" %>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%
    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    vh.addRulesByForm("upload", "image");
    vh.isPrompt(true);
    vh.isUpdate(true);
    ah.reply(vh.verify(ah.getRequestData()));
%>