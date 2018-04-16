<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%!
    StringBuilder makeMenu(List<Map> list, String acti) {
        StringBuilder menus = new StringBuilder();
        for(Map menu : list) {
            String text = (String) menu.get("text");
            String href = (String) menu.get("href");
            String hrel = (String) menu.get("hrel");
//          String icon = (String) menu.get("icon");

            if (href.startsWith("!")) {
                continue;
            }
            if (href.startsWith("-")) {
                menus.append("<li class=\"divider\"></li>");
                continue;
            }

            String actc = href.equals(acti) ? "active" : "";

            List<Map> subs = (List) menu.get( "menus" );
            if (subs != null && ! subs.isEmpty()) {
                href  = Core.BASE_HREF +"/"+ href;
                hrel  = Core.BASE_HREF +"/"+ hrel;
                actc += " dropdown";
                hrel += "\" data-toggle=\"dropdown\""
                     +   " class=\"dropdown-toggle\""  ;
                text += "<span class=\"caret\"></span>";
                menus.append("<li class=\"")
                     .append(actc).append("\">" );
                menus.append( "<a href=\"" )
                     .append(href).append("\" data-href=\"")
                     .append(hrel).append("\">" )
                     .append(text).append("</a>");
                menus.append("<ul class=\"dropdown-menu\">")
                     .append(makeMenu(subs, acti))
                     .append("</ul>");
                menus.append("</li>");
            } else
            if (!href.startsWith("common/menu.")) {
                href  = Core.BASE_HREF +"/"+ href;
                hrel  = Core.BASE_HREF +"/"+ hrel;
                menus.append("<li class=\"")
                     .append(actc).append("\">" );
                menus.append( "<a href=\"" )
                     .append(href).append("\" data-href=\"")
                     .append(hrel).append("\">" )
                     .append(text).append("</a>");
                menus.append("</li>");
            }
        }
        return menus;
    }
%>
<%
    ActionHelper helper = (ActionHelper) Core.getInstance(ActionHelper.class);

    NaviMap curr = NaviMap.getInstance("centra");
    List    menu = curr.getMenuTranslated("common/menu.act?m=centra", 2);

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
    <a class="navbar-brand" style="font-size: 15px; font-weight: bolder;" href="<%=Core.BASE_HREF%>/centra/">
        <span style="color:#f00;" class="glyphicon glyphicon-fire"></span>
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
        </li>
    </ul>
</div>

<script type="text/javascript">
    (function($) {
        var context = $("#main-context");
        var menubar = $("#main-menubar");
//      var userbar = $("#user-menubar");

        $(function() {
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

        $("#sign-out")
            .click(function() {
                $.get(hsFixUri("centra/sign/delete.act"), function() {
                    location.assign(hsFixUri( "centra/login.html" ));
                });
            });
        $("#user-set")
            .click(function() {
                $.hsOpen("centra/manage/mime.html");
            });
        $("#note-msg")
            .click(function() {
                $.hsOpen("centra/manage/note.html");
            });
    })(jQuery);
</script>
