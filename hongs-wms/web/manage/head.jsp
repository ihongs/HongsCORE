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
            href = Core.BASE_HREF +"/"+ href;
            hrel = Core.BASE_HREF +"/"+ hrel;

            List<Map> subs = (List) menu.get( "menus" );
            if (subs != null && ! subs.isEmpty()) {
                actc += " dropdown";
                hrel += "\" data-toggle=\"dropdown\""  ;
                hrel +=  " class=\"dropdown-toggle\""  ;
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
            } else {
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

    NaviMap curr = NaviMap.getInstance("manage");
    List    menu = curr.getMenuTranslates(1 , 2);

    String  acti = helper.getParameter("active");
    String  user = (String) helper.getSessibute("uname");
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
    <a class="navbar-brand" style="font-size: 15px; font-weight: bolder;" href="<%=Core.BASE_HREF%>/manage/">
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
        <li class="headico">
            <a href="javascript:;">
                <span class="headimg" style="background-image:url(<%=head%>);"></span>
            </a>
        </li>
        <li class="dropdown">
            <a href="javascript:;" class="dropdown-toggle" data-toggle="dropdown">
                <%if (user != null) {%>
                <span><%=user%></span>
                <%} /* End If */%>
                <span class="badge"></span>
                <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu">
                <li><a href="javascript:;" id="note-msg"><%=CoreLocale.getInstance().translate("fore.notify")%></a></li>
                <li><a href="javascript:;" id="user-set"><%=CoreLocale.getInstance().translate("fore.modify")%></a></li>
                <li><a href="javascript:;" id="sign-out"><%=CoreLocale.getInstance().translate("fore.logout")%></a></li>
            </ul>
        </li>
    </ul>
</div><!-- /.navbar-collapse -->

<script type="text/javascript">
    (function($) {
        var menubar = $("#main-menubar");
        var context = $("#main-context");

        function initMenu (g) {
            var a, b, h, l;
            h = location.href.replace(/^\w+:\/\/[^\/]+/, '');
            a = menubar.find("a[href='"+ h +"']");
            b = menubar;
            if (a.size() < 1 && g) {
                a = menubar.find("li a").first( );
            }
            
            if (a.size() > 0) {
                h = a.attr("href");
                l = a.data("href");
                if (!l || l==='/') {
                    a = a.closest("li").find("li a").first();
                    if ( !h && g ) {
                        b.trigger("loseMenu");
                    } else {
                        location.replace( h );
                    }
                    return;
                }

                context.hsLoad(l);
                b.find("li").removeClass("active");
                a.parents("li").addClass("active");
            }
        }

        $(function() {
            var a = menubar.find( ".active" );
            if (a.size() > 0) {
                a.parents("li").addClass("active");
                return;
            }

            initMenu(true );
        });
        $(window).on("hashchange", function() {
            initMenu(false);
        });

        $("#sign-out")
            .click(function() {
                $.get(hsFixUri("manage/sign/delete.act"), function() {
                    location.assign(hsFixUri( "manage/login.html" ));
                });
            });
        $("#user-set")
            .click(function() {
                $.hsOpen("manage/global/mime.html");
            });
        $("#note-msg")
            .click(function() {
                $.hsOpen("manage/global/note.html");
            });
    })(jQuery);
</script>
