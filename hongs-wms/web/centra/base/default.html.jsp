<%@page import="io.github.ihongs.action.FormSet"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    // 仅开放接口则抛出资源缺失异常
    if ($hrel != null && $hrel.startsWith("!")) {
        throw new CruxException(404, $locale.translate("core.error.no.thing"));
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

    // 表单是否存在
    Boolean $form = false;
    for (String n : new String [ ] {$module +"/"+ $entity , $module} ) {
        try {
            FormSet.getInstance(n).getForm( $entity );
            $form = true ;
            break ;
        } catch (CruxException e ) {
            if (910!=e.getErrno( )
            &&  912!=e.getErrno()) {
                throw  e ; // 非表单缺失
            }
        }
    }
%>
<!doctype html>
<html>
    <head>
        <title><%=$title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/common.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/centra.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=$module%>/<%=$entity%>/defines.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/common.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centra.js" ></script>
        <script type="text/javascript" src="<%=$module%>/<%=$entity%>/defines.js"></script>
    </head>
    <body class="sider-open toper-open">
        <div id="context">
            <div id="headbox">
                <div id="menu-context" data-load="centra/head.jsp" data-active="<%=$module+"/"+$entity+"/"%>" data-title="<%=$title%>"></div>
            </div>
            <div id="bodybox">
                <div id="main-context" class="container-fluid labs laps">
                    <div>
                        <ul class="nav nav-tabs tabs hide-less-bread hide-icon-after board" data-toggle="hsTabs" data-target="+">
                        <%if ( $tabs.length() > 0 ) {%>
                            <%=$tabs%>
                        <%} else {%>
                            <li class="active">
                                <a href="javascript:;" data-href="<%=$href%>">
                                    <span class="title"><%=$locale.translate("fore.entire.title", $title)%></span>
                                </a>
                            </li>
                        <%} /* End if */%>
                        <%if ( $form ) {%>
                            <li class="pull-right" data-tab="swap">
                                <a href="javascript:;" data-href="<%=$module+"/"+$entity+"/swap.html"%>" title="<%=$locale.translate("fore.manual.title", $title)%>">
                                    <i class="bi bi-hi-manual"></i>
                                    <span class="title"><%=$locale.translate("fore.manual.title", $title)%></span>
                                </a>
                            </li>
                            <li class="pull-right" data-tab="snap">
                                <a href="javascript:;" data-href="<%=$module+"/"+$entity+"/snap.html"%>" title="<%=$locale.translate("fore.record.title", $title)%>">
                                    <i class="bi bi-hi-reveal"></i>
                                    <span class="title"><%=$locale.translate("fore.record.title", $title)%></span>
                                </a>
                            </li>
                        <%} /* End if */%>
                        </ul>
                        <div data-load="<%=$href%>"></div>
                    </div>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            (function($) {
                var context = $("#main-context>:last");

                // 权限检查
                if (! H$('!<%=$module%>/<%=$entity%>/select.act')) {
                    context.find(">ul>li[data-tab=swap]").remove();
                }
                if (! H$('!<%=$module%>/<%=$entity%>/reveal.act')) {
                    context.find(">ul>li[data-tab=snap]").remove();
                }
                context.find(">ul" ).trigger("hsStab");

                // 外部定制
                window["<%=$func%>"] && window["<%=$func%>"](context);
            })(jQuery);
        </script>
    </body>
</html>
