<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "browse");
    String _pageId = (_module + "_" + _entity + "_" + _action).replace('/', '_');
    String _funcId = "in_"+(_module + "_" + _entity + "_list").replace('/', '_');

    String _conf   = FormSet.hasConfFile(_module + "/" + _entity)
                  || NaviMap.hasConfFile(_module + "/" + _entity)
                   ? _module + "/" + _entity : _module ;

    StringBuilder _ob = new StringBuilder( "-,-boost,-mtime,-ctime");
    StringBuilder _rb = new StringBuilder("id,name");
    Set<String>   _wd = getWordable();
    Set<String>   _sd = getSrchable();
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-list">
    <form class="findbox toolbox board row">
        <div class="col-xs-6">
            <div class="btn-group">
                <%if ( "select".equals(_action)) {%>
                <button type="button" class="commit btn btn-primary"><%=_locale.translate("fore.select", _title)%></button>
                <%} // End If %>
                <button type="button" class="create btn btn-default"><%=_locale.translate("fore.create", _title)%></button>
                <%if (!"select".equals(_action)) {%>
                <button type="button" class="update for-choose btn btn-default"><%=_locale.translate("fore.update", _title)%></button>
                <button type="button" class="review for-choose btn btn-default"><%=_locale.translate("fore.review", _title)%></button>
                <button type="button" class="revert for-choose btn btn-default" title="<%=_locale.translate("fore.revert", _title)%>"><span class="glyphicon glyphicon-time "></span></button>
                <button type="button" class="recopy for-checks btn btn-default" title="<%=_locale.translate("fore.recopy", _title)%>"><span class="glyphicon glyphicon-copy "></span></button>
                <button type="button" class="delete for-checks btn btn-warning" title="<%=_locale.translate("fore.delete", _title)%>"><span class="glyphicon glyphicon-trash"></span></button>
                <%} // End If %>
            </div>
        </div>
        <div class="col-xs-6">
            <div class="input-group">
                <!-- <%=_wd%> -->
                <%
                    StringBuilder sp = new StringBuilder( );
                    if (! _wd.isEmpty()) {
                    for(String ss : _wd) {
                        ss = Dict.getValue(_fields, "" , ss , "__text__");
                        if (ss.length() != 0) sp.append( ss ).append(",");
                    }   if (sp.length() != 0) sp.setLength(sp.length()-1);
                    } else {
                        sp.append("\" disabled=\"disabled");
                    }
                %>
                <input  type="search" class="form-control" name="<%=Cnst.WD_KEY%>" placeholder="<%=sp%>" />
                <span class="input-group-btn">
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="glyphicon glyphicon-search"></span></button>
                    <button type="button" class="filter btn btn-default" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-filter"></span></button>
                    <button type="button" class="statis btn btn-default" title="<%=_locale.translate("fore.statis", _title)%>"><span class="glyphicon glyphicon-stats "></span></button>
                <%if (!"select".equals(_action)) {%>
                    <button type="button" class="record btn btn-default" title="<%=_locale.translate("fore.record", _title)%>"><span class="glyphicon glyphicon-record"></span></button>
                    <button type="button" class="manual btn btn-default" title="<%=_locale.translate("fore.manual", _title)%>"><span class="glyphicon glyphicon-book  "></span></button>
                <%} // End If %>
                </span>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
    <form class="findbox filtbox invisible panel panel-body">
        <%
        Iterator it2 = _fields.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry et = (Map.Entry) it2.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");

            if ("@".equals(name) || "id".equals(name)
            || !Synt.declare(info.get("filtable"), false)) {
                continue;
            }
        %>
        <div class="filt-group form-group form-group-sm clearfix" data-find="<%=name%>">
            <label class="col-xs-3 form-control-static control-label text-right"><%=text%></label>
            <div class="col-xs-6">
            <%if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                <div class="input-group">
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.GE_REL%>" />
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.LE_REL%>" />
                </div>
            <%} else if ("date".equals(type) || "time" .equals(type) || "datetime" .equals(type)) {%>
                <%
                    if ("datetime".equals(type)) {
                        type = "datetime-local";
                    }
                %>
                <div class="input-group">
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.GE_REL%>" data-toggle="hsTime" data-type="<%=info.get("type")%>" />
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.LE_REL%>" data-toggle="hsTime" data-type="<%=info.get("type")%>" />
                </div>
            <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                <%
                    String fn = name;
                    if (fn.endsWith( "." )) {
                        fn = fn.substring(0, fn.length() - 1);
                    }
                    String kn = fn +"_fork";
                    if (fn.endsWith("_id")) {
                        fn = fn.substring(0, fn.length() - 3);
                        kn = fn;
                    }
                    String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                    String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                    String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                    String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                %>
                <ul  class="pickbox" data-ft="_fork" data-fn="ar.0.<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></button>
            <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="ar.0.<%=name%>" data-ft="_enum"></select>
            <%} else {%>
                <%
                    // 搜索类型优先模糊匹配
                    if (_sd.contains(name)
                    && (_wd.contains(name)
                    ||  "textarea".equals(type)
                    ||  "textview".equals(type))) {
                        name += "."+ Cnst.CQ_REL + "\" placeholder=\"模糊匹配\"";
                    }
                %>
                <input class="form-control" type="text" name="ar.0.<%=name%>" />
            <%} /*End If */%>
            </div>
        </div>
        <%} /*End For*/%>
        <div class="form-group form-group-sm clearfix" style="margin: 0 0">
            <div class="col-xs-6 col-xs-offset-3">
                <button type="submit" class="btn btn-default">过滤</button>
                <span style="padding: 0.1em;"></span>
                <button type="reset"  class="btn btn-link"   >重置</button>
            </div>
        </div>
    </form>
    <!-- 统计 -->
    <form class="findbox statbox invisible row">
        <%
        Iterator it3 = _fields.entrySet().iterator();
        while (it3.hasNext()) {
            Map.Entry et = (Map.Entry) it3.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");

            if ("@".equals(name) || "id".equals(name)
            || !Synt.declare(info.get("statable"), false)) {
                continue;
            }

            String rb;

            if ("number".equals(type)) {
                String enumConf = Synt.defxult((String) info.get("conf"),_conf);
                String enumName = Synt.defxult((String) info.get("enum"), name);
                Map    enumData ;
                try {
                    enumData  = FormSet.getInstance(enumConf).getEnum(enumName);
                } catch (HongsException ex) {
                    enumData  = null;
                }
                if (enumData != null ) {
                    StringBuilder sb = new StringBuilder();
                    for (Object code : enumData.keySet( )) {
                        sb.append("&rb.=")
                          .append(name)
                          .append(":" )
                          .append(code);
                    }
                    rb = sb.toString( );
                } else {
                    rb = "&rb.="+ name ;
                }
                type = "amount";
            } else {
                rb   = "&rb.=" +  name ;
                type = "acount";
            }
        %>
        <div class="stat-group col-xs-6" data-find="<%=name%>" data-name="<%=name%>" data-text="<%=text%>" data-type="<%=type%>" data-rb="<%=rb%>">
            <div class="panel clearfix">
                <div class="col-xs-3 checkbox" style="height: 250px; overflow: auto;"></div>
                <div class="col-xs-9 chartbox" style="height: 250px; margin: 10px 0;"></div>
            </div>
        </div>
        <%} /*End For*/%>
    </form>
    <!-- 列表 -->
    <div class="table-responsive-revised">
    <div class="table-responsive listbox">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="id[]" data-ft="_check" class="_check">
                        <input type="checkbox" class="checkall" name="id[]"/>
                    </th>
                <%
                Iterator it = _fields.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry et = (Map.Entry) it.next();
                    Map     info = (Map ) et.getValue();
                    String  name = (String) et.getKey();
                    String  type = (String) info.get("__type__");
                    String  text = (String) info.get("__text__");

                    if ("@".equals(name) || "hidden".equals(type)
                    || !Synt.declare(info.get("listable"), false)) {
                        continue;
                    }

                    String ob = "";
                    String oc = "";
                    if (Synt.declare(info.get("sortable"), false)) {
                        ob = (String)info.get("data-ob" );
                        if (ob == null) {
                            ob = name;
                        }
                        ob = "data-ob=\""+ob+"\"";
                        oc = "sortable";
                    }

                    // Unix 时间戳类需乘 1000 以转换为毫秒
                    if ( "datetime".equals(type)
                    ||       "date".equals(type)
                    ||       "time".equals(type)) {
                        Object typa = info.get ( "type" );
                    if ("timestamp".equals(typa)
                    ||  "datestamp".equals(typa)) {
                        ob += " data-fl=\"!v?v:v* 1000\"";
                    }}

                    _rb.append(',').append(name);
                %>
                <%if ("number".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> numerial text-right"><%=text%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date"  <%=ob%> class="<%=oc%> numerial date"><%=text%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time"  <%=ob%> class="<%=oc%> numerial time"><%=text%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_htime" <%=ob%> class="<%=oc%> numerial _htime"><%=text%></th>
                <%} else if (  "url".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_ulink" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("email".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_email" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("image".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_image" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("video".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_video" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("audio".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_audio" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("file".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_files" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("enum".equals(type) || "type".equals(type) || "check".equals(type) || "radio".equals(type) || "select".equals(type)) {%>
                    <%
                        if (name.endsWith( "." )) {
                            name = name.substring(0, name.length() - 1);
                        }
                        if (name.endsWith("_id")) {
                            name = name.substring(0, name.length() - 3);
                        } else {
                            name = name + "_text";
                        }
                    %>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        if (name.endsWith( "." )) {
                            name = name.substring(0, name.length() - 1);
                        }
                        if (name.endsWith("_id")) {
                            name = name.substring(0, name.length() - 3);
                        } else {
                            name = name + "_fork";
                        }
                        String subn = "name";
                        if (info.get("data-ak") != null) {
                            name = (String) info.get("data-ak");
                        }
                        if (info.get("data-tk") != null) {
                            subn = (String) info.get("data-tk");
                        }
                        if (Synt.declare(info.get("__repeated__"), false)) {
                            name = name + "." ;
                        }
                        name = name +"."+ subn;
                    %>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if (!"primary".equals(info.get("primary")) && !"foreign".equals(info.get("foreign"))) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} /*End If */%>
                <%} /*End For*/%>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div></div>
    <div class="pagebox clearfix"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");
    var loadbox = context.closest(".loadbox");
    var findbox = context.find(".findbox");
    var filtbox = context.find(".filtbox");
    var statbox = context.find(".statbox");

    // 权限控制
    if (!hsChkUri("<%=_module%>/<%=_entity%>/create.act")) context.find(".create").remove();
    if (!hsChkUri("<%=_module%>/<%=_entity%>/update.act")) context.find(".update").remove();
    if (!hsChkUri("<%=_module%>/<%=_entity%>/delete.act")) context.find(".delete").remove();
    if (!hsChkUri("<%=_module%>/<%=_entity%>/revert/search.act")) context.find(".revert").remove();
    if (!hsChkUri("<%=_module%>/<%=_entity%>/revert/search.act")) context.find(".record").remove();

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
              '.review', '@' ],
            [ '<%=_module%>/<%=_entity%>/snap.html?<%=Cnst.ID_KEY%>={ID}',
              '.revert', '@' ],
            [ '<%=_module%>/<%=_entity%>/snap.html',
              '.record', '@' ],
            [ '<%=_module%>/<%=_entity%>/swap.html',
              '.manual', '@' ]
        ],
        send: hsSendWithMemo,
        <%if ("select".equals(_action)) {%>
        _fill__check: hsListFillSele,
        <%} /*End If */%>
        // 链接填充, 支持多值, 占格子窄
        _fill__ulink: hsListWrapOpen("link" ),
        _fill__files: hsListWrapOpen("file" ),
        _fill__email: hsListWrapOpen("email"),
        _fill__image: hsListWrapOpen("image"),
        _fill__video: hsListWrapOpen("video"),
        _fill__audio: hsListWrapOpen("audio")
    });

    var filtobj = filtbox.hsForm({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=!enum",
        _prep__enum : hsListPrepFilt,
        _fill__enum : hsListFillFilt
    });

    var statobj = context.hsStat({
        aurl: "<%=_module%>/<%=_entity%>/statis/survey.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.AB_KEY%>=_text",
        curl: "<%=_module%>/<%=_entity%>/statis/search.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.AB_KEY%>=_text,_fork"
    });

    var loadarr = hsSerialArr(loadbox);
    var loadres = hsSerialDat(loadarr);

    // 绑定参数
    listobj._url = hsSetPms(listobj._url, loadarr);
    statobj.aurl = hsSetPms(statobj.aurl, loadarr);
    statobj.curl = hsSetPms(statobj.curl, loadarr);

    // 移除参数限定的过滤项
    for(var fn in loadres) {
    if (loadres   [ fn ] ) {
        findbox.children("[data-find='"+fn+"']").remove();
        findbox.find(".record").remove();
    }}

    // 无过滤或统计则隐藏之
    if (filtbox.find(".filt-group").size() == 0) {
        findbox.find(".filter").remove();
    }
    if (statbox.find(".stat-group").size() == 0) {
        findbox.find(".statis").remove();
    }

    // 延迟加载
    filtbox.on("opened", function() {
        if (filtbox.data("fetched") != true) {
            filtbox.data("fetched"  ,  true);
            filtobj.load();
        }
    });
    statbox.on("opened", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed"  ,  null);
            statobj.load();
        }
    });

    // 管理动作
    findbox.find(".filter").click(function() {
        filtbox.toggleClass("invisible");
        if (! filtbox.is("invisible")) {
            filtbox.trigger("opened");
        }
    });
    findbox.find(".statis").click(function() {
        statbox.toggleClass("invisible");
        if (! statbox.is("invisible")) {
            statbox.trigger("opened");
        }
    });
    findbox.find(".recopy").click(function() {
        hsCopyListData(listobj.listBox);
    });
    findbox.find(".search").click(function() {
        filtbox.addClass("invisible");
    });
    filtbox.find(":submit").click(function() {
        filtbox.addClass("invisible");
    });
    filtbox.find(":reset" ).click(function() {
        filtbox.find("[data-ft=_fork]").each(function() {
            hsFormFillFork($(this), {});
        });
        setTimeout(function() {
            filtbox.find(":submit").click( );
        } , 500);
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, listobj);
        }

        // 加载数据
        listobj.load(listobj._url, findbox);
    });
})(jQuery);
</script>