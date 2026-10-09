<%@page import="io.github.ihongs.CoreConfig"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.util.Dist"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/markdown" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
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

# <%=_locale.translate("fore.manual.title", _title)%>

- 域名: <%=baseHref%>
- 方法: POST
- 标头:
  - Cookie: {{COOKIE}}
  - Authorization: Bearer {{API_KEY}}
  - Accept: application/json,*/*;q=0.8
  - Content-Type: application/json
  - X-Requested-With: XMLHttpRequest

COOKIE 或 API_KEY 作为身份标识。
  
## 数据字典

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

    StringBuilder tags = new StringBuilder();
    if (Synt.declare(info.get("__repeated__"), false)) tags.append("数组, ");
    if (Synt.declare(info.get("__required__"), false)) tags.append("必填, ");
    if (Synt.declare(info.get(  "sortable"  ), false)) tags.append("可排序, ");
    if (Synt.declare(info.get(  "wordable"  ), false)) tags.append("可搜索, ");
    if (Synt.declare(info.get(  "findable"  ), false)) tags.append("可筛选, ");
    if (Synt.declare(info.get(  "rankable"  ), false)) tags.append("可比较, ");
    if (Synt.declare(info.get(  "srchable"  ), false)) tags.append("可匹配, ");
    if (Synt.declare(info.get(  "statable"  ), false)) tags.append("可统计, ");
    if (tags.length() > 0) tags.setLength(tags.length() - 2);
%>
- <%=name%>
  - 类型: <%=info.get("__type__")%>
  - 名称: <%=info.get("__text__")%>
  - 标识: <%=tags.toString()%>
<%} /* End while */%>

## 选项数据

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/recipe<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.AB_KEY%>": [ ] // 模式, .enfo 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .fall 深入子级表单(适用 form/part 类型)
  }
  ```
- 返回:
  ```json
  {
    "enfo": {
      "字段名": [
        ["值", "文本"]
      ]
    },
    "info": {
      "字段名": "默认值"
    }
  }
  ```

## 查询列表

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/search<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.WD_KEY%>": "搜索关键词",
    "<%=Cnst.PN_KEY%>": 1, // 页码, 起始: <%=Cnst_PN_ONE%>; 为 0 仅获取分页数据
    "<%=Cnst.RN_KEY%>": 1, // 条数, 默认: <%=Cnst.RN_DEF%>; 为 0 则获取全部数据
    "<%=Cnst.QN_KEY%>": 0, // 跳过条数,
<%if (sortable.length() > 0) {%>
    "<%=Cnst.OB_KEY%>": [ ], // 排序, 取值: <%=sortable.substring(1)%>; 字段前加 - 表示逆序
<%}%>
<%if (listable.length() > 0) {%>
    "<%=Cnst.RB_KEY%>": [ ], // 字段, 取值: <%=listable.substring(1)%>; 字段前加 - 表示排除
<%}%>
    "<%=Cnst.AB_KEY%>": [ ], // 模式, .enfo 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .fall 深入子级表单(适用 form/part 类型)
    "字段名": "查询值",
    "字段名": {"符号": "取值"}, // 符号: eq 等于, ne 不等于, in 包含, no 不包含, on 全包含, lt 小于, le 小于或等于, gt 大于, ge 大于或等于, at 区间, se 搜索匹配, ns 搜索排除
    "<%=Cnst.OR_KEY%>": [ { /*条件组*/ } ], // 多组或条件(OR)
    "<%=Cnst.AR_KEY%>": [ { /*条件组*/ } ], // 多组与条件(AND)
    "<%=Cnst.AR_KEY%>": [ { /*条件组*/ } ], // 多组否条件(NOT)
    // <%=Cnst.OR_KEY%>|<%=Cnst.AR_KEY%>|<%=Cnst.NR_KEY%> 可互相嵌套
  }
  ```
- 返回:
  ```json
  {
    "list": [
      {
        "字段名": "字段值"
      }
    ],
    "page": {
        "total": 1, // 总页数
        "count": 1, // 总行数
        "state": 1  // 1 正常, 0 错误: count 等于 0 表示列表为空, count 大于 0 表示页码超出
    },
    "enfo": { } // 参见选项数据接口
    // ...
  }
  ```

## 获取详情

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/recite<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.ID_KEY%>": "1", // 必需的记录 id
<%if (listable.length() > 0) {%>
    "<%=Cnst.RB_KEY%>": [ ], // 字段, 取值: <%=listable.substring(1)%>; 字段前加 - 表示排除; 默认不传取全部
<%}%>
    "<%=Cnst.AB_KEY%>": [ ], // 模式, .enfo 提供选项数据, .info 提供缺省数据, _text 补全选项文本, _time 附加数字时间, _link 附加完整链接, _fork 增加关联数据, .fall 深入子级表单(适用 form/part 类型)
    "字段名": "查询值",
    "字段名": {"符号": "取值"}, // 符号: eq 等于, ne 不等于, in 包含, no 不包含, on 全包含, lt 小于, le 小于或等于, gt 大于, ge 大于或等于, at 区间, se 搜索匹配, ns 搜索排除
    "<%=Cnst.OR_KEY%>": [ { /*条件组*/ } ], // 多组或条件(OR)
    "<%=Cnst.AR_KEY%>": [ { /*条件组*/ } ], // 多组与条件(AND)
    "<%=Cnst.AR_KEY%>": [ { /*条件组*/ } ], // 多组否条件(NOT)
    // <%=Cnst.OR_KEY%>|<%=Cnst.AR_KEY%>|<%=Cnst.NR_KEY%> 可互相嵌套
  }
  ```
- 返回:
  ```json
  {
    "info": [
      "字段名": "字段值"
    ],
    "page": {
        "count": 1, // 总行数
        "state": 1  // 1 正常, 0 错误: count 等于 0 表示数据缺失, count 大于 0 表示无权查阅
    },
    "enfo": { } // 参见选项数据接口
    // ...
  }
  ```

## 新增记录

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/create<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "字段名": "字段值"
  }
  ```
- 返回:
  ```json
  {
    "<%=Cnst.ID_KEY%>": "新的记录ID"
  }
  ```

## 更新记录

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.ID_KEY%>": "1", // 必需的记录 id, 亦可为数组
    "<%=Cnst.AR_KEY%>": { }, // 可选的附加约束条件
    "字段名": "字段值"
  }
  ```
- 返回:
  ```json
  {
    "<%=Cnst.RN_KEY%>": "更新的条数"
  }
  ```

  ## 删除记录

- 接口: <%=baseHref%>/<%=_module%>/<%=_entity%>/update<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.ID_KEY%>": "1", // 必需的记录 id, 亦可为数组
    "<%=Cnst.AR_KEY%>": { }  // 可选的附加约束条件
  }
  ```
- 返回:
  ```json
  {
    "<%=Cnst.RN_KEY%>": "更新的条数"
  }
  ```

## 统计数据

- 接口:
  - 计数: <%=baseHref%>/<%=_module%>/<%=_entity%>/acount<%=Cnst.ACT_EXT%>
  - 聚合: <%=baseHref%>/<%=_module%>/<%=_entity%>/assort<%=Cnst.ACT_EXT%>
- 请求:
  ```json
  {
    "<%=Cnst.RN_KEY%>": 10, // 条数, 按统计数量从多到少排列, 默认取前 <%=Cnst.RN_DEF%> 个
    "<%=Cnst.RB_KEY%>": [ ] // 字段, 需统计的; acount 用于一般选项计数; assort 用于维度聚合计算
  }
  ```
- 说明:
  聚合计算中, 字段分维度和指标. 指标形式为: 字段!方法; 指标方法有: !count 计数, !sum 求和, !min 最小, !max 最大, !total 综合[sum,min,max], !crowd 去重计数, !flock 所有值, !first 首个值.
  聚合计算还可以接受类似查询接口(search)的分页(<%=Cnst.PN_KEY%>)和排序(<%=Cnst.OB_KEY%>)参数.
- 返回:
  ```json
  {
    // 计数:
    "enfo": {
      // 选项计数
      "字段名": [
        ["值", "文本", "数量"]
      ]
      // 数值计算
      "字段名": [
        ["值", "文本", "数量", "求和", "最小值", "最大值"]
      ]
    },
    // 聚合:
    "list": [
      {
        "维度字段": "字段值",
        "指标字段|方法": "计算值"
      }
    ]
}
