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
    StringBuilder makeMenu(List<Map> list, String path) {
        // 针对 data 的特殊逻辑
        Pattern patt = Pattern.compile("^manage/data/[^/]+$");
        String  pret ;
        if (patt.matcher(path).matches()) {
            pret = "manage/data/#";
        } else {
            pret = path + "/#";
        }

        StringBuilder menus = new StringBuilder();
        for(Map menu : list) {
            String disp = (String) menu.get("disp");
            String href = (String) menu.get("href");
            String hrel = (String) menu.get("hrel");
//          String icon = (String) menu.get("icon");
            String acti = "";

            if (disp.equals/**/(  ""  )
            ||  href.startsWith( "!" )) {
                continue;
            }
            if (href.equals(path +"/")) {
                acti = "class=\"active\"";
            }
            if (href.startsWith( pret)) {
                href = path+"/#"+href.substring(pret.length());
                hrel = Core.BASE_HREF +"/"+ hrel;
            } else {
                href = Core.BASE_HREF +"/"+ href;
                hrel = "";
            }

            menus.append("<li "+ acti +">")
                 .append("<a data-href=\"")
                 .append(hrel)
                 .append("\" href=\"")
                 .append(href)
                 .append("\">"  )
                 .append(disp)
                 .append("</a>" )
                 .append("</li>");
        }
        return menus;
    }
%>
<%
    ActionHelper helper = (ActionHelper) Core.getInstance(ActionHelper.class);
    String w = helper.getParameter("w");
    if (w == null || "".equals(w)) {
        w = "manage";
    }
    String m = helper.getParameter("n");
    if (m == null || "".equals(m)) {
        m =  w;
    }
    String u = helper.getParameter("u");
    if (u == null || "".equals(u)) {
        u =  m;
    }

    NaviMap main = NaviMap.getInstance(w);
    NaviMap curr = NaviMap.getInstance(m);
    List<Map> mainMenu = main.getMenuTranslated();
    List<Map> currMenu = curr.getMenuTranslates();

    String  user = (String ) helper.getSessibute("uname");
    String  head = (String ) helper.getSessibute("uhead");
    Integer msgc = (Integer) helper.getSessibute( "msgc");
    String  msgs = msgc == null ? null : (msgc > 9 ? "9+" : Integer.toString(msgc));

    if (head != null && !"".equals(head)) {
        head = head.replaceFirst("\\.[^\\.]+$","_sm.jpg");
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
    <a class="navbar-brand" style="font-weight: bolder;" href="<%=request.getContextPath()%>/manage/">
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
    <ul class="nav navbar-nav navbar-left " id="curr-menubar">
        <%=makeMenu(currMenu, u)%>
    </ul>
    <ul class="nav navbar-nav navbar-right" id="main-menubar">
        <li class="headico">
            <a href="javascript:;" data-toggle="hsOpen" data-href="manage/global/mime.html">
                <span class="headimg" style="background-image:url(<%=head%>);"></span>
            </a>
        </li>
        <li class="dropdown">
            <a href="javascript:;" class="dropdown-toggle" data-toggle="dropdown">
                <%if (user != null) {%>
                <span <%/*uname*/%>><%=user%></span>
                <%} /* End If */%>
                <%if (msgs != null) {%>
                <span class="badge"><%=msgs%></span>
                <%} /* End If */%>
                <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu">
                <%=makeMenu(mainMenu, u)%>
                <%if (user != null) {%>
                <li class="divider"></li>
                <li><a href="javascript:;" id="sign-out"><%=CoreLocale.getInstance().translate("fore.logout")%></a></li>
                <%} /* End If */%>
            </ul>
        </li>
    </ul>
</div><!-- /.navbar-collapse -->

<script type="text/javascript">
    (function($) {
        $("#curr-menubar>li>a")
            .filter(function() {
                return !! $(this).attr("data-href");
            })
            .click(function() {
                var h  =  $(this).attr("data-href");
                var p  =  $(this).attr("data-hreq");
                if (p) {
                    $(this).removeAttr("data-hreq");
                    if (h.index('?') != -1 ) {
                        h += '?' + p;
                    } else {
                        h += '&' + p;
                    }
                }
                $("#main-context").hsLoad(h);
                $(this).closest("li").addClass("active")
                       .siblings().removeClass("active");
            });
        $("#main-menubar>li>a")
            .click(function() {
                var that = $(this);
                setTimeout(function() {
                    that.parent( ).removeClass("active");
                    that.blur  ( );
                }, 100);
            });
        $("#sign-out")
            .click(function() {
                $.get(hsFixUri("manage/sign/delete.act"), function() {
                    location.href = hsFixUri("manage/login.html");
                });
            });

        $(function() {
            if ($("#curr-menubar .active").size()) {
                return;
            }
            if ($("#curr-menubar li").size() == 0) {
                $( document ).trigger ( "noMenu" );
                return;
            }
            // Click the first available menu item
            var a;
            if (location.hash) {
                // #def&x=1&y=2
                var h = location.hash ;
                var p = h.indexOf('&');
                h = "<%=u%>/" + h;
                p = p != -1 ? h.substring(p + 1) : "" ;
                a = $("#curr-menubar a[href='"+h+"']");
                a.attr("data-hreq", p);
            } else {
                a = $("#curr-menubar a").first();
            }
            if (a.size() == 0) {
                a = $("#main-menubar ul.dropdown-menu a").first();
            }
            a.click();
        });
    })(jQuery);
</script>
