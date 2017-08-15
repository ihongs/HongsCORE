<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_more_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("form.action"), "create");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
%>
<!-- 表单 -->
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="row">
    <form action="<%=_module%>/<%=_entity%>/<%=_action%>.act"
          method="POST" enctype="multipart/form-data">
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
            String  hint = (String) info.get("__hint__");
            String  rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
            String  rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";

            if (text != null) text = _locale.translate(text);
            
            if (!"".equals(rptd)) {
                rptd += " size=\"3\"";
                name += ".";
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
                    <textarea class="form-control" name="<%=name%>" placeholder="<%=hint%>" <%=rqrd%>></textarea>
                <%} else if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type)) {%>
                    <%
                        String extr = "";
                        if ("string".equals(type)) type = "text";
                        if (info.containsKey("size")) extr += " size=\""+info.get("size").toString()+"\"";
                        if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                        if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                        if (info.containsKey("pattern")) extr += " pattern=\""+info.get("pattern").toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%>/>
                <%} else if ("number".equals(type) || "range".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("step")) extr += " step=\""+info.get("step").toString()+"\"";
                        if (info.containsKey("min")) extr += " min=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("max")) extr += " max=\""+info.get("max").toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" placeholder="<%=hint%>" <%=rqrd%><%=extr%>/>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String extr = " data-type=\""+info.get( "type" ).toString()+"\"";
                        if (info.containsKey("format")) {
                            extr += " data-format=\""+info.get("format").toString()+"\"";
                        } else {
                            extr += " data-format=\""+type+"\"";
                        }
                    %>
                    <input class="form-control input-date" type="text" name="<%=name%>" value="" placeholder="<%=hint%>" data-fl="v ? v : new Date().getTime() / 1000" <%=rqrd%><%=extr%> data-toggle="hsDate"/>
                <%} else if ("check".equals(type)) {%>
                    <div class="checkbox" data-fn="<%=name%>" data-ft="_check" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>"></div>
                <%} else if ("radio".equals(type)) {%>
                    <div class="radio"    data-fn="<%=name%>" data-ft="_radio" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>"></div>
                <%} else if ("enum".equals(type) || "select".equals(type)) {%>
                    <select class="form-control" name="<%=name%>" <%=rqrd%> <%=rptd%>></select>
                <%} else if ("file".equals(type) || "upload".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String size = "";
                        String keep = "";
                        String ft =  "_file";
                        String fm = "hsFile";
                        if (info.containsKey("type")) {
                            extr += " accept=\""+info.get("type").toString()+"\"";
                        } else {
                            extr += " accept=\""+type+"/*\"";
                        }
                        if ("image".equals(type)) {
                            ft =  "_view";
                            fm = "hsView";
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
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <input type="file" name="<%=name%>" class="form-ignored invisible" <%=extr%>/>
                    <ul class="pickbox" data-ft="<%=ft%>" data-fn="<%=name%>" data-size="<%=size%>" data-keep="<%=keep%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=fm%>"><%=_locale.translate("fore.file.browse")%></button>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") :  "id" ;
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :
                                  ( info.containsKey("form"   ) ? (String) info.get("form"   ) : name.replaceFirst("_id$", "") );
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :
                                  ( info.containsKey("conf"   ) ? (String) info.get("conf"   ) : _module )
                                + ( info.containsKey("form"   ) ? (String) info.get("form"   ) :    ak   )
                                + "list_fork.html";
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <ul class="pickbox" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=_locale.translate("fore.select.lebel", text)%></button>
                <%} else {%>
                    <input class="form-control" <%="type=\""+type+"\" name=\""+name+"\" "+rqrd%>/>
                <%} /*End If */%>
                </div>
                <div class="col-sm-6 col-sm-offset-3 help-block"></div>
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

    var formobj = context.hsForm({
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });

    // 附加脚本
    if (self.inMyForm) {
        self.inMyForm(context);
    }

    // 加载数据
    formobj.load("<%=_module%>/<%=_entity%>/search.act"
            + "?id=" + H$("&id", context)
            + "&md=" + H$("&md", context) );
})( jQuery );
</script>