<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@page import="app.hongs.CoreConfig"%>
<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Mview"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Map"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%
    String  _module, _entity, _action; int i;
    _module = ActionDriver.getWorkPath(request);
    i = _module.lastIndexOf('/');
    _module = _module.substring(1, i);
    i = _module.lastIndexOf('/');
    _entity = _module.substring(i+ 1);
    _module = _module.substring(0, i);
    _action = (String)request.getAttribute("form.action");
    if (_action == null) _action = "create";

    Mview view = new Mview(DB.getInstance(_module).getModel(_entity));
    Map<String, Map<String, String>> flds = view.getFields();
    CoreLocale lang = CoreLocale.getInstance().clone();
               lang.loadIgnrFNF(_module);

    String title = view.getTitle();
    String idKey = view.getIdKey();

    Map<String, String[]> data = request.getParameterMap();
    if (data.containsKey("id")) {
        data.put( idKey , data.get("id"));
    } else if (! data.containsKey(idKey)) {
        data.put("id", new String[]{"0"});
    }

    Set hide = new HashSet();
    CoreConfig conf = CoreConfig.getInstance();
    hide.add(conf.getProperty("core.table.ctime.field", "ctime"));
    hide.add(conf.getProperty("core.table.mtime.field", "mtime"));
    hide.add(conf.getProperty("core.table.etime.field", "etime"));
    hide.add(conf.getProperty("core.table.state.field", "state"));
%>
<!-- 表单 -->
<h2><%=lang.translate("fore."+_action+".title", title)%></h2>
<object class="config" name="hsInit" data="">
    <param name="width" value="600px"/>
</object>
<div id="<%=_module%>-<%=_entity%>-<%=_action%>">
    <object class="config" name="hsForm" data="">
        <param name="loadUrl" value="<%=_module%>/<%=_entity%>/retrieve.act?<%=idKey%>=<%=data.get("id")[0]%>&md=1"/>
        <param name="saveUrl" value="<%=_module%>/<%=_entity%>/<%=_action%>.act"/>
        <param name="_fill__pick" value="(hsFormFillPick)"/>
    </object>
    <form action="" method="POST">
        <div class="row"><div class="col-md-6">
            <%
            for(Map.Entry et : flds.entrySet()) {
                Map    info = (Map ) et.getValue();
                String name = (String) et.getKey();
                String type = (String) info.get("__type__");
                String disp = (String) info.get("__disp__");
                String rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
                String rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";
                if (!"".equals(rptd)) {
                    rptd += " size=\"3\"";
                    name += ".";
                }

                if ("1".equals(info.get("hideInForm")) || hide.contains(name)) {
                    continue ;
                }
             %>
            <%if ("hidden".equals(type) || data.get(name) != null) {%>
                <input type="hidden" name="<%=name%>"/>
            <%} else if ("checkbag".equals(type)) {%>
                <h3><%=disp%></h3>
                <div class="form-group" data-ft="_checkbag" data-fn="<%=name%>" data-vk="<%=info.get("data-vk")%>" data-tk="<%=info.get("data-tk")%>" <%=rqrd%>></div>
            <%} else {%>
                <div class="form-group">
                    <label class="control-label"><%=disp%></label>
                    <%if ("textarea".equals(type)) {%>
                        <textarea class="form-control" name="<%=name%>" <%=rqrd%>></textarea>
                    <%} else if ("string".equals(type) || "text".equals(type) || "tel".equals(type) || "url".equals(type) || "email".equals(type)) {%>
                        <%
                            String extr = "";
                            if ("string".equals(type)) type = "text";
                            if (info.containsKey("size")) extr += " size=\""+info.get("size").toString()+"\""; 
                            if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                            if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                            if (info.containsKey("pattern")) extr += " pattern=\""+info.get("pattern").toString()+"\"";
                        %>
                        <input class="form-control" type="text" name="<%=name%>" value="" <%=rqrd%><%=extr%>/>
                    <%} else if ("number".equals(type) || "range".equals(type)) {%>
                        <%
                            String extr = "";
                            if (info.containsKey("step")) extr += " step=\""+info.get("min").toString()+"\"";
                            if (info.containsKey("min")) extr += " min=\""+info.get("min").toString()+"\"";
                            if (info.containsKey("max")) extr += " max=\""+info.get("max").toString()+"\"";
                        %>
                        <input class="form-control" type="number" name="<%=name%>" value="" <%=rqrd%><%=extr%>/>
                    <%} else if ("date".equals(type)) {%>
                        <input class="form-control input-date" type="text" name="<%=name%>" value="" data-toggle="datetimepicker" <%=rqrd%>/>
                    <%} else if ("time".equals(type)) {%>
                        <input class="form-control input-time" type="text" name="<%=name%>" value="" data-toggle="datetimepicker" <%=rqrd%>/>
                    <%} else if ("datetime".equals(type)) {%>
                        <input class="form-control input-datetime" type="text" name="<%=name%>" value="" data-toggle="datetimepicker" <%=rqrd%>/>
                    <%} else if ("check".equals(type)) {%>
                        <div class="checkbox" data-fn="<%=name%>" data-ft="_check"></div>
                    <%} else if ("radio".equals(type)) {%>
                        <div class="radio"    data-fn="<%=name%>" data-ft="_radio"></div>
                    <%} else if ("enum".equals(type) || "select".equals(type)) {%>
                        <select class="form-control" name="<%=name%>" <%=rqrd%> <%=rptd%>><option value="">--<%=lang.translate("fore.select.lebel")%>--</option></select>
                    <%} else if ("pick".equals(type)) {%>
                        <%
                            String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") :  "id" ;
                            String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                            String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :
                                      ( info.containsKey("form"   ) ? (String) info.get("form"   ) : name.replace("_id$", "") );
                            String al = info.containsKey("data-al") ? (String) info.get("data-al") :
                                      ( info.containsKey("conf"   ) ? (String) info.get("conf"   ) : _module )
                                    + ( info.containsKey("form"   ) ? (String) info.get("form"   ) : /**/ ak )
                                    + "list4select.html";
                        %>
                        <ul class="pickbox" data-ft="_pick" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>" <%=rqrd%>></ul>
                        <button type="button" class="btn btn-default form-control" data-toggle="hsPick" data-target="@" data-href="<%=al%>"><%=lang.translate("fore.select.lebel", (String)disp)%></button>
                    <%} else {%>
                        <input class="form-control" type="<%=type%>" name="<%=name%>" value="" <%=rqrd%>/>
                    <%} /*End If */%>
                </div>
            <%} /*End If */%>
            <%} /*End For*/%>
        </div></div>
        <div>
            <button type="submit" class="ensure btn btn-primary">提交</button>
            <button type="button" class="cancel btn btn-link">取消</button>
        </div>
    </form>
</div>