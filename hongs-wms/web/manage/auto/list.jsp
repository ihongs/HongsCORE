<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.CoreConfig"%>
<%@page import="app.hongs.HongsError"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Mview"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
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
    _action = Synt.declare(request.getAttribute("list.action"), "list");

    // 获取字段集合
    CoreLocale lang;
    Map        flds;
    Set        lsts = null;
    Set        srts = null;
    do {
        try {
            Mview view = new Mview(DB.getInstance(_module).getTable(_entity));
            lang = view.getLang(  );
            flds = view.getFields();
            lsts = Synt.asTerms(view.listable);
            srts = Synt.asTerms(view.sortable);
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

        FormSet form = FormSet.hasConfFile(_module +"/"+ _entity)
                     ? FormSet.getInstance(_module +"/"+ _entity)
                     : FormSet.getInstance(_module);
        flds = form.getFormTranslated(_entity );
        lsts = Synt.asTerms(Dict.getDepth(flds, "@", "listable"));
        srts = Synt.asTerms(Dict.getDepth(flds, "@", "sortable"));
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
            nm = (String) menu.get("disp");
            if (nm != null) {
                nm  = lang.translate( nm );
                break;
            }
        }

        nm = Dict.getValue( flds, "", "@", "disp" );
    } while (false);
%>
<h2><%=lang.translate("fore."+_action+".title", nm)%></h2>
<div id="<%=id%>" class="row">
    <div>
        <div class="toolbox col-md-9 btn-group">
            <%if ( "select".equals(_action)) {%>
            <button type="button" class="ensure btn btn-primary"><%=lang.translate("fore.select", nm)%></button>
            <%} // End If %>
            <button type="button" class="create btn btn-default"><%=lang.translate("fore.create", nm)%></button>
            <%if (!"select".equals(_action)) {%>
            <button type="button" class="update for-choose btn btn-default"><%=lang.translate("fore.update", nm)%></button>
            <button type="button" class="delete for-checks btn btn-warning" title="<%=lang.translate("fore.delete", nm)%>"><span class="glyphicon glyphicon-trash"></span></button>
            <%} // End If %>
        </div>
        <form class="findbox col-md-3 input-group" action="" method="POST">
            <input type="search" name="wd" class="form-control input-search"/>
            <span class="input-group-btn">
                <button type="submit" class="btn btn-default" title="<%=lang.translate("fore.search", nm)%>"><span class="glyphicon glyphicon-search"></span></button>
            </span>
        </form>
    </div>
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="id[]" data-ft="<%if ("select".equals(_action)) {%>_pick<%} else {%>_check<%}%>" class="_check">
                        <input type="checkbox" class="checkall" name="id[]"/>
                    </th>
                <%
                Iterator it = flds.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry et = (Map.Entry)it.next( );
                    Map     info = (Map ) et.getValue( );
                    String  name = (String) et.getKey( );
                    String  type = (String) info.get("__type__");
                    String  disp = (String) info.get("__disp__");

                    if ("@".equals(name) || "hidden".equals(type)) {
                        continue;
                    }

                    if ( (lsts != null
                    && !lsts.contains(name) )
                    || !Synt.declare(info.get("listable"), false)) {
                        continue;
                    }

                    String ob = "";
                    String oc = "";
                    if ( (srts != null
                    &&  srts.contains(name) )
                    ||  Synt.declare(info.get("sortable"), false)) {
                        ob = (String)info.get("data-ob" );
                        if (ob == null) {
                            ob = name;
                        }
                        ob = "data-ob=\""+ob+"\"";
                        oc = "sortable";
                    }
                %>
                <%if ("number".equals(type) || "range".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> text-right"><%=disp%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_htime" <%=ob%> class="<%=oc%> datetime"><%=disp%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date" <%=ob%> class="<%=oc%> date"><%=disp%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time" <%=ob%> class="<%=oc%> time"><%=disp%></th>
                <%} else if ("file".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_file" <%=ob%> class="<%=oc%>"><%=disp%></th>
                <%} else if ("enum".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                    <th data-fn="<%=name%>_disp" <%=ob%> class="<%=oc%>"><%=disp%></th>
                <%} else if ("pick".equals(type) ||   "fork".equals(type)) {%>
                    <th data-fn="<%=info.get("data-ak")%>.<%=info.get("data-tk")%>" data-ft="_fork" <%=ob%> class="<%=oc%>"><%=disp%></th>
                <%} else if ("form".equals(type)) {%>
                    <th data-fn="<%=info.get("name")%>.<%=info.get("data-tk")%>" data-ft="_form" <%=ob%> class="<%=oc%>"><%=disp%></th>
                <%} else if (!"primary".equals(info.get("primary")) && !"foreign".equals(info.get("foreign"))) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=disp%></th>
                <%} /*End If */%>
                <%} /*End For*/%>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <div class="pagebox"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=id%>");

    context.hsList({
        loadUrl : "<%=_module%>/<%=_entity%>/retrieve.act?md=2",
        openUrls: [
            ['.create', '<%=_module%>/<%=_entity%>/form.html?md=0', '@'],
            ['.update', '<%=_module%>/<%=_entity%>/form_edit.html?md=1&id={ID}', '@']
        ],
        sendUrls: [
            ['.delete', '<%=_module%>/<%=_entity%>/delete.act', '<%=lang.translate("fore.delete.confirm", nm)%>']
        ],
        _fill__fork: hsListFillFork
    });
})( jQuery );
</script>