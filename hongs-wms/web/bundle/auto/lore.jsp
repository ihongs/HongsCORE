<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_more_.jsp"%>
<%
    String _pageId = (_module + "_" + _entity + "_lore").replace('/', '_');
    
    StringBuilder listable = new StringBuilder();
    StringBuilder sortable = new StringBuilder();
    
    Iterator it = _fields.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry et = (Map.Entry) it.next();
        Map     info = (Map ) et.getValue();
        String  name = (String) et.getKey();

        if ("@".equals(name)) {
            continue;
        }
        
            listable.append(",").append(name);
        if (Synt.declare(info.get("sortable"), false)) {
            sortable.append(",").append(name);
        }
    }
%>
<h2><%=_locale.translate("fore.manual.title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId%> listbox table-responsive">
    <fieldset>
        <legend class="dropdown dropdown-toggle">列表 <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
    GET <%=Core.BASE_HREF%>/api/bundle/data/<%=_module%>
    或 <%=Core.BASE_HREF%>/bundle/data/<%=_module%>/search.api
    或 <%=Core.BASE_HREF%>/bundle/data/<%=_module%>/search.act
                </pre>
            </div>
            <div class="form-group">
                <label class="control-label">参数</label>
                <pre class="form-control-static">
    wd=搜索
    md=模式, 0 仅要选项数据, 1 补全选项名称, 2 增加选项数据, 4 增加关联数据
    pn=分页, 从 1 开始
    rn=条数
    <%if (sortable.length() > 0) {%>ob=排序, 取值 <%=sortable.substring(1)%>, 逗号分隔, 字段前加 - 表示逆序<%}%>
    <%if (listable.length() > 0) {%>rb=列举, 取值 <%=listable.substring(1)%>, 逗号分隔, 字段前加 - 表示排除<%}%>
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">返回</label>
                <pre class="form-control-static">
    {
        list: [{
            
        }],
        enum: {
        },
        page: {
        }
        // ....
    }
                </pre>
            </div>
        </div>
    </fieldset>
    <fieldset>
        <legend class="dropdown dropdown-toggle">其他</legend>
        <div class="dropdown-body">
            <div class="form-gorup">
                <pre>
    /api 和 .api 接口可以使用 .conv,.call 等参数, .act 接口是基础.
                </pre>
            </div>
        </div>
    </fieldset>
</div>
<style type="text/css">
    .<%=_pageId%> pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }
</style>
<script type="text/javascript">
    (function($) {
        var context = $("#<%=_pageId%>");
        
        context.find("pre").each(function() {
            $(this).html($(this).html().replace(/(^[\r\n]+|[\r\n ]+$)/g, ''));
        });
    })(jQuery);
</script>