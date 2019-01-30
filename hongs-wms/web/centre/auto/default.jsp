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
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="centre/base.css"/>
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
        <script type="text/javascript" src="<%=_module%>/<%=_entity%>/defines.js"></script>
        <script type="text/javascript">
            HsDEPS["<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/defines.js"]=1;
            HsDEPS["__DEFINED__"]=1;

            // 虚拟路由
            jQuery(function() {
                var listBox = H$("#main-context");
                var paneBox =  $(".labs").first();
                var listUrl = "<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/list.html";
                var infoUrl = "<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/info.html";
                var formUrl = "<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/form.html";
                var addsUrl = "<%=request.getContextPath()%>/<%=_module%>/<%=_entity%>/form_adds.html";
                var inState ;
                var IF= "1" ; // 处于表单中
                var NF= null; // 非表单模式

                /**
                 * 列表和查看对公共区很重要
                 * 如果有给编号则打开详情页
                 */
                listBox.hsLoad(listUrl + hsSetParam(location.search, "id", null));
                $(window).on("popstate", function() {
                    inState = true ;
                    paneBox.children( ).eq(2).children( ).hsClose( );
                    if (H$("@id")) {
                        if (H$("@if") ) {
                            paneBox.hsOpen(formUrl+ location.search);
                        } else {
                            paneBox.hsOpen(infoUrl+ location.search);
                        }
                    } else {
                        if (H$("@if") ) {
                            paneBox.hsOpen(addsUrl+ location.search);
                        }
                    }
                });
                $(window).trigger("popstate");
                inState = false;

                /**
                 * 还需要处理内部加载的页面
                 */
                paneBox.on("hsReady", ".loadbox", function() {
                    if (inState) {
                        inState = false;
                        return;
                    }

                    var id ;
                    var ul = location.search;
                    var rl = hsFixUri($(this).data("href"));

                    if (rl.substr(0 , infoUrl.length) == infoUrl) {
                        id = hsGetParam(rl, "id");
                        ul = hsSetParam(ul, "id", id);
                        ul = hsSetParam(ul, "if", NF);
                        history.pushState({}, "", location.pathname + ul);
                    } else
                    if (rl.substr(0 , formUrl.length) == formUrl) {
                        id = hsGetParam(rl, "id");
                        ul = hsSetParam(ul, "id", id);
                        ul = hsSetParam(ul, "if", IF);
                        history.pushState({}, "", location.pathname + ul);
                    } else
                    if (rl.substr(0 , addsUrl.length) == addsUrl) {
                        ul = hsSetParam(ul, "id", NF);
                        ul = hsSetParam(ul, "if", IF);
                        history.pushState({}, "", location.pathname + ul);
                    }
                });
                paneBox.on("hsClose", ".loadbox", function() {
                    if (inState) {
                        inState = false;
                        return;
                    }

                    var ul = location.search;
                    var rl = hsFixUri($(this).data("href"));

                    if (rl.substr(0 , infoUrl.length) == infoUrl
                    ||  rl.substr(0 , formUrl.length) == formUrl
                    ||  rl.substr(0 , addsUrl.length) == addsUrl) {
                        ul = hsSetParam(ul, "id", NF);
                        ul = hsSetParam(ul, "if", NF);
                        history.pushState({}, "", location.pathname + ul);
                    }
                });
            });
        </script>
    </head>
    <body>
        <div id="notebox"></div>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <div class="row" data-load="centre/head.jsp?active=<%=encodeURL(_module+"/"+_entity+"/")%>"></div>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container">
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
                    <div id="main-context"></div>
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
