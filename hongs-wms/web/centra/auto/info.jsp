<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("info.action"), "review");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_info").replace('/', '_');
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
            String  kind = "_review";

            if (  "select".equals(type)
            ||     "check".equals(type)
            ||     "radio".equals(type)
            ||      "type".equals(type)
            ||      "enum".equals(type)) {
                name += "_text";
            } else
            if ("datetime".equals(type)
            ||      "date".equals(type)
            ||      "time".equals(type)) {
                // 日期类需注意 Unix 时间戳需要乘 1000
                String typa = (String) info.get("type");
                if ("timestamp".equals(typa)
                ||  "datestamp".equals(typa)) {
                    kind =  "_" + type + "\" data-fl=\"!v ?v :v *1000";
                } else {
                    kind =  "_" + type;
                }
            } else
            if ("textarea".equals(type)) {
                // 文本域可能被应用上富文本、源代码等
                String typa = (String) info.get("type");
                String mode = (String) info.get("mode");
                if (typa == null || typa.length() == 0) {
                    type =  "text";
                } else
                if ("html".equals(typa)) {
                    type =  "text";
                    kind = "_html";
                } else {
                    type =   typa ;
                    kind = "_text\" data-type=\""+typa+"\" data-mode=\""+mode;
                }
            }
        %>
        <%if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>"/>
        <%} else if ("line".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else {%>
            <div class="form-group row">
                <label class="col-xs-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-xs-6">
                <%if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        kind = "_file";
                        if ("image".equals(type)) {
                            kind = "_view";
                            String size = Synt.declare (info.get("thumb-size"), "");
                            String keep = Synt.declare (info.get("thumb-mode"), "");
                            if (! "keep".equals(keep)) {
                                keep = "" ;
                            }
                            if (size.length( ) != 0  ) {
                                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)").matcher(size);
                                if ( m.find( ) ) {
                                    // 限制最大宽度, 避免撑开容器
                                    int w = Synt.declare(m.group(1), 300);
                                    int h = Synt.declare(m.group(2), 300);
                                    if (w > 300) {
                                        h = 300  * h / w;
                                        w = 300;
                                        size = w +"*"+ h;
                                    } else {
                                        size = m.group();
                                    }
                                } else {
                                    size = "100*100";
                                }
                            } else {
                                size = "100*100";
                            }
                            kind += "\" data-size=\""+size+"\" data-keep=\""+keep;
                        }
                    %>
                    <ul class="pickbox pickrol" data-fn="<%=name%>" data-ft="<%=kind%>"></ul>
                    <button type="button" data-toggle="hsFile" class="hide"></button>
                <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        kind =  "_fork" ;
                        String fn = name;
                        String kn = fn +"_data";
                        if (fn.endsWith("_id")) {
                            fn = fn.substring(0, fn.length() - 3);
                            kn = fn;
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                        kind += "\" data-ak=\""+ak+"\" data-tk=\""+tk+"\" data-vk=\""+vk;
                        if (Synt.declare(info.get("__repeated__"), false)) {
                            kind  += "\" data-repeated=\"true";
                        }
                    %>
                    <ul class="pickbox pickrol" data-fn="<%=name%>" data-ft="<%=kind%>"></ul>
                    <button type="button" data-toggle="hsFork" class="hide"></button>
                <%} else if ("code".equals(type) || "mark".equals(type)) {%>
                    <pre class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></pre>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} /*End If */%>
                </div>
            </div>
        <%} /*End if */%>
        <%} /*End For*/%>
        <div class="form-group row">
            <div class="col-xs-6 col-xs-offset-3">
                <button type="button" class="cancel btn btn-primary">返回</button>
            </div>
        </div>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");
    var loadbox = context.closest(".loadbox");
    var formbox = context.find("form");

    var formobj = context.hsForm({
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView,
        _url : "<%=_module%>/<%=_entity%><%="revert".equals(_action)?"/revert":""%>/search.act?<%=Cnst.AB_KEY%>=_enum,_fork"
    });

    hsRequires("<%=_module%>/<%=_entity%>/custom.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, formobj);
        }

        // 特殊控件
        setInfoItems( formbox , loadbox);

        // 加载数据
        formobj.load(undefined, loadbox);
    });
})( jQuery );
</script>