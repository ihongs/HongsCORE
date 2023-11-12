<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CruxException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page extends="io.github.ihongs.jsp.Proclet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    if (!Core.ACTION_NAME.get().endsWith("/create"+Cnst.ACT_EXT)) {
        throw new CruxException(404, "Unsupported action!");
    }

    VerifyHelper vh = new VerifyHelper();
    ActionHelper ah = ActionHelper.getInstance();
    vh.addRulesByForm("centra/data/upload", "image");
    ah.reply(vh.verify(ah.getRequestData(), true, true));
%>