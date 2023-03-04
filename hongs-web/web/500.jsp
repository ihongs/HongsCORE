<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%!
    private static String escapePRE(String str) {
        StringBuilder b = new StringBuilder();
        int  l = str.length();
        int  i = 0;
        char c ;
        while ( i < l) {
            c = str.charAt(i);
            switch (c) {
              case '<': b.append("&lt;" ); break;
              case '>': b.append("&gt;" ); break;
              case '&': b.append("&amp;"); break;
              default : b.append(c);
            }
            i ++;
        }
        return b.toString().replaceAll("(\r\n|\r|\n)", "\r\n");
    }
%>
<%
    // 如果有内部返回, 则不要显示此页
    if (null != request.getAttribute(Cnst.RESPON_ATTR)) {
        return;
    }

    Integer     code ;
    String      text ;
    String      trac ;
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
        if (text == null ) {
            text  = lang.translate( "core.error.unknwn" );
        }   trac  = null;
    } else {
        text  = exception.getLocalizedMessage();
        if (text == null || text.isEmpty()) {
            text  = exception.getClass().getName();
        }
        // 调试模式输出异常栈以便检测
        if (4 == (4 & Core.DEBUG) ) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(o));
            trac  = new String (o.toByteArray(), "utf-8");
        } else {
            text  = text.replaceFirst("\\s*Stacktrace:\\s*$", "");
            trac  = null;
        }
    }
%>
<!--MSG: <%=escapeXML(text)%> -->
<!--ERN: Er<%=code != null ? code : 500%> -->
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
                { font-size: 8em; font-weight: 800; margin: 0; }
            html, body, .jumbotron, .container
                { color: #fff; background-color: #800; }
            blockquote, pre
                { color: #ddd; background-color: #700; }
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
                <h1> :( </h1>
                <p style="white-space: pre-line;"><%=escapeXML(text)%></p>
                <%if (trac != null) {%>
                <pre style="overflow: auto;"><%=escapePRE(trac)%></pre>
                <%}%>
                <p>&nbsp;</p>
                <p style="font-size: small;">
                    <%=lang.translate("core.error.500.txt")%>
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
                    <span>Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                </p></blockquote>
            </div>
        </div>
    </body>
</html>