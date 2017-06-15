<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="java.util.Map"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    int i;
    String _module, _entity;
    _module = ActionDriver.getWorkPath(request);
    i = _module.lastIndexOf('/');
    _module = _module.substring(1, i);
    i = _module.lastIndexOf('/');
    _entity = _module.substring(i+ 1);
    _module = _module.substring(0, i);

    CoreLocale lang = CoreLocale.getInstance().clone();
               lang.loadIgnrFNF(_module);
               lang.loadIgnrFNF(_module +"/"+ _entity);
    NaviMap    site = NaviMap.hasConfFile(_module+"/"+_entity)
                    ? NaviMap.getInstance(_module+"/"+_entity)
                    : NaviMap.getInstance(_module);
    Map        menu = site.getMenu(_module +"/"+ _entity +"/");

    String nm = menu == null ? "" : (String) menu.get( "text");
%>
<!doctype html>
<html>
    <head>
        <title>HongsCORE::<%=lang.translate(nm)%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/addons/bootstrap-fileinput/css/fileinput.css"/>
        <link rel="stylesheet" type="text/css" href="static/addons/bootstrap-datetimepicker/css/datetimepicker.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="static/addons/bootstrap-fileinput/fileinput.min.js"></script>
        <script type="text/javascript" src="static/addons/bootstrap-datetimepicker/datetimepicker.min.js"></script>
        <script type="text/javascript">
            $(document).on("noMenu", function() {
                $('#main-context').empty().append(
                    '<div class="alert alert-info"><p>'
                  + ':( 糟糕! 这里什么也没有, <a href="manage/">换个地方</a>瞧瞧去?'
                  + '</p></div>'
                );
                setTimeout(function() {
                    location.href = hsFixUri("manage/");
                } , 3000);
            });
        </script>
    </head>
    <body>
        <div id="notebox"></div>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <div class="row" data-load="manage/head.jsp?m=<%=_module%>/<%=_entity%>"></div>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container" id="main-context">加载中...</div>
        </div>
        <nav id="footbox" class="navbar navbar-default navbar-fixed-bottom">
            <div class="container">
                <div class="row" data-load="manage/foot.jsp?m=<%=_module%>/<%=_entity%>"></div>
            </div>
        </nav>
    </body>
</html>
