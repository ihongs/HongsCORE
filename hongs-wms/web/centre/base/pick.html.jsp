<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "select");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_pick").replace('/', '_');

    StringBuilder _ob = new StringBuilder( "-,-boost,-mtime,-ctime");
    StringBuilder _rb = new StringBuilder("id,name,note,logo,mtime");
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-list board board-end">
    <div style="display: table; width: 100%;">
        <div style="display: table-cell; width: 15px; vertical-align: middle;">
            <div class="pagebox btn-group" style="white-space: nowrap; padding-right: 15px;">
                <button type="button" class="page-prev btn btn-default" style="float: none; display: inline;" title="<%=_locale.translate("fore.list.prev.page")%>" data-pn=""><span class="glyphicon glyphicon-menu-left "></span></button>
                <button type="button" class="page-next btn btn-default" style="float: none; display: inline;" title="<%=_locale.translate("fore.list.next.page")%>" data-pn=""><span class="glyphicon glyphicon-menu-right"></span></button>
            </div>
        </div>
        <div style="display: table-cell; width: 100%; vertical-align: middle;">
            <form class="findbox input-group" method="POST" action="">
                <input type="search" name="<%=_fields.containsKey("word") ? "word" : "wd"%>" class="form-control input-search"/>
                <span class="input-group-btn">
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="glyphicon glyphicon-search"></span></button>
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
                    <th data-fn="mtime" data-ob="mtime" data-ft="_htime" data-fl="v*1000" class="sortable" style="width: 5em;"><%=Dict.getDepth(_fields, "mtime", "__text__")%></th>
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

    // 简单分页, 选择条目

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, listobj);
        }

        // 加载数据
        listobj.load(hsSetPms(listobj._url, loadbox), findbox);
    });
})(jQuery);
</script>