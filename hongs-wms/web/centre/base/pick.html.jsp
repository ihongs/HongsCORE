<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "select");
    String _funcId = "in_"+(_module + "_" + _entity + "_pick").replace('/', '_');
    String _pageId = /* */ (_module + "-" + _entity + "-pick").replace('/', '-');

    String NAME = Synt.declare(_params.get("field-name"), "name");
    String NOTE = Synt.declare(_params.get("field-note"), "note");
    String LOGO = Synt.declare(_params.get("field-logo"), "logo");
    String USER = Synt.declare(_params.get("field-user"),"cuser");

    StringBuilder _ob = new StringBuilder("boost!,mtime!,ctime!");
    StringBuilder _rb = new StringBuilder("id,"+NAME+","+NOTE+","+LOGO+","+USER);
    Set<String>   _wd = getWordable (_fields);
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId+" "+_action%>-list">
    <div style="display: table; width: 100%;">
        <div style="display: table-cell; width: 15px; vertical-align: middle;">
            <div class="pagebox btn-group" style="white-space: nowrap; padding-right: 15px;">
                <button type="button" class="page-prev btn btn-default" style="float: none; display: inline;" title="<%=_locale.translate("fore.list.prev.page")%>" data-pn=""><span class="bi bi-hi-prev"></span></button>
                <button type="button" class="page-next btn btn-default" style="float: none; display: inline;" title="<%=_locale.translate("fore.list.next.page")%>" data-pn=""><span class="bi bi-hi-next"></span></button>
            </div>
        </div>
        <div style="display: table-cell; width: 100%; vertical-align: middle;">
            <form class="findbox input-group" method="POST" action="">
                <%
                    StringBuilder sp = new StringBuilder( );
                    if (! _wd.isEmpty()) {
                    for(String ss : _wd) {
                        ss = Dict.getValue(_fields, "", ss , "__text__" );
                        if (ss.length() != 0) sp.append(ss).append(", " );
                    }   if (sp.length() != 0) sp.setLength(sp.length()-2);
                    } else {
                        sp.append("\" disabled=\"disabled");
                    }
                %>
                <input type="search" class="form-control" name="<%=Cnst.WD_KEY%>" placeholder="<%=sp%>" /><!--<%=_wd%>-->
                <span class="input-group-btn">
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="bi bi-hi-search"></span></button>
                </span>
            </form>
        </div>
    </div>
    <!-- 列表 -->
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="id." data-ft="_check" class="_check">
                        <input name="id." type="checkbox" class="checkall"/>
                    </th>
                    <th data-fn="name" data-ob="name" class="sortable name"><%=Dict.getDepth(_fields, "name" , "__text__")%></th>
                    <th data-fn="note" data-ob="note" class="noteable note"><%=Dict.getDepth(_fields, "note" , "__text__")%></th>
                    <th data-fn="mtime" data-ob="mtime" data-ft="_htime" data-fill="v*1000" class="sortable" style="width: 5em;"><%=Dict.getDepth(_fields, "mtime", "__text__")%></th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <div style="display: table; width: 100%;">
        <div style="display: table-cell; width: 100%; vertical-align: middel;">
            <div class="toolbox" style="margin-bottom: 0;">
                <button type="button" class="commit btn btn-primary"><%=_locale.translate("fore.select")%></button>
                <button type="button" class="cancel btn btn-link   "><%=_locale.translate("fore.cancel")%></button>
                <span class="btn btn-text text-muted picksum"><%=_locale.translate("fore.selected")%> <b class="picknum"></b></span>
            </div>
        </div>
        <div style="display: table-cell; width: 15px; vertical-align: middle;">
            <div class="pagebox" style="margin-bottom: 0; text-align: right ;">
                <span class="page-curr btn btn-link" data-pn=""></span>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var findbox = context.find   (".findbox");

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork,created&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
         fillPage   : hsListFillNext,
        _fill__check: hsListFillFork
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, listobj))
         .then(function() {

        // 加载数据
        listobj.load(hsSetPms(listobj._url, loadbox), findbox);

        }); // End Promise
    });
})(jQuery);
</script>