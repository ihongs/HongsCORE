<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
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
        <legend class="dropdown dropdown-toggle">获取(列表和详情) <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
<b>列表:</b>
    GET <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search.api
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search.act
<b>详情:</b>
    GET <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search.api?id=ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search.act?id=ID
                </pre>
            </div>
            <div class="form-group">
                <label class="control-label">参数</label>
                <pre class="form-control-static">
    wd=搜索
    pn=分页, 从 1 开始
    rn=条数, 默认 20 条
    md=模式, 0 仅要选项数据, 1 补全选项名称, 2 增加选项数据, 4 增加关联数据
<%if (sortable.length() > 0) {%>
    ob=排序, 取值 <%=sortable.substring(1)%>, 逗号分隔, 字段前加 - 表示逆序
<%}%>
<%if (listable.length() > 0) {%>
    rb=列举, 取值 <%=listable.substring(1)%>, 逗号分隔, 字段前加 - 表示排除
<%}%>
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">返回</label>
                <pre class="form-control-static">
<b>列表:</b>
    {
        "list": [{
            
        }],
        "enum": {
        },
        "page": {
        },
        // ...
    }
<b>详情:</b>
    {
        "info": {
            // 同 list 中的条目
        },
        // ...
    }
                </pre>
            </div>
        </div>
    </fieldset>
    <fieldset>
        <legend class="dropdown dropdown-toggle">新增 <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
    POST <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create.api
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create.act
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">请求</label>
                <pre class="form-control-static">
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">响应</label>
                <pre class="form-control-static">
    {
        "info": {
            // 同 list 中的条目
        },
        // ...
    }
                </pre>
            </div>
        </div>
    </fieldset>
    <fieldset>
        <legend class="dropdown dropdown-toggle">更新 <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
    PUT|PATCH <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update.api?id=ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update.act?id=ID
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">请求</label>
                <pre class="form-control-static">
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">响应</label>
                <pre class="form-control-static">
    {
        "rows": "更新的条数",
        // ...
    }
                </pre>
            </div>
        </div>
    </fieldset>
    <fieldset>
        <legend class="dropdown dropdown-toggle">删除 <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
    DELETE <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete.api?id=ID
    或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete.act?id=ID
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">请求</label>
                <pre class="form-control-static">
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">响应</label>
                <pre class="form-control-static">
    {
        "rows": "删除的条数",
        // ...
    }
                </pre>
            </div>
        </div>
    </fieldset>
    <fieldset>
        <legend class="dropdown dropdown-toggle">其他 <span class="caret"></span></legend>
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
            var html = $(this).html();
            // 去除中间空行, 统一换行符号
            html = html.replace(/(\r\n(\r\n)*|\r\r*|\n\n*)/g, "\r\n");
            // 去除末尾空行
            html = html.replace(/[\r\n ]+$/g, "");
            $(this).html(html);
        });
    })(jQuery);
</script>