<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "browse");
    String _pageId = (_module + "_" + _entity + "_" + _action).replace('/', '_');
    String _funcId = "in_"+(_module + "_" + _entity + "_list").replace('/', '_');

    String _conf   = FormSet.hasConfFile(_module + "/" + _entity)
                  || NaviMap.hasConfFile(_module + "/" + _entity)
                   ? _module + "/" + _entity : _module ;

    StringBuilder _rb = new StringBuilder(   "id,name"   );
    StringBuilder _ob = new StringBuilder("-mtime,-ctime");
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-list">
    <div>
        <div class="toolbox col-md-8 btn-group">
            <%if ( "select".equals(_action)) {%>
            <button type="button" class="ensure btn btn-primary"><%=_locale.translate("fore.select", _title)%></button>
            <%} // End If %>
            <button type="button" class="create btn btn-default"><%=_locale.translate("fore.create", _title)%></button>
            <%if (!"select".equals(_action)) {%>
            <button type="button" class="update for-choose btn btn-default"><%=_locale.translate("fore.update", _title)%></button>
            <button type="button" class="review for-choose btn btn-default"><%=_locale.translate("fore.review", _title)%></button>
            <button type="button" class="revert for-choose btn btn-warning" title="<%=_locale.translate("fore.revert", _title)%>"><span class="glyphicon glyphicon-time" ></span></button>
            <button type="button" class="delete for-checks btn btn-danger " title="<%=_locale.translate("fore.delete", _title)%>"><span class="glyphicon glyphicon-trash"></span></button>
            <button type="button" class="manual            btn btn-default" title="<%=_locale.translate("fore.manual", _title)%>"><span class="glyphicon glyphicon-book" ></span></button>
            <%} // End If %>
        </div>
        <form class="findbox col-md-4 input-group" action="" method="POST">
            <input type="search" name="<%=_fields.containsKey("word") ? "word" : "wd"%>" class="form-control input-search"/>
            <span class="input-group-btn">
                <button type="submit" class="btn btn-default search" title="<%=_locale.translate("fore.search", _title)%>"><span class="glyphicon glyphicon-search"></span></button>
            <%if (!"select".equals(_action)) {%>
                <button type="button" class="btn btn-default filter" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-filter"></span></button>
                <button type="button" class="btn btn-default statis" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-stats" ></span></button>
                <button type="button" class="btn btn-default export" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-save"  ></span></button>
            <%} // End If %>
            </span>
        </form>
    </div>
    <%
    if (!"select".equals(_action)) {
    %>
    <!-- 筛选 -->
    <form class="findbox fitrbox invisible row" style="background-color: #EEE; margin-left: 0; margin-right: 0">
        <div class="form-group"></div>
        <%
        Iterator it2 = _fields.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry et = (Map.Entry) it2.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");

            if ("@".equals(name) || "id".equals(name)
            || !Synt.declare(info.get("fitrable"), false)) {
                continue;
            }
        %>
        <div class="form-group form-group-sm row">
            <label class="col-sm-3 form-control-static control-label text-right"><%=text%></label>
            <div class="col-sm-6">
            <%if ("enum".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="ar.0.<%=name%>" data-ft="_enum"></select>
            <%} else if ("number".equals(type) || "range".equals(type)) {%>
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
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.GE_REL%>" data-toggle="hsTime" data-type="timestamp" />
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>.<%=Cnst.LE_REL%>" data-toggle="hsTime" data-type="timestamp" />
                </div>
            <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
                <%
                    String fm = _module;
                    String fn =  name  ;
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
                    String al = info.containsKey("data-al") ? (String) info.get("data-al") :
                              ( info.containsKey("conf"   ) ? (String) info.get("conf"   ) :  fm )
                        +"/"+ ( info.containsKey("form"   ) ? (String) info.get("form"   ) :  fn )
                        +"/list_fork.html";
                %>
                <ul  class="pickbox" data-ft="_fork" data-fn="ar.0.<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></button>
            <%} else {%>
                <input type="text" class="form-control" name="ar.0.<%=name%>" />
            <%} /*End If */%>
            </div>
        </div>
        <%} /*End For*/%>
        <div class="form-group form-group-sm row">
            <div class="col-sm-6 col-sm-offset-3">
                <button type="submit" class="btn btn-default">过滤</button>
                <span style="padding:0.1em;"></span>
                <button type="reset"  class="btn btn-default">重置</button>
            </div>
        </div>
    </form>
    <!-- 统计 -->
    <form class="findbox statbox invisible row" style="background-color: #EEE; margin-left: 0; margin-right: 0">
        <div class="clearfix" style="padding: 5px;">
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
                type = "statis";
            } else {
                rb   = "&rb.=" +  name ;
                type = "counts";
            }
        %>
        <div data-name="<%=name%>" data-text="<%=text%>" data-type="<%=type%>" data-rb="<%=rb%>" class="col-md-6" style="padding: 5px;">
            <div class="clearfix" style="background: #fff;">
                <div class="col-sm-3 checkbox" style="height: 250px; overflow: hidden; overflow-y: auto;"></div>
                <div class="col-sm-9 chartbox" style="height: 250px; margin: 10px 0; border-left: 1px dotted #ccc;"></div>
            </div>
        </div>
        <%} /*End For*/%>
        </div>
    </form>
    <%} /*End If */%>
    <!-- 列表 -->
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="id[]" data-ft="<%if ("select".equals(_action)) {%>_fork<%} else {%>_check<%}%>" class="_check">
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
                <%if ("number".equals(type) || "range".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> text-right"><%=text%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_htime" <%=ob%> class="<%=oc%> datetime"><%=text%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date" <%=ob%> class="<%=oc%> date"><%=text%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time" <%=ob%> class="<%=oc%> time"><%=text%></th>
                <%} else if ("email".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_email" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("url".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_ulink" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("file".equals(type) ||  "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_ulink" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("enum".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
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
                            name = name + "_data";
                        }
                        String subn = "name";
                        if (info.get("data-ak") != null) {
                            name = (String) info.get("data-ak");
                        }
                        if (info.get("data-tk") != null) {
                            subn = (String) info.get("data-tk");
                        }
                        name = name + "." + subn;
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
    <div class="pagebox"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");
    var loadbox = context.closest(".loadbox");
    var statbox = context.find(".statbox");
    var fitrbox = context.find(".fitrbox");
    var formbox = context.find(".findbox");
    var findbox = formbox.eq(0);

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        sendUrls: [
            ['<%=_module%>/<%=_entity%>/delete.act',
             '.delete',
             '<%=_locale.translate("fore.delete.confirm", _title)%>']
        ],
        openUrls: [
            ['<%=_module%>/<%=_entity%>/form.html?<%=Cnst.AB_KEY%>=!enum',
             '.create', '@'],
            ['<%=_module%>/<%=_entity%>/form_edit.html?<%=Cnst.ID_KEY%>={ID}',
             '.update', '@'],
            ['<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
             '.review', '@'],
            ['<%=_module%>/<%=_entity%>/logs.html?<%=Cnst.ID_KEY%>={ID}',
             '.revert', '@'],
            ['<%=_module%>/<%=_entity%>/lore.html',
             '.manual', '@']
        ],
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_enum,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>"
    });

    <%if (!"select".equals(_action)) {%>
    var filtobj = fitrbox.hsForm({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=!enum"
    });

    var statobj = context.hsStat({
        surl: "<%=_module%>/<%=_entity%>/statis/search.act?<%=Cnst.AB_KEY%>=_enum",
        curl: "<%=_module%>/<%=_entity%>/counts/search.act?<%=Cnst.AB_KEY%>=_enum,_fork"
    });

    if (fitrbox.find(".form-group").size() == 2) {
        findbox.find(".filter").remove();
    }
    if (statbox.find( ".col-md-6" ).size() == 0) {
        findbox.find(".statis").remove();
    }

    fitrbox.on("opened", function() {
        if (fitrbox.data("fetched") != true) {
            fitrbox.data("fetched"  ,  true);
            filtobj.load();
        }
    });
    statbox.on("opened", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed"  ,  null);
            statobj.load();
        }
    });

    filtobj._fill__enum = function(x, v, n, t) {
        hsListFillFilt.call(this , x, v, n, t);
    };
    <%} else {%>
    listobj._fill__fork = function(x, v, n, t) {
        hsListFillFork.call(this , x, v, n, t);
        x.find("input")
         .attr("title", this._info.name)
         .data(         this._info     );
    };
    <%} /*End If */%>

    // 创建时将关联 ID 往表单页传递
    listobj.open = function(btn, box, url, data) {
        if (btn.is(".create")) {
                data  = [];
                var d = hsSerialArr( loadbox );
            for(var i = 0; i < d.length; i ++) {
                if (/_id$/.test( d[i].name ) ) {
                    data.push( d[i] );
                }
            }
        }
        HsList.prototype.open.call(this, btn, box, url, data);
    };

    // 管理动作
    findbox.find(".filter").click(function() {
        fitrbox.toggleClass("invisible");
        if (!fitrbox.is("invisible")) {
            fitrbox.trigger("opened");
        }
    });
    findbox.find(".statis").click(function() {
        statbox.toggleClass("invisible");
        if (!statbox.is("invisible")) {
            statbox.trigger("opened");
        }
    });
    findbox.find(".export").click(function() {
        location.replace("<%=_module%>/<%=_entity%>/stream.act?" + formbox.serialize());
    });
    findbox.find(".search").click(function() {
        fitrbox.addClass("invisible");
    });
    fitrbox.find(":submit").click(function() {
        fitrbox.addClass("invisible");
    });
    fitrbox.find(":reset" ).click(function() {
        fitrbox.find("[data-ft=_fork]").each(function() {
            hsFormFillFork($(this), {});
        });
        setTimeout(function() {
            fitrbox.find(":submit").click( );
        } , 500);
    });

    hsRequires("<%=_module%>/<%=_entity%>/custom.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, listobj);
        }

        // 加载数据
        listobj.load(hsSetPms(listobj._url, loadbox), findbox);
    });
})(jQuery);
</script>