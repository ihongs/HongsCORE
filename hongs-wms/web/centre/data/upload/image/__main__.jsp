<%@page import="java.util.Map"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    if (! Core.ACTION_NAME.get().endsWith("/create" + Cnst.ACT_EXT)) {
        throw new HongsException(0x1104, "Unsupported action!");
    }

    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    vh.addRulesByForm("upload", "image");
    ah.reply(vh.verify(ah.getRequestData() , true , true));
%>