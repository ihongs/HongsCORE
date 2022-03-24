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
    String  name = (String) helper.getSessibute("uname");
    String  head = (String) helper.getSessibute("uhead");

    if (head != null && !"".equals(head)) {
        head = head.replaceFirst("(_[^_]+)?\\.[^\\.]+$", "_sm.png");
    } else {
        head = "static/assets/img/head_icon_sm.jpg";
    }
%>

<div id="head-handler">
    <a href="javascript:;"></a>
</div>
<div id="body-handler">
    <a href="javascript:;"></a>
</div>

<div id="main-namebar">
    <div><a href="<%=Core.SERV_PATH%>/centra/">
         <%=CoreLocale.getInstance().translate("fore.centra.title")%></a></div>
    <div><%=CoreLocale.getInstance().translate("fore.centra.sub.title")%></div>
</div>

<hr />

<div id="user-menubar">
    <a href="javascript:;" style="display: block;">
        <div class="uhead img" style="background-image: url(<%=head%>);"></div>
        <div class="uname" title="<%=name%>"><%=name%></div>
        <div class="caret"></div>
        <div class="badge"></div>
    </a>
    <ul>
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
        <li><a href="javascript:;" id="logout">
            <span class="bi bi-hi-logout"></span>
            <%=CoreLocale.getInstance().translate("fore.logout")%>
        </a></li>
    </ul>
</div>

<hr />

<div id="main-menubar">
    <ul>
<%=makeMenu(menu, acti)%>
    </ul>
</div>

<hr />

<blockquote>
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
        var context = $("#main-context");
        var menubar = $("#main-menubar");
        var userbar = $("#user-menubar");
        var menubox = $("#menu-context");
        var namebar = $("#main-namebar");

        $(function() {
            if (menubar.find("li.active").size()) {
                return;
            }

            var a, b, h, l;

            h = menubar.closest(".loadbox").data("active")
            || location.href.replace(/^\w+:\/\/[^\/]+/,'');
            a = menubar.find("a[href='"+hsFixUri(h) +"']");
            h = menubar.find("a").attr("href");
            b = menubar;

            b.find("li").removeClass("active")
                        .removeClass("acting");
            a.closest("li").addClass("active")
             .parents("li").addClass("acting");

            /**
             * 容器不存在或容器已预载,
             * 则无需重复载入内容页面;
             * 没有则最终转向首个链接.
             */
            if (context.size() === 0
            ||  context.data("load")
            ||  context.children().size()) {
                return ;
            }

            l = a.data ("href");
            if (l && l !== '/') {
                context .hsLoad(l);
            } else {
                location.assign(h);
            }
        });

        $(window).on("hashchange" , function() {
            var a, b, h, l;

            h = location.href.replace(/^\w+:\/\/[^\/]+/, '');
            a = menubar .find("a[href='"+h+"']");
            b = menubar ;

            b.find("li").removeClass("active")
                        .removeClass("acting");
            a.closest("li").addClass("active")
             .parents("li").addClass("acting");

            l = a.data ("href");
            if (l && l !== '/') {
                context .hsLoad(l);
            } else {
                location.assign(h);
            }
        });

        // 页面滚动回顶部
        $("#body-handler")
        .insertAfter($("#bodybox"))
        .click(function( ) {
            context.scrollTop( 0 );
        });

        // 边栏隐藏与显示
        $("#head-handler")
        .insertAfter($("#headbox"))
        .click(function( ) {
            $(document.body).toggleClass("sider-open");
        }); $(document.body).   addClass("sider-open");

        // 菜单折叠和展开
        userbar.children("ul").hide();
        menubar.find("li> ul").hide();
        menubar.find("li.acting> ul").toggle( );
        menubar.find("li.acting> a ").toggleClass("dropup");
        $().add(menubar).add(userbar)
           .on ("click", "a", function() {
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
        if (actived.size() && actived.offset().top + actived.height() > $( window ).height()) {
            menubox.scrollTop(actived.offset().top - actived.height() * 2 - namebar.height());
        }

        // 回退复位滚动条
        context
            .on("hsRetir", ">.labs.laps>div", function ( ) {
                if ($(this).data("top") === undefined) {
                    $(this).data("top", context.scrollTop());
                }
            })
            .on("hsRecur", ">.labs.laps>div", function ( ) {
                if ($(this).data("top") !== undefined) {
                    context.scrollTop( $(this).data("top") );
                    $(this).removeData ( "top" );
                }
            })
            .on( "click" , ">.tabs.laps>li" , function ( ) {
                if ($(this).is(".dont-close,.dont-crumb")) {
                    return;
                }
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
