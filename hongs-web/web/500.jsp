<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
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
    if (request.getAttribute(Cnst.RESP_ATTR) != null) {
        return;
    }

    String  text  = null ;
    String  trac  = null ;
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
        }
    } else {
        text  = (String) request.getAttribute("javax.servlet.error.message");
        if (text == null ) {
            text  = CoreLocale.getInstance( ).translate("core.error.unknwn");
        }
    }
%>
<!doctype html>
<html>
    <head>
        <title>HongsCORE::Login</title>
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
            #footbox.navbar, body, .jumbotron, .container
                { background-color: #8F0000; color: #fff; border: 0; }
            #footbox blockquote
                { background-color: #8F0000; color: #fff; }
            h1, h3, pre
                { font-weight: bold; }
            pre
                { background-color: transparent; color: #ddd; border: 0; }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1>: (</h1>
                <p>  <%=escapeXML(text.trim())%>  </p>
                <%if (trac != null) {%>
                <pre><%=escapePRE(trac.trim())%></pre>
                <%}%>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-fixed-bottom">
            <div class="container">
                <blockquote><p>Copyleft &copy; 2015 黄弘. <small class="pull-right">Powered by <a href="https://github.com/ihongs/HongsCORE/" target="_blank">HongsCORE</a>, and <a href="static/power.html" target="_blank">others</a>.</small></p></blockquote>
            </div>
        </nav>
    </body>
</html>