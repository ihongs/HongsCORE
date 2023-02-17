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
                    menu.append("<li class=\"").append(actc).append("\">")
                        .append( "<a title=\"").append(hint).append("\" ")
                        .append(     "href=\"").append(href).append("\" ")
                        .append("data-href=\"").append(hrel).append("\">")
                        .append("<span class=\"caret\"></span>")
                        .append( text )
                        .append("</a>")
                        .append("\r\n")
                        .append("<ul>")
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

    NaviMap curr = NaviMap.getInstance("centra");
    Set     role = curr.getRoleSet(   );
    if (null == role) {
            role = Synt.setOf("public");
    }
    List    menu = curr.getMenuTranslated("common/menu.act?m=centra", 2, role);

    String  acti = helper.getParameter("active");
    String  titl = helper.getParameter("title" );
    String  name = (String) helper.getSessibute("uname");
    String  head = (String) helper.getSessibute("uhead");

    if (head == null || head.isEmpty()) {
        head = "static/assets/img/head_icon.jpg";
    }
%>

<div id="head-handler">
    <a href="javascript:;"></a>
</div>
<div id="body-top-handler">
    <a href="javascript:;"></a>
</div>
<div id="body-bot-handler">
    <a href="javascript:;"></a>
</div>

<nav class="navbar navbar-default navbar-fixed-top" role="navigation" id="headbar">
    <div>
        <div class="navbar-header">
            <a class="navbar-brand" href="<%=Core.SERV_PATH%>/centra/"
               title="<%=CoreLocale.getInstance().translate("fore.centra.sub.title")%>">
                <span class="ulogo img" style="background-image:url(<%=CoreLocale.getInstance().translate("fore.centra.logo")%>);"></span>
                <%=CoreLocale.getInstance().translate("fore.centra.title")%>
            </a>
        </div>
        <div>
            <ol id="navi-menubar" class="navbar-left breadcrumb tabs laps halt-close" data-topple="hsTabs" data-target="#main-context">
                <li class="home-crumb active">
                    <a href="javascript:;">
                        <i class="bi bi-hi-path"></i>
                        <b class="title"><%=Synt.defxult(titl, "")%></b>
                    </a>
                </li>
            </ol>
            <ul id="user-menubar" class="navbar-right navbar-nav nav">
                <li class="dropdown">
                    <a href="javascript:;" class="dropdown-toggle" data-toggle="dropdown" aria-expanded="true">
                        <span class="uhead img" style="background-image:url(<%=head%>);" title="<%=name%>"></span>
                        <span class="badge"></span>
                        <span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="javascript:;" id="manage-morn">
                            <span class="bi bi-hi-morn"></span>
                            <%=CoreLocale.getInstance().translate("fore.manage.morn")%>
                        </a></li>
                        <li><a href="javascript:;" id="manage-mine">
                            <span class="bi bi-hi-mine"></span>
                            <%=CoreLocale.getInstance().translate("fore.manage.mine")%>
                        </a></li>
                        <li><a href="javascript:;" id="manage-mima">
                            <span class="bi bi-hi-mima"></span>
                            <%=CoreLocale.getInstance().translate("fore.manage.mima")%>
                        </a></li>
                        <li role="separator" class="divider"></li>
                        <li><a href="javascript:;" id="logout">
                            <span class="bi bi-hi-logout"></span>
                            <%=CoreLocale.getInstance().translate("fore.logout")%>
                        </a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div id="main-menubar">
    <ul>
<%=makeMenu(menu, acti)%>
    </ul>
</div>

<blockquote>
    <p>
        <span style="font-weight: 600;"><%=CoreLocale.getInstance().translate("fore.centra.title")%></span><br>
        <span style="font-weight: 400;"><%=CoreLocale.getInstance().translate("fore.centra.sub.title")%></span>
    </p>
    <p>
        <span>&copy;&nbsp;</span>
        <span class="copy-right"><%=CoreLocale.getInstance().translate("fore.copy.right")%></span>
        <br/>
        <span class="site-links"><%=CoreLocale.getInstance().translate("fore.site.links")%></span>
    </p>
    <p>Powered by <a href="<%=Core.SERV_PATH%>/power.html" target="_blank">HongsCORE</a></p>
</blockquote>

<script type="text/javascript">
    (function($) {
        var navibar = $("#navi-menubar");
        var menubar = $("#main-menubar");
        var menubox = $("#menu-context");
        var context = $("#main-context");
        var content = context.children( ).last( );

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

            b.find("li").removeClass("active")
                        .removeClass("acting");
            a.closest("li").addClass("active")
             .parents("li").addClass("acting");

            /**
             * 容器不存在或容器已预载,
             * 则无需重复载入内容页面;
             * 没有则最终转向首个链接.
             */
            if (content.size() === 0
            ||  content.data("load")
            || !content.is(":empty")) {
                return ;
            }

            h = menubar.find("a").attr("href");
            l = a.data("href");
            if (l && l != '/') {
                content .hsLoad(l);
            } else if ( !  l ) {
                location.assign(h);
                location.reload( ); // 规避只有 hash 变了
            }
        });

        $(function() {
            // 获取并设置当前模块标题
            var b, t;
            b = navibar.find(".home-crumb .title");
            t = b.text( );
            if (t) {  return;  }
            t = navibar.closest(".loadbox").data("title");
            if (t) {  b.text(t); return;  }
            t = menubar.find(".active > a").text();
            if (t) {  b.text(t); return;  }
            t = menubar.find(".acting > a").text();
            if (t) {  b.text(t); return;  }
        });

        $(window).on("hashchange" , function() {
            var a, b, h, l;

            h = location.href.replace(/^\w+:\/\/[^\/]+/, '');
            a = menubar .find("a[href='"+h+"']");
            b = menubar ;

            // 可能因参数、锚而找不到
            if (a.size() === 0) {
                h = h.replace( /#.*/, '');
                a = menubar.find("a[href='"+h+"']");
            if (a.size() === 0) {
                h = h.replace(/\?.*/, '');
                a = menubar.find("a[href='"+h+"']");
            }}

            b.find("li").removeClass("active")
                        .removeClass("acting");
            a.closest("li").addClass("active")
             .parents("li").addClass("acting");
        });

        // 标识导航组件已开启, 悬浮组件需避免层叠
        $(document.body).addClass("toper-open");
        $(document.body).addClass("sider-open");

        // 菜单折叠和展开
        menubar.find("li> ul").hide();
        menubar.find("li.acting> ul").toggle( );
        menubar.find("li.acting> a ").toggleClass("dropup");
        menubar.on("click", "a", function() {
            var la = $(this);
            var ul = la.siblings( "ul" );
            if (ul.size( ) ) {
                ul.slideToggle( "fast" );
                la.toggleClass("dropup");
                return false;
            }
        });

        // 定位到当前菜单
        var actived = menubar.find("li.active");
        if (actived.size()) {
            var at = actived.offset().top - menubox.offset().top;
            var ah = actived.prop("offsetHeight");
            var mh = menubox.prop("clientHeight");
            if (at + ah > mh) {
                menubox.scrollTop(at + ah / 2 - mh / 2);
            }
        }

        // 边栏隐藏与显示
        $("#head-handler")
        .insertAfter($("#headbox"))
        .click(function( ) {
            $( document.body ).toggleClass( "sider-open" ) ;
        });

        // 页面滚动到上下
        $("#body-bot-handler")
        .insertAfter($("#bodybox"))
        .click(function( ) {
            context.scrollTop(context.prop("scrollHeight"));
        });
        $("#body-top-handler")
        .insertAfter($("#bodybox"))
        .click(function( ) {
            context.scrollTop( 0 );
        });

        // 滚动及尺寸变化
        context.on("scroll resize", function() {
            var st = $(this).prop("scrollTop") || 0;
            var sh = $(this).prop("scrollHeight") || 0;
            var ch = $(this).prop("clientHeight") || 0;
            var sb = sh - ch - st;
            $("#body-top-handler").toggle(st > 20);
            $("#body-bot-handler").toggle(sb > 20);
        }); var ch = sh = 0;
        setInterval(function() {
            var eh = context.prop("clientHeight") || 0;
            var zh = context.prop("scrollHeight") || 0;
            if (ch !== eh || sh !== zh) {
                ch  =  eh;   sh  =  zh;
                context.trigger("resize");
            }
        }, 1000);

        // 回退复位滚动条
        context
            .on("hsHide", ">div", function (ev) {
                if (this !== ev.target) return ;
                if ($(this).data("top") === undefined) {
                    $(this).data("top", context.scrollTop());
                }
            })
            .on("hsShow", ">div", function (ev) {
                if (this !== ev.target) return ;
                if ($(this).data("top") !== undefined) {
                    context.scrollTop($(this).data( "top" ));
                    $(this).removeData( "top" );
                }
            });
        navibar
            .on("click" , ">li" , function (ev) {
                // 直接点击导航不要自动滚动
                var i = $(this).index ( );
                var l = $(this).parent( ).data("labs");
                if (l) l.children().eq(i).removeData ("top");
            });

        $("#logout")
            .click( function() {
                $.hsWarn(
                    "您确定要退出登录吗?\r\n" +
                    "点击“确定”将离开系统, 点击“取消”可继续使用. 感谢您的使用, 祝您工作顺利!",
                    function() {
                        $.get(hsFixUri("centra/sign/delete.act"), function() {
                            location.assign(hsFixUri( "centra/login.html" ));
                        });
                    },
                    function() {
                        // Nothing todo.
                    }
                );
            });
        $("#manage-mima")
            .click( function() {
                $.hsOpen("centra/manage/mima.html");
            });
        $("#manage-mine")
            .click( function() {
                $.hsOpen("centra/manage/mine.html");
            });
        $("#manage-morn")
            .click( function() {
                $.hsOpen("centra/manage/morn.html");
            });
    })(jQuery);
</script>
