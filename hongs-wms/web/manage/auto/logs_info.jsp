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
    String _pageId = (_module + "-" + _entity + "-logs-info").replace('/','-');
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

            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
            
            if ("date".equals(type)) {
                type = "_date";
            } else
            if ("time".equals(type)) {
                type = "_time";
            } else
            if ("datetime".equals(type)) {
                type = "_datetime";
            } else
            {
                type = "_review";
            }
        %>
        <%if ("hidden".equals(type)) {%>
        <%} else if ("line".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else {%>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6 form-control-static" data-fn="<%=name%>" data-ft="<%=type%>"></div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");

    context.hsForm({
        loadUrl: "<%=_module%>/<%=_entity%>/revert/search.act?id=\${id}&ctime=\${ctime}",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });
})( jQuery );
</script>