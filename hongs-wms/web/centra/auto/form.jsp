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
    String _action = Synt.declare(request.getAttribute("form.action"), "create");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_form").replace('/', '_');
%>
<!-- 表单 -->
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-form">
    <form action="<%=_module%>/<%=_entity%>/<%=_action%>.act"
          method="POST" enctype="multipart/form-data">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            || !Synt.declare(info.get("editable"), true)
            || !Synt.declare(info.get("saveable"), true)) {
                continue ;
            }

            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
            String  hint = (String) info.get("__hint__");
            String  rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
            String  rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";

            if (!"".equals(rptd)) {
                name = name + "." ;
            }
        %>
        <%if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>" value="<%="form_id".equals(name)?_entity:""%>"/>
        <%} else if ("line".equals(type)) {%>
            <legend class="form-group"><%=text%></legend>
        <%} else if ("checkbag".equals(type)) {%>
            <fieldset>
                <legend><%=text%></legend>
                <div class="form-group" data-ft="_checkset" data-fn="<%=name%>" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>" <%=rqrd%>></div>
            </fieldset>
        <%} else {%>
            <div class="form-group row">
                <label class="col-sm-3 control-label form-control-static text-right"><%=text%></label>
                <div class="col-sm-6">
                <%if ("textarea".equals(type)) {%>
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
                <%} else if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type)) {%>
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
                <%} else if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                    <%
                        String extr = "";
                        if ("sorted".equals(type)) type = "number";
                        if (info.containsKey("step")) extr += " step=\""+info.get("step").toString()+"\"";
                        if (info.containsKey("min" )) extr += " min=\"" +info.get("min" ).toString()+"\"";
                        if (info.containsKey("max" )) extr += " max=\"" +info.get("max" ).toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%>/>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String fomt = Synt.declare(info.get("format"),  type      );
                        String typa = Synt.declare(info.get( "type" ), "timestamp");
                        String extr = " data-type=\""+typa +"\" data-format=\""+fomt+"\"";
                        if (info.containsKey("min" )) extr += " data-min=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("max" )) extr += " data-max=\""+info.get("max").toString()+"\"";
                        if ("time".equals(typa) || "date".equals(typa)) {
                            extr += " data-fl=\"v ? v : new Date().getTime()\""     ;
                        } else {
                            extr += " data-fl=\"v ? v : new Date().getTime()/1000\"";
                        }
                    %>
                    <input class="form-control input-date" type="text" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%> data-toggle="hsDate"/>
                <%} else if ("check".equals(type)) {%>
                    <%if ("".equals(rqrd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */%>
                    <div class="checkbox" data-fn="<%=name%>" data-ft="_check" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>"></div>
                <%} else if ("radio".equals(type)) {%>
                    <%if ("".equals(rqrd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */%>
                    <div class="radio"    data-fn="<%=name%>" data-ft="_radio" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>"></div>
                <%} else if ("enum".equals(type) || "select".equals(type)) {%>
                    <%if ("".equals(rqrd) && !"".equals(rptd)) {%>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */%>
                    <select class="form-control" name="<%=name%>" <%=rqrd%> <%=rptd%>></select>
                <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        String kind =  "_fork";
                        String mode = "hsFork";
                        String fm = _module;
                        String fn =  name  ;
                        if (! "".equals(rptd) ) {
                            rptd  = "data-repeated=\"repeated\"" ;
                            fn = fn.substring(0, fn.length() - 1);
                        }
                        String kn = fn +"_data";
                        if (fn.endsWith("_id")) {
                            fn = fn.substring(0, fn.length() - 3);
                            kn = fn;
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :
                                  ( info.containsKey("conf"   ) ? (String) info.get("conf"   ) :  fm )
                            +"/"+ ( info.containsKey("form"   ) ? (String) info.get("form"   ) :  fn )
                            +"/list_fork.html";
                        kind += "\" data-ak=\""+ak+"\" data-tk=\""+tk+"\" data-vk=\""+vk+"\"";
                        mode += "\" data-href=\""+al+"\" data-target=\"@\"";
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>" <%=rqrd%> <%=rptd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.fork.select", text)%></button>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String mode = "hsFile";
                        String kind =  "_file";
                        String typa = (String) info.get("type");
                        if (typa != null && typa.length() != 0) {
                            typa  = " accept=\"" + typa + "\"" ;
                        } else {
                            typa  = " accept=\"" + type +"/*\"";
                        }
                        if ("image".equals(type)) {
                            mode  = "hsView";
                            kind  =  "_view";
                            String size = Synt.declare( info.get("thumb-size"), "");
                            String keep = Synt.declare( info.get("thumb-mode"), "");
                            if (! "keep".equals(keep)) {
                                keep = "";
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
                            kind += "\" data-size=\""+size+"\" data-keep=\""+keep+"\"";
                        }
                    %>
                    <input type="file" name="<%=name%>" class="form-ignored invisible" <%=typa%>/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.file.browse", text)%></button>
                <%} else {%>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" <%=rqrd%> <%=rptd%>/>
                <%} /*End If */%>
                </div>
                <div class="col-sm-3 help-block form-control-static"></div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <div class="form-group row">
            <div class="col-sm-6 col-sm-offset-3">
                <button type="submit" class="ensure btn btn-primary"><%=_locale.translate("fore.ensure")%></button>
                <button type="button" class="cancel btn btn-link"   ><%=_locale.translate("fore.cancel")%></button>
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
        _url : "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=<%="create".equals(_action)?"!enum":".enum,_fork"%>"
    });

    hsRequires("<%=_module%>/<%=_entity%>/custom.js", function() {
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