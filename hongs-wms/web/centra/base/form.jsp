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
    String _action = Synt.declare(request.getAttribute("form.action"), "update");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_form").replace('/', '_');
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-form">
    <form action="<%=_module%>/<%=_entity%>/<%=_action%>.act"
          method="POST" enctype="multipart/form-data" class="form-horizontal">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get("readonly"), false)) {
                continue ;
            }

            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
            String  hint = (String) info.get("__hint__");
            String  rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
            String  rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";

            if (rptd.length() != 0) {
                name += ".";
            }
            if (text == null) {
                text  = "" ;
            }
            if (hint == null) {
                hint  = "" ;
            }
        %>
        <%/****/ if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>" />
        <%} else if ("legend".equals(type)) {%>
            <legend class="text-center"><%=text%></legend>
        <%} else {%>
            <div class="form-group row">
                <label class="col-xs-3 col-md-2 control-label text-right"><%=text%></label>
                <div class="col-xs-9 col-md-8">
                <%if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <%
                        String extr = "";
                        String typa = (String) info.get("type");
                        String mode = (String) info.get("mode");
                        if (null != typa &&!"".equals(typa)) {
                            extr += " data-type=\"" + typa + "\"";
                        if (null != mode &&!"".equals(mode)) {
                            extr += " data-mode=\"" + mode + "\"";
                        }
                            extr += " style=\"width:100%; height:15em; border:0;\"";
                        } else {
                            extr += " class=\"form-control\" style=\"height:5em;\"";
                        }
                    %>
                    <textarea id="<%=_pageId%>-<%=name%>" name="<%=name%>" placeholder="<%=hint%>" <%=rqrd%><%=extr%>></textarea>
                    <%hint ="";%>
                <%} else if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type)) {%>
                    <%
                        String extr = "";
                        if (!"".equals(rptd)) {
                            name  = name.substring(0, name.length() - 1);
                            extr += " data-toggle=\"tagsinput\"";
                            info.remove("minlength");
                            info.remove("maxlength");
                            info.remove( "pattern" );
                        }
                        if ("string".equals(type)) type = "text";
                        if (info.containsKey("size")) extr += " size=\""+info.get("size").toString()+"\"";
                        if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                        if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                        if (info.containsKey( "pattern" )) extr += " pattern=\""  +info.get("pattern"  ).toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%>/>
                    <%hint ="";%>
                <%} else if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                    <%
                        String extr = "";
                        if ("sorted".equals(type)) type = "number";
                        if (info.containsKey("step")) extr += " step=\""+info.get("step").toString()+"\"";
                        if (info.containsKey("min" )) extr += " min=\"" +info.get("min" ).toString()+"\"";
                        if (info.containsKey("max" )) extr += " max=\"" +info.get("max" ).toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%>/>
                    <%hint ="";%>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String fomt = Synt.declare(info.get("format"),  type );
                        String typa = Synt.declare(info.get( "type" ), "date");
                        String extr = " data-type=\""+typa +"\" data-format=\""+fomt+"\"";
                        if (info.containsKey("min" )) extr += " min=\"" +info.get("min" ).toString()+"\"";
                        if (info.containsKey("max" )) extr += " max=\"" +info.get("max" ).toString()+"\"";
                        if ( ! "" .equals(rqrd) ) {
                        if ("time".equals(typa) || "date".equals(typa)) {
                            extr += " data-fl=\"v ? v : new Date().getTime()\""     ;
                        } else {
                            extr += " data-fl=\"v ? v : new Date().getTime()/1000\"";
                        }}
                    %>
                    <input class="form-control" type="text" name="<%=name%>" value="" <%=rqrd%><%=extr%> data-toggle="hsDate"/>
                <%} else if ("check".equals(type)) {%>
                    <%if ("".equals(rqrd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} else { rqrd = "data-"+rqrd; }%>
                    <div class="checkbox" data-fn="<%=name%>" data-ft="_check" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>" <%=rqrd%>></div>
                <%} else if ("radio".equals(type)) {%>
                    <%if ("".equals(rqrd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} else { rqrd = "data-"+rqrd; }%>
                    <div class="radio"    data-fn="<%=name%>" data-ft="_radio" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>" <%=rqrd%>></div>
                <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type)) {%>
                    <%if ("".equals(rqrd) && !"".equals(rptd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */%>
                    <select class="form-control" name="<%=name%>" <%=rqrd%> <%=rptd%>></select>
                <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        String mode = "hsFork";
                        String kind =  "_fork";
                        String fn = name;
                        if (! "".equals(rptd) ) {
                            rptd  = "data-repeated=\"repeated\"" ;
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
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                        rl = rl.replace("centre", "centra");
                        al = al.replace("centre", "centra");
                        kind += "\" data-ak=\""+ak+"\" data-tk=\""+tk+"\" data-vk=\""+vk
                             +  "\" data-href=\""+rl+"\" data-target=\"@";
                        mode += "\" data-href=\""+al+"\" data-target=\"@";
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>" <%=rqrd%> <%=rptd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.fork.select", text)%></button>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String mode = "hsFile";
                        String kind =  "_file";
                        String typa = (String) info.get("type");
                        if (typa == null || typa.length() == 0) {
                            typa  = type +"/*";
                        }
                        if ("image".equals(type)) {
                            mode  = "hsView";
                            kind  =  "_view";
                            String moda = Synt.declare( info.get("thumb-mode"), "");
                            String size = Synt.declare( info.get("thumb-size"), "");
                            if (size.length( ) != 0  ) {
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
                    <input type="file" name="<%=name%>" accept="<%=typa%>" <%=rptd%> class="form-ignored invisible"/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.file.browse", text)%></button>
                <%} else {%>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hint%>" <%=rqrd%> <%=rptd%>/>
                    <%hint ="";%>
                <%} /*End If */%>
                    <%
                        String hiss;
                        hiss = Synt.declare(info.get("hiss"),  "" );
                        hint = Synt.declare(info.get("hint"), hint);
                    %>
                    <div class="help-block text-error form-control-static"><%=hiss%></div>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <hr/>
        <div class="row">
            <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                <button type="submit" class="commit btn btn-primary"><%=_locale.translate("fore.commit")%></button>
                <button type="button" class="cancel btn btn-link"   ><%=_locale.translate("fore.cancel")%></button>
            </div>
        </div>
        <br/>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var formbox = context.find("form");

    var formobj = context.hsForm({
        <%if ("create".equals(_action)) {%>
        _url: "<%=_module%>/<%=_entity%>/select.act?<%=Cnst.RN_KEY%>=0&<%=Cnst.AB_KEY%>=.enum,.info,.form,_fork,_text",
        <%} else {%>
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.RN_KEY%>=0&<%=Cnst.AB_KEY%>=.enum,.info,.form,_fork,_text",
        <%if (! _fields.containsKey("memo")) {%>
        save: hsSaveWithMemo('<%=_locale.translate("fore.update.confirm", _title)%>'),
        <%}} /* End if */%>
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
        setFormItems( formbox , loadbox);

        // 加载数据
        formobj.load(undefined, loadbox);
    });
})( jQuery );
</script>