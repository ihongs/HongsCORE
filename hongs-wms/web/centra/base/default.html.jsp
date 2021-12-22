<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    // 仅开放接口则抛出资源缺失异常, 兼容旧版 NONE
    if ($hrel != null && ($hrel.startsWith("!") || $hrel.equals("NONE"))) {
        throw new HongsException(404, $locale.translate("core.error.no.thing"));
    }
    String $func = "in_"+($module+"_"+$entity).replace ('/', '_');
    String $href = $hrel == null || $hrel.isEmpty()
                 ? $module+"/"+$entity+"/list.html"
                 : $hrel ;
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
        <link rel="stylesheet" type="text/css" href="static/centra/base.min.css"/>
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
        <script type="text/javascript" src="common/auth/centra.js" ></script>
        <script type="text/javascript" src="<%=$module%>/<%=$entity%>/defines.js"></script>
    </head>
    <body class="sider-open">
        <div id="context">
            <%if (!"!HIDE".equals($hrel) && !"HIDE".equals($hrel)) {%>
            <div id="headbox">
                <div id="menu-context" data-load="centra/head.jsp" data-active="<%=$module+"/"+$entity+"/"%>"></div>
            </div>
            <%} /* End if */%>
            <div id="bodybox">
                <div id="main-context" class="container-fluid">
                    <ol class="breadcrumb show-close tabs laps" data-topple="hsTabs">
                        <li class="hook-crumb dont-crumb pull-right" data-eval="H$('!<%=$module%>/<%=$entity%>/select.act') || $(this).hide()">
                            <a href="javascript:;" data-href="<%=$module+"/"+$entity+"/swap.html"%>" title="<%=$locale.translate("fore.manual", $title)%>">
                                <i class="glyphicon glyphicon-book"></i>
                                <span class="title hide">...</span>
                            </a>
                        </li>
                        <li class="hook-crumb dont-crumb pull-right" data-eval="H$('!<%=$module%>/<%=$entity%>/reveal.act') || $(this).hide()">
                            <a href="javascript:;" data-href="<%=$module+"/"+$entity+"/snap.html"%>" title="<%=$locale.translate("fore.record", $title)%>">
                                <i class="glyphicon glyphicon-time"></i>
                                <span class="title hide">...</span>
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
                        <div>
                            <%
                            Map menus  = (Map) $menu.get("menus");
                            if (menus != null && ! menus.isEmpty ()) {
                                boolean _acti = false;
                                String  _href = $href;
                                StringBuilder sb = new StringBuilder ();
                                for ( Object  ot : menus.entrySet()) {
                                    Map.Entry et = (Map.Entry) ot ;
                                    Map menu = (Map) et.getValue();
                                    String href = (String) et.getKey ();
                                    String hrel = (String) menu.get("hrel");
                                    String text = (String) menu.get("text");
                                    if (href != null &&  href.startsWith("!")
                                    &&  hrel != null && !hrel.startsWith("!")) {
                                        text  = $locale.translate(text);
                                        if (hrel.isEmpty()
                                        ||  hrel.startsWith("?")
                                        ||  hrel.startsWith("#")) {
                                            hrel = _href + hrel ;
                                        }
                                        sb.append("<li");
                                        if (_acti == false) {
                                            _acti  =  true;
                                            $href  =  hrel;
                                            sb.append(" class=\"active\"");
                                        }
                                        sb.append("><a href=\"javascript:;\" data-href=\"")
                                          .append(hrel )
                                          .append("\">")
                                          .append(text )
                                          .append("</a></li>");
                                    }
                                }
                            if ( _acti ) {
                            %>
                            <ul class="nav nav-tabs board">
                                <%=sb%>
                            </ul>
                            <%}} /* End menus */%>
                            <div data-load="<%=$href%>"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            (function($) {
                var context = $("#main-context");

                // 子级菜单
                var loadBox = context.find("[data-load]").first();
                var tabsBox = context.find( ".nav-tabs" ).first();
                tabsBox.on("click", "a", function() {
                    if ( $(this).parent().is(".active") ) return ;
                    loadBox.hsLoad($(this).data("href") );
                    $(this).parent().addClass("active")
                      .siblings().removeClass("active");
                });

                // 外部定制
                window["<%=$func%>"] && window["<%=$func%>"](context);
            })(jQuery);
        </script>
    </body>
</html>
