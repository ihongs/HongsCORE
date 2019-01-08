<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("info.action"), "review");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_info").replace('/', '_');
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-info">
    <form action="" onsubmit="return false"
          method="POST" enctype="multipart/form-data">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get("invisble"), false)) {
                continue ;
            }

            String  text = (String) info.get("__text__");
            String  type = (String) info.get("__type__");
            String  kind = "_review";

            if ("datetime".equals(type)
            ||      "date".equals(type)
            ||      "time".equals(type)) {
                // 日期类需注意 Unix 时间戳需要乘 1000
                String typa = (String) info.get("type");
                if (text == null || text.length() == 0) {
                    type  = "hidden"  ;
                } else
                if (typa == null || typa.length() == 0
                ||  "date".equals(typa)
                ||  "time".equals(typa)) {
                    kind  = "_" + type;
                } else {
                    kind  = "_" + type;
                    kind += "\" data-fl=\"!v?v:v*1000" ;
                }
            } else
            if (  "select".equals(type)
            ||     "check".equals(type)
            ||     "radio".equals(type)
            ||      "type".equals(type)
            ||      "enum".equals(type)) {
                // 选项类字段在查看页仅需读取其文本即可
                name += "_text";
            }
            if (Synt.declare(info.get("__repeated__"), false)) {
                // 为与表单一致而对多值字段的名称后加点
                name += "."/**/;
            }
        %>
        <%if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>"/>
        <%} else if ("splitbar".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else {%>
            <div class="form-group row">
                <label class="col-md-3 col-sm-2 control-label form-control-static text-right"><%=text%></label>
                <div class="col-md-6 col-sm-10">
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
                        if (fn.endsWith( "." )) {
                            fn = fn.substring(0, fn.length() - 1);
                        }
                        String kn = fn +"_fork";
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
                <%} else if ("textaera".equals(type) || "textview".equals(type)) {%>
                    <%
                        String typa = (String) info.get("type");
                        String mode = (String) info.get("mode");
                    %>
                    <%       if ("code".equals(typa)) {%>
                        <pre class="form-control-static" data-fn="<%=name%>" data-ft="_text" data-type="<%=typa%>" data-mode="<%=mode%>"></pre>
                    <%} else if ("html".equals(typa)) {%>
                        <div class="form-control-static" data-fn="<%=name%>" data-ft="_html" style="white-space: normal ;"></div>
                    <%} else {%>
                        <div class="form-control-static" data-fn="<%=name%>" data-ft="_text" style="white-space:pre-wrap;"></div>
                    <%}%>
                <%} else if ("email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type)) {%>
                    <div class="form-control-static"><a data-fn="<%=name%>" data-ft="<%=kind%>" class="a-<%=type%>" target="_blank" ></a></div>
                <%} else if ( name.endsWith(".")) {%>
                    <div class="form-control-static"><p data-fn="<%=name%>" data-ft="<%=kind%>" class="label-list" data-item-class="label-info label"></p></div>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} /*End If */%>
                </div>
            </div>
        <%} /*End if */%>
        <%} /*End For*/%>
        <div class="form-group row">
            <div class="col-md-6 col-md-offset-3 col-sm-8 col-sm-offset-2">
                <button type="button" class="cancel btn btn-default">返回</button>
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
        _url: "<%=_module%>/<%=_entity%><%="revert".equals(_action)?"/revert":""%>/search.act?<%=Cnst.RN_KEY%>=0&<%=Cnst.AB_KEY%>=_text,_fork,.form",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
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