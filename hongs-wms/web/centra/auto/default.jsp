<%@page import="io.github.ihongs.Core"%>
<%@page import="java.io.File"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<!doctype html>
<html>
    <head>
        <title><%=_title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="centra/base.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centra.js" ></script>
        <script type="text/javascript" src="centra/auto/_core_.js" ></script>
        <script type="text/javascript" src="centra/auto/_edit_.js" ></script>
        <script type="text/javascript" src="<%=_module%>/<%=_entity%>/defines.js"></script>
        <script type="text/javascript">
            HsDEPS["<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/defines.js"]=1;
            HsDEPS["__DEFINED__"]=1;
        </script>
    </head>
    <body>
        <div id="notebox"></div>
        <div id="context">
            <div id="headbox">
                <div id="menu-context" data-load="centra/head.jsp?active=<%=encodeURL(_module+"/"+_entity+"/")%>"></div>
            </div>
            <div id="bodybox">
                <div id="main-context" class="container-fluid">
                    <ol class="breadcrumb tabs laps hide row" data-toggle="hsTabs">
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
                    <div class="labs laps">
                        <div></div>
                        <div data-load="<%=_module%>/<%=_entity%>/list.html"></div>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
