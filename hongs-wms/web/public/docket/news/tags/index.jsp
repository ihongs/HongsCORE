<%@page import="app.hongs.serv.auth.AuthKit"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.util.Locale"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Map"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.Core"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
String id = Core.ACTION_NAME.get(  ).substring( 24 );
String ms = request.getHeader( "If-Modified-Since" );
if ( "".equals(id) || "index.jsp".equals(id)) {
    throw new HongsException (0x1104, "ID required");
}

id = decodeURL(id);
Data news = Data.getInstance("centre/data/docket/news", "news");
Map  info = news.getOne(Synt.mapOf(
    "ob", Synt.setOf("-mtime"),
    "rb", Synt.setOf( "mtime"),
    "tags", id
));
if (info == null || info.isEmpty()) {
    throw new HongsException(0x1104, "Not found !");
}

SimpleDateFormat hfmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                 hfmt.setTimeZone(TimeZone.getTimeZone("GMT") );
long mtime = Synt.asLong(info.get("mtime"));

// 更新判断
if (ms != null) {
    long xtime  =  hfmt.parse(ms).getTime() / 1000 ;
    if ( mtime != xtime ) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return;
    }
}

// 时间标识
String htime = hfmt.format(new Date(mtime * 1000));
response.setHeader("Last-Modified", htime);
response.setHeader("ETag", AuthKit.getCrypt(id)+":"+mtime);

ActionHelper helper = Core.getInstance(ActionHelper.class);
Map data = helper.getRequestData();
data.remove( "id" );
data.put   ("tags", id );
data = news.search(data);
List<Map> list = (List) data.get("list");
%>
<!doctype html>
<html>
<head>
    <title><%=escape((String)info.get("name"))%></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="keywords" content="<%=escape((String)info.get("word"))%>">
    <meta name="description" content="<%=escape((String)info.get("note"))%>">
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
</head>
<body>
    <div class="invisible">
        <img src="<%=info.get("logo")%>"/>
    </div>
    <div class="container">
        <%for (Map item : list) {%>
            <div><%=item.get("name")%></div>
        <%} /*End for*/%>
    </div>
</body>
</html>