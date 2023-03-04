<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%
    // 如果有内部返回, 则不要显示此页
    if (null != request.getAttribute(Cnst.RESPON_ATTR)) {
        return;
    }

    Integer     code ;
    String      text ;
    CoreLocale  lang = CoreLocale.getInstance();

    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    if (null != code) {
        response.setStatus(code); // 不知何故 sendError 之后总是 500, 此为修正
    }
    if (null == exception) {
        exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
    }
    if (null == exception) {
        text  = (String) request.getAttribute( "javax.servlet.error.message");
        if (null == text
        ||  "NOT FOUND".equalsIgnoreCase(text)) {
            text  = lang.translate("core.error.no.thing");
        } else
        if ("FORBIDDEN".equalsIgnoreCase(text)) {
            text  = lang.translate("core.error.no.power");
        }
    } else {
        text  = exception.getLocalizedMessage();
        if (null == text ) {
            text  = exception.getClass().getName();
        }
    }
%>
<!--MSG: <%=escapeXML(text)%> -->
<!--ERN: Er<%=code != null ? code : 400%> -->
<!doctype html>
<html>
    <head>
        <title><%=response.getStatus()%> Error</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style type="text/css">
            html, body
                { margin: 0; padding: 0; }
            h1, h2, h3
                { margin: 0; font-size: 8em; font-weight: 800; }
            html, body, .jumbotron, .container
                { color: #eee; background-color: #000; }
            blockquote
                { color: #ccc; background-color: #222; }
            blockquote
                { margin: 0; padding: 10px 15px; }
            .jumbotron
                { margin: 20px 0; padding: 20px 0; }
            .container
                { margin: 0 auto; padding: 0 15px; }
            @media (min-width: 1200px) {
                .container { width: 1170px; }
            }
            @media (min-width: 992px) {
                .container { width: 970px; }
            }
            @media (min-width: 768px) {
                .container { width: 750px; }
            }
            .copy-right a, .site-links a
                { margin-right: 1.0em; }
            a:link, a:hover, a:active, a:visited
                { color: white; }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1> : ( </h1>
                <p style="white-space: pre-line"><%=escapeXML(text)%></p>
                <p>&nbsp;</p>
                <p style="font-size: small;">
                    <%=lang.translate("core.error.400.txt")%>
                    <a href="javascript:history.back();">
                        <b><%=lang.translate("core.error.go.back")%></b>
                    </a>,
                    <a href="<%=request.getContextPath()%>/">
                        <b><%=lang.translate("core.error.go.home")%></b>
                    </a>.
                </p>
                <p>&nbsp;</p>
            </div>
            <div class="container">
                <blockquote style="font-size: small;"><p>
                    <span>&copy;&nbsp;</span><span class="copy-right"><%=lang.translate("fore.copy.right")%></span>
                    <span>&nbsp;&nbsp;</span><span class="site-links"><%=lang.translate("fore.site.links")%></span>
                    <span style="float: right;">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                    <span style="clear: both ;"></span>
                </p></blockquote>
            </div>
        </div>
    </body>
</html>