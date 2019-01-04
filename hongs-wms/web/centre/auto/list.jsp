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
    StringBuilder _rb = new StringBuilder("id,name,note,logo,cuser");
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_action%>-list">
    <div class="row">
    <div class="col-md-6 col-sm-8 center-block">
        <div style="display: table; width: 100%;">
        <div style="display: table-cell; width: 15px; vertical-align: middle;">
            <div class="toolbox btn-group">
                <button type="button" class="create btn btn-primary" style="margin-right: 15px;"><%=_locale.translate("fore.create", _title)%></button>
            </div>
        </div>
        <div style="display: table-cell; width: 100%; vertical-align: middle;">
            <form class="findbox input-group">
                <input type="search" name="<%=_fields.containsKey("word") ? "word" : "wd"%>" class="form-control input-search">
                <span class="input-group-btn">
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="glyphicon glyphicon-search"></span></button>
                    <button type="button" class="filter btn btn-default" title="<%=_locale.translate("fore.filter", _title)%>"><span class="glyphicon glyphicon-filter"></span></button>
                </span>
            </form>
        </div>
        </div>
    </div>
    </div>
    <!-- 筛选 -->
    <form class="findbox filtbox statbox panel invisible">
        <div class="form-group clearfix"></div>
        <%
        Iterator it2 = _fields.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry et = (Map.Entry) it2.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
        %>
        <%if ("@".equals(name) || "id".equals(name)) {%>
        <%} else if (Synt.declare(info.get("statable"), false)) {%>
        <div class="stat-group form-group form-group-sm clearfix">
            <label class="col-md-3 col-sm-2 form-control-static control-label text-right"><%=text%></label>
            <div class="col-md-6 col-sm-8">
                <%
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
                <div class="checkbox" style="margin: 6px 0px; font-size: 13px; line-height: 1.5;" data-name="<%=name%>" data-type="<%=type%>" data-rb="<%=rb%>"></div>
            </div>
        </div>
        <%} else if (Synt.declare(info.get("findable"), false)) {%>
        <div class="filt-group form-group form-group-sm clearfix">
            <label class="col-md-3 col-sm-2 form-control-static control-label text-right"><%=text%></label>
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
                    al = al.replace("centra", "centre").replace("list_fork", "pick");
                %>
                <ul  class="pickbox" data-ft="_fork" data-fn="<%=name%>" data-ak="<%=ak%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                <button type="button" class="btn btn-default form-control" data-toggle="hsFork" data-target="" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></button>
            <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="<%=name%>" data-ft="_enum"></select>
            <%} else {%>
                <input type="text" class="form-control" name="<%=name%>" />
            <%} /*End If */%>
            </div>
        </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <div class="form-group form-group-sm clearfix">
            <label class="col-md-3 col-sm-2 form-control-static control-label text-right">排序</label>
            <div class="col-md-6 col-sm-8">
                <input type="hidden" name="ob" value="<%=_ob%>" data-ft="_sort"/>
                <div>
                    <select class="form-control" style="width: auto; display: inline-block;">
                        <option value="<%=_ob%>"></option>
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
                        <option value="<%=name%>"><%=text%></option>
        <%} /*End for*/%>
                    </select>
                    <select class="form-control" style="width: auto; display: inline-block;">
                        <option value="" >正序</option>
                        <option value="-">逆序</option>
                    </select>
                </div>
            </div>
        </div>
        <div class="form-group form-group-sm clearfix" style="margin-bottom: 0px">
            <div class="col-md-6 col-md-offset-3 col-sm-8 col-sm-offset-2">
                <button type="submit" class="btn btn-sm btn-default">应用</button>
                <span style="padding:0.1em;"></span>
                <button type="reset"  class="btn btn-sm btn-default">重置</button>
                <span class="form-control-static owner" style="vertical-align: middle;">
                    <label>
                        <input name="cuser" type="checkbox" style="margin-right: 5px;"/>
                        <span>我创建的</span>
                    </label>
                </span>
            </div>
        </div>
        <div class="form-group clearfix"></div>
    </form>
    <!-- 列表 -->
    <div class="itembox col-md-4 col-sm-6" style="display: none; padding: 0 7.5px 15px 7.5px;">
        <input class="rowid" type="hidden" name="id" data-fn="id" data-fl="$(this).val(v)&&null"/>
        <div class="panel panel-default" style="margin: 0; padding: 0; position: relative;">
            <div class="panel-body" style="display: table; width: 100%;">
                <%if (_fields.containsKey("logo")) {%>
                <div style="display: table-cell; width: 10px; padding: 0px; vertical-align: top;">
                    <div class="review" style="height: 100px; overflow: hidden; cursor: pointer;">
                        <div data-fn="logo" style="width: 100px; height: 100px; margin-right: 15px; border-radius: 4px;"></div>
                    </div>
                </div>
                <%} /*End if */%>
                <div style="display: table-cell; width: 100%; padding: 0px; vertical-align: top;">
                    <div class="review" style="height: 100px; overflow: hidden; cursor: pointer;">
                        <div data-fn="name" style="color: #444;"></div>
                        <div data-fn="note" style="color: #888;"></div>
                    </div>
                    <div data-ft="edit" data-fn="cuser" class="btn-group" style="position: absolute; right: 0; bottom: 0; opacity: 0.8; display: none;">
                        <button type="button" class="btn btn-sm btn-default update"><span class="glyphicon glyphicon-edit "></span></button>
                        <button type="button" class="btn btn-sm btn-default delete"><span class="glyphicon glyphicon-trash"></span></button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="listbox clearfix" style="margin: 0px -7.5px;">
    </div>
    <div class="pagebox clearfix" style="text-align: center;">
    </div>
</div>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");
    var loadbox = context.closest(".loadbox");
    var findbox = context.find(".findbox");
    var filtbox = context.find(".filtbox");
    var statbox = context.find(".statbox");

    // 权限控制
    if (!hsChkUri("centre")) context.find(".owner").remove();
    if (!hsChkUri("<%=_module%>/<%=_entity%>/create.act")) context.find(".create").remove();

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete',
              '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
        ],
        openUrls: [
            [ '<%=_module%>/<%=_entity%>/form_adds.html?'+$.param(hsSerialArr(loadbox)),
              '.create', '@' ],
            [ '<%=_module%>/<%=_entity%>/form.html?<%=Cnst.ID_KEY%>={ID}',
              '.update', '@' ],
            [ '<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
              '.review', '@' ]
        ],
        fillList    : hsListFillItem,
        fillPage    : hsListFillMore,
        _fill_logo  : hsListFillLogo,
        _fill_edit  : hsListShowEdit
    });

    var filtobj = filtbox.hsForm({
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=!enum",
        _fill__enum : hsListFillFilt,
        _fill__sort : hsListInitSort
    });

    var statobj = context.hsCate({
        surl: "<%=_module%>/<%=_entity%>/statis/search.act?<%=Cnst.AB_KEY%>=_text",
        curl: "<%=_module%>/<%=_entity%>/counts/search.act?<%=Cnst.AB_KEY%>=_text,_fork"
    });

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

    // 我创建的
    filtbox.find(".owner").change(function() {
        $(this).closest (".form-control-static")
               .siblings(":submit").click( );
    }).val ( HsUSER.uid );

    /**
     * 因详情页可能被分享
     * 故为其分配特定 URL
     * History 处理较麻烦
     * 暂不支持返回和前进
     */
    context.on("openBack", ".review", function(ev, box, req) {
            location.replace(location.pathname +"#"+ req.id);
        box.on("hsClose" , function() {
            location.replace(location.pathname +"#");
        });
    });
    var mat= /^#(\w+)$/.exec(location.hash);
    if (mat) {
        var url = "<%=_module%>/<%=_entity%>/info.html";
        var box = context.hsFind("@");
        var btn = context.hsFind(".review").first( );
        listobj.open(btn, box, url, { id: mat[1] } );
    }

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        if (window["<%=_funcId%>"]) {
            window["<%=_funcId%>"](context, listobj);
        }

        // 加载数据
        listobj.load(hsSetPms(listobj._url, loadbox), findbox);
    });
})(jQuery);
</script>