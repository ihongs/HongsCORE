<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Model"%>
<%@page import="app.hongs.db.Table"%>
<%@page import="app.hongs.serv.medium.ABaseModel"%>
<%@page import="app.hongs.serv.medium.Article"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
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
    ABaseModel  sec = (ABaseModel) DB.getInstance("medium").getModel("section");
                sec.setType("default");
    String      sid = request.getParameter("id");
    Map         map = sec.get(sid);

    if ((map == null || map.isEmpty()) && !"0".equals(sid)) {
        request.getRequestDispatcher("/medium/actoin/404.jsp?msg=分类不存在")
               .forward(request, response);
        return;
    }

    String      srl = "medium/section/" + sid ;
    String      pid = (String) map.get("pid" );
    String     root = request.getContextPath();
    List<Map>  path = getSectPath (pid);
    String     titl = (String) map.get("name");
    
    String     wd   = request.getParameter("wd");
    if (titl == null) {
        if (wd != null && !"".equals(wd)) {
            titl = "搜素";
        } else {
            titl = "文章";
        }
    }

    // 查询结构
    ActionHelper ah = Core.getInstance(ActionHelper.class);
    Map rd = ah.getRequestData();
    Set rb = new HashSet();
    rb.add("id");
    rb.add("name");
    rb.add("note");
    rb.add("snap");
    rb.add("mtime");
    rd.remove("id");
    rd.put("rb", rb);
    if (Synt.declare(rd.get("md"), 0) == 1) {
        rd.put("sect_id" , getSectIds(sid));
    } else {
        rd.put("sect_id" , sid);
    }

    Article     art = Article.getInstance("default");
    Map        data = art.retrieve(rd);
    Map        paga = (Map ) data.get("page" );
    List<Map>  list = (List) data.get("list" );

    // 分页计算
    int i, p, t, pmin, pmax;
    int err = Synt.declare(paga.get("err"), 0);
    i = Synt.declare(paga.get("pags"     ), 5);
    p = Synt.declare(paga.get("page"     ), 0);
    t = Synt.declare(paga.get("pagecount"), 0);
    if (p == 0) p = 1;
    if (t == 0) t = 1;
    pmin  = p - i / 2;
    if (pmin < 1) pmin = 1;
    pmax = pmin + i - 1;
    if (pmax > t) pmax = t;
    pmin = pmax - i + 1;
    if (pmin < 1) pmin = 1;
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
        <link rel="stylesheet" type="text/css" href="<%=root%>/static/addons/bootstrap-fileinput/css/fileinput.min.css"/>
        <script type="text/javascript" src="<%=root%>/static/addons/bootstrap-fileinput/fileinput.min.js"></script>
        <title><%=titl%></title>
        <style type="text/css">
            .section_name, section_note
                { color: #333333; font-size: 18px; font-weight: 600; }
            .section_name:hover
                { color: #3da9f7; }
            .section_desc
                { font-size: 13px; }
            .section_snap img
                { width: 160px; max-width: 160px; max-height: 120px; }
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
                    <li><%=titl%></li>
                </ol>
                <div class="row">
                    <div class="col-md-9">
                        <%if (err != 0) {%>
                        <div class="alert alert-info">
                            <%if (err == 2) {%>
                            <script type="text/javascript">
                                setTimeout(function() {
                                    location.href="<%=root+"/"+srl%>";
                                }, 3000);
                            </script>
                            抱歉，当前页码不正确，即将转到第一页。
                            <%} else {%>
                            抱歉，此分类下无内容，点点其它的看看。
                            <%} /*End If*/%>
                        </div>
                        <%} else { %>
                        <%
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        %>
                        <%for (Map info : list) {%>
                        <%
                            String  aid = (String) info.get( "id" );
                            String snap = (String) info.get("snap");
                            String name = (String) info.get("name");
                            String note = (String) info.get("note");
                            long  utime = Synt.declare(info.get("mtime"),0L);
                            String time = sdf.format(new Date(utime * 1000));
                            if (snap == null || snap.equals("")) {
                                snap  = root + "/medium/404.jpg";
                            } else
                            if (!snap.startsWith("/") && !snap.matches("^(\\w+:)?//")) {
                                snap  = root + "/" + snap;
                            }
                            if (note == null) {
                                note  = "";
                            }
                            int cb = Synt.declare(info.get("count_browses"), 0);
                            int cn = Synt.declare(info.get("count_consent"), 0);
                            int cm = Synt.declare(info.get("count_comment"), 0);
                        %>
                        <section class="row" style="margin-top:0.5em;">
                            <div class="col-md-3">
                                <a class="section_snap" href="<%=root%>/medium/article/<%=aid%>">
                                    <img src="<%=snap%>" alt="<%=name%>"/>
                                </a>
                            </div>
                            <div class="col-md-9">
                                <a class="section_name" href="<%=root%>/medium/article/<%=aid%>"><%=name%></a>
                                <p class="section_info">
                                    发布时间: <%=time%>
                                    浏览: <span class="count_browses"><%=cb%></span>
                                    点赞: <span class="count_consent"><%=cn%></span>
                                    评论: <span class="count_comment"><%=cm%></span>
                                </p>
                                <p class="section_desc"><%=note%></p>
                            </div>
                        </section>
                        <%} /*End For*/%>
                        <nav>
                            <ul class="pagination">
                                <%if (1 < p) {%>
                                <li class="page-prev"><a href="<%=root%>/medium/section/<%=sid%>/<%=(p-1)%>" title="上一页">&laquo;</a></li>
                                <%} else {%>
                                <li class="page-prev disabled"><a href="javascript:;" title="上一页">&laquo;</a></li>
                                <%} /*End If*/%>
                                <%for(i = pmin; i < pmax + 1; i ++) {%>
                                <li class="page-link <%=(i == p ? "active" : "")%>"><a href="<%=root%>/medium/section/<%=sid%>/<%=i%>" title="第<%=i%>页"><%=i%></a></li>
                                <%} /*End For*/%>
                                <%if (t > p) {%>
                                <li class="page-prev"><a href="<%=root%>/medium/section/<%=sid%>/<%=(p+1)%>" title="下一页">&raquo;</a></li>
                                <%} else {%>
                                <li class="page-prev disabled"><a href="javascript:;" title="下一页">&raquo;</a></li>
                                <%} /*End If*/%>
                            </ul>
                        </nav>
                        <%} /*End If*/%>
                    </div>
                    <div class="col-md-3">
                        <jsp:include page="../side/sub_secs.jsp" >
                            <jsp:param name="sid" value="<%=sid%>"/>
                        </jsp:include>
                        <jsp:include page="../side/sug_arts.jsp">
                            <jsp:param name="sid" value="<%=sid%>"/>
                        </jsp:include>
                        <jsp:include page="../side/hot_arts.jsp" >
                            <jsp:param name="sid" value="<%=sid%>"/>
                        </jsp:include>
                        <jsp:include page="../side/hot_tags.jsp" >
                            <jsp:param name="sid" value="<%=sid%>"/>
                        </jsp:include>
                    </div>
                </div>
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
