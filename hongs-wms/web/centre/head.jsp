<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.List"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    StringBuilder makeMenu(List<Map> list, String acti) {
        StringBuilder menu = new StringBuilder();
        makeMenu(menu, list, acti);
        return menu;
    }
    int makeMenu(StringBuilder menu, List<Map> list, String acti) {
        int code  = 0;

        if (list != null) for (Map item: list) {
            String text = (String) item.get("text");
            String hint = (String) item.get("hint");
            String href = (String) item.get("href");
            String hrel = (String) item.get("hrel");
//          String icon = (String) item.get("icon");

            if (href == null
            ||  href.startsWith("!")) {
                continue;
            }
            if (hrel != null
            &&  hrel.startsWith("!")) {
                continue;
            }
            // 兼容旧版
            if ("NONE".equals(hrel)
            ||  "HIDE".equals(hrel) ) {
                continue;
            }

            if (text == null) text = "";
            if (hint == null) hint = "";

            String actc ;
            if (href.equals(acti)) {
                actc  = "active";
                code  = 2 ;
            } else
            if (code == 0) {
                actc  = "";
                code  = 1 ;
            } else
            {
                actc  = "";
            }

            List<Map> subs = (List) item.get("menus");
            StringBuilder subm = new StringBuilder( );
            switch ( makeMenu(subm, subs, acti) ) {
                case 2 :
                    code = 2 ;
                    actc = "acting" ;
                case 1 :
                    href = Core.SERV_PATH +"/"+ href;
                    hrel = Core.SERV_PATH +"/"+ hrel;
                    menu.append("<li class=\"").append(actc).append(" dropdown\">")
                        .append( "<a title=\"").append(hint).append("\" ")
                        .append(     "href=\"").append(href).append("\" ")
                        .append("data-href=\"").append(hrel).append("\" ")
                        .append("data-toggle=\"dropdown\" " )
                        .append("class=\"dropdown-toggle\">")
                        .append( text )
                        .append("<span class=\"caret\"></span>")
                        .append("</a>")
                        .append("\r\n")
                        .append("<ul class=\"dropdown-menu\" >")
                        .append("\r\n")
                        .append( subm )
                        .append("</ul>\r\n")
                        .append("</li>\r\n");
                    break;
                case 0 :
                    if (href.startsWith("common/menu.")) {
                        continue;
                    }
                    actc = "actual " + actc ;
                    href = Core.SERV_PATH +"/"+ href;
                    hrel = Core.SERV_PATH +"/"+ hrel;
                    menu.append("<li class=\"").append(actc).append("\">")
                        .append( "<a title=\"").append(hint).append("\" ")
                        .append(     "href=\"").append(href).append("\" ")
                        .append("data-href=\"").append(hrel).append("\">")
                        .append( text )
                        .append("</a>")
                        .append("</li>\r\n");
                    break;
            }
        }

        return  code;
    }
%>
<%
    ActionHelper helper = ActionHelper.getInstance();

    NaviMap curr = NaviMap.getInstance("centre");
    Set     role = curr.getRoleSet(   );
    if (null == role) {
            role = Synt.setOf("public");
    }
    List    menu = curr.getMenuTranslated("common/menu.act?m=centre", 2, role);

    String  acti = helper.getParameter("active");
    String  titl = helper.getParameter("title" );
    String  name = (String) helper.getSessibute("uname");
    String  head = (String) helper.getSessibute("uhead");

    if (head == null || head.isEmpty()) {
        head = "static/assets/img/head_icon.jpg";
    }
%>

<nav class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#main-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="<%=Core.SERV_PATH%>/centre/"
               title="<%=CoreLocale.getInstance().translate("fore.centre.sub.title")%>">
                <span class="ulogo img" style="background-image:url(<%=CoreLocale.getInstance().translate("fore.centre.logo")%>);"></span>
                <%=CoreLocale.getInstance().translate("fore.centre.title")%>
            </a>
        </div>
        <div class="collapse navbar-collapse" id="main-collapse">
            <ul class="nav navbar-nav navbar-left " id="main-menubar">
                <%=makeMenu(menu, acti)%>
            </ul>
            <ul class="nav navbar-nav navbar-right" id="user-menubar">
                <li class="dropdown">
                    <%if (role.contains("centre")) {%>
                    <a href="javascript:;" class="dropdown-toggle" data-toggle="dropdown">
                        <span class="uhead img" style="background-image:url(<%=head%>);" title="<%=name%>"></span>
                        <span class="badge"></span>
                        <span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="javascript:;" id="manage-morn"><%=CoreLocale.getInstance().translate("fore.manage.morn")%></a></li>
                        <li><a href="javascript:;" id="manage-mine"><%=CoreLocale.getInstance().translate("fore.manage.mine")%></a></li>
                        <li><a href="javascript:;" id="manage-mima"><%=CoreLocale.getInstance().translate("fore.manage.mima")%></a></li>
                        <li role="separator" class="divider"></li>
                        <li><a href="javascript:;" id="logout"><%=CoreLocale.getInstance().translate("fore.logout")%></a></li>
                    </ul>
                    <%} else {%>
                    <a href="javascript:;" id="login"><%=CoreLocale.getInstance().translate("fore.login")%></a>
                    <%} /*End If*/%>
                </li>
            </ul>
        </div>
    </div>
</nav>

<nav class="navbar navbar-default navbar-fixed-bottom container">
    <blockquote>
        <p class="clearfix">
            <span>&copy;&nbsp;</span><span class="copy-right"><%=CoreLocale.getInstance().translate("fore.copy.right")%></span>
            <span>&nbsp;&nbsp;</span><span class="site-links"><%=CoreLocale.getInstance().translate("fore.site.links")%></span>
            <span class="pull-right text-muted">Powered by <a href="<%=request.getContextPath()%>/power.html" target="_blank">HongsCORE</a></span>
        </p>
    </blockquote>
</nav>

<div class="container">
    <ol class="breadcrumb tabs laps home-bread hold-live-close hide-less-bread panel panel-default" id="navi-menubar" data-target="#main-context" data-topple="hsTabs">
        <li class="home-crumb active">
            <a href="javascript:;">
                <i class="bi bi-hi-path "></i>
                <b class="title"><%=Synt.defxult(titl, "")%></b>
            </a>
        </li>
    </ol>
</div>

<script type="text/javascript">
    (function($) {
        var navibar = $("#navi-menubar");
        var menubar = $("#main-menubar");
        var context = $("#main-context");
        var content = context.children().last();

        // 标识导航组件已开启, 悬浮组件需避免层叠
        $(document.body).addClass("toper-open");

        $(function() {
            if (menubar.find("li.active").size()) {
                return;
            }

            var a, b, h, l;

            h = menubar.closest(".loadbox").data("active")
            || location.href.replace(/^\w+:\/\/[^\/]+/,'');
            a = menubar.find("a[href='"+hsFixUri(h) +"']");
            b = menubar;

            // 可能因参数、锚而找不到
            if (a.size() === 0) {
                h = h.replace( /#.*/, '');
                a = menubar.find("a[href='"+h+"']");
            if (a.size() === 0) {
                h = h.replace(/\?.*/, '');
                a = menubar.find("a[href='"+h+"']");
            }}

            b.find("li").removeClass("active");
            a.parents("li").addClass("active");

            /**
             * 容器不存在或容器已预载,
             * 则无需重复载入内容页面;
             * 没有则最终转向首个链接.
             */
            if (content .size() === 0
            ||  content .data("load")
            ||  content .children().size()) {
                return;
            }

            h = menubar.find("a").attr("href"); // 首页
            l = a.data("href");
            if (l && l != '/') {
                content .hsLoad(l);
            } else if ( !  l ) {
                location.assign(h);
            // 规避只有 hash 不同
            if (h.indexOf('#') != -1) {
                location.reload( );
            }}
        });

        $(function() {
            // 获取并设置当前模块标题
            var b, t;
            b = navibar.find(".home-crumb .title");
            t = b.text( );
            if (t) { return; }
            t = navibar.closest(".loadbox").data("title") ;
            if (t) { b.text(t); document.title = t; return; }
            t = menubar.find(".active > a").text();
            if (t) { b.text(t); document.title = t; return; }
            t = menubar.find(".acting > a").text();
            if (t) { b.text(t); document.title = t; return; }
        });

        // 返回联动导航条
        $(window).on("popstate", function(ev) {
            var ov = ev.originalEvent;
            var li = navibar.find ( "li.active" );
            if (ov && ov.state && ov.state.crumb) {
                if (! li.is(".home-crumb")
                &&  ! li.is(".host-crumb")
                &&  ! li.is(".hold-crumb")) {
                    li.hsClose();
                }
                history.pushState({crumb: true}, null, null);
            }
        });
        history.pushState({crumb: true}, null, null);
        history.pushState({crumb: true}, null, null);

        // 新开导航滚到底
        context.on("hsReady", ">*>.openbox", function(ev) {
            if (ev.target == this) {
                navibar.scrollLeft(navibar.prop("scrollWidth" ) || 0);
            //  navibar.scrollTop (navibar.prop("scrollHeight") || 0);
            }
        });

        $("#login" )
            .click(function() {
                var r = location.pathname + location.search + location.hash;
                    r = r.substr(<%=Core.SERV_PATH.length() + 1 %>);
                    r = encodeURIComponent(r);
                location.assign(hsFixUri("centre/login.html?r="+r));
            });
        $("#logout")
            .click(function() {
                $.get(hsFixUri("centre/sign/delete.act"), function() {
                    location.reload();
                });
            });
        $("#manage-mima")
            .click(function() {
                $.hsOpen("centre/manage/mima.html");
            });
        $("#manage-mine")
            .click(function() {
                $.hsOpen("centre/manage/mine.html");
            });
        $("#manage-morn")
            .click(function() {
                $.hsOpen("centre/manage/morn.html");
            });
    })(jQuery);
</script>
