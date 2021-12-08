<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.util.Dawn"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%!
    private final int Cnst_PN_ONE = 1;
    private final Pattern FORK_AT = Pattern.compile("^(.*)/[^/?&#]+");
    private final Pattern FORK_RB = Pattern.compile("[\\?&]"+Cnst.RB_KEY+"=([^&#]+)");
%>
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

    String baseHref = Core.SERV_PATH;
%>
<h2><%=_locale.translate("fore.manual.title", _title)%></h2>
<div id="<%=_pageId%>" class="swap-info board board-end">
    <div class="row">
    <div class="col-xs-6">
        <div class="table-responsive-revised">
        <div class="table-responsive listbox">
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
            Map ts = FormSet.getInstance("default")
                            .getEnum ( "__types__");

            it = _fields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry et = (Map.Entry)it.next();
                Map     info = (Map ) et.getValue();
                String  name = (String) et.getKey();
                if ( "@".equals(name) )  continue  ;
                String  type = (String) ts.get(info.get("__type__"));
            %>
                <tr>
                    <td>
                    <%if ("enum".equals(type)) {%>
                        <a href="javascript:;" class="view-enum"><%=name%></a>
                    <%} else if ("form".equals(type)) {%>
                        <%
                            String conf = Synt.declare(info.get("conf"), _config);
                            String form = Synt.declare(info.get("form"),  name  );
                            Map fs = FormSet.getInstance(conf).getFormTranslated(form);
                            List<Object[]> fl = new ArrayList(fs.size());
                            for(Object ot : fs.entrySet()) {
                                Map.Entry xt = (Map.Entry) ot;
                                Map fc = (Map) xt.getValue( );
                                Object n = xt.getKey();
                                if ( "@".equals(n) ) continue;
                                Object t = fc.get("__type__");
                                Object l = fc.get("__text__");
                                fl.add(new Object[]{n, t, l});
                            }
                        %>
                        <a href="javascript:;" class="show-form" data-data="<%=escape(Dawn.toString(fl, true))%>"><%=name%></a>
                    <%} else if ("fork".equals(type)) {%>
                        <%
                            String ak = Synt.declare(info.get("data-ak"), "" );
                            String at = Synt.declare(info.get("data-at"), "" );
                            String rb ;
                            // 关联名称
                            if (ak.isEmpty()) {
                                ak = ! name.endsWith("_id") ? name + "_fork"
                                     : name.substring(0, -3 + name.length( ) );
                            }
                            // 内部字段
                            Matcher m0 = FORK_RB.matcher(at);
                            if (m0.find()) rb = m0.group(01);
                            else rb = Synt.declare(info.get("data-vk"), "id" )
                                +","+ Synt.declare(info.get("data-tk"),"name");
                            // 关联资源
                            Matcher m1 = FORK_AT.matcher(at);
                            if (m1.find()) at = m1.group(01);
                            else at = Synt.declare(info.get("conf"), _config )
                                +"/"+ Synt.declare(info.get("form"),   name  );
                        %>
                        <a href="javascript:;" class="show-fork" data-rb="<%=rb%>" data-ak="<%=ak%>" data-at="<%=at%>"><%=name%></a>
                    <%} else if ("file".equals(type)) {%>
                        <%
                            String ft = Synt.declare(info.get("type"), "" );
                            String fx = Synt.declare(info.get("extn"), "" );
                            String fz = Synt.declare(info.get("size"), "");
                            String tm = Synt.declare(info.get("thumb-mode"), "");
                            String tx = Synt.declare(info.get("thumb-extn"), "");
                            String tz = Synt.declare(info.get("thumb-size"), "");
                        %>
                        <a href="javascript:;" class="show-file" data-file-type="<%=ft%>" data-file-extn="<%=fx%>" data-file-size="<%=fz%>" data-thumb-mode="<%=tm%>" data-thumb-extn="<%=tx%>" data-thumb-size="<%=tz%>"><%=name%></a>
                    <%} else { %>
                        <%=name%>
                    <%}%>
                    </td>
                    <td><%=info.get("__type__")%></td>
                    <td><%=info.get("__text__")%></td>
                    <td>
                        <%if (Synt.declare(info.get("__required__"), false)) {%><span class="label label-primary" style="margin-right: 2px;">必填</span><%}%>
                        <%if (Synt.declare(info.get("__repeated__"), false)) {%><span class="label label-primary" style="margin-right: 2px;">多值</span><%}%>
                        <%if (Synt.declare(info.get(  "listable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">列举</span><%}%>
                        <%if (Synt.declare(info.get(  "sortable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">排序</span><%}%>
                        <%if (Synt.declare(info.get(  "statable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">统计</span><%}%>
                        <%if (Synt.declare(info.get(  "findable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">筛选</span><%}%>
                        <%if (Synt.declare(info.get(  "rankable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">区间</span><%}%>
                        <%if (Synt.declare(info.get(  "srchable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">匹配</span><%}%>
                        <%if (Synt.declare(info.get(  "wordable"  ), false)) {%><span class="label label-default" style="margin-right: 2px;">搜索</span><%}%>
                        <%if (Synt.declare(info.get(  "readonly"  ), false)) {%><span class="label label-success" style="margin-right: 2px;">只读</span><%}%>
                        <%if (Synt.declare(info.get(  "disabled"  ), false)) {%><span class="label label-warning" style="margin-right: 2px;">内部</span><%}%>
                        <%if (Synt.declare(info.get( "unreadable" ), false)) {%><span class="label label-danger " style="margin-right: 2px;">禁查看</span><%}%>
                        <%if (Synt.declare(info.get( "unwritable" ), false)) {%><span class="label label-danger " style="margin-right: 2px;">禁编辑</span><%}%>
                    </td>
                </tr>
            <%} /*End For*/%>
            </tbody>
        </table>
        </div></div>
    </div>
    <div class="col-xs-6">
        <fieldset>
            <legend data-toggle="hsDrop">获取列表或详情 <span class="caret"></span></legend>
            <div class="dropdown-body">
                <div class="form-group">
                    <label class="control-label">接口</label>
                    <pre class="form-control-static">
<b>列表: GET</b> <%=baseHref%>/api/<%=_module%>/<%=_entity%>
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/search<%=Cnst.API_EXT%>
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>
<br/>
<b>详情: GET</b> <%=baseHref%>/api/<%=_module%>/<%=_entity%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/search<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">参数</label>
                    <pre class="form-control-static">
<b><%=Cnst.WD_KEY%></b>=搜索
<b><%=Cnst.PN_KEY%></b>=分页, 起始: <%=Cnst_PN_ONE%>; 为 0 仅获取分页数据
<b><%=Cnst.RN_KEY%></b>=条数, 默认: <%=Cnst.RN_DEF%>; 为 0 则获取全部数据
<%if (sortable.length() > 0) {%>
<b><%=Cnst.OB_KEY%></b>=排序, 取值: <%=sortable.substring(1)%>; 逗号分隔, 字段前加 - 表示逆序
<%}%>
<%if (listable.length() > 0) {%>
<b><%=Cnst.RB_KEY%></b>=列举, 取值: <%=listable.substring(1)%>; 逗号分隔, 字段前加 - 表示排除
<%}%>
<b><%=Cnst.AB_KEY%></b>=模式, .enfo 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .fall 深入子级表单(适用 form/part 类型)
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
        "state": "1 正常, 0 错误: count 等于 0 表示列表为空, count 大于 0 表示页码超出"
    },
    "enfo": {}, // 参见 select<%=Cnst.ACT_EXT%> 接口
    // ...
}
<b>详情:</b> {
    "info": {
        "字段名": "字段值"
    },
    "page": {
        "count": "1 或 0"
        "state": "1 正常, 0 错误: count 等于 0 表示数据缺失, count 大于 0 表示无权查阅"
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
<b>ADD|POST</b> <%=baseHref%>/api/<%=_module%>/<%=_entity%>
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/create<%=Cnst.API_EXT%>
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/create<%=Cnst.ACT_EXT%>
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
    "<%=Cnst.ID_KEY%>": "新的记录ID",
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
<b>PUT|PATCH</b> <%=baseHref%>/api/<%=_module%>/<%=_entity%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/update<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
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
    "<%=Cnst.RN_KEY%>": "更新的条数",
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
<b>DELETE</b> <%=baseHref%>/api/<%=_module%>/<%=_entity%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.API_EXT%>?<%=Cnst.ID_KEY%>=ID
或 <%=baseHref%>/<%=_module%>/<%=_entity%>/delete<%=Cnst.ACT_EXT%>?<%=Cnst.ID_KEY%>=ID
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
    "<%=Cnst.RN_KEY%>": "删除的条数",
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
<%=baseHref%>/<%=_module%>/<%=_entity%>/select<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">参数</label>
                    <pre class="form-control-static">
<b><%=Cnst.AB_KEY%></b>=模式, .enfo 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .fall 深入子级表单(适用 form/part 类型)
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">返回</label>
                    <pre class="form-control-static">
{
    "info": {
        "字段名": "字段值"
    },
    "enfo": {
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
计数: <%=baseHref%>/<%=_module%>/<%=_entity%>/acount<%=Cnst.ACT_EXT%>
计算: <%=baseHref%>/<%=_module%>/<%=_entity%>/amount<%=Cnst.ACT_EXT%>
聚合: <%=baseHref%>/<%=_module%>/<%=_entity%>/assort<%=Cnst.ACT_EXT%>
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">参数</label>
                    <pre class="form-control-static">
<b><%=Cnst.RN_KEY%></b>=条数, 按统计数量从多到少排列, 默认取前
<b><%=Cnst.RB_KEY%></b>=字段, 可用于统计的; acount 用于一般选项计数; amount 用于数值区间统计; assort 用于维度聚合计算.
聚合计算中, 字段分维度和指标. 指标形式为: 字段!方法; 指标方法有: !count 计数, !sum 求和, !min 最小, !max 最大, !ratio 综合[sum,min,max], !crowd 去重计数, !flock 所有值, !first 首个值.
聚合计算还可以接受类似 search<%=Cnst.ACT_EXT%> 接口的分页(<%=Cnst.PN_KEY%>)和排序(<%=Cnst.OB_KEY%>)参数.
                    </pre>
                </div>
                <div class="form-group">
                    <label class="control-label">返回</label>
                    <pre class="form-control-static">
<b>计数:</b> {
    "enfo": {
        "字段名": [
            ["值", "文本", "数量"],
        ]
    },
    // ...
}
<b>计算:</b> {
    "enfo": {
        "字段名": [
            ["值", "文本", "数量", "求和", "最小值", "最大值"],
        ]
    },
    // ...
}
<b>聚合:</b> {
    "list": [{
        "维度字段": "字段值",
        "指标字段|方法": "计算值"
    }],
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
                </div>
            </div>
        </fieldset>
        <div class="alert alert-info">
            <p>
                如果已经开放公共访问, 则开放接口只需将上述路径的前缀
                <a href="javascript:;"><%=baseHref%>/centr<b style="color: red;">a</b>/</a>
                换成
                <a href="javascript:;"><%=baseHref%>/centr<b style="color: red;">e</b>/</a>
                即可, 其他部分不用变.
                管理和开放接口可定制, 有作特殊处理则可能细节稍有不同.
            </p>
        </div>
    </div>
    </div>
</div>
<style type="text/css">
    .swap-info pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }
    .swap-info .alert-info a:hover b {
        line-height: 0.7em;
        font-size: 1.4em;
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

    // 查看选项数据
    context.on("click", "a.view-enum", function() {
        var rb = $(this).text();
        $.get(
            '<%=baseHref%>/<%=_module%>/<%=_entity%>/select<%=Cnst.ACT_EXT%>?<%=Cnst.AB_KEY%>=.enfo&<%=Cnst.RB_KEY%>='+rb,
            function(rd) {
                rd = rd.enfo[rb];
                var table = $('<table class="table table-hover table-striped"></table>');
                var thead = $('<thead></thead>').appendTo(table);
                var tbody = $('<tbody></tbody>').appendTo(table);
                var tr;
                tr = $('<tr></tr>').appendTo(thead);
                $('<th></th>').appendTo(tr).text('取值');
                $('<th></th>').appendTo(tr).text('文本');
                for(var i = 0; i < rd.length; i ++) {
                    var a = rd[i];
                    tr = $('<tr></tr>').appendTo(tbody);
                    $('<td></td>').appendTo(tr).text(a[0]);
                    $('<td></td>').appendTo(tr).text(a[1]);
                }
                $.hsMask({
                    'title': '选项列表',
                    'node' : table
                });
            },
            'json'
        );
    });

    // 查看子级内容
    context.on("click", "a.show-form", function() {
        var data  = $(this).data("data");
        if (typeof  data  ===  "string")
            data  = eval('('+ data +')');
        var table = $('<table class="table table-hover table-striped"></table>');
        var thead = $('<thead></thead>').appendTo(table);
        var tbody = $('<tbody></tbody>').appendTo(table);
        var tr;
        tr = $('<tr></tr>').appendTo(thead);
        $('<th></th>').appendTo(tr).text('字段');
        $('<th></th>').appendTo(tr).text('类型');
        $('<th></th>').appendTo(tr).text('名称');
        for(var i = 0; i < data.length; i ++) {
            var a = data[i];
            tr = $('<tr></tr>').appendTo(tbody);
            $('<td></td>').appendTo(tr).text(a[0]);
            $('<td></td>').appendTo(tr).text(a[1]);
            $('<td></td>').appendTo(tr).text(a[2]);
        }
        $.hsMask({
            'title': '数据结构',
            'node' : table
        });
    });

    // 查看关联内容
    context.on("click", "a.show-fork", function() {
        var at = $(this).data("at");
        var ak = $(this).data("ak");
        var rb = $(this).data("rb");
        var table = $('<table class="table table-hover table-striped"></table>');
        $('<col style="width: 80px;"/>').appendTo(table);
        var tbody = $('<tbody></tbody>').appendTo(table);
        var tr;
        tr = $('<tr></tr>').appendTo(tbody);
        $('<th></th>').appendTo(tr).text('关联资源');
        $('<td></td>').appendTo(tr).text(at);
        tr = $('<tr></tr>').appendTo(tbody);
        $('<th></th>').appendTo(tr).text('关联名称');
        $('<td></td>').appendTo(tr).text(ak);
        tr = $('<tr></tr>').appendTo(tbody);
        $('<th></th>').appendTo(tr).text('内部字段');
        $('<td></td>').appendTo(tr).text(rb);
        $.hsMask({
            'title': '关联参数',
            'node' : table
        });
    });
    
    // 查看文件参数
    context.on("click", "a.show-file", function() {
        var type  = $(this).data('file-type');
        var extn  = $(this).data('file-extn');
        var size  = $(this).data('file-size');
        var tmode = $(this).data('thumb-mode');
        var textn = $(this).data('thumb-extn');
        var tsize = $(this).data('thumb-size');
        var table = $('<table class="table table-hover table-striped"></table>');
        $('<col style="width: 80px;"/>').appendTo(table);
        var tbody = $('<tbody></tbody>').appendTo(table);
        var tr;
        if (type) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('文件类型');
            $('<td></td>').appendTo(tr).text(type);
        }
        if (extn) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('类型后缀');
            $('<td></td>').appendTo(tr).text(extn);
        }
        if (size) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('大小限定');
            $('<td></td>').appendTo(tr).text(size);
        }
        if (tmode) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('裁剪模式');
            $('<td></td>').appendTo(tr).text(tmode);
        }
        if (textn) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('存储格式');
            $('<td></td>').appendTo(tr).text(textn);
        }
        if (tsize) {
            tr = $('<tr></tr>').appendTo(tbody);
            $('<th></th>').appendTo(tr).text('存储尺寸');
            $('<td></td>').appendTo(tr).text(tsize);
        }
        $.hsMask({
            'title': '数据结构',
            'node' : table
        });
    });
})(jQuery);
</script>