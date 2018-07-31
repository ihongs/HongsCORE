<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _pageId = (_module + "_" + _entity + "_lore").replace('/', '_');
    
    StringBuilder listable = new StringBuilder();
    StringBuilder sortable = new StringBuilder();
    
    Iterator it = _fields.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry et = (Map.Entry)it.next();
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
<div id="<%=_pageId%>" class="lore-info row">
    <div class="col-xs-6">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th>字段</th>
                    <th>类型</th>
                    <th>名称</th>
                    <th>标识</th>
                </tr>
            </thead>
            <tbody>
            <%
            it = _fields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry et = (Map.Entry)it.next();
                Map     info = (Map ) et.getValue();
                String  name = (String) et.getKey();
                if ( "@".equals(name) )  continue  ;
            %>
                <tr>
                    <td><%=name%></td>
                    <td><%=info.get("__type__")%></td>
                    <td><%=info.get("__text__")%></td>
                    <td>
                        <%if (Synt.declare(info.get("__required__"), false)) {%><span class="label label-primary" style="margin-right: 2px;">必填</span><%}%>
                        <%if (Synt.declare(info.get("__repeated__"), false)) {%><span class="label label-primary" style="margin-right: 2px;">多值</span><%}%>
                        <%if (Synt.declare(info.get(  "listable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">列举</span><%}%>
                        <%if (Synt.declare(info.get(  "sortable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">排序</span><%}%>
                        <%if (Synt.declare(info.get(  "findable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">筛选</span><%}%>
                        <%if (Synt.declare(info.get(  "statable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">统计</span><%}%>
                        <%if (Synt.declare(info.get(  "readonly"  ), false)) {%><span class="label label-success" style="margin-right: 2px;">只读</span><%}%>
                    </td>
                </tr>
            <%} /*End For*/%>
            </tbody>
        </table>
    </div>
    <div class="col-xs-6">
        <fieldset>
            <legend class="dropdown dropdown-toggle">获取列表或详情 <span class="caret"></span></legend>
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
        "字段名": "字段值"
    }],
    "enum": {
        "字段名": [
            ["值", "文本"],
        ]
    },
    "page": {
        "page": "页码参数",
        "rows": "行数参数",
        "pagecount": "总页数",
        "rowscount": "总行数",
    },
    // ...
}
<b>详情:</b> {
    "info": {
        "字段名": "字段值"
    },
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend class="dropdown dropdown-toggle">新增信息 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>ADD|POST</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.API_EXT%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-gorup">
                    <label class="control-label">请求</label>
                    <pre class="form-control-static">
字段名=字段值...
                    </pre>
                </div>
                <div class="form-gorup">
                    <label class="control-label">响应</label>
                    <pre class="form-control-static">
{
    "info": {
        "字段名": "字段值"
    },
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend class="dropdown dropdown-toggle">批量更新 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>PUT|PATCH</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>~ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
                    </pre>
                </div>
                <div class="form-gorup">
                    <label class="control-label">请求</label>
                    <pre class="form-control-static">
id=ID 或 id.=ID1&id.=ID2...
字段名=字段值...
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
            <legend class="dropdown dropdown-toggle">批量删除 <span class="caret"></span></legend>
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
id=ID 或 id.=ID1&id.=ID2...
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
                    <p>
                        <%=Cnst.ACT_EXT%> 是基础接口;
                        <%=Cnst.API_EXT%> 和 /api 接口可使用 .conv,.data,.wrap,.scok 等特殊参数.
                        除 /api 接口外均不限 HTTP 方法, 可以任意使用 GET,POST,PUT,DELETE 等方法.
                    </p>
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