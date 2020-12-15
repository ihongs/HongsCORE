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
        if (null == text || text.equalsIgnoreCase("FORBIDDEN")) {
            text  = CoreLocale.getInstance().translate("core.error.no.power");
        }
    }
    String  link  = CoreLocale.getInstance().translate("core.error.to.index");
%>
<!--MSG: <%=escapeXML(text)%>-->
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
            h1
                { font-weight: bolder; }
            #footbox .site-links a
                { margin-right: 1.0em; }
            #footbox blockquote
                { color: #ddd; background-color: #036; }
            #footbox.navbar, body, .jumbotron, .container
                { color: #fff; background-color: #048; }
        </style>
    </head>
    <body>
        <div class="jumbotron">
            <div class="container">
                <h1>: (</h1>
                <p> &nbsp; </p>
                <p> <%=escapeXML(text)%> </p>
                <p> <a href="<%=request.getContextPath()%>/" class="btn btn-lg btn-primary">
                    <%=escapeXML(link)%>
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