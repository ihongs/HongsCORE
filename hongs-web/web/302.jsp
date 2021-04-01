<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%
    Integer code;
    String  text;
    String  href;
    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    text = (String  ) request.getAttribute("javax.servlet.error.message");
    href = (String  ) request.getAttribute("javax.servlet.location");
    if (href == null) {
        href  = request.getContextPath() + "/";
    }
    if (code != null
    &&  code != 301
    &&  code != 302 ) {
        response.setStatus(code);
    }
%>
<!--MSG: <%=escapeXML(text)%>-->
<!doctype html>
<html>
    <head>
        <title><%=escapeXML(link)%></title>
        <meta http-equiv="Refresh" content="3; url=<%=escapeXML( href )%>">
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
            h1
                { font-weight: bolder; }
            #footbox .site-links a
                { margin-right: 1.0em; }
            #footbox blockquote
                { color: #444; background-color: #ccc; }
            #footbox.navbar, body, .jumbotron, .container
                { color: #222; background-color: #eee; }
            @media ( prefers-color-scheme: dark ) {
            #footbox blockquote
                { color: #ccc; background-color: #444; }
            #footbox.navbar, body, .jumbotron, .container
                { color: #eee; background-color: #222; }
            }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1>: )</h1>
                <p> &nbsp; </p>
                <p> <%=escapeXML(text)%> </p>
                <p> <a class="btn btn-lg btn-success" href="<%=escapeXML(href)%>">
                    <%=CoreLocale.getInstance().translate("core.error.redirect")%>
                    </a>
                </p>
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