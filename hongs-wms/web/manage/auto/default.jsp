<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_base_.jsp"%>
<!doctype html>
<html>
    <head>
        <title>HongsCORE::<%=_title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
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
        <script type="text/javascript" src="common/auth/manage.js"></script>
        <script type="text/javascript">
            $(document).on("loseMenu", function() {
                $('#main-context').empty().append(
                    '<div class="alert alert-info"><p>'
                  + ':( 糟糕! 这里什么也没有, <a href="manage/">换个地方</a>瞧瞧去?'
                  + '</p></div>'
                );
                setTimeout(function() {
                    location.href = hsFixUri("manage/");
                } , 3000);
            });
            
            $.ajaxSetup({cache:true});
        </script>
    </head>
    <body>
        <div id="notebox"></div>
        <nav id="headbox" class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">
                <div class="row" data-load="manage/head.jsp"></div>
            </div>
        </nav>
        <div id="bodybox">
            <div class="container" id="main-context"><%=_locale.translate("fore.loading")%></div>
        </div>
        <nav id="footbox" class="navbar navbar-default navbar-fixed-bottom">
            <div class="container">
                <div class="row" data-load="manage/foot.jsp"></div>
            </div>
        </nav>
    </body>
</html>
