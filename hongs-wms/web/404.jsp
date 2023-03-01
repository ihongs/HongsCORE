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

    Integer code;
    String  text;
    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    if (null != code) {
        response.setStatus(code); // 不知何故 sendError 之后总是 500, 此为修正
    }
    if (null == exception) {
        exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
    }
    if (null != exception) {
        text  = exception.getLocalizedMessage();
    } else {
        text  = (String) request.getAttribute("javax.servlet.error.message" );
        if (null == text || text.equalsIgnoreCase("NOT FOUND")) {
            text  = CoreLocale.getInstance().translate("core.error.no.thing");
        }
    }
%>
<!--MSG: <%=escapeXML(text)%> -->
<!--ERN: Er<%=code != null ? code : 404%> -->
<!doctype html>
<html>
    <head>
        <title>Redirect...</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/common.min.css"/>
        <style type="text/css">
            #footbox .copy-right a ,
            #footbox .site-links a {
                margin-right : 1em;
            }
            h1, h2, h3 {
                font-size  : 8em;
                font-weight: 800; 
            }
            html, body {
                width     : 100%;
                height    : 100%;
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
        <div class="jumbotron">
            <div class="container">
                <h1> :( </h1>
                <p style="white-space: pre-line;"><%=escapeXML(text)%></p>
                <p>&nbsp;</p>
                <p style="font-size: small;">
                    <%=CoreLocale.getInstance().translate("core.error.404.txt")%>
                    <a href="javascript:history.back();">
                        <b><%=CoreLocale.getInstance().translate("core.error.go.back")%></b>
                    </a>,
                    <a href="<%=request.getContextPath()%>/">
                        <b><%=CoreLocale.getInstance().translate("core.error.go.home")%></b>
                    </a>.
                </p>
            </div>
        </div>
        <nav id="footbox" class="navbar">
            <div class="container">
                <blockquote><p class="clearfix0" style="font-size: small;">
                    <span>&copy;&nbsp;</span><span class="copy-right"><%=CoreLocale.getInstance().translate("fore.copy.right")%></span>
                    <span>&nbsp;&nbsp;</span><span class="site-links"><%=CoreLocale.getInstance().translate("fore.site.links")%></span>
                    <span class="pull-right text-muted">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                </p></blockquote>
            </div>
        </nav>
    </body>
</html>
