<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_more_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("info.action"), "review");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = (_module + "_" + _entity + "_info").replace('/', '_');
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-info">
    <form action="" method="POST" enctype="multipart/form-data">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            || (!Synt.declare(info.get("editable"), true )
            &&  !Synt.declare(info.get("listable"), false))) {
                continue ;
            }

            String  text = (String) info.get("__text__");
            String  type = (String) info.get("__type__");
            String  kind ;

            if (Synt.declare(info.get("__repeated__"), false)) {
                name += ".";
            }
            
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
            <input type="hidden" name="<%=name%>"/>
        <%} else if ("line".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
            <%
                String size = "";
                String keep = "";
                kind =  "_file" ;
                if ("image".equals(type)) {
                    kind =  "_view";
                    size = Synt.declare( info.get("thumb-size"), "" );
                    keep = Synt.declare( info.get("thumb-mode"), "" );
                    if (! "keep".equals( keep )) {
                        keep = "";
                    }
                    if (size.length() != 0) {
                        Pattern pat = Pattern.compile("\\d+\\*\\d+");
                        Matcher mat = pat.matcher(size);
                        if (mat.matches( )) {
                            size = mat.group( );
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
                        }
                    } else {
                        size = "100*100";
                    }
                }
            %>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                    <ul class="pickbox pickrol" data-ft="<%=kind%>" data-fn="<%=name%>" data-size="<%=size%>" data-keep="<%=keep%>"></ul>
                    <button type="button" data-toggle="hsFile" class="hide"></button>
                </div>
            </div>
        <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
            <%
                String fn = name;
                String kn ;
                if (fn.endsWith( "." )) {
                    fn = fn.substring(0, fn.length() - 1);
                }
                if (fn.endsWith("_id")) {
                    int  ln = fn.length() - 3;
                    fn = fn.substring(0 , ln);
                    kn = fn;
                } else {
                    kn = fn + "_data";
                }
                String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
            %>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                    <ul class="pickbox pickrol" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                    <button type="button" data-toggle="hsFork" class="hide"></button>
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
    var loadbox = context.closest( ".loadbox" );

    var formobj = context.hsForm({
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView,
        _url : "<%=_module%>/<%=_entity%><%="revert".equals(_action)?"/revert":""%>/search.act"
    });

    hsRequires("<%=_module%>/<%=_entity%>/custom.js", function() {
        // 外部定制
        if (window["in_<%=_funcId%>"]) {
            window["in_<%=_funcId%>"](context, formobj);
        }

        // 加载数据
        formobj.load(null, loadbox);
    });
})( jQuery );
</script>