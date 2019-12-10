<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _pageId = (_module + "-" + _entity + "-swap").replace('/', '-');

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
                        <%if (Synt.declare(info.get(  "wordable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">搜寻</span><%}%>
                        <%if (Synt.declare(info.get(  "srchable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">搜索</span><%}%>
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
            <legend data-toggle="hsDrop">获取列表或详情 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>列表: GET</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.API_EXT%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>
<br/>
<b>详情: GET</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>=ID
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
<b><%=Cnst.AB_KEY%></b>=模式, .enum 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .form 深入子级表单(适用 form/part 类型)
<%if (sortable.length() > 0) {%>
<b><%=Cnst.OB_KEY%></b>=排序, 取值 <%=sortable.substring(1)%>, 逗号分隔, 字段前加 - 表示逆序
<%}%>
<%if (listable.length() > 0) {%>
<b><%=Cnst.RB_KEY%></b>=列举, 取值 <%=listable.substring(1)%>, 逗号分隔, 字段前加 - 表示排除
<%}%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">返回</label>
                    <pre class="form-control-static">
<b>列表:</b> {
    "list": [{
        "字段名": "字段值"
    }],
    "page": {
        "<%=Cnst.PN_KEY%>": "页码参数",
        "<%=Cnst.RN_KEY%>": "行数参数",
        "pages": "总页数",
        "count": "总行数",
    },
    // ...
}
<b>详情:</b> {
    "info": {
        "字段名": "字段值"
    },
    "enum": {
        "字段名": [
            ["值", "文本"],
        ]
    },
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend data-toggle="hsDrop">新增信息 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>ADD|POST</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.API_EXT%>
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/create<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">请求</label>
                    <pre class="form-control-static">
字段名=字段值...
                    </pre>
                </div>
                <div class="form-group">
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
            <legend data-toggle="hsDrop">批量更新 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>PUT|PATCH</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">请求</label>
                    <pre class="form-control-static">
id=ID 或 id.=ID1&id.=ID2...
字段名=字段值...
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">响应</label>
                    <pre class="form-control-static">
{
    "size": "更新的条数",
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend data-toggle="hsDrop">批量删除 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>DELETE</b> <%=Core.BASE_HREF%>/api/<%=_module%>/<%=_entity%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">请求</label>
                    <pre class="form-control-static">
id=ID 或 id.=ID1&id.=ID2...
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">响应</label>
                    <pre class="form-control-static">
{
    "size": "删除的条数",
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend data-toggle="hsDrop">选项数据 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/select<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">参数</label>
                    <pre class="form-control-static">
<b><%=Cnst.AB_KEY%></b>=模式, .enum 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .form 深入子级表单(适用 form/part 类型)
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">返回</label>
                    <pre class="form-control-static">
{
    "info": {
        "字段名": "字段值"
    },
    "enum": {
        "字段名": [
            ["值", "文本"],
        ]
    },
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend data-toggle="hsDrop">统计数据 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
计数: <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/acount<%=Cnst.ACT_EXT%>
计算: <%=Core.BASE_HREF%>/<%=_module%>/<%=_entity%>/amount<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">参数</label>
                    <pre class="form-control-static">
<b><%=Cnst.RB_KEY%></b>=字段, 可用于统计的; acount 用于一般选项计数; amount 用于数值区间统计; amount 可返回求和等数据.
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">返回</label>
                    <pre class="form-control-static">
计数: {
    "info": {
        "字段名": [
            ["值", "文本", "数量"],
        ]
    },
    // ...
},
计算: {
    "info": {
        "字段名": [
            ["值", "文本", "数量", "求和", "最小值", "最大值"],
        ]
    },
    // ...
}
                    </pre>
                </div>
            </div>
        </fieldset>
        <fieldset>
            <legend data-toggle="hsDrop">其他 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <p>
                        <%=Cnst.ACT_EXT%> 是基础接口;
                        <%=Cnst.API_EXT%> 及 /api 接口可用 .data 和 .mode 参数,
                        .data 用于集中发送主要数据, .mode 用于对响应做特殊处理,
                        .mode 可选值有: wrap, scok, all2str, num2str, null2str,
                        bool2num, bool2str, date2num, date2sec, flat.map, flat_map.
                        除 /api 接口外均不限 HTTP 方法, 可以任意使用 GET, POST 等方法名;
                        另 /api 路径可包含当前资源和上级资源 ID, 但规则与 REST 略有不同, 是用 =ID 而非 /ID 形式.
                    </p>
                    <p>
                        字段类型命名采用 HTML 的表单控件类型, 如 text,number,select,textarea 等,
                        另有一些 H5 的类型如 tel,url,email,date,datetime(实为datetime-local) 等.
                        还有一些自定义的类型, 如: string 同 text, enum 同 select,
                        fork/pick 表示关联, form/part 表示内联.
                    </p>
                    <p>
                        <%=Cnst.AB_KEY%>=.enum 可返回键为 enum 的选项数据,
                        enum 在 JavaScript 等语言中为关键词, 不方便对象化,
                        <%=Cnst.AB_KEY%>=.menu 则返回键为 menu 的选项数据.
                    </p>
                    <p>
                        如果已经开放公共访问, 则开放接口只需将上述路径的前缀 <%=Core.BASE_HREF%>/centra/ 更换为 <%=Core.BASE_HREF%>/centre/ 即可, 其他部分不变.
                        管理和开放接口可定制, 有作特殊处理则可能细节稍有不同.
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
    var context = H$("#<%=_pageId%>");

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