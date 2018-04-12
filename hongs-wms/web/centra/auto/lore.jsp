<%@page import="app.hongs.Cnst"%>
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
<div id="<%=_pageId%>" class="lore-info">
    <div class="listbox table-responsive">
    <fieldset>
        <legend class="dropdown dropdown-toggle">获取(列表和详情) <span class="caret"></span></legend>
        <div class="dropdown-body">
            <div class="form-group">
                <label class="control-label">接口</label>
                <pre class="form-control-static">
<b>列表: GET</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.API_EXT%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>
<br/>
<b>详情: GET</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
                </pre>
            </div>
            <div class="form-group">
                <label class="control-label">参数</label>
                <pre class="form-control-static">
<b><%=Cnst.WD_KEY%></b>=搜索
<b><%=Cnst.PN_KEY%></b>=分页, 从 1 开始
<b><%=Cnst.RN_KEY%></b>=条数, 默认 20 条
<b><%=Cnst.AB_KEY%></b>=模式, !enum 仅要选项数据, .enum 增加选项数据, _enum 补全选项名称, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据
<%if (sortable.length() > 0) {%>
<b><%=Cnst.OB_KEY%></b>=排序, 取值 <%=sortable.substring(1)%>, 逗号分隔, 字段前加 - 表示逆序
<%}%>
<%if (listable.length() > 0) {%>
<b><%=Cnst.RB_KEY%></b>=列举, 取值 <%=listable.substring(1)%>, 逗号分隔, 字段前加 - 表示排除
<%}%>
                </pre>
            </div>
            <div class="form-gorup">
                <label class="control-label">返回</label>
                <pre class="form-control-static">
<b>列表:</b> {
    "list": [{

    }],
    "enum": {
    },
    "page": {
    },
    // ...
}
<b>详情:</b> {
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
<b>POST</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.API_EXT%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.ACT_EXT%>
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
<b>PUT</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
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
<b>DELETE</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
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
                <pre class="form-control-static">
<%=Cnst.ACT_EXT%> 是基础接口; <%=Cnst.API_EXT%> 和 /api 接口可使用 .conv,.data,.wrap,.scok 等特殊参数. 除 /api 接口外均不限 HTTP 方法, 可以任意使用 GET,POST,PUT,DELETE. 
                </pre>
            </div>
        </div>
    </fieldset>
    </div>
</div>
<style type="text/css">
    .lore-info pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }
</style>
<script type="text/javascript">
    (function($) {
        var context = $("#<%=_pageId%>").removeAttr("id");
        
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