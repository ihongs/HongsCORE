<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page extends="io.github.ihongs.jsp.Proclet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    if (!Core.ACTION_NAME.get().endsWith("/create"+Cnst.ACT_EXT)) {
        throw new HongsException(404, "Unsupported action!");
    }

    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    vh.addRulesByForm ("centre/data/upload", "image");
    ah.reply(vh.verify(ah.getRequestData() , true , true));
%>