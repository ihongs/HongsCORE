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

    Integer code;
    String  text;
    String  trac;
    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    if (null != code) {
        response.setStatus(code); // 不知何故 sendError 之后总是 500, 此为修正
    }
    if (null == exception) {
        exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
    }
    if (null != exception) {
        text  = exception.getLocalizedMessage(   );
        if (text == null ) {
            text  = exception.getClass().getName();
        }
        // 调试模式输出异常栈以便检测
        if (0 !=      Core.DEBUG
        &&  1 == (1 & Core.DEBUG)
        &&  2 != (2 & Core.DEBUG)
        &&  8 != (8 & Core.DEBUG) ) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(o));
            trac  = new  String(o.toByteArray(), "utf-8");
        } else {
            trac  = null ;
        }
    } else {
        text  = (String) request.getAttribute("javax.servlet.error.message");
        if (text == null ) {
            text  = CoreLocale.getInstance().translate("core.error.unknwn" );
        }   trac  = null ;
    }
%>
<!--MSG: <%=escapeXML( text.trim( ) )%> -->
<!--ERN: <%=code != null ? code : 500%> -->
<!doctype html>
<html>
    <head>
        <title><%=response.getStatus()%> Error</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="icon" type="image/x-icon" href="<%=request.getContextPath()%>/favicon.ico"/>
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/static/assets/css/hongscore.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="<%=request.getContextPath()%>/static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="<%=request.getContextPath()%>/static/assets/jquery.min.js"></script>
        <style type="text/css">
            h1, h2
                { font-weight: bolder; }
            #footbox .copy-right a,
            #footbox .site-links a
                { margin-right: 1.0em; }
            #footbox blockquote
                { color: #ddd; background-color: #600; }
            #footbox.navbar, html, body, .jumbotron, .container
                { color: #fff; background-color: #800; }
            pre
                { color: #ddd; background-color: transparent; border-color: transparent; }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1> :( </h1>
                <p>&nbsp;</p>
                <p style="white-space: pre-line;"><%=escapeXML(text.trim())%></p>
                <p>&nbsp;</p>
                <p style="font-size: small;">
                    <%=CoreLocale.getInstance().translate("core.error.500.txt")%>
                    <a style="color: white;" href="javascript:history.back();">
                        <b><%=CoreLocale.getInstance().translate("core.error.go.back")%></b>
                    </a>,
                    <a style="color: white;" href="<%=request.getContextPath()%>/">
                        <b><%=CoreLocale.getInstance().translate("core.error.go.home")%></b>
                    </a>.
                </p>
                <%if (trac != null) {%>
                <p>&nbsp;</p>
                <pre><%=escapePRE(trac.trim())%></pre>
                <%}%>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-fixed-bottom">
            <div class="container">
                <blockquote><p class="clearfix">
                    <span>&copy;&nbsp;</span><span class="copy-right"><%=CoreLocale.getInstance().translate("fore.copy.right")%></span>
                    <span>&nbsp;&nbsp;</span><span class="site-links"><%=CoreLocale.getInstance().translate("fore.site.links")%></span>
                    <span class="pull-right text-muted">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
                </p></blockquote>
            </div>
        </nav>
    </body>
</html>