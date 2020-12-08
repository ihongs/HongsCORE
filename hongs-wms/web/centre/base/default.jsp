<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    // 仅开放接口则抛出资源缺失异常, 兼容旧版 NONE
    if ("!DENY".equals($hrel) || "NONE".equals($hrel)) {
        throw new HongsException(404, $locale.translate("core.error.no.thing"));
    }
    String $func = "in_"+($module+"_"+$entity).replace ('/', '_');
%>
<!doctype html>
<html>
    <head>
        <title><%=$title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/centre/base.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=$module%>/<%=$entity%>/defines.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="static/assets/hongsedge.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centre.js" ></script>
        <script type="text/javascript" src="<%=$module%>/<%=$entity%>/defines.js"></script>
    </head>
    <body>
        <div id="context">
            <%if (!"!HIDE".equals($hrel) && !"HIDE".equals($hrel)) {%>
            <div id="headbox">
                <div data-load="centre/head.jsp" data-active="<%=$module+"/"+$entity+"/"%>"></div>
            </div>
            <%} else {%>
            <div id="leadbox"></div>
            <%} /* End if */%>
            <div id="bodybox">
                <div id="main-context" class="container">
                    <ol class="breadcrumb tabs laps" data-topple="hsTabs">
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
                        <div><div data-load="<%=$module%>/<%=$entity%>/list.html"></div></div>
                    </div>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            (function($) {
                var context = $("#main-context");

                // 外部定制
                window["<%=$func%>"] && window["<%=$func%>"](context);

                // 虚拟导航
                context.hsPops({
                    info: "<%=$module%>/<%=$entity%>/info.html",
                    form: "<%=$module%>/<%=$entity%>/form.html",
                    adds: "<%=$module%>/<%=$entity%>/form_init.html"
                });
            })(jQuery);
        </script>
    </body>
</html>
