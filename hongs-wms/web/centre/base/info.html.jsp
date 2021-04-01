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
          method="POST" enctype="multipart/form-data" class="form-horizontal">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get( "disabled" ), false)
            ||  Synt.declare(info.get("unreadable"), false)
            ||  Synt.declare(info.get("unopenable"), false)) {
                continue ;
            }

            String  text = (String) info.get("__text__");
            String  type = (String) info.get("__type__");

            // 显示 ID
            if (name.equals(Cnst.ID_KEY)) {
                if (type == null
                ||  type.length( ) == 0
                || "hidden".equals(type)) {
                    type = "text";
                }
                if (text == null
                ||  text.length( ) == 0 ) {
                    text =  "ID" ;
                }
            }
        %>
        <%/****/ if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>" />
        <%} else if ("legend".equals(type)) {%>
            <legend class="text-center"><%=text%></legend>
        <%} else {%>
            <%
                String  kind = "_review";

                if ("datetime".equals(type)
                ||      "date".equals(type)
                ||      "time".equals(type)) {
                    kind = "_" + type;
                    // 日期类需注意 Unix 时间戳需要乘 1000
                    Object typa = info.get("type");
                    if ("timestamp".equals( typa )
                    ||  "datestamp".equals( typa )) {
                        kind += "\" data-fl=\"!v?v:v*1000";
                    }
                    // 自定义格式化
                    String frmt = (String) info.get("format");
                    if (frmt != null && frmt.length( ) != 0 ) {
                        kind += "\" data-format=\"" + frmt;
                    }
                } else
                if (  "number".equals(type)
                ||     "range".equals(type)
                ||     "color".equals(type)) {
                    // 自定义格式化
                    String frmt = (String) info.get("format");
                    if (frmt != null && frmt.length( ) != 0 ) {
                        kind += "\" data-format=\"" + frmt;
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
                    name += ".";
                }
            %>
            <div class="form-group row">
                <label class="col-sm-3 col-md-2 control-label text-right"><%=text%></label>
                <div class="col-sm-9 col-md-8">
                <%if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <%
                        String typa = (String) info.get("type");
                        String mode = (String) info.get("mode");
                    %>
                    <%  /**/ if ("code".equals(typa)) {%>
                        <pre class="form-control-static _code" data-fn="<%=name%>" data-ft="_text" data-type="<%=typa%>" data-mode="<%=mode%>"></pre>
                    <%} else if ("html".equals(typa)) {%>
                        <div class="form-control-static _html" data-fn="<%=name%>" data-ft="_html"></div>
                    <%} else {%>
                        <div class="form-control-static _text" data-fn="<%=name%>" data-ft="_text"></div>
                    <%}%>
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
                        String rl = info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" ;
                        rl = rl.replace("centra", "centre");
                        kind += "\" data-ak=\""+ak+"\" data-tk=\""+tk+"\" data-vk=\""+vk
                             +  "\" data-href=\""+rl+"\" data-target=\"@";
                    %>
                    <ul class="pickbox pickrol" data-fn="<%=name%>" data-ft="<%=kind%>"></ul>
                    <button type="button" data-toggle="hsFork" class="hide"></button>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        kind = "_file";
                        if ("image".equals(type)) {
                            kind = "_view";
                            String size = Synt.declare(info.get("thumb-size"), "");
                            String moda = Synt.declare(info.get("thumb-mode"), "");
                            if (name.endsWith ( "." )
                            &&  moda.length( ) == 0 ) {
                                moda = "keep";
                            }
                            if (size.length( ) != 0 ) {
                                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)").matcher(size);
                                if ( m.find( ) ) {
                                    // 限制最大尺寸, 避免撑开容器
                                    int w  = 450 ;
                                    int h  = 150 ;
                                    int sw = Synt.declare(m.group(1), w);
                                    int sh = Synt.declare(m.group(2), h);
                                    if (sw > w) {
                                        sh = w * sh / sw;
                                        sw = w;
                                    }
                                    if (sh > h) {
                                        sw = h * sw / sh;
                                        sh = h;
                                    }
                                    size = sw+"*"+sh;
                                } else {
                                    size = "150*150";
                                }
                            } else {
                                size = "150*150";
                            }
                            kind += "\" data-size=\""+size+"\" data-mode=\""+moda;
                        }
                    %>
                    <ul class="pickbox pickrol" data-fn="<%=name%>" data-ft="<%=kind%>"></ul>
                    <button type="button" data-toggle="hsFile" class="hide"></button>
                <%} else if ("tel".equals(type) || "sms".equals(type) || "email".equals(type)) {%>
                    <div class="form-control-static"><a data-fn="<%=name%>" data-ft="<%=kind%>" class="a-<%=type%>"></a></div>
                <%} else if ("url".equals(type)) {%>
                    <div class="form-control-static"><a data-fn="<%=name%>" data-ft="<%=kind%>" class="a-<%=type%>" target="_blank"></a></div>
                <%} else if (name.endsWith(".")) {%>
                    <div class="form-control-static"><p data-fn="<%=name%>" data-ft="<%=kind%>" class="repeated" data-item-class="label label-default"></p></div>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} /*End If */%>
                </div>
            </div>
        <%} /*End if */%>
        <%} /*End For*/%>
        <hr/>
        <div class="btns-group row">
            <div class="col-sm-9 col-md-8 col-sm-offset-3 col-md-offset-2">
                <button type="button" class="cancel btn btn-primary"><%=_locale.translate("fore.goback")%></button>
            </div>
        </div>
        <br/>
    </form>
    <div class="pagebox"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");

    var formobj = context.hsForm({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork,.fall",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, formobj);
        }

        var loadbox = formobj.loadBox;
        var formbox = formobj.formBox;
        var formurl = formobj._url;

        // 特殊控件
        setFormItems (formbox,loadbox);

        // 加载数据
        formobj.load (formurl,loadbox);
    });
})( jQuery );
</script>