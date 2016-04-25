<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@include file="../Functions.jsp"%>
<%
    String root = request.getContextPath();
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=root%>/"/>
        <link rel="icon" type="image/x-icon" href="<%=root%>/favicon.ico"/>
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/assets/css/hongscore.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="<%=root%>/static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="<%=root%>/static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="<%=root%>/static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="<%=root%>/static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="<%=root%>/common/conf/default.js"></script>
        <script type="text/javascript" src="<%=root%>/common/lang/default.js"></script>
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/addons/bootstrap-fileinput/css/fileinput.min.css"/>
        <script type="text/javascript" src="<%=root%>/static/addons/bootstrap-fileinput/fileinput.min.js"></script>
        <title>我的文章</title>
    </head>
    <body>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <jsp:include page="../../../handle/head.jsp"/>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container">
                <ul class="nav nav-tabs">
                    <li role="presentation" class="active"><a href="#">我发布的</a></li>
                    <li role="presentation"><a href="#">我收藏的</a></li>
                    <li role="presentation"><a href="#">我的举报</a></li>
                </ul>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-default navbar-fixed-bottom" role="navigation">
            <div class="container">
                <jsp:include page="../../../handle/foot.jsp"/>
            </div>
        </nav>
    </body>
</html>
