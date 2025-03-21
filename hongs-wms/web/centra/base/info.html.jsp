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
        <div class="rollbox panel panel-default">
        <div class="form-body">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get( "disabled" ), false)
            ||  Synt.declare(info.get("unreadable"), false)) {
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
            <div class="form-group" data-name="<%=name%>">
                <%
                    String extr = "";
                    String kind =  "_form";
                    String href = Synt.defxult(Synt.asString(info.get("data-rl")), "");
                    href = href.replace( "centre", "centra" );
                    if (rptd) {
                        name  = name + "."; // 多选末尾加点
                        extr += " data-repeated=\"repeated\"";
                    }
                %>
                <div class="col-xs-12">
                    <legend class="group"><%=text%></legend>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                    <div class="form-subs" data-ft="<%=kind%>" data-fn="<%=name%>" data-href="<%=href%>"<%=extr%> data-sub-class="group panel panel-body panel-default"></div>
                </div>
            </div>
            <%continue; } /*End sub form*/%>
            <div class="form-group row" data-name="<%=name%>">
                <label class="col-xs-3 col-md-2 text-right form-label control-label form-control-static"><%=text%></label>
                <div class="col-xs-9 col-md-8">
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
                        rt = rt.replace("centre", "centra");
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
                        <ul class="repeated labelbox forkbox" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
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
                        <ul class="repeated labelbox <%=klas%>box" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
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
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated labelbox" data-item-class="label label-default"></ul>
                    </div>
                <%} else if ( rptd ) {%>
                    <%
                            name += ".";
                    %>
                    <div class="form-control-static">
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated labelbox" data-item-class="label label-default"></ul>
                    </div>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="_review"></div>
                <%} /*End If */%>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
        <%} /*End if */%>
        <%} /*End For*/%>
            <hr/><!-- 兼容占位 -->
        </div><!-- end form-body -->
        </div><!-- end panel -->
        <div class="form-foot">
            <div class="form-group row">
                <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                    <div class="btn-toolbar">
                        <button type="button" class="cancel btn btn-default"><%=_locale.translate("fore.goback")%></button>
                        <%if ("reveal".equals(_action)) {%>
                        <div class="btn-group expand">
                            <button type="button" class=" newer btn btn-default" disabled="disabled"><%=_locale.translate("fore.reveal.newer", _title)%></button>
                            <button type="button" class=" older btn btn-default" disabled="disabled"><%=_locale.translate("fore.reveal.older", _title)%></button>
                        </div>
                        <%} else {%>
                        <div class="btn-group expand">
                            <button type="button" class="reveal btn btn-default"><span class="text-normal"><%=_locale.translate("fore.reveal", _title)%></span></button>
                            <button type="button" class="update btn btn-default"><span class="text-normal"><%=_locale.translate("fore.update", _title)%></span></button>
                            <button type="button" class="delete btn btn-default"><span class="text-danger"><%=_locale.translate("fore.delete", _title)%></span></button>
                        </div>
                        <%} /*End If */%>
                    </div>
                </div>
            </div>
            <br/><!-- 兼容占位 -->
        </div><!-- end form-foot -->
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
        <%if ("reveal".equals(_action)) {%>
        _url : "<%=_module%>/<%=_entity%>/remind.act?<%=Cnst.AB_KEY%>=_text,_fork,.fall,older,newer",
        <%} else {%>
        _url : "<%=_module%>/<%=_entity%>/recite.act?<%=Cnst.AB_KEY%>=_text,_fork,.fall",
        _reveal_url: "<%=_module%>/<%=_entity%>/snap.html" ,
        _update_url: "<%=_module%>/<%=_entity%>/form.html" ,
        _delete_act: "<%=_module%>/<%=_entity%>/delete.act",
        _delete_msg: "<%=_locale.translate("fore.delete.confirm", _title)%>",
        <%} /* End if */%>
        _data: loadres,
        _fill__file: hsFormFillFile,
        _fill__pict: hsFormFillPict,
        _fill__fork: hsFormFillFork,
        _fill__form: hsFormFillPart,
        _feed__form: hsFormFeedPart
    });

    <%if ("reveal".equals(_action)) {%>
    // 新旧记录快速切换
    context.on("loadBack", function(evt, rst) {
        delete formobj._init;
        if (rst.info && rst.snap && rst.snap.newer ) {
            var tt = hsFmtDate(rst.snap.newer*1000 , H$(":datetime.format"));
            context.find(".newer").attr("title", tt).prop("disabled", false)
                                  .data({id: rst.info.id , ctime: rst.snap.newer});
        } else {
            context.find(".newer").attr("title", "").prop("disabled", true );
        }
        if (rst.info && rst.snap && rst.snap.older ) {
            var tt = hsFmtDate(rst.snap.older*1000 , H$(":datetime.format"));
            context.find(".older").attr("title", tt).prop("disabled", false)
                                  .data({id: rst.info.id , ctime: rst.snap.older});
        } else {
            context.find(".older").attr("title", "").prop("disabled", true );
        }
    });
    context.on("click", ".newer,.older", function( ) {
        var loadbox = formobj.loadBox;
        loadbox.hsLoad(loadbox.data("href"), $(this).data());
    });
    <%} else {%>
    // 底部附加快捷操作
    context.on("click", ".reveal", function() {
        var url  = formobj._reveal_url;
        var data = {
            "id" : H$( "?id", loadres )
        };
        hsOpenThenLoad.call(formobj, this, "@", url, data);
    });
    context.on("click", ".update", function() {
        var url  = formobj._update_url;
        var data = {
            "id" : H$( "?id", loadres )
        };
        hsOpenThenLoad.call(formobj, this, "@", url, data);
    });
    context.on("click", ".delete", function() {
        var msg  = formobj._delete_msg;
        var url  = formobj._delete_act;
        var data = {
            "id" : H$( "?id", loadres )
        };
        hsSendWithMemo.call(formobj, this, msg, url, data);
    });
    <%} /*End If */%>

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, formobj))
         .then(function() {

        // 权限控制
        $.each({"update":".update", "delete":".delete", "reveal":".reveal"}
        , function(k, v) {
            if (! hsGetAuth("<%=_module%>/<%=_entity%>/"+k+".act")) {
                context.find(v).remove();
            }
        });
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

        // 自适滚动
        hsFlexRoll(formbox.children(".rollbox"), $("#main-context"));

        // 特殊控件
        setInfoItems(formbox, loadbox);

        // 加载数据
        formobj.load();

        }); // End Promise
    });
})(jQuery);
</script>