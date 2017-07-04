<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    // 获取路径动作
    int i;
    String _module, _entity;
    _module = ActionDriver.getWorkPath(request);
    i = _module.lastIndexOf('/');
    _module = _module.substring(1, i);
    i = _module.lastIndexOf('/');
    _entity = _module.substring(i+ 1);
    _module = _module.substring(0, i);
    
    // 获取字段集合
    CoreLocale lang;
    Map        flds;
    do {
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
    id = (_module +"-"+ _entity +"-logs-info").replace('/','-');
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
    
    String tm = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        .format(new Date(Synt.declare(request.getParameter("ctime"), 0L) * 1000));
%>
<h2><%=nm%>历史(<%=tm%>)</h2>
<div id="<%=id%>">
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
            String  hint = (String) info.get("__hint__");
            String  rqrd = Synt.declare(info.get("__required__"), false) ? "required=\"required\"" : "";
            String  rptd = Synt.declare(info.get("__repeated__"), false) ? "multiple=\"multiple\"" : "";

            if (!"".equals(rptd)) {
                rptd += " size=\"3\"";
                name += ".";
            }
        %>
        <%if ("hidden".equals(type)) {%>
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
                <div class="col-sm-6 form-control-static" data-fn="<%=name%>"></div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=id%>").removeAttr("id");

    context.hsForm({
        loadUrl: "<%=_module%>/<%=_entity%>/revert/search.act?id=\${id}&md=\${md}",
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });
})( jQuery );
</script>