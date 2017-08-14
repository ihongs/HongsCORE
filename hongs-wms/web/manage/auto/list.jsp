<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Set"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_init_more_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "list");
    String _pageId = (_module +"-" + _entity +"-" + _action).replace('/', '-');
    String _confId = FormSet.hasConfFile(_module+"/"+_entity)
                  || NaviMap.hasConfFile(_module+"/"+_entity)
                   ? _module +"/"+ _entity : _module;
%>
<h2><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="row">
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
            <%} // End If %>
        </div>
        <form class="findbox col-md-4 input-group" action="" method="POST">
            <input type="search" name="wd" class="form-control input-search"/>
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
    <!-- 筛选 -->
    <form class="findbox filtbox invisible row bg-info" style="margin-left: 0; margin-right: 0">
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
            || !Synt.declare(info.get("siftable"), false)) {
                continue;
            }
        %>
        <div class="form-group form-group-sm row">
            <label class="col-sm-3 form-control-static control-label text-right" style="color:#eee;"><%=text%></label>
            <div class="col-sm-6">
            <%if ("enum".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                <select class="form-control" name="ar.0.<%=name%>" data-ft="_enum"></select>
            <%} else if ("number".equals(type) || "range".equals(type)) {%>
                <div class="input-group">
                    <input type="number"    class="form-control" name="ar.0.<%=name%>!ge"/>
                    <span class="input-group-addon input-sm">~</span>
                    <input type="number"    class="form-control" name="ar.0.<%=name%>!le"/>
                </div>
            <%} else if ("date".equals(type) || "time" .equals(type) || "datetime" .equals(type)) {%>
                <div class="input-group">
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>!ge"/>
                    <span class="input-group-addon input-sm">~</span>
                    <input type="<%=type%>" class="form-control" name="ar.0.<%=name%>!le"/>
                </div>
            <%} else {%>
                <input type="text" class="form-control" name="ar.0.<%=name%>"/>
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
    <form class="findbox statbox invisible row bg-info" style="margin-left: 0; margin-right: 0">
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
                String enumConf = Synt.defxult((String) info.get("conf"),_confId);
                String enumName = Synt.defxult((String) info.get("enum"), name  );
                Map    enumData ;
                try {
                    enumData  = FormSet.getInstance(enumConf ).getEnum(enumName );
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
                %>
                <%if ("number".equals(type) || "range".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> text-right"><%=text%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_htime" <%=ob%> class="<%=oc%> datetime"><%=text%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date"  <%=ob%> class="<%=oc%> date"><%=text%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time"  <%=ob%> class="<%=oc%> time"><%=text%></th>
                <%} else if ("file".equals(type) ||  "url".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_ulink" <%=ob%> class="<%=oc%> link"><%=text%></th>
                <%} else if ("enum".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                    <th data-fn="<%=name%>_text" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        if (info.get("data-ak") != null) name  = (String) info.get("data-ak");
                        if (info.get("data-tk") != null) name +=   "."  + info.get("data-tk");
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
<script type="text/javascript" src="static/addons/echarts/echarts.js"></script>
<script type="text/javascript">
(function($) {
    var context = $("#<%=_pageId%>").removeAttr("id");
    var toolbox = context.find(".toolbox");
    var formbox = context.find(".findbox");
    var findbox = formbox.eq(0);
    var filtbox = context.find(".filtbox");
    var statbox = context.find(".statbox");

    //** 列表、搜索表单 **/

    context.hsList({
        loadUrl : "<%=_module%>/<%=_entity%>/search.act?md=6",
        sendUrls: [
            ['<%=_module%>/<%=_entity%>/delete.act',
             '.delete',
             '<%=_locale.translate("fore.delete.confirm", _title)%>']
        ],
        openUrls: [
            ['<%=_module%>/<%=_entity%>/form.html?md=0',
             '.create', '@'],
            ['<%=_module%>/<%=_entity%>/form_edit.html?md=1&id={ID}',
             '.update', '@'],
            ['<%=_module%>/<%=_entity%>/info.html?md=6&id={ID}',
             '.review', '@'],
            ['<%=_module%>/<%=_entity%>/list_logs.html?md=6&id={ID}',
             '.revert', '@']
        ],
        _fill__fork: hsListFillFork
    });

    filtbox.hsForm({
        loadUrl : "<%=_module%>/<%=_entity%>/search.act?md=0",
        fillInfo: function() { },
        _fill__enum: function(td, v, n, t) {
            if ("enum" != t) {
                return v;
            }
            n  = n.replace(/^ar\.0\./, "");
            return this._enum[n];
        }
    });

    context.find(".export").click(function() {
        var url = "<%=_module%>/<%=_entity%>/stream.act?";
        var req = formbox.serialize( );
        window.open(url+req, "_blank");
    });

    function statis() {
        var rb = [];
        statbox.find("[data-type=statis]").each(function() {
            rb.push($(this).attr("data-rb"));
        });
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url: "<%=_module%>/<%=_entity%>/statis/search.act?md=1"+rb.join(""),
            data: formbox.serialize(),
            dataType: "json",
            context: statbox,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__total__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     setStatisCheck(n, d);
                     setStatisChart(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statbox.find("[name='"+k+"']" ).val(data[k]);
                }
            }
        });
    }

    function counts() {
        var rb = [];
        statbox.find("[data-type=counts]").each(function() {
            rb.push($(this).attr("data-rb"));
        });
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url: "<%=_module%>/<%=_entity%>/counts/search.act?md=1"+rb.join(""),
            data: formbox.serialize(),
            dataType: "json",
            context: statbox,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__total__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     setCountsCheck(n, d);
                     setCountsChart(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statbox.find("[name='" + k + "']").val(data[k]);
                }
                if (statbox.find(":checked"  ).size( )   ==   0   ) {
                    statbox.find(".checkall2").prop("checked",true);
                }
            }
        });
    }

    function setStatisCheck(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find( ".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] == "" || v[2] == 0) continue;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ", "+ v[3] +")" );
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name+"!ir.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    }

    function setCountsCheck(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find( ".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] == "" || v[2] == 0) continue;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name+"!in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    }

    function setCountsChart(box, data) {
        var chart = box.data("echart");
        var xData = [];
        var bData = [];
        var pData = [];
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] == "" || v[2] == 0) continue;
            xData.push(v[1]);
            bData.push(v[2]);
            pData.push({
                value: v[2],
                name : v[1]
            });
        }

        var opts = {
            series: [{
                data: pData,
                type: 'pie'
            }],
            xAxis : [],
            yAxis : [],
            grid: {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myPie: {
                        show: true,
                        icon: 'M56.3,20.1 C52.1,9,40.5,0.6,26.8,2.1C12.6,3.7,1.6,16.2,2.1,30.6 M3.7,39.9c4.2,11.1,15.8,19.5,29.5,18 c14.2-1.6,25.2-14.1,24.7-28.5',
                        title: '饼视图',
                        onclick: function () {
                            chart.setOption({
                                series: [{
                                    data: pData,
                                    type: 'pie'
                                }],
                                xAxis : [{
                                    show: false
                                }],
                                yAxis : [{
                                    show: false
                                }]
                            });
                        }
                    },
                    myBar: {
                        show: true,
                        icon: 'M6.7,22.9h10V48h-10V22.9zM24.9,13h10v35h-10V13zM43.2,2h10v46h-10V2zM3.1,58h53.7',
                        title: '柱状图',
                        onclick: function () {
                            chart.setOption({
                                series: [{
                                    data: bData,
                                    type: 'bar'
                                }],
                                xAxis : [{
                                    data: xData,
                                    show: true,
                                    type: 'category'
                                }],
                                yAxis : [{
                                    show: true,
                                    type: "value"
                                }]
                            });
                        }
                    }
                }
            }
        };

        chart.resize();
        chart.setOption(opts);
    }

    function setStatisChart(box, data) {
        var chart = box.data("echart");
        var xData = [];
        var bData1 = [];
        var bData2 = [];
        var pData1 = []; // Count
        var pData2 = []; // Sum
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] == "" || v[2] == 0) continue;
            xData.push(v[1]);
            bData1.push(v[2]);
            bData2.push(v[3]);
            pData1.push({
                value: v[2],
                name : v[1]
            });
            pData2.push({
                value: v[3],
                name : v[1]
            });
        }

        var opts = {
            series: [{
                radius : [ 0, 50],
                data: pData1,
                type: 'pie'
            }, {
                radius : [60, 80],
                data: pData2,
                type: 'pie'
            }],
            xAxis : [],
            yAxis : [],
            grid: {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myPie: {
                        show: true,
                        icon: 'M56.3,20.1 C52.1,9,40.5,0.6,26.8,2.1C12.6,3.7,1.6,16.2,2.1,30.6 M3.7,39.9c4.2,11.1,15.8,19.5,29.5,18 c14.2-1.6,25.2-14.1,24.7-28.5',
                        title: '饼视图',
                        onclick: function () {
                            chart.setOption({
                                series: [{
                                    radius : [ 0, 50],
                                    data: pData1,
                                    type: 'pie'
                                }, {
                                    radius : [60, 80],
                                    data: pData2,
                                    type: 'pie'
                                }],
                                xAxis : [{
                                    show: false
                                }],
                                yAxis : [{
                                    show: false
                                }]
                            });
                        }
                    },
                    myBar: {
                        show: true,
                        icon: 'M6.7,22.9h10V48h-10V22.9zM24.9,13h10v35h-10V13zM43.2,2h10v46h-10V2zM3.1,58h53.7',
                        title: '柱状图',
                        onclick: function () {
                            chart.setOption({
                                series: [{
                                    data: bData1,
                                    type: 'bar'
                                }, {
                                    data: bData2,
                                    type: 'bar'
                                }],
                                yAxis : [{
                                    show: true,
                                    type: "value"
                                }, {
                                    show: true,
                                    type: "value",
                                    position: "right"
                                }],
                                xAxis : [{
                                    data: xData,
                                    show: true,
                                    type: 'category'
                                }]
                            });
                        }
                    }
                }
            }
        };

        chart.resize();
        chart.setOption(opts);
    }

    if (filtbox.find(".form-group").size() == 2) {
        findbox.find(".filter").hide( );
    }
    if (statbox.find( ".col-md-6" ).size() == 0) {
        findbox.find(".statis").hide( );
    }

    findbox.find(".filter").click(function() {
        filtbox.toggleClass("invisible");
        if (!filtbox.is("invisible")) {
            filtbox.trigger("opened");
        }
    });
    findbox.find(".statis").click(function() {
        statbox.toggleClass("invisible");
        if (!statbox.is("invisible")) {
            statbox.trigger("opened");
        }
    });
    findbox.find(".search").click(function() {
        filtbox.addClass("invisible");
    });
    filtbox.find(":submit").click(function() {
        filtbox.addClass("invisible");
    });
    filtbox.find(":reset" ).click(function() {
        setTimeout(function() {
            filtbox.find(":submit").click();
        }, 500);
    });

    toolbox.on("saveBack", function() {
        if (statbox.is(".invisible")) {
            statbox.data("changed", true );
        } else {
            statbox.data("changed", false);
            setTimeout(function() {
                statis();
                counts();
            }, 1000);
        }
    });
    formbox.on("submit", function(  ) {
        if (statbox.is(".invisible")) {
            statbox.data("changed", true );
        } else {
            statbox.data("changed", false);
            setTimeout(function() {
                statis();
                counts();
            }, 1000);
        }
    });
    statbox.on("opened", function( ) {
        if (statbox.data("changed")) {
            statbox.data("changed", false);
            statis();
            counts();
        }
    });
    statbox.on("change", ":checkbox", function() {
        if ($(this).is(".checkall2")) {
            $(this).closest(".checkbox").find(".checkone2").prop("checked", false);
        } else {
            $(this).closest(".checkbox").find(".checkall2").prop("checked", false);
        }
        findbox.find(":submit").click();
    });

    statbox.data("changed", true);
    statbox.find("[data-type=counts],[data-type=statis]").each(function() {
        var box = $(this).find(".chartbox")[0];
        var obj = echarts.init(box);
        $(this).data("echart", obj);
    });
})(jQuery);
</script>