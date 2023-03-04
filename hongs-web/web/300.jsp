<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%
    Integer     code ;
    String      text ;
    String      href ;
    CoreLocale  lang = CoreLocale.getInstance();

    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    text = (String  ) request.getAttribute("javax.servlet.error.message");
    href = (String  ) request.getAttribute("javax.servlet.location");
    if (href == null) {
        href  = request.getContextPath() + "/" ;
    }
    if (code != null
    &&  code >= 400 ) {
        response.setStatus(code);
    }
%>
<!--MSG: <%=escapeXML(text)%> -->
<!--ERR: Goto <%=escapeXML(href)%> -->
<!--ERN: Er<%=code != null ? code : 302%> -->
<!doctype html>
<html>
    <head>
        <title>Redirect...</title>
        <meta http-equiv="Refresh" content="3; url=<%=escapeXML( href )%>">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style type="text/css">
            html, body
                { margin: 0; padding: 0; }
            h1, h2, h3
                { font-size: 8em; font-weight: 800; }
            html, body, .jumbotron, .container
                { color: #fff; background-color: #060; }
            blockquote, pre
                { color: #ddd; background-color: #050; }
            blockquote, pre
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
                <h1> :) </h1>
                <p style="white-space: pre-line;"><%=escapeXML(text)%></p>
                <p>&nbsp;</p>
                <p style="font-size: large;">
                    <a href="<%=escapeXML(href)%>">
                        <b><%=lang.translate("core.error.redirect")%></b>
                    </a>
                </p>
                <p>&nbsp;</p>
            </div>
            <div class="container">
                <blockquote style="font-size: small;"><p>
                    <span>&copy;&nbsp;</span><span class="copy-right"><%=lang.translate("fore.copy.right")%></span>
                    <span>&nbsp;&nbsp;</span><span class="site-links"><%=lang.translate("fore.site.links")%></span>
                    <span>Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                </p></blockquote>
            </div>
        </div>
    </body>
</html>