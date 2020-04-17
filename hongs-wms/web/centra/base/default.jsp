<%@page import="io.github.ihongs.Core"%>
<%@page import="java.io.File"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    // 仅开放接口则抛出资源缺失异常
    if ($hide) {
        throw new HongsException(404, $locale.translate("core.error.no.thing"));
    }
%>
<!doctype html>
<html>
    <head>
        <title><%=$title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="centra/base.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="static/assets/hongsedit.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centra.js" ></script>
        <script type="text/javascript" src="centra/base.min.js"></script>
        <script type="text/javascript" src="<%=$module%>/<%=$entity%>/defines.js"></script>
        <script type="text/javascript">
            HsDEPS["<%=request.getContextPath()%>/<%=$module%>/<%=$entity%>/defines.js"]=1;
            HsDEPS["__DEFINED__"]=1;
        </script>
    </head>
    <body class="sider-open">
        <div id="context">
            <div id="headbox">
                <div id="menu-context" data-load="centra/head.jsp" data-active="<%=$module+"/"+$entity+"/"%>"></div>
            </div>
            <div id="bodybox">
                <div id="main-context" class="container-fluid">
                    <ol class="breadcrumb tabs laps" data-toggle="hsTabs">
                        <li class="hook-crumb dont-crumb pull-right">
                            <a href="javascript:;" data-href="<%=$module+"/"+$entity+"/swap.html"%>" title="<%=$locale.translate("fore.manual", $title)%>">
                                <i class="glyphicon glyphicon-book"></i>
                            </a>
                        </li>
                        <li class="back-crumb dont-close pull-right">
                            <a href="javascript:;">
                                <i class="glyphicon glyphicon-remove-sign"></i>
                            </a>
                        </li>
                        <li class="home-crumb active">
                            <a href="javascript:;">
                                <i class="glyphicon glyphicon-folder-open"></i>
                                <b><%=$title%></b>
                            </a>
                        </li>
                    </ol>
                    <div class="labs laps">
                        <div></div>
                        <div></div>
                        <div data-load="<%=$module%>/<%=$entity%>/list.html"></div>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
