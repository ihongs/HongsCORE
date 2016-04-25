<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Table"%>
<%@page import="app.hongs.serv.medium.ABaseModel"%>
<%@page import="app.hongs.serv.medium.Article"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.LinkedHashSet"%>
<%@page import="java.util.List"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@include file="../Functions.jsp"%>
<%
    Article     art = Article.getInstance ("default");
    String      aid = request.getParameter("id");
    Map         map = art.get(aid);

    if (map == null || map.isEmpty()) {
        request.getRequestDispatcher("/medium/action/404.jsp?msg=文章不存在")
               .forward(request, response);
        return;
    }

    String      sid = getArtiSid  (aid);
    String      srl = "medium/section/" + sid ;
    String     root = request.getContextPath();
    List<Map>  path = getSectPath (sid);
    String     name = Synt.declare(map.get("name"), "");
    String     note = Synt.declare(map.get("note"), "");
    String     word = Synt.declare(map.get("word"), "");
    String     html = Synt.declare(map.get("html"), "");
    String     desc = note.replaceAll("(\r\n|\r|\r)", " ");
    String     head = note.replaceAll("(\r\n|\r|\n)", "<br/>");
    String     mark = word.replaceAll("\\s*,\\s*","</span><span>");
    byte      stave = Synt.declare(map.get("stave"), (byte) 0);

    if (!"".equals(head)) {
        head = "<span>" + head + "</span>";
    }
    if (!"".equals(mark)) {
        mark = "<span>" + mark + "</span>";
    }
%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=root%>/"/>
        <link rel="icon" type="image/x-icon" href="<%=root%>/favicon.ico"/>
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/assets/css/hongscore.min.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="<%=root%>/static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="<%=root%>/static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="<%=root%>/static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="<%=root%>/static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="<%=root%>/common/conf/default.js"></script>
        <script type="text/javascript" src="<%=root%>/common/lang/default.js"></script>
        <meta name="description" content="<%=desc%>">
        <meta name="keywords"    content="<%=word%>">
        <title><%=name%></title>
        <style type="text/css">
            .dissent .badge {display: none;}
        </style>
    </head>
    <body>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <jsp:include page="../../../handle/head.jsp" >
                    <jsp:param name="u" value="<%=srl%>"/>
                </jsp:include>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container">
                <ol class="breadcrumb">
                    <li><a href="<%=root%>/">首页</a></li>
                    <%for (Map row : path) {%>
                    <li><a href="<%=root%>/medium/section/<%=row.get("id")%>"><%=row.get("name")%></a></li>
                    <%} /*End For*/%>
                    <li><%=name%></li>
                </ol>
                <div class="row">
                    <div class="col-md-9">
                        <div>
                            <a href="javascript:;" class="share">分享</a>
                            <a href="javascript:;" class="browses" style="margin-left:1em;">浏览<span class="badge">0</span></a>
                            <a href="javascript:;" class="consent" style="margin-left:1em;">赞<span class="badge">0</span></a>
                            <a href="javascript:;" class="dislike" style="margin-left:1em;">踩</a>
                            <a href="javascript:;" class="dissent" style="margin-left:1em;">举报<span class="badge">0</span></a>
                        </div>
                        <article>
                            <%if (1 != (1 & stave)) {%>
                            <header>
                                <h1><%=name%></h1>
                                <%if (!"".equals(head)) {%>
                                <blockquote class="article-desc">概要: <%=head%></blockquote>
                                <%} /*End If*/%>
                            </header>
                            <%=html%>
                            <footer>
                                <%if (!"".equals(mark)) {%>
                                <blockquote class="article-tags">标签: <%=mark%></blockquote>
                                <%} /*End If*/%>
                            </footer>
                            <%} else {%>
                            <%=html%>
                            <%} /*End If*/%>
                        </article>
                    </div>
                    <div class="col-md-3">
                        <jsp:include page="../side/sim_arts.jsp" >
                            <jsp:param name="aid" value="<%=aid%>"/>
                        </jsp:include>
                        <jsp:include page="../side/sim_tags.jsp" >
                            <jsp:param name="aid" value="<%=aid%>"/>
                        </jsp:include>
                    </div>
                </div>
                <%if (2 != (2 & stave)) {%>
                <div class="row" id="comments"
                    data-module="hsList"
                    data-load-url="handle/medium/comment/retrieve.act?link_id=<%=aid%>&ob=ctime"
                    data-fill-list="(hsListFillItem)">
                    <h3>网友评论</h3>
                    <section class="itembox" style="display:none;">
                        <h4>
                            <span data-fn="ctime" data-ft="_htime"></span>
                            <span data-fn="user.name"></span>:
                        </h4>
                        <p data-fn="note"></p>
                    </section>
                    <div class="listbox"></div>
                    <div class="pagebox"></div>
                </div>
                <div class="row" id="commentc"
                    data-module="hsForm">
                    <h3>发表评论</h3>
                    <form action="handle/medium/comment/create.act?link_id=<%=aid%>" class="col-md-6">
                        <div class="form-group">
                            <textarea name="note" class="form-control"></textarea>
                        </div>
                        <div class="form-group">
                            <button type="submit" class="btn btn-primary">提交</button>
                        </div>
                    </form>
                </div>
                <%} /*End If*/%>
                <!-- 举报对话框 -->
                <div class="modal fade dissent-form" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel">
                    <div class="modal-dialog modal-md">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 class="modal-title">举报</h4>
                            </div>
                            <form action="handle/medium/dissent/create.act?link_id=<%=aid%>" method="POST"
                                  data-module="hsForm">
                            <div class="modal-body">
                                <div class="form-group">
                                    <label>原因</label>
                                    <select name="cause" class="form-control">
                                        <option value="1">虚假信息</option>
                                        <option value="2">敏感信息</option>
                                        <option value="3">淫秽色情</option>
                                        <option value="4">人身攻击</option>
                                        <option value="9">其他</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>说明</label>
                                    <textarea name="note" class="form-control"></textarea>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>
                                <button type="submit" class="btn btn-primary">提交</button>
                            </div>
                            </form>
                        </div>
                    </div>
                </div>
                <script type="text/javascript">
                    (function($) {
                        var listBox = $("#comments");
                        var formBox = $("#commentc");
                        formBox.on("saveBack", function(rst) {
                            listBox.data("HsList").load(null, {pn:-1});
                        });

                        function loadBasics() {
                            $.ajax({
                                url     : hsFixUri("handle/medium/article/basics/retrieve.act?id=<%=aid%>"),
                                type    : "POST",
                                dataType: "JSON",
                                success : function(rst) {
                                    if (rst.info) {
                                        $(".browses .badge").text(rst.info.count_browses);
                                        $(".consent .badge").text(rst.info.count_consent);
                                        $(".dissent .badge").text(rst.info.count_dissent);
                                    }
                                }
                            });
                        }
                        loadBasics();

                        $(".consent").click(function() {
                            $.ajax({
                                url     : hsFixUri("handle/medium/consent/update.act?link_id=<%=aid%>"),
                                type    : "POST",
                                dataType: "JSON",
                                success : function(rst) {
                                    $.hsNote(rst.msg);
                                }
                            });
                        });

                        $(".dislike").click(function() {
                            $.ajax({
                                url     : hsFixUri("handle/medium/consent/update.act?link_id=<%=aid%>&score=-1"),
                                type    : "POST",
                                dataType: "JSON",
                                success : function(rst) {
                                    $.hsNote(rst.msg);
                                }
                            });
                        });

                        $(".dissent").click(function() {
                            $(".dissent-form").modal();
                        });

                        $(".dissent-form").on("saveBack", function() {
                            $(".dissent-form").modal("hide");
                        });
                    })(jQuery);
                </script>
            </div>
        </div>
        <nav id="footbox" class="navbar navbar-default navbar-fixed-bottom" role="navigation">
            <div class="container">
                <jsp:include page="../../../handle/foot.jsp" >
                    <jsp:param name="u" value="<%=srl%>"/>
                </jsp:include>
            </div>
        </nav>
    </body>
</html>
