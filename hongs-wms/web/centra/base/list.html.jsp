<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.HashSet" %>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "search");
    String _funcId = "in_"+(_module + "_" + _entity + "_list").replace('/', '_');
    String _pageId = /* */ (_module + "-" + _entity + "-list").replace('/', '-');

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
                <button type="button" class="checks btn btn-warning"><%=_locale.translate("fore.selall", _title)%></button>
                <%} /*End If*/%>
                <button type="button" class="create btn btn-default"><%=_locale.translate("fore.create", _title)%></button>
                <%if ("search".equals(_action)) {%>
                <button type="button" class="update for-choose btn btn-default"><%=_locale.translate("fore.update", _title)%></button>
                <button type="button" class="recite for-choose btn btn-default"><%=_locale.translate("fore.recite", _title)%></button>
                <button type="button" class="reveal for-choose btn btn-default" title="<%=_locale.translate("fore.reveal", _title)%>"><span class="bi bi-hi-reveal"></span></button>
                <button type="button" class="copies for-checks btn btn-default" title="<%=_locale.translate("fore.copies", _title)%>"><span class="bi bi-hi-export"></span></button>
                <button type="button" class="delete for-checks btn btn-default" title="<%=_locale.translate("fore.delete", _title)%>"><span class="bi bi-hi-remove text-danger"></span></button>
                <%} /*End If*/%>
            </div>
            <%if ("select".equals(_action)) {%>
            <div class="btn btn-text text-muted picksum"><%=_locale.translate("fore.selected", _title)%> <b class="picknum"></b></div>
            <div class="for-checks for-choose invisible"></div>
            <%} /*End If*/%>
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
                    <button type="button" class="sifter btn btn-default" title="<%=_locale.translate("fore.sifter", _title)%>"><span class="bi bi-hi-sifter"></span></button>
                    <button type="button" class="statis btn btn-default" title="<%=_locale.translate("fore.statis", _title)%>"><span class="bi bi-hi-statis"></span></button>
                    <button type="button" class="column btn btn-default" title="<%=_locale.translate("fore.column", _title)%>"><span class="bi bi-hi-column"></span></button>
                </span>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
<%@include file="_sift_.jsp"%>
    <!-- 统计 -->
<%@include file="_stat_.jsp"%>
    <!-- 列表 -->
    <div class="listbox rollbox panel panel-default table-responsive">
        <table class="table table-hover table-striped table-compressed">
            <thead>
                <tr>
                    <th data-fn="id." data-ft="_check" class="_check">
                        <input name="id." type="checkbox" class="checkall"/>
                    </th>
                    <%if ("search".equals(_action)) {%>
                    <th data-fn="_" data-ft="_admin" class="_admin _amenu">
                        <div class="dropdown invisible">
                            <a href="javascript:;" data-toggle="dropdown"><span class="bi bi-hi-action"></span></a>
                            <ul class="dropdown-menu">
                                <li><a href="javascript:;" class="update"><%=_locale.translate("fore.update", _title)%></a></li>
                                <li><a href="javascript:;" class="recite"><%=_locale.translate("fore.recite", _title)%></a></li>
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

                    if ("datetime".equals(type)
                    ||      "date".equals(type)
                    ||      "time".equals(type)) {
                        // Unix 时间戳类需乘 1000 以转换为毫秒
                        Object typa = info.get("type");
                        if ("timestamp".equals( typa )
                        ||  "datestamp".equals( typa )) {
                            ob += " data-fill=\"!v?v:v*1000\"";
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
                    ||    "sorted".equals(type)
                    ||     "range".equals(type)
                    ||     "color".equals(type)) {
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            ob += " data-format=\"" + frmt + "\"";
                        }
                    } else
                    if ("textarea".equals(type)
                    ||  "textview".equals(type)) {
                        ob += " data-type=" + Synt.declare(info.get("type"), "text");
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
                <%} else if ("image".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_image" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("video".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_video" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("audio".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_audio" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ( "file".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_files" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if (  "url".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_links" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("email".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_email" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_texts" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        String subn =  "name" ;
                        if (info.get("data-tk") != null) {
                            subn = (String) info.get("data-tk");
                        }
                        if (info.get("data-ln") != null) {
                            name = (String) info.get("data-ln");
                        } else
                        if (name.endsWith("_id")) {
                            name = name.substring(0 , name.length( ) - 3 );
                        } else
                        {
                            name = name + "_fork";
                        }
                        if (Synt.declare(info.get("__repeated__"), false)) {
                            name = name + "." ;
                        }
                        name = name +"."+ subn;
                    %>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("enum".equals(type) || "type".equals(type) || "check".equals(type) || "radio".equals(type) || "select".equals(type)) {%>
                    <%
                        name = name + "_text" ;
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
    var siftbox = context.find(".siftbox");
    var statbox = context.find(".statbox");

    var loadres = hsSerialDic(loadbox);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        loadData: loadres,
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete', '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
        ],
        openUrls: [
            [ '<%=_module%>/<%=_entity%>/form_init.html?'+$.param(hsSerialArr(loadres)),
              '.create', '@' ],
            [ '<%=_module%>/<%=_entity%>/form.html?<%=Cnst.ID_KEY%>={ID}',
              '.update', '@' ],
            [ '<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
              '.recite', '@' ],
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
        _fill__links: hsListWrapOpen("link" ),
        _fill__files: hsListWrapOpen("file" ),
        _fill__image: hsListWrapOpen("image"),
        _fill__video: hsListWrapOpen("video"),
        _fill__audio: hsListWrapOpen("audio")
    });

    var siftobj = siftbox.hsSift({
        _url: "<%=_module%>/<%=_entity%>/recipe.act?<%=Cnst.AB_KEY%>=.enfo"
    });

    var statobj = context.hsStat({
        _url: "<%=_module%>/<%=_entity%>/acount.act?<%=Cnst.AB_KEY%>=_text,_fork,linked&"
            + "<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.OB_KEY%>=-&"
            +  $.param(hsSerialArr(loadres))
    });

    // 延迟加载
    context.on("opened",".siftbox", function() {
        if (siftbox.data("fetched") != true) {
            siftbox.data("fetched"  ,  true);
            siftobj.load();
        }
    });
    context.on("opened",".statbox", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed"  ,  null);
            statobj.load();
        }
    });

    // 管理动作
    context.on("click", ".toolbox .sifter", function() {
        siftbox.toggleClass("invisible");
        if (! siftbox.is("invisible")) {
            siftbox.trigger("opened");
        }
        statbox.addClass("invisible");
    });
    context.on("click", ".toolbox .statis", function() {
        statbox.toggleClass("invisible");
        if (! statbox.is("invisible")) {
            statbox.trigger("opened");
        }
        siftbox.addClass("invisible");
    });
    context.on("click", ".toolbox :submit", function() {
        siftbox.addClass("invisible");
    });
    context.on("click", ".siftbox :submit", function() {
        siftbox.addClass("invisible");
    });
    context.on("click", ".toolbox .copies", function() {
        hsCopyListData(listbox);
    });
    context.on("click", ".toolbox .checks", function() {
        hsPickListMore(listbox);
    });
    context.on("click", ".toolbox .column", function() {
        hsHideListCols(listbox);
    });
    hsSaveListCols(listbox, "<%=_pageId%>");

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, listobj, siftobj, statobj))
         .then(function() {

        // 权限控制
        $.each({"recite":".recite", "create":".create", "update":".update", "delete":".delete", "reveal":".reveal"}
        , function(k, v) {
            if (! hsGetAuth("<%=_module%>/<%=_entity%>/"+k+".act")) {
                context.find(v).remove();
            }
        });
        // 外部限制
        $.each(denycss ? denycss . split (",") : [ ]
        , function(i, n) {
            if (/^find\./.test(n)) {
                n = n.substring(5);
                n = "[data-sift=fn]>[value='"+n+"'],"
                  + ".filt-group[data-name='"+n+"'],"
                  + ".stat-group[data-name='"+n+"']";
                findbox.find(n).remove();
            } else
            if (/^list\./.test(n)) {
                n = n.substring(5);
                n = "th[data-fn='"+n+"']";
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
        if (siftbox.find("[data-sift=fn]>[value!='-']").size() == 0) {
            siftbox.find(".sift-body" ).hide();
        }
        if (siftbox.find(".filt-group").size() == 0) {
            siftbox.find(".filt-body" ).hide();
        }
        if (siftbox.find("[data-sift=fn]>[value!='-']").size() == 0
        &&  siftbox.find(".filt-group").size() == 0) {
            findbox.find(".sifter").remove();
        }
        if (statbox.find(".stat-group").size() == 0) {
            findbox.find(".statis").remove();
        }
        <%if ("select".equals(_action)) {%>
        // 单选移除跨页全选
        if (! loadbox.is(".pickmul")) {
            findbox.find(".checks").remove();
        }
        <%} /*End If */%>

        // 自适滚动
        var h = hsFlexRoll(listbox.filter(".rollbox"), $("#main-context"));
        if (h > 300) {
            statbox.css ("max-height", h+"px")
                   .css ("overflow-y", "auto");
            h = h - 80; // 去掉边框和底部高度
        if (h > 300) {
            siftbox.find(".rollbox")
                   .css ("max-height", h+"px")
                   .css ("overflow-y", "auto");
        }}

        // 加载数据
        listobj.load();

        }); // End Promise
    });
})(jQuery);
</script>