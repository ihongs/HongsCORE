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
        StringBuilder topm = new StringBuilder();
        StringBuilder subm ;
        for(Map menu: list) {
            String text = (String) menu.get("text");
            String href = (String) menu.get("href");
            String hrel = (String) menu.get("hrel");
//          String icon = (String) menu.get("icon");

            if (href.startsWith("!")) {
                continue;
            }
            if (hrel.equals("HIDE" )) {
                continue;
            }
            if (hrel.equals("LINE" )) {
                topm.append("<li class=\"divider\"></li>" );
                continue;
            }

            String actc = href.equals(acti) ? "active" : "";
            List<Map> subs = (List) menu.get( "menus" );

            if (subs != null && !subs.isEmpty()) {
                subm  = makeMenu(subs,acti);
                if (0 > subm.length()) {
                    continue;
                }

                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;
                text += "<span class=\"caret\"></span>";

                topm.append("<li class=\"")
                    .append(actc).append("\">" );
                topm.append( "<a href=\"" )
                    .append(href).append("\" data-href=\"")
                    .append(hrel).append("\">" )
                    .append(text).append("</a>");
                topm.append("<ul class=\"\">")
                    .append(subm)
                    .append("</ul>");
                topm.append("</li>");
            } else
            if (!href.startsWith("common/menu.")) {
                href = Core.BASE_HREF +"/"+ href;
                hrel = Core.BASE_HREF +"/"+ hrel;

                topm.append("<li class=\"")
                    .append(actc).append("\">" );
                topm.append( "<a href=\"" )
                    .append(href).append("\" data-href=\"")
                    .append(hrel).append("\">" )
                    .append(text).append("</a>");
                topm.append("</li>");
            }
        }

        return  topm;
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

<div style="margin-bottom: 26px; position: relative;">
    <a href="<%=Core.BASE_HREF%>/centra/" class="btn">
        <span><img src="<%=Core.BASE_HREF%>/favicon.gif" style="border-radius: 4px; margin-top: -2px;"/></span>
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
    <a href="javascript:;" class="btn" id="menu-laq">&laquo;</a>
    <a href="javascript:;" class="btn" id="menu-raq">&raquo;</a>
</div>

<div class="menubox clearfix">
    <ul id="main-menubar">
        <%=makeMenu(menu, acti)%>
    </ul>
</div>

<hr style="margin-top: 18px; border-top: 1px dashed #666;"/>

<div class="menubox clearfix">
    <ul id="user-menubar">
        <li><a href="javascript:;" id="user-set"><span class="glyphicon glyphicon-user"   ></span>&nbsp;<%=CoreLocale.getInstance().translate("fore.modify") %></a></li>
        <li><a href="javascript:;" id="sign-out"><span class="glyphicon glyphicon-log-out"></span>&nbsp;<%=CoreLocale.getInstance().translate("fore.logout") %></a></li>
    </ul>
</div>

<p style="color: #666; font-size: 11px; margin: 13px 15px;">
    Powered by <a href="<%=Core.BASE_HREF%>/power.html" target="_blank" style="color: #844;">HongsCORE</a>
</p>

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

            b.find("li").removeClass( "acting" );
            b.find("li").removeClass( "active" );
            a.parents("li").addClass( "acting" );
            a.closest("li").addClass( "active" );

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
                $.hsWarn(
                   "您确定要退出登录吗?",
                   "点击“确定”将离开系统, 点击“取消”可继续使用. 感谢您的使用, 祝您工作顺利!",
                   function() {
                    $.get(hsFixUri("centra/sign/delete.act"), function() {
                        location.assign(hsFixUri( "centra/login.html" ));
                    });
                }, function() {
                    // Nothing todo.
                }).addClass("alert-warning");
            });
        $("#user-set")
            .click(function() {
                $.hsOpen("centra/manage/mime.html");
            });
        $("#note-msg")
            .click(function() {
                $.hsOpen("centra/manage/note.html");
            });

        // 菜单隐藏和显示
        $("#menu-raq").appendTo(document.body)
            .click(function() {
                $('#headbox' ).show();
                $("#menu-raq").hide();
            });
        $("#menu-laq")
            .click(function() {
                $('#headbox' ).hide();
                $("#menu-raq").show();
            });

        // 菜单折叠和展开
        menubar.find("li>ul" ).hide();
        menubar.find("li.acting>ul").show();
        menubar.find("li.acting>a" ).addClass("dropup");
        menubar.on("click", "a", function() {
            $(this).toggleClass( "dropup" );
            var ul = $(this).next(  );
            if (ul.size( ) ) {
                ul.toggle( );
                return false;
            }
        });
    })(jQuery);
</script>
