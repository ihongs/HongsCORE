<%@page import="app.hongs.Core"%>
<%@page import="java.io.File"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<!doctype html>
<html>
    <head>
        <title>HongsCORE::<%=_title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centre.js" ></script>
        <script type="text/javascript" src="centre/auto/_core_.js" ></script>
        <script type="text/javascript" src="centre/auto/_edit_.js" ></script>
        <script type="text/javascript" src="<%=_module%>/<%=_entity%>/custom.js"></script>
        <script type="text/javascript">
            HsDEPS["<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/custom.js"]=1;
            HsDEPS["__COMMITED__"] = 1;
        </script>
    </head>
    <body>
        <div id="notebox"></div>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <div class="row" data-load="centre/head.jsp?active=<%=encodeURL(request.getContextPath()+"/"+_module+"/"+_entity+"/")%>"></div>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container">
                <ol class="breadcrumb paths row hide" data-toggle="hsTabs">
                    <li class="back-crumb dont-close pull-right">
                        <a href="javascript:;">
                            <i class="glyphicon glyphicon-remove-sign"></i>
                        </a>
                    </li>
                    <li class="home-crumb active">
                        <a href="javascript:;">
                            <i class="glyphicon glyphicon-folder-open"></i>
                            <b></b>
                        </a>
                    </li>
                </ol>
                <div class="panes paths row">
                    <div></div>
                    <div data-load="<%=_module%>/<%=_entity%>/list.html"></div>
                </div>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-default navbar-fixed-bottom">
            <div class="container">
                <div class="row" data-load="centre/foot.jsp"></div>
            </div>
        </nav>
    </body>
</html>
