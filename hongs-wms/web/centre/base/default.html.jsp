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

    // 下级内部菜单
    StringBuilder $tabs = new StringBuilder ();
    Map menus  = (Map) $menu.get("menus");
    if (menus != null && ! menus.isEmpty ()) {
        boolean _acti = false;
        String  _href = $href;
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
                $tabs.append("<li");
                if (_acti == false) {
                    _acti  =  true;
                    $href  =  hrel;
                    $tabs.append(" class=\"active\"");
                }
                $tabs.append("><a href=\"javascript:;\" data-href=\"")
                     .append(hrel )
                     .append("\">")
                     .append(text )
                     .append("</a></li>");
            }
        }
    }
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
        <link rel="stylesheet" type="text/css" href="static/centre/css/base.min.css"/>
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
    <body class="toper-open">
        <div id="context">
            <%if (!"!HIDE".equals($hrel) && !"HIDE".equals($hrel)) {%>
            <div id="headbox">
                <div id="menu-context" data-load="centre/head.jsp" data-active="<%=$module+"/"+$entity+"/"%>" data-title="<%=$title%>"></div>
            </div>
            <%} /* End if */%>
            <div id="bodybox">
                <div id="main-context" class="container labs laps">
                    <div><div>
                        <%if ( $tabs.length() > 0 ) {%>
                        <ul class="nav nav-tabs tabs halt-title panel" data-toggle="hsTabs" data-target="+">
                            <%=$tabs%>
                        </ul>
                        <%} /* End if */%>
                        <div data-load="<%=$href%>"></div>
                    </div></div>
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
                    init: "<%=$module%>/<%=$entity%>/form_init.html"
                });
            })(jQuery);
        </script>
    </body>
</html>
