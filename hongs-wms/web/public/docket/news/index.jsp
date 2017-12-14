<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Map"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
String an  = Core.ACTION_NAME.get();
String id  = an.substring(19); // public/docket/news/
if ("".equals(id) || "index.jsp".equals(id)) {
    throw new HongsException(0x1104, "ID required");
}

Data news  = Data.getInstance("centre/data/docket/news", "news");
Map  info  = news.get(id);
if ( info == null || info.isEmpty() ) {
    throw new HongsException(0x1104, "Not found !");
}

// 统计数据
Map  stat  = DB.getInstance("medium")
        .getTable("statist")
        .filter("link = ? AND link_id = ?", "article", id)
        .one();

// 修改时间
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
String mtime = sdf.format(new Date(Synt.asLong(info.get("mtime")) * 1000));
%>
<!doctype html>
<html>
<head>
    <title><%=info.get("name")%></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="keywords" content="<%=escapeXML((String) info.get("find"))%>">
    <meta name="description" content="<%=escapeXML((String) info.get("note"))%>">
    <base href="<%=request.getContextPath()%>/" target="_blank"/>
    <link rel="icon" type="image/x-icon" href="favicon.ico"/>
    <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
    <!--[if glt IE8.0]>
    <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
    <![endif]-->
    <script type="text/javascript" src="static/assets/jquery.min.js"></script>
    <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
    <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
    <script type="text/javascript" src="common/conf/default.js"></script>
    <script type="text/javascript" src="common/lang/default.js"></script>
    <style type="text/css">
        h1 {text-align: center;}
    </style>
</head>
<body>
    <div class="invisible">
        <img src="<%=info.get("logo")%>"/>
    </div>
    <div class="container">
        <h1><%=info.get("name")%></h1>
        <p style="text-align:center;">
            <%=mtime%>
        </p>
        <div>
            <%=info.get("body")%>
        </div>
        <p>
            <span style="margin-left: 1em;"></span>
            <a href="javascript:;">
                <span class="glyphicon glyphicon-thumbs-up"  ></span>
                <span class="badge"><%=Synt.defoult(stat.get("endorse_count"), "")%></span>
            </a>
            <span style="margin-left: 1em;"></span>
            <a href="javascript:;">
                <span class="glyphicon glyphicon-thumbs-down"></span>
            </a>
        </p>
    </div>
</body>
</html>