<%@page import="app.hongs.CoreConfig"%>
<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.HongsError"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Mview"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.HashMap"%>
﻿<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    // 获取路径动作
    int i;
    String _module, _entity, _action;
    _module = ActionDriver.getWorkPath(request);
    i = _module.lastIndexOf('/');
    _module = _module.substring(1, i);
    i = _module.lastIndexOf('/');
    _entity = _module.substring(i+ 1);
    _module = _module.substring(0, i);
    _action = Synt.declare(request.getAttribute("form.action"), "create");

    // 获取字段集合
    CoreLocale lang;
    Map        flds;
    do {
        try {
            Mview view = new Mview(DB.getInstance(_module).getTable(_entity));
            lang = view.getLang(  );
            flds = view.getFields();
            break;
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x1039) {
                throw ex;
            }
        } catch (HongsError ex) {
            if (ex.getErrno() != 0x2a  ) {
                throw ex;
            }
        }

        FormSet form = FormSet.hasConfFile(_module+"/"+_entity)
                     ? FormSet.getInstance(_module+"/"+_entity)
                     : FormSet.getInstance(_module);
        flds = form.getFormTranslated(_entity );
        lang = CoreLocale.getInstance().clone();
        lang.loadIgnrFNF(_module);
        lang.loadIgnrFNF(_module +"/"+ _entity);
    } while (false);

    // 获取资源标题
    String id , nm ;
    id = (_module +"-"+ _entity +"-"+ _action).replace('/','-');
    do {
        NaviMap site = NaviMap.hasConfFile(_module+"/"+_entity)
                     ? NaviMap.getInstance(_module+"/"+_entity)
                     : NaviMap.getInstance(_module);
        Map menu  = site.getMenu(_module+"/#"+_entity);
        if (menu != null) {
            nm = (String) menu.get("text");
            if (nm != null) {
                nm  = lang.translate( nm );
                break;
            }
        }

        nm = Dict.getValue( flds, "", "@", "text" );
    } while (false);
%>
<!-- 表单 -->
<h2><%=lang.translate("fore."+_action+".title", nm)%></h2>
<div id="<%=id%>" class="row">
    <form action="" method="POST" enctype="multipart/form-data">
        <%
        Iterator it = flds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next( );
            Map     info = (Map ) et.getValue( );
            String  name = (String) et.getKey( );

            if ("@".equals(name)
            || !Synt.declare(info.get("editable"), true)) {
                continue ;
            }

            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
            String  rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
            String  rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";

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
                <label class="col-sm-3 form-control-static control-label text-right"><%=text%></label>
                <div class="col-sm-6">
                <%if ("textarea".equals(type)) {%>
                    <textarea class="form-control" name="<%=name%>" <%=rqrd%>></textarea>
                <%} else if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type)) {%>
                    <%
                        String extr = "";
                        if ("string".equals(type)) type = "text";
                        if (info.containsKey("size")) extr += " size=\""+info.get("size").toString()+"\"";
                        if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                        if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                        if (info.containsKey("pattern")) extr += " pattern=\""+info.get("pattern").toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" <%=rqrd%><%=extr%>/>
                <%} else if ("number".equals(type) || "range".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("step")) extr += " step=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("min")) extr += " min=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("max")) extr += " max=\""+info.get("max").toString()+"\"";
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" value="" <%=rqrd%><%=extr%>/>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("format")) extr += " data-format=\""+info.get("format").toString()+"\"";
                    %>
                    <input class="form-control input-date" type="text" name="<%=name%>" value="" readonly="readonly" <%=rqrd%><%=extr%> data-toggle="hsDate"/>
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
                        } else
                        if ("image".equals(type) || "video".equals(type) || "audio".equals(type)) {
                            extr += " accept=\""+type+"/*\"";
                            if ("image".equals(type)) {
                                ft =  "_view";
                                fm = "hsView";
                                size = Synt.asserts( info.get("thumb-size"), "" );
                                keep = Synt.asserts( info.get("thumb-mode"), "" );
                                if (size.length()==0) {
                                    size = "100*100";
                                    keep = "keep";
                                } else {
                                    size = size.replaceFirst("\\d+\\*\\d+", "$0");
                                    if ( ! keep.equals("keep") ) {
                                        keep = "";
                                    }
                                }
                            }
                        }
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <input type="file" name="<%=name%>" class="form-ignored invisible" <%=extr%>/>
                    <ul class="pickbox" data-ft="<%=ft%>" data-fn="<%=name%>" data-size="<%=size%>" data-keep="<%=keep%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=fm%>"><%=lang.translate("fore.file.browse")%></button>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") :  "id" ;
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :
                                  ( info.containsKey("form"   ) ? (String) info.get("form"   ) : name.replaceFirst("_id$", "") );
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :
                                  ( info.containsKey("conf"   ) ? (String) info.get("conf"   ) : _module )
                                + ( info.containsKey("form"   ) ? (String) info.get("form"   ) : /**/ ak )
                                + "list_fork.html";
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <ul class="pickbox" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>" <%=rqrd%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=lang.translate("fore.select.lebel", text)%></button>
                <%} else {%>
                    <input class="form-control" <%="type=\""+type+"\" name=\""+name+"\" "+rqrd%>/>
                <%} /*End If */%>
                </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <div class="form-group row">
            <div class="col-sm-6 col-sm-offset-3">
                <button type="submit" class="ensure btn btn-primary"><%=lang.translate("fore.ensure")%></button>
                <button type="button" class="cancel btn btn-link"   ><%=lang.translate("fore.cancel")%></button>
            </div>
        </div>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=id%>");

    context.hsForm({
        loadUrl: "<%=_module%>/<%=_entity%>/search.act?id=\${id}&md=\${md}",
        saveUrl: "<%=_module%>/<%=_entity%>/<%=_action%>.act",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });
})( jQuery );
</script>