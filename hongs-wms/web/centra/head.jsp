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
        int code = 0;

        for(Map item: list) {
            String text = (String) item.get("text");
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

            List<Map> subs = ( List ) item.get ("menus");
            if (subs != null && ! subs.isEmpty ( /***/ )) {
                StringBuilder subm = new StringBuilder();
                switch ( makeMenu ( subm , subs , acti )) {
                    case 0 : continue ;
                    case 2 : code = 2 ; actc = "acting" ;
                }

                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;
                menu.append("<li class=\"").append(actc).append("\">")
                    .append(  "<a href=\"").append(href).append("\" ")
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
            } else
            if (!href.startsWith("common/menu.")) {
                actc = "actual " + actc ;
                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;
                menu.append("<li class=\"").append(actc).append("\">")
                    .append(  "<a href=\"").append(href).append("\" ")
                    .append("data-href=\"").append(hrel).append("\">")
                    .append( text )
                    .append("</a>")
                    .append("</li>\r\n");
            }
        }

        return  code;
    }
%>
<%
    ActionHelper helper = (ActionHelper) Core.getInstance(ActionHelper.class);

    NaviMap curr = NaviMap.getInstance("centra");
    Set     role = curr.getRoleSet();
    if (role == null)role = Synt.setOf("public");
    List    menu = curr.getMenuTranslated("common/menu.act?m=centra",2, role);

    String  acti = helper.getParameter("active");
    String  name = (String) helper.getSessibute("uname");
    String  head = (String) helper.getSessibute("uhead");

    if (head != null && !"".equals(head)) {
        head = head.replaceFirst("(_[^_]+)?\\.[^\\.]+$", "_sm.png");
    } else {
        head = "static/assets/img/head_icon_sm.jpg";
    }
%>

<div id="main-menubar">
    <ul>
<%=makeMenu(menu, acti)%>
    </ul>
</div>

<div id="user-menubar">
    <ul>
        <li>
            <a href="javascript:;" id="user-set">
                <span class="glyphicon glyphicon-user"></span><%=CoreLocale.getInstance().translate("fore.modify")%>
            </a>
        </li>
        <li>
            <a href="javascript:;" id="sign-out">
                <span class="glyphicon glyphicon-off "></span><%=CoreLocale.getInstance().translate("fore.logout")%>
            </a>
        </li>
    </ul>
    <a href="javascript:;" class="dropup" style="display: block;">
        <div class="caret"></div>
        <div class="badge"></div>
        <div class="uhead" style="background-image: url(<%=head%>)"></div>
        <div class="uname" title="<%=name%>"><%=name%></div>
    </a>
</div>

<div id="main-namebar">
    <div><%=CoreLocale.getInstance().translate("fore.centra.title"    )%></div>
    <div><%=CoreLocale.getInstance().translate("fore.centra.sub.title")%></div>
</div>

<div id="head-powered">
    <p>
        <span>&copy;&nbsp;</span><span><%=CoreLocale.getInstance().translate("fore.copy.right")%></span>
    <!--<span>&nbsp;&nbsp;</span><span><%=CoreLocale.getInstance().translate("fore.site.links")%></span>-->
        <span>&nbsp;&nbsp;</span><span>Powered by <a href="<%=Core.BASE_HREF%>/power.html" target="_blank">HongsCORE</a></span>
    </p>
</div>

<div id="head-handler">
    <a href="javascript:;"></a>
</div>

<script type="text/javascript">
    (function($) {
        var context = $("#main-context");
        var menubar = $("#main-menubar");
        var userbar = $("#user-menubar");

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
            if (context .size() === 0
            ||  context .data("load")
            ||  context .children().size()) {
                return;
            }

            l = a.data("href");
            if (l && l != '/') {
                context .hsLoad(l);
            } else if ( !  l ) {
                location.assign(h);
            }
        });

        $(window).on("hashchange" , function() {
            var a, b, h, l;

            h = location.href.replace(/^\w+:\/\/[^\/]+/, '');
            a = menubar .find("a[href='"+h+"']");
            b = menubar ;

            b.find("li").removeClass( "acting" );
            b.find("li").removeClass( "active" );
            a.parents("li").addClass( "acting" );
            a.closest("li").addClass( "active" );

            l = a.data("href");
            if (l && l != '/') {
                context .hsLoad(l);
            } else {
                location.assign(h);
            }
        });

        $("#sign-out")
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
            } );
        $("#user-set")
            .click( function() {
                $.hsOpen("centra/manage/mine.html");
            } );
        $("#note-msg")
            .click( function() {
                $.hsOpen("centra/manage/note.html");
            } );

        // 菜单折叠和展开
        userbar.children("ul").hide();
        menubar.find("li> ul").hide();
        menubar.find("li.acting> ul").show();
        menubar.find("li.acting> a ").addClass("dropup");
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

        // 边栏隐藏与显示
        $("#head-handler").click(function() {
            $(document.body).toggleClass("sider-open");
        }); $(document.body).   addClass("sider-open");
    })(jQuery);
</script>
