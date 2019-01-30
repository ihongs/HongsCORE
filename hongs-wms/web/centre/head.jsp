<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.List"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="io.github.ihongs.Cnst"%>
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

            if (href.startsWith("!")) {
                continue;
            }
            if (hrel.equals("HIDE" )) {
                continue;
            }
            if (hrel.equals("LINE" )) {
                menu.append("<li class=\"divider\"></li>");
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
                    case 2 : code = 2 ; actc = "active" ;
                }

                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;
                menu.append("<li class=\"").append(actc).append(" dropdown\">")
                    .append(  "<a href=\"").append(href).append("\" ")
                    .append("data-href=\"").append(hrel).append("\" ")
                    .append("data-toggle=\"dropdown\" " )
                    .append("class=\"dropdown-toggle\">")
                    .append(text)
                    .append("<span class=\"caret\"></span>")
                    .append("</a>" )
                    .append("<ul class=\"dropdown-menu\">" )
                    .append(subm)
                    .append("</ul>")
                    .append("</li>");
            } else
            if (!href.startsWith("common/menu.")) {
                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;
                menu.append("<li class=\"").append(actc).append("\">")
                    .append(  "<a href=\"").append(href).append("\" ")
                    .append("data-href=\"").append(hrel).append("\">")
                    .append(text)
                    .append("</a>" )
                    .append("</li>");
            }
        }

        return  code;
    }
%>
<%
    ActionHelper helper = (ActionHelper) Core.getInstance(ActionHelper.class);

    NaviMap curr = NaviMap.getInstance("centre");
    Set     role = curr.getRoleSet();
    if (role == null)role = Synt.setOf("public");
    List    menu = curr.getMenuTranslated("common/menu.act?m=centre",2, role);

    String  acti = helper.getParameter("active");
    String  name = (String) helper.getSessibute("uname");
    String  head = (String) helper.getSessibute("uhead");

    if (head != null && !"".equals(head)) {
        head = head.replaceFirst("(_[^_]+)?\\.[^\\.]+$", "_sm.png");
    } else {
        head = "static/assets/img/head_icon_sm.jpg";
    }
%>

<div class="navbar-header">
    <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#main-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
    </button>
    <a href="<%=Core.BASE_HREF%>/centre/" class="navbar-brand" style="font-size: 16px;">
        <span><img src="<%=Core.BASE_HREF%>/favicon.gif" style="border-radius: 3px; margin-top: -3px;"/></span>
        <span style="color:#833">H</span>
        <span style="color:#722">o</span>
        <span style="color:#611">n</span>
        <span style="color:#722">g</span>
        <span style="color:#833">s</span>
        <span style="color:#eaa">C</span>
        <span style="color:#ebb">O</span>
        <span style="color:#fcc">R</span>
        <span style="color:#fdd">E</span>
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
                <span class="uhead" style="background-image:url(<%=head%>);" title="<%=name%>"></span>
                <span class="badge"></span>
                <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu">
                <li><a href="javascript:;" id="user-set"><%=CoreLocale.getInstance().translate("fore.modify")%></a></li>
                <li role="separator" class="divider"></li>
                <li><a href="javascript:;" id="sign-out"><%=CoreLocale.getInstance().translate("fore.logout")%></a></li>
            </ul>
            <%} else {%>
            <a href="javascript:;" id="sign-in"><%=CoreLocale.getInstance().translate("fore.login")%></a>
            <%} /*End If*/%>
        </li>
    </ul>
</div>

<script type="text/javascript">
    (function($) {
        var context = $("#main-context");
        var menubar = $("#main-menubar");
//      var userbar = $("#user-menubar");

        $(function() {
            if (menubar.find("li.active").size()) {
                return;
            }

            var a, b, h, l;

            h = location.href.replace(/^\w+:\/\/[^\/]+/, '');
            a = menubar .find("a[href='"+h+"']");
            h = menubar .find("a").attr("href" );
            b = menubar ;

            b.find("li").removeClass( "active" );
            a.parents("li").addClass( "active" );

            /**
             * 容器不存在或容器已预载,
             * 则无需重复载入内容页面;
             * 没有则最终转向首个链接.
             */
            if (context .size() === 0
            ||  context .data("load")) {
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

            b.find("li").removeClass( "active" );
            a.parents("li").addClass( "active" );

            l = a.data("href");
            if (l && l != '/') {
                context .hsLoad(l);
            } else {
                location.assign(h);
            }
        });

        /*
        var badge = userbar.find(".badge" );
        var timer = setInterval (function() {
            if (! badge.is(":visible")) {
                clearInterval( timer );
                return;
            }
            $.ajax({
                url     : hsFixUri("centre/medium/suggest/search.act?unit=message"),
                type    : "GET" ,
                dataType: 'JSON',
                complete: function(rst) {
                    badge.text(rst.size ? rst.size : "");
                }
            });
        }, 10000);
        */

        $("#sign-in" )
            .click(function() {
                var r = location.pathname + location.search + location.hash;
                <%if ( Core.BASE_HREF.length( ) != 0 ) {%>
                    r = r.substring( <%=Core.BASE_HREF.length()%> );
                <%} /*End if */%>
                    r = encodeURIComponent( r );
                location.assign(hsFixUri("centre/login.html?r="+r));
            });
        $("#sign-out")
            .click(function() {
                $.get(hsFixUri("centre/sign/delete.act"), function() {
                    location.reload();
                });
            });
        $("#user-set")
            .click(function() {
                $.hsOpen("centre/manage/mime.html");
            });
        $("#note-msg")
            .click(function() {
                $.hsOpen("centre/manage/note.html");
            });
    })(jQuery);
</script>
