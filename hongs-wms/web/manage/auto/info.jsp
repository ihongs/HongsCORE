<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_more_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("info.action"), "review");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
%>
<h2><%=_title%>详情</h2>
<div id="<%=_pageId%>">
    <form action="" method="POST" enctype="multipart/form-data">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            || !Synt.declare(info.get("editable"), true)) {
                continue ;
            }

            String  text = (String) info.get("__text__");
            String  type = (String) info.get("__type__");
            String  kind ;

            if ("date".equals(type)) {
                kind = "_date";
            } else
            if ("time".equals(type)) {
                kind = "_time";
            } else
            if ("datetime".equals(type)) {
                kind = "_datetime";
            } else
            {
                kind = "_review";
            }
        %>
        <%if ("hidden".equals(type)) {%>
        <%} else if ("line".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else if ("file".equals(type) || "upload".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                <%
                    String size = "";
                    String keep = "";
                    kind =  "_file" ;
                    if ("image".equals(type)) {
                        kind =  "_view";
                        size = Synt.declare( info.get("thumb-size"), "" );
                        keep = Synt.declare( info.get("thumb-mode"), "" );
                        if (size.length()!=0) {
                            size = size.replaceFirst("\\d+\\*\\d+", "$0");
                            if ( ! keep.equals("keep") ) {
                                keep = "";
                            }
                            // 限制最大宽度, 避免撑开容器
                            String[ ] wh = size.split("\\*");
                            int w = Synt.declare(wh[0], 300);
                            int h = Synt.declare(wh[1], 300);
                            if (w > 300) {
                                h = 300  * h / w;
                                w = 300;
                                size = w +"*"+ h;
                            }
                        } else {
                            size = "100*100";
                            keep = "keep";
                        }
                    }
                %>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                    <ul class="pickbox" data-ft="<%=kind%>" data-fn="<%=name%>" data-size="<%=size%>" data-keep="<%=keep%>"></ul>
                </div>
            </div>
            <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                <%
                    String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") :  "id" ;
                    String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                    String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :
                              ( info.containsKey("form"   ) ? (String) info.get("form"   ) : name.replaceFirst("_id$", "") );
                %>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                    <ul class="pickbox" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                </div>
            </div>
            <%} else {%>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                    <p class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></p>
                </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <div class="form-group row">
            <div class="col-sm-6 col-sm-offset-3">
                <button type="button" class="cancel btn btn-primary">返回</button>
            </div>
        </div>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");

    /**
     * 文件、图片等控件都是为表单准备的
     * 需去掉删除按钮及更换按钮样式才行
     */
    context.on("loadOver", function() {
        context.find(".pickbox .close").hide();
        context.find(".pickbox .btn-info" )
               .removeClass("form-control")
               .removeClass("btn-info").addClass("btn-link");
    });

    context.hsForm({
        loadUrl: "<%=_module%>/<%=_entity%><%if ("revert".equals(_action)){%>/revert<%}%>/search.act?id=\${id}&md=\${md}&ctime=\${ctime}",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });
})( jQuery );
</script>