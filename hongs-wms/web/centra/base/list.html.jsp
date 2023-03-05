<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
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

    StringBuilder _ob = new StringBuilder("boost!,mtime!,ctime!");
    StringBuilder _rb = new StringBuilder("id,name");
    Set<String>   _wd = getWordable (_fields);
    Set<String>   _sd = getSrchable (_fields);
%>
<h2 class="hide"><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId+" "+_action%>-list">
    <form class="findbox toolbox board row">
        <div class="col-xs-7">
            <div class="btn-group">
                <%if ("select".equals(_action)) {%>
                <button type="button" class="commit btn btn-primary"><%=_locale.translate("fore.select", _title)%></button>
                <%} // End If %>
                <button type="button" class="create btn btn-default"><%=_locale.translate("fore.create", _title)%></button>
                <%if ("browse".equals(_action)) {%>
                <button type="button" class="update for-choose btn btn-default"><%=_locale.translate("fore.update", _title)%></button>
                <button type="button" class="review for-choose btn btn-default"><%=_locale.translate("fore.review", _title)%></button>
                <button type="button" class="reveal for-choose btn btn-default" title="<%=_locale.translate("fore.reveal", _title)%>"><span class="bi bi-hi-reveal"></span></button>
                <button type="button" class="copies for-checks btn btn-default" title="<%=_locale.translate("fore.copies", _title)%>"><span class="bi bi-hi-export"></span></button>
                <button type="button" class="delete for-checks btn btn-default" title="<%=_locale.translate("fore.delete", _title)%>"><span class="bi bi-hi-remove text-danger"></span></button>
                <%} // End If %>
            </div>
            <%if ("select".equals(_action)) {%>
            <div class="btn btn-text text-muted picksum"><%=_locale.translate("fore.selected", _title)%> <b class="picknum"></b></div>
            <div class="for-checks for-choose invisible"></div>
            <%} // End If %>
        </div>
        <div class="col-xs-5">
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
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="bi bi-hi-search"></span></button>
                    <button type="button" class="filter btn btn-default" title="<%=_locale.translate("fore.filter", _title)%>"><span class="bi bi-hi-filter"></span></button>
                    <button type="button" class="statis btn btn-default" title="<%=_locale.translate("fore.statis", _title)%>"><span class="bi bi-hi-statis "></span></button>
                    <button type="button" class="column btn btn-default" title="<%=_locale.translate("fore.column", _title)%>"><span class="bi bi-hi-column"></span></button>
                </span>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
    <form class="findbox filtbox invisible well form-horizontal">
        <div class="group-end">
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
        <div class="filt-group form-group form-group-sm row" data-name="<%=name%>">
            <label class="col-xs-3 text-right control-label form-control-static">
                <%=text != null ? text : ""%>
            </label>
            <div class="col-xs-9">
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
                    String kn = fn +"_fork";
                    if (fn.endsWith("_id")) {
                        fn = fn.substring(0, fn.length() - 3);
                        kn = fn;
                    }
                    String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                    String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                    String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                    String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                    al = al.replace("centre", "centra");
                    // 选择时禁用创建
                    if ( ! al.isEmpty (   )) {
                    if ( ! al.contains("#")) {
                        al = al + "#.deny=.create";
                    } else {
                        al = al + "&.deny=.create";
                    }}
                %>
                <ul class="pickbox pickmul" data-ft="_fork" data-fn="<%=name%>.<%=Cnst.ON_REL%>." data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>" data-item-class="btn btn-sm btn-info" data-icon-class="-"></ul>
                <button type="button" class="btn btn-sm btn-default form-control" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></button>
            <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="<%=name%>.<%=Cnst.ON_REL%>" data-ft="_enum"></select>
            <%} else {%>
                <%
                    // 搜索类型优先模糊匹配
                    if (_sd.contains(name)
                    && (_wd.contains(name)
                    ||  "textarea".equals(type)
                    ||  "textview".equals(type))) {
                        name += "."+ Cnst.CQ_REL + "\" placeholder=\"模糊匹配";
                    } else {
                        name += "."+ Cnst.ON_REL + "\" placeholder=\"精确匹配";
                    }
                %>
                <input class="form-control" type="text" name="<%=name%>" />
            <%} /*End If */%>
            </div>
        </div>
        <%} /*End For*/%>
        <hr  style="clear: both;"/>
        <div class="btns-group form-group form-group-sm row">
            <div class="col-xs-12 text-center">
                <button type="submit" class="btn btn-primary">过滤</button>
                <span style="padding: 0.1em;"></span>
                <button type="reset"  class="btn btn-default">重置</button>
            </div>
        </div>
        </div>
    </form>
    <!-- 统计 -->
    <form class="findbox statbox invisible well">
        <div class="board-end row">
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

            // 图表类型
            String plot = (String)info.get("stat-plot");
            if (plot != null && ! plot.isEmpty()) {
                type += "\" data-plot=\"" + plot;
            }

            // 附加参数
            String prms = (String)info.get("stat-prms");
            if (prms != null && ! prms.isEmpty()) {
                type += "\" data-prms=\"" + prms;
            }
        %>
        <div class="stat-group col-xs-6" data-name="<%=name%>" data-text="<%=text%>" data-type="<%=type%>">
            <div class="panel panel-body panel-default clearfix" style="height: 302px;">
                <div class="checkbox col-xs-3" style="height: 100%; display:none"></div>
                <div class="chartbox col-xs-9" style="height: 100%; display:none"></div>
                <div class="alertbox"><div><%=text%> <%=_locale.translate("fore.loading")%></div></div>
            </div>
        </div>
        <%} /*End For*/%>
        </div>
    </form>
    <!-- 列表 -->
    <div class="listbox rollbox panel panel-default table-responsive">
        <table class="table table-hover table-striped table-compressed">
            <thead>
                <tr>
                    <th data-fn="id." data-ft="_check" class="_check">
                        <input name="id." type="checkbox" class="checkall"/>
                    </th>
                    <%if ("browse".equals(_action)) {%>
                    <th data-fn="_" data-ft="_admin" class="_admin _amenu">
                        <div class="dropdown invisible">
                            <a href="javascript:;" data-toggle="dropdown"><span class="bi bi-hi-action"></span></a>
                            <ul class="dropdown-menu">
                                <li><a href="javascript:;" class="update"><%=_locale.translate("fore.update", _title)%></a></li>
                                <li><a href="javascript:;" class="review"><%=_locale.translate("fore.review", _title)%></a></li>
                                <li><a href="javascript:;" class="reveal"><%=_locale.translate("fore.reveal", _title)%></a></li>
                                <li><a href="javascript:;" class="delete"><span class="text-danger"><%=_locale.translate("fore.delete", _title)%></span></a></li>
                            </ul>
                        </div>
                    </th>
                    <%} /*End If*/%>
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
                            ob  = "*,"+ name;
                        }
                        ob = "data-ob=\""+ob+"\"";
                        oc = "sortable";
                    }

                    if ("textarea".equals(type)
                    ||  "textview".equals(type)) {
                        ob += " data-type=" + Synt.declare(info.get("type"), "text");
                    } else
                    if ("datetime".equals(type)
                    ||      "date".equals(type)
                    ||      "time".equals(type)) {
                        // Unix 时间戳类需乘 1000 以转换为毫秒
                        Object typa = info.get("type");
                        if ("timestamp".equals( typa )
                        ||  "datestamp".equals( typa )) {
                            ob += " data-fl=\"!v?v:v*1000\"";
                        }
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            ob += " data-format=\"" + frmt + "\"";
                        } else
                        // 默认为短格式
                        if ("datetime" .equals( type )) {
                            type  = "htime";
                        } else
                        if ("date"     .equals( type )) {
                            type  = "hdate";
                        }
                    } else
                    if (  "number".equals(type)
                    ||     "range".equals(type)
                    ||     "color".equals(type)) {
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            ob += " data-format=\"" + frmt + "\"";
                        }
                    }

                    _rb.append(',').append(name);
                %>
                <%if ("number".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> numerial text-right"><%=text%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_datetime" <%=ob%> class="<%=oc%> numerial datetime"><%=text%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date" <%=ob%> class="<%=oc%> numerial date"><%=text%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time" <%=ob%> class="<%=oc%> numerial time"><%=text%></th>
                <%} else if ("hdate".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_hdate" <%=ob%> class="<%=oc%> numerial _hdate"><%=text%></th>
                <%} else if ("htime".equals(type)) {%>
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
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_texts" <%=ob%> class="<%=oc%>"><%=text%></th>
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
    </div>
    <div class="pagebox clearfix">
        <em class="page-text">...</em>
    </div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var listbox = context.find(".listbox");
    var findbox = context.find(".findbox");
    var filtbox = context.find(".filtbox");
    var statbox = context.find(".statbox");

    var loadres = hsSerialDic(loadbox);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _data : loadres,
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete',
              '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
        ],
        openUrls: [
            [ '<%=_module%>/<%=_entity%>/form_init.html?'+$.param(hsSerialArr(loadres)),
              '.create', '@' ],
            [ '<%=_module%>/<%=_entity%>/form.html?<%=Cnst.ID_KEY%>={ID}',
              '.update', '@' ],
            [ '<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
              '.review', '@' ],
            [ '<%=_module%>/<%=_entity%>/snap.html?<%=Cnst.ID_KEY%>={ID}',
              '.reveal', '@' ]
        ],
        load: hsLoadWithWord,
        send: hsSendWithMemo,
        <%if ("select".equals(_action)) {%>
        _fill__check: hsListFillSele,
        <%} /*End If */%>
        // 多行文本, 富文本等
        _fill__texts: hsFillListMore,
        // 链接填充, 支持多值, 占格子窄
        _fill__ulink: hsListWrapOpen("link" ),
        _fill__files: hsListWrapOpen("file" ),
        _fill__image: hsListWrapOpen("image"),
        _fill__video: hsListWrapOpen("video"),
        _fill__audio: hsListWrapOpen("audio")
    });

    var filtobj = filtbox.hsForm({
        _url: "<%=_module%>/<%=_entity%>/select.act?<%=Cnst.AB_KEY%>=.enfo",
        _doll__enum : hsListDollFilt,
        _fill__enum : hsListFillFilt
    });

    var statobj = context.hsStat({
        murl: "<%=_module%>/<%=_entity%>/amount.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.OB_KEY%>=-&<%=Cnst.AB_KEY%>=linked,_text",
        curl: "<%=_module%>/<%=_entity%>/acount.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.OB_KEY%>=-&<%=Cnst.AB_KEY%>=linked,_text,_fork"
    });

    // 绑定参数
    listobj._url = hsSetPms(listobj._url, loadres);
    statobj.murl = hsSetPms(statobj.murl, loadres);
    statobj.curl = hsSetPms(statobj.curl, loadres);

    // 延迟加载
    context.on("opened",".filtbox", function() {
        if (filtbox.data("fetched") != true) {
            filtbox.data("fetched"  ,  true);
            filtobj.load();
        }
    });
    context.on("opened",".statbox", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed"  ,  null);
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
    context.on("click", ".toolbox .statis", function() {
        statbox.toggleClass("invisible");
        if (! statbox.is("invisible")) {
            statbox.trigger("opened");
        }
    });
    context.on("click", ".toolbox :submit", function() {
        filtbox.addClass("invisible");
    });
    context.on("click", ".filtbox :submit", function() {
        filtbox.addClass("invisible");
    });
    context.on("click", ".toolbox .copies", function() {
        hsCopyListData(listbox);
    });
    context.on("click", ".toolbox .column", function() {
        hsHideListCols(listbox);
    });
    hsSaveListCols(listbox, "<%=_pageId%>");

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        Promise.resolve(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, listobj, filtobj, statobj))
               .then(function() {

        // 权限控制
        $.each({"create":".create", "update":".update",
                "delete":".delete", "reveal":".reveal"}
        , function(k, v) {
            if (! hsChkUri("<%=_module%>/<%=_entity%>/"+k+".act")) {
                context.find(v).remove();
            }
        });
        // 外部限制
        $.each(denycss ? denycss . split (",") : [ ]
        , function(i, n) {
            if (/^stat\./.test(n)) {
                n = ".form-group[data-name='"+n.substring(5)+"']";
                statbox.find(n).remove();
            } else
            if (/^filt\./.test(n)) {
                n = ".form-group[data-name='"+n.substring(5)+"']";
                filtbox.find(n).remove();
            } else
            if (/^find\./.test(n)) {
                n = ".form-group[data-name='"+n.substring(5)+"']";
                findbox.find(n).remove();
            } else
            if (/^list\./.test(n)) {
                n = "th[data-fn='"+n.substring(5)+"']";
                listbox.find(n).remove();
            } else
            {
                context.find(n).remove();
            }
        });
        // 无行内菜单项则隐藏之
        if (listbox.find("thead ._amenu ul>li>a").size() == 0) {
            listbox.find("thead ._amenu").addClass( "hidden" );
        }
        // 无操作按钮则隐藏选择
        if (findbox.find(".for-choose").size() == 0
        &&  findbox.find(".for-checks").size() == 0) {
            listbox.find("thead ._check").addClass( "hidden" );
        }
        // 无过滤或统计则隐藏之
        if (filtbox.find(".filt-group").size() == 0) {
            findbox.find(".filter").remove();
        }
        if (statbox.find(".stat-group").size() == 0) {
            findbox.find(".statis").remove();
        }

        // 自适滚动
        var h = hsFlexRoll(listbox.filter(".rollbox"), $("#main-context"));
        if (h > 0) {
            filtbox.css("max-height", h+"px");
            filtbox.css("overflow-y", "auto");
            statbox.css("max-height", h+"px");
            statbox.css("overflow-y", "auto");
        }

        // 加载数据
        listobj.load(null, findbox);

        }); // End Promise
    });
})(jQuery);
</script>