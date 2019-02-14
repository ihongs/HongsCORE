<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="io.github.ihongs.util.verify.IsFile"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    if (! Core.ACTION_NAME.get().endsWith("/create" + Cnst.ACT_EXT)) {
        throw new HongsException(0x1104, "Unsupported action!");
    }

    ActionHelper  ah = Core.getInstance(ActionHelper.class);
    VerifyHelper  vh = new VerifyHelper();
    vh.addRule("file", new IsFile().config(Synt.mapOf(
            "href", "${BASE_HREF}/static/upload/image",
            "path", "${BASE_PATH}/static/upload/image",
            "extn", "jpeg,jpg,png,gif,bmp"
    )));
    ah.reply(vh.verify(ah.getRequestData() , true , true ));
%>