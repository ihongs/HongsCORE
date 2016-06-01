<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.db.Model"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.serv.medium.ABaseModel"%>
<%@page import="app.hongs.db.DB"%>
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
        StringBuilder menus = new StringBuilder();
        for(Map menu : list) {
            String disp = (String) menu.get("disp");
            String href = (String) menu.get("href");
//          String hrel = (String) menu.get("hrel");
//          String icon = (String) menu.get("icon");
            String acti = "";

            if (disp.equals/**/( "" )
            ||  href.startsWith("!")) {
                continue;
            }
            if (href.equals/**/(path)
            ||  href.startsWith(path +"/")) {
                acti = "class=\"active\"";
            }
            if (! href.matches("^http://")) {
                href = Core.BASE_HREF +"/"+ href;
            }

            menus.append("<li "+ acti +">")
                 .append("<a href=\"")
                 .append(href)
                 .append("\">"  )
                 .append(disp)
                 .append("</a>" )
                 .append("</li>");
        }
        return menus;
    }
    List<Map> getSubSecs(Model mod, String sid) throws HongsException {
        return mod.table.fetchCase()
                .where  ("pid = ? AND state > ?", sid, 0)
                .orderBy("seria")
                .select ("id, name")
                .all    ();
    }
%>
<%
    String n = request.getParameter("n");
    if (n == null || "".equals(n)) {
        n  = "handle";
    }
    String u = request.getParameter("u");
    if (u == null || "".equals(u)) {
        u  = "";
    }

    NaviMap   curr = NaviMap.getInstance( n );
    List<Map> menu = curr.getMenuTranslates();
    String    root = request.getContextPath();
    
    String    wd = (String ) request.getParameter( "wd"  );
    String  name = (String ) session.getAttribute("uname");
    String  head = (String ) session.getAttribute("uhead");
    Integer msgc = (Integer) session.getAttribute( "msgc");
    String  msgs = msgc == null ? null : (msgc > 9 ? "9+" : Integer.toString(msgc));

    if ( wd  == null || "".equals( wd )) {
         wd  = "";
    }
    if (name == null || "".equals(name)) {
        name = "请登录";
    }
    if (head == null || "".equals(head)) {
        head = "static/assets/img/head_icon_sm.jpg";
    } else {
        head =  head.replaceFirst("\\.[^\\.]+$", "_sm.jpg");
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
        <span style="color:#f00;" class="glyphicon glyphicon-fire"></span><span style="color:#833">H</span><span style="color:#722">o</span><span style="color:#611">n</span><span style="color:#722">g</span><span style="color:#833">s</span><span style="color:#eaa">C</span><span style="color:#ebb">O</span><span style="color:#fcc">R</span><span style="color:#fdd">E</span>
    </a>
</div>

<div class="collapse navbar-collapse" id="main-collapse">
    <ul class="nav navbar-nav navbar-left " id="curr-menubar">
        <%=makeMenu(menu, u)%>
    </ul>
    <form class="navbar-form navbar-left" role="search"
          method="GET" action="<%=root%>/medium/section/0?md=1">
        <div class="form-group">
            <input type="search" name="wd" value="<%=wd%>" class="form-control">
        </div>
        <button type="submit" class="btn btn-default glyphicon glyphicon-search"></button>
    </form>
    <ul class="nav navbar-nav navbar-right" id="main-menubar">
        <%if (session.getAttribute(Cnst.UID_SES) == null) {%>
        <li class="headico">
            <a href="javascript:;" id="sign-in">
                <span class="headimg" style="background-image: url(<%=root%>/<%=head%>);" title="<%=name%>"></span>
            </a>
        </li>
        <%} else {%>
        <li class="dropdown">
            <a href="javascript:;" class="dropdown-toggle" data-toggle="dropdown">
                <span class="headimg" style="background-image: url(<%=root%>/<%=head%>);" title="<%=name%>"></span>
                <%if (msgs != null) {%>
                <span class="badge"><%=msgs%></span>
                <%} /* End If */%>
                <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu">
                <li><a href="medium/action/self/">我的文章</a></li>
                <li class="divider"></li>
                <li><a href="javascript:;" id="mine-inf">修改密码</a></li>
                <li><a href="javascript:;" id="sign-out">退出登录</a></li>
            </ul>
        </li>
        <%} /* End If */%>
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
                $.get(hsFixUri("handle/sign/delete.act"), function() {
                    location.reload();
                });
            });
        $("#mine-inf" )
            .click(function() {
                $.hsOpen('<%=root%>/handle/global/mine.html');
            });
        $("#sign-in" )
            .click(function() {
                $.hsOpen('<%=root%>/handle/global/sign.html');
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
