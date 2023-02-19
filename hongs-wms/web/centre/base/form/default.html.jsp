<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    // 判断是否许可
    {
        String[] a= {_module +"/"+ _entity , _module , "centre"};
        for (String name : a) try {
            NaviMap site = NaviMap.getInstance ( name );
            Map menu;
            menu  = site.getMenu(_module +"/"+ _entity +"/");
            if (menu != null) {
                String hrel = (String) menu.get("hrel");
                if ("!DENY".equals(hrel)) {
                    throw new HongsException(404, _locale.translate("core.error.no.thing"));
                }
                break;
            }
            menu  = site.getMenu(_module +"/#"+_entity);
            if (menu != null) {
                String hrel = (String) menu.get("hrel");
                if ("!DENY".equals(hrel)) {
                    throw new HongsException(404, _locale.translate("core.error.no.thing"));
                }
                break;
            }
        } catch (HongsException ex) {
            // 忽略配置文件缺失的异常情况
            if (ex.getErrno() != 920) {
                throw ex ;
            }
        }
    }

    // 定制页面内容
    String _heading = (String) _params.get("page-heading");
    String _header  = (String) _params.get("page-header" );
    String _footer  = (String) _params.get("page-footer" );
    String _link    = (String) _params.get("page-link"   );
    String _style   = (String) _params.get("page-style"  );
    String _script  = (String) _params.get("page-script" );

    String $func = "in_"+(_module+"_"+_entity).replace('/', '_');
%>
<!doctype html>
<html>
    <head>
        <title><%=_title%></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <base href="<%=request.getContextPath()%>/">
        <link rel="icon" href="favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/centre/css/base.min.css"/>
        <link rel="stylesheet" type="text/css" href="static/centre/css/form.min.css"/>
        <link rel="stylesheet" type="text/css" href="<%=_module%>/<%=_entity%>/form/defines.css"/>
        <!--[if glt IE8.0]>
        <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
        <![endif]-->
        <script type="text/javascript" src="static/assets/jquery.min.js"></script>
        <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
        <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
        <script type="text/javascript" src="static/assets/hongsedge.min.js"></script>
        <script type="text/javascript" src="common/conf/default.js"></script>
        <script type="text/javascript" src="common/lang/default.js"></script>
        <script type="text/javascript" src="common/auth/centre.js" ></script>
        <script type="text/javascript" src="<%=_module%>/<%=_entity%>/form/defines.js"></script>
        <%if ( _style != null && ! _style .isEmpty()) {%>
        <style  type="text/css"><%=_style%></style>
        <%}%>
        <%if (_script != null && ! _script.isEmpty()) {%>
        <script type="text/javascript"><%=_script%></script>
        <%}%>
    </head>
    <body>
        <div id="context" class="container">
            <%if (_heading != null && !_heading.isEmpty()) {%>
            <h1 class="page-heading"><%=_heading%></h1>
            <%}%>
            <%if (_header  != null && !_header .isEmpty()) {%>
            <header><%=_header%></header>
            <%}%>
            <div id="main-context" data-load="<%=_module%>/<%=_entity%>/form/form.html"></div>
            <%if (_footer  != null && !_footer .isEmpty()) {%>
            <footer><%=_footer%></footer>
            <%}%>
        </div>
        <script type="text/javascript">
            (function($) {
                var contact = $("#context");
                var context = $("#main-context");

                HsLANG["form.invalid"] = "<%=Synt.declare(_params.get("page-form-invalid"), "请按提示修改后再次提交。")%>";
                HsLANG["form.success"] = "<%=Synt.declare(_params.get("page-form-success"), "提交成功！感谢您的参与。")%>";

                // 外部定制
                window["<%=$func%>"] && window["<%=$func%>"](context);

                // 完成动作
                context.on("saveBack", function(ev) {
                    if (ev.isDefaultPrevented()) {
                        return;
                    }
                    <%if (_link == null || _link.isEmpty()) {%>
                    contact.hsLoad("<%=_module+"/"+_entity+"/form/stop.html"%>");
                    <%} else if ( ! _link.contains( "/" ) ) {%>
                    contact.hsLoad("<%=_module+"/"+_entity+"/form/" + _link %>");
                    <%} else {%>
                    location.replace("<%=_link%>");
                    <%}/*END*/%>
                });
            })(jQuery);
        </script>
    </body>
</html>
