<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("info.action"), "recite");
    String _funcId = "in_"+(_module + "_" + _entity + "_info").replace('/', '_');
    String _pageId = /* */ (_module + "-" + _entity + "-info").replace('/', '-');
%>
<h2 class="hide"><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId+" "+_action%>-info">
    <form action="" onsubmit="return false"
          method="POST" enctype="multipart/form-data" class="form-horizontal">
        <div class="panel panel-default">
        <div class="form-body">
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

            String  type = Synt.asString(info.get("__type__"));
            String  text = Synt.asString(info.get("__text__"));
            boolean rptd = Synt.declare(info.get("__repeated__"), false);

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
        <%if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>" data-fn/>
        <%} else if ("legend".equals(type)) {%>
            <%
                text = Synt.defxult(Synt.asString(info.get("info-text")), text, "");
            %>
            <div class="form-group row" data-name="<%=name%>">
                <div class="col-xs-12">
                    <legend><%=text%></legend>
                </div>
            </div>
        <%} else if ("figure".equals(type)) {%>
            <%
                text = Synt.defxult(Synt.asString(info.get("info-text")), text, "");
            %>
            <div class="form-group row" data-name="<%=name%>">
                <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                    <figure><%=text%></figure>
                </div>
            </div>
        <%} else {%>
            <%
                String hint;
                text = Synt.defxult(Synt.asString(info.get("info-text")), text, "");
                hint = Synt.defxult(Synt.asString(info.get("info-hint")), "" );
            %>
            <%if ("form".equals(type) || "part".equals(type)) {%>
            <div class="form-group" data-name="<%=name%>" style="margin:0;">
                <%
                    String extr = "";
                    String kind =  "_form";
                    String href = Synt.defxult(Synt.asString(info.get("data-rt")), Synt.asString(info.get("data-rl")), "");
                    href = href.replace( "centra", "centre" );
                        if (rptd) {
                        name  = name + "."; // 多选末尾加点
                        extr += " data-repeated=\"repeated\"";
                    }
                %>
                <legend class="group"><%=text%></legend>
                <div class="help-block text-muted form-control-static"><%=hint%></div>
                <div class="form-subs" data-ft="<%=kind%>" data-fn="<%=name%>" data-href="<%=href%>"<%=extr%> data-sub-class="group panel panel-body panel-default" data-sub-style="padding-left:0;padding-right:0;"></div>
            </div>
            <%continue; } /*End sub form*/%>
            <div class="form-group row" data-name="<%=name%>">
                <label class="col-sm-3 col-md-2 text-right form-label control-label form-control-static"><%=text%></label>
                <div class="col-sm-9 col-md-8">
                <%if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFork";
                        String kind =  "_fork";
                        String kn   =    name ;
                        if (kn.endsWith("_id")) {
                            kn = kn.substring(0, kn.length() - 3);
                        } else {
                            kn = kn +  "_fork"; // 增加特定后缀
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " data-repeated=\"repeated\"";
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") :  kn ;
                        String rt = info.containsKey("data-rt") ? (String) info.get("data-rt") :
                                  ( info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" );
                        rt = rt.replace("centra", "centre");
                        /**
                         * 默认禁止扩展功能
                         */
                        if (!rt.isEmpty() && !rt.contains(".deny=")) {
                            if (!rt.contains("?") && !rt.contains("#")) {
                                rt = rt + "?.deny=.expand";
                            } else {
                                rt = rt + "&.deny=.expand";
                            }
                        }
                        kind += "\" data-ln=\""+ln+"\" data-tk=\""+tk+"\" data-vk=\""+vk
                             +  "\" data-href=\""+rt+"\" data-target=\"@";
                    %>
                    <div class="form-control-static">
                        <ul class="repeated forkbox" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
                    </div>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFile";
                        String kind =  "_file";
                        String klas =   "file";
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " data-repeated=\"repeated\"";
                        }
                        if ("image".equals(type)) {
                            mode = "hsPict";
                            kind =  "_pict";
                            klas =   "pict";
                            String size = Synt.declare(info.get("thumb-size"), "");
                            String moda = Synt.declare(info.get("thumb-mode"), "");
                            if (rptd
                            &&  moda.length() == 0) {
                                moda = "keep"; // 多选但未指定模式，采用保留以便对齐
                            }
                            if (size.length() != 0) {
                                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)").matcher(size);
                                if ( m.find() ) {
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
                    <div class="form-control-static">
                        <ul class="repeated <%=klas%>box" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
                    </div>
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
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
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String kind = "_"+ type;
                        // 日期类需注意 Unix 时间戳需要乘 1000
                        Object typa = info.get("type");
                        if ("timestamp".equals( typa )
                        ||  "datestamp".equals( typa )) {
                            kind += "\" data-fill=\"!v?v:v*1000" ;
                        }
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            kind += "\" data-format=\"" + frmt;
                        }
                    %>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} else if ("number".equals(type) || "sorted".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <%
                        String kind = "_review";
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            kind += "\" data-format=\"" + frmt;
                        }
                    %>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                    <%
                        name += "_text";
                        if (rptd) {
                            name += ".";
                        }
                    %>
                    <div class="form-control-static">
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated" data-item-class="label label-default"></ul>
                    </div>
                <%} else if ( rptd ) {%>
                    <%
                            name += ".";
                    %>
                    <div class="form-control-static">
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated" data-item-class="label label-default"></ul>
                    </div>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="_review"></div>
                <%} /*End If */%>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        </div><!-- end form-body -->
        <div class="form-foot">
            <div class="form-group row">
                <div class="col-sm-9 col-md-8 col-sm-offset-3 col-md-offset-2">
                    <div class="btn-toolbar">
                        <button type="button" class="cancel btn btn-default"><%=_locale.translate("fore.goback")%></button>
                    </div>
                </div>
            </div>
        </div><!-- end form-foot -->
        </div><!-- end panel -->
    </form>
    <div class="pagebox"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest( ".loadbox" );
    var formbox = context.find("form").first( );

    var loadres = hsSerialDic(loadbox);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    var formobj = context.hsForm({
        _url : "<%=_module%>/<%=_entity%>/recite.act?<%=Cnst.AB_KEY%>=_text,_fork,.fall",
        _data: loadres,
        _fill__file: hsFormFillFile,
        _fill__pict: hsFormFillPict,
        _fill__fork: hsFormFillFork,
        _fill__form: hsFormFillPart,
        _feed__form: hsFormFeedPart
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, formobj))
         .then(function() {

        // 外部限制
        $.each(denycss ? denycss.split(",") : []
        , function(i, n) {
            if (/^form\./.test(n)) {
                n = ".form-group[data-name='"+n.substring(5)+"']";
                formbox.find(n).remove();
            } else {
                context.find(n).remove();
            }
        });

        // 特殊控件
        setInfoItems(formbox, loadbox);

        // 加载数据
        formobj.load();

        }); // End Promise
    });
})(jQuery);
</script>