<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "browse");
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_list").replace('/', '_');

    String _conf   = FormSet.hasConfFile(_module + "/" + _entity)
                  || NaviMap.hasConfFile(_module + "/" + _entity)
                   ? _module + "/" + _entity : _module ;

    String NAME = Synt.declare(_params.get("field-name"), "name");
    String NOTE = Synt.declare(_params.get("field-note"), "note");
    String LOGO = Synt.declare(_params.get("field-logo"), "logo");
    String USER = Synt.declare(_params.get("field-user"),"cuser");

    StringBuilder _ob = new StringBuilder("-boost,-mtime,-ctime");
    StringBuilder _rb = new StringBuilder("id,"+NAME+","+NOTE+","+LOGO+","+USER);
    Set<String>   _wd = getWordable (_fields);
    Set<String>   _sd = getSrchable (_fields);
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-list board board-end">
    <form class="findbox toolbox board row">
        <div class="col-md-6 col-sm-8 center-block">
            <div style="display: table; width: 100%;">
            <div style="display: table-cell; width: 15px; vertical-align: middle;">
                <div class="btn-group">
                    <button type="button" class="create btn btn-primary" style="margin-right: 15px;"><%=_locale.translate("fore.create", _title)%></button>
                </div>
            </div>
            <div style="display: table-cell; width: 100%; vertical-align: middle;">
                <div class="input-group">
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
                        <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="glyphicon glyphicon-search"></span></button>
                        <button type="button" class="filter btn btn-default" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-filter"></span></button>
                    </span>
                </div>
            </div>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
    <form class="findbox filtbox statbox invisible well form-horizontal">
        <%
        Iterator it2 = _fields.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry et = (Map.Entry) it2.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get ("__type__");
            String  text = (String) info.get ("__text__");

            if ("@".equals(name) || "id".equals(name)
            ||  Synt.declare(info.get("unopenable"), false)) {
                continue;
            }
        %>
        <% /***/ if (Synt.declare(info.get("statable"), false)) {%>
        <div class="stat-group form-group form-group-sm row" data-name="<%=name%>">
            <label class="col-md-3 col-sm-2 control-label text-right"><%=text%></label>
            <div class="col-md-6 col-sm-8">
                <%
                    // 检查是否有枚举数据
                    String enumConf = Synt.defxult((String) info.get("conf"),_conf);
                    String enumName = Synt.defxult((String) info.get("enum"), name);
                    Map    enumData = null;
                    try {
                        enumData  = FormSet.getInstance(enumConf).getEnum(enumName);
                    } catch ( HongsException ex) {
                    if (ex.getErrno() != 913 ) {
                        throw ex;
                    }}

                    if ("number".equals(type)) {
                        if (enumData != null ) {
                            type = "amount";
                        } else {
                            type = "acount";
                        }
                    } else {
                        type = "acount";
                    }

                    // 附加参数
                    String prms = (String)info.get("stat-prms");
                    if (prms != null && ! prms.isEmpty()) {
                        type += "\" data-prms=\"" + prms;
                    }
                %>
                <div class="checkbox" data-name="<%=name%>" data-type="<%=type%>"></div>
            </div>
        </div>
        <%} else if (Synt.declare(info.get("filtable"), false)) {%>
        <div class="filt-group form-group form-group-sm row" data-name="<%=name%>">
            <label class="col-md-3 col-sm-2 control-label text-right"><%=text%></label>
            <div class="col-md-6 col-sm-8">
            <%if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                <div class="input-group">
                    <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.GE_REL%>" />
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.LE_REL%>" />
                </div>
            <%} else if ("date".equals(type) || "time" .equals(type) || "datetime" .equals(type)) {%>
                <%
                    if ("datetime".equals(type)) {
                        type = "datetime-local";
                    }
                %>
                <div class="input-group">
                    <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.GE_REL%>" data-toggle="hsTime" data-type="<%=info.get("type")%>" />
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.LE_REL%>" data-toggle="hsTime" data-type="<%=info.get("type")%>" />
                </div>
            <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                <%
                    String fn = name;
                    if (fn.endsWith( "." )) {
                        fn = fn.substring(0, fn.length() - 1);
                    }
                    String kn = fn +"_data";
                    if (fn.endsWith("_id")) {
                        fn = fn.substring(0, fn.length() - 3);
                        kn = fn;
                    }
                    String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                    String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                    String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                    String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                    al = al.replace("centra", "centre");
                    // 选择时禁用创建
                    if ( ! al.isEmpty (   )) {
                    if ( ! al.contains("#")) {
                        al = al + "#.deny=.create";
                    } else {
                        al = al + "&.deny=.create";
                    }}
                %>
                <ul  class="pickbox" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></button>
            <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="<%=name%>" data-ft="_enum"></select>
            <%} else {%>
                <%
                    // 搜索类型优先模糊匹配
                    if (_sd.contains(name)
                    && (_wd.contains(name)
                    ||  "textarea".equals(type)
                    ||  "textview".equals(type))) {
                        name += "."+ Cnst.CQ_REL + "\" placeholder=\"搜索";
                    }
                %>
                <input type="text" class="form-control" name="<%=name%>" />
            <%} /*End If */%>
            </div>
        </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <div class="mine-group form-group form-group-sm row" data-name="cuser">
            <label class="col-md-3 col-sm-2 control-label text-right">属主</label>
            <div class="col-md-6 col-sm-8">
                <input type="hidden" name="cuser" value="" data-ft="_mine"/>
                <div class="radio">
                    <label>
                        <input type="radio" value="0" checked="checked"/>
                        <span>全部</span>
                    </label>
                    <label>
                        <input type="radio" value="1"/>
                        <span>我创建的</span>
                    </label>
                    <label>
                        <input type="radio" value="2"/>
                        <span>其他人的</span>
                    </label>
                </div>
            </div>
        </div>
        <div class="sort-group form-group form-group-sm row" data-name="ob">
            <label class="col-md-3 col-sm-2 control-label text-right">排序</label>
            <div class="col-md-6 col-sm-8">
                <input type="hidden" name="ob" value="<%=_ob%>" data-ft="_sort"/>
                <div>
                    <select class="form-control" style="width: auto; display: inline-block;">
                        <option value="<%=_ob%>" style="color: #ccc;">默认</option>
        <%
        Iterator it4 = _fields.entrySet().iterator();
        while (it4.hasNext()) {
            Map.Entry et = (Map.Entry) it4.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  text = (String) info.get ("__text__");

            if ("@".equals(name) || "id".equals(name)
            || !Synt.declare(info.get("sortable"), false)) {
                continue;
            }
        %>
                        <option value="<%=name%>,*"><%=text%></option>
        <%} /*End for*/%>
                    </select
                    <span>&nbsp;</span>
                    <select class="form-control" style="width: auto; display: inline-block;">
                        <option value="" >正序</option>
                        <option value="-">逆序</option>
                    </select>
                </div>
            </div>
        </div>
        <div class="group-end">
            <input type="submit" class="invisible"/>
        </div>
    </form>
    <!-- 列表 -->
    <div class="itembox col-md-4 col-sm-6 col-xs-12" style="display: none; padding: 0 7.5px 15px 7.5px;">
        <input type="hidden" name="id" data-fn="id" data-fl="$(this).val(v) && undefined" />
        <div class="panel panel-default" style="margin: 0; padding: 0; position: relative;">
            <div class="panel-body" style="display: table; width: 100%;">
                <%if (_fields.containsKey(LOGO)) {%>
                <div style="display: table-cell; width: 10px; padding: 0px; vertical-align: top;">
                    <div class="review" style="height: 100px; overflow: hidden; cursor: pointer;">
                        <div data-fn="<%=LOGO%>" data-ft="_logo" style="width: 100px; height: 100px; margin-right: 15px; border-radius: 4px;"></div>
                    </div>
                </div>
                <%} /*End If*/%>
                <div style="display: table-cell; width: 100%; padding: 0px; vertical-align: top;">
                    <div class="review" style="height: 100px; overflow: hidden; cursor: pointer;">
                        <div data-fn="<%=NAME%>" style="color: #444;"></div>
                        <div data-fn="<%=NOTE%>" style="color: #888;"></div>
                    </div>
                    <div data-fn="<%=USER%>" data-ft="_edit" class="btn-group edit-group" style="position: absolute; right: 0; bottom: 0; opacity: 0.8; display: none;">
                        <button type="button" class="btn btn-sm btn-default update"><span class="glyphicon glyphicon-edit "></span></button>
                        <button type="button" class="btn btn-sm btn-default delete"><span class="glyphicon glyphicon-trash"></span></button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="listbox clearfix flex-center" style="margin: 0 -7.5px 5px -7.5px;">
    </div>
    <div class="pagebox clearfix text-center">
    </div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var findbox = context.find(".findbox");
    var filtbox = context.find(".filtbox");
    var statbox = context.find(".statbox");

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete',
              '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
        ],
        openUrls: [
            [ '<%=_module%>/<%=_entity%>/form_init.html?'+$.param(hsSerialArr(loadbox)),
              '.create', '@' ],
            [ '<%=_module%>/<%=_entity%>/form.html?<%=Cnst.ID_KEY%>={ID}',
              '.update', '@' ],
            [ '<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
              '.review', '@' ]
        ],
        fillList    : hsListFillItem,
        fillPage    : hsListFillLess,
        _fill__logo : hsListFillLogo,
        _fill__edit : hsListShowEdit
    });

    var filtobj = filtbox.hsForm({
        _url: "<%=_module%>/<%=_entity%>/select.act?<%=Cnst.AB_KEY%>=.enfo",
        _doll__enum : hsListDollFilt,
        _fill__enum : hsListFillFilt,
        _fill__sort : hsListInitSort,
        _fill__mine : hsListInitMine
    });

    var statobj = context.hsCate({
        murl: "<%=_module%>/<%=_entity%>/amount.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.AB_KEY%>=_text",
        curl: "<%=_module%>/<%=_entity%>/acount.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.AB_KEY%>=_text,_fork"
    });

    var loadres = hsSerialDic(loadbox);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    // 绑定参数
    listobj._url = hsSetPms(listobj._url, loadres);
    statobj.murl = hsSetPms(statobj.murl, loadres);
    statobj.curl = hsSetPms(statobj.curl, loadres);

    // 延迟加载
    context.on("opened",".filtbox", function() {
        if (filtbox.data("fetched") != true) {
            filtbox.data("fetched",    true);
            filtobj.load();
        }
    });
    context.on("opened",".statbox", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed",    null);
            statobj.load();
        }
    });

    // 管理动作
    context.on("click", ".toolbox .filter", function() {
        filtbox.toggleClass("invisible");
        if (! filtbox.is("invisible")) {
            filtbox.trigger("opened");
        }
    });
    context.on("click", ".findbox :submit", function() {
        filtbox.addClass("invisible");
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, listobj, filtobj, statobj);
        }

        // 权限控制
        $.each({"create":".create", "update":".update", "delete":".delete"}
        , function(k, v) {
            if (! hsChkUri("<%=_module%>/<%=_entity%>/"+k+".act")) {
                context.find(v).remove();
            }
        });
        // 外部限制
        $.each(denycss ? denycss . split (",") : [ ]
        , function(i, n) {
            if (/^find\./.test(n)) {
                n = ".findbox .form-group[data-name='"+n.substring(5)+"']";
                findbox.find(n).remove();
            } else
            {
                context.find(n).remove();
            }
        });
        // 用户未登录则隐藏我的
        if (!H$.uid()) {
            findbox.find(".mine-group").remove();
        }
        // 无可排序选项则隐藏之
        if (findbox.find(".sort-group option").size() <= 3) {
            findbox.find(".sort-group").remove();
        }
        // 无行内菜单项则隐藏之
        if (context.find(".edit-group button").size() == 0) {
            context.find(".edit-group").remove();
        }
        // 无过滤或统计则隐藏之
        if (filtbox.find(".filt-group").size() == 0
        &&  statbox.find(".stat-group").size() == 0) {
            context.find(".filter").remove();
        }

        // 加载数据
        listobj.load(listobj._url, findbox);
    });
})(jQuery);
</script>