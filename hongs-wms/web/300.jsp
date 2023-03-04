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
        href  = request.getContextPath() + "/";
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
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/common.min.css"/>
        <style type="text/css">
            h1, h2, h3 {
                font-size  : 8em;
                font-weight: 800; 
            }
            html, body {
                width     : 100%;
                height    : 100%;
            }
            .navbar .copy-right a ,
            .navbar .site-links a {
                margin-right : 1em;
            }
            .jumbotron {
                color     : #ccc;
                background: #222;
            }
            @media ( min-width: 768px ) {
                .jumbotron {
                    background: #202222 linear-gradient(
                        -45deg, #202222 15%, #20282f 40%, #202222 40%
                    );
                }
            }
        </style>
    </head>
    <body>
        <div style="width : 100%;">
        <div class="jumbotron">
            <div class="container">
                <h1> :) </h1>
                <p style="white-space: pre-line;"><%=escapeXML(text)%></p>
                <p>&nbsp;</p>
                <p style="font-weight: initial ;">
                    <a class="btn btn-lg btn-success" href="<%=escapeXML(href)%>">
                        <b><%=lang.translate("core.error.redirect")%></b>
                    </a>
                </p>
            </div>
        </div>
        <nav class="navbar">
            <div class="container">
                <blockquote><p class="clearfix" style="font-size: small;">
                    <span>&copy;&nbsp;</span><span class="copy-right"><%=lang.translate("fore.copy.right")%></span>
                    <span>&nbsp;&nbsp;</span><span class="site-links"><%=lang.translate("fore.site.links")%></span>
                    <span class="pull-right text-muted">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                </p></blockquote>
            </div>
        </nav>
        </div>
        <script type="text/javascript">
            if (document.body.clientHeight >= document.body.scrollHeight) {
                document.body.setAttribute("style", "display: flex; align-items: center;");
            }
        </script>
    </body>
</html>