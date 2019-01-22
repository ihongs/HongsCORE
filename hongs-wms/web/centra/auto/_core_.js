/* global echarts, jQuery, HsForm, HsList, hsListFillFork */

/**
 * 设置当前用户ID
 */
if (hsChkUri('centre')) {
    window.HsCUID = H$('%HsCUID');
} else {
    window.HsCUID = null ;
    H$ ( '%HsCUID', null);
}

/**
 * 获取当前模块对象
 */
function S$() {
    var context = $(".HsList,.HsForm").filter(":visible:first");
    return context.data("HsList")
        || context.data("HsForm");
}
S$.src  = function() {
    return (S$()._url || location.pathname)
        .replace(/\?.*/, '')
        .replace(/\/[^\/]+$/, '');
};
S$.send = function(url, req) {
    var rzt;
    $.hsAjax({
        url     : hsFixUri   (url),
        data    : hsSerialArr(req),
        success : function   (rst) {
            rzt = hsResponse (rst);
        },
        type    : "post",
        dataType: "json",
        async   : false ,
        cache   : false ,
        global  : false
    });
    return  rzt ;
};
S$.search = function(req) {
    var url = S$.src() + "/search.act";
    return S$.send(url, req);
};
S$.create = function(req) {
    var url = S$.src() + "/create.act";
    return S$.send(url, req);
};
S$.update = function(req) {
    var url = S$.src() + "/update.act";
    return S$.send(url, req);
};
S$.delete = function(req) {
    var url = S$.src() + "/delete.act";
    return S$.send(url, req);
};

/**
 * 筛选列表填充数据
 */
function hsListFillSele(x, v, n) {
    hsListFillFork.call(this, x, v , n );
        x.find("input")
         .attr("title", this._info.name)
         .data(         this._info     );
}

/**
 * 列表填充过滤选项
 */
function hsListFillFilt(x, v, n) {
    n = n.replace(/^ar\.\d\./, "");
    v = this._info[n];
    return v;
}

/**
 * 列表预置过滤选项
 */
function hsListPrepFilt(x, v, n) {
    n = n.replace(/^ar\.\d\./, "");
    v = this._enum[n];

    /**
     * 列表的初始条件是空的
     * 如果选项没有设置空值
     * 需要补充一个空的选项
     */
    var vk = x.attr("data-vk") || 0;
    var tk = x.attr("data-tk") || 1;
    var ek = true;
    for(var i = 0; i < v.length; i ++) {
        var k = hsGetValue(v[i], vk  );
        if (k == '') {
            ek = false ; break ;
        }
    }
    if (ek) {
        var a = $.isArray(v) ? [] : {};
        a[vk] =  "" ;
        a[tk] =  "" ;
        v.unshift(a);
    }

    return HsForm.prototype._prep__select.call(this, x, v, n);
}

/**
 * 列表填充打开链接
 */
function hsListFillOpen(x, v, n, t) {
    if (!v || !v.length) {
        return ;
    }
    if (!$.isArray( v )) {
        v = [v];
    }
    x.addClass("dont-check"); // 点击链接不要选中行

    switch  (t) {
        case "email": n = "glyphicon glyphicon-envelope"; break;
        case "image": n = "glyphicon glyphicon-picture" ; break;
        case "video": n = "glyphicon glyphicon-play"    ; break;
        case "audio": n = "glyphicon glyphicon-play"    ; break;
        case "file" : n = "glyphicon glyphicon-file"    ; break;
        default     : n = "glyphicon glyphicon-link"    ; break;
    }

    if (v.length == 1) {
        var a = $('<a target="_blank" '+'><span class="'+n+'"></span></a>');
        a.attr("href", t === "email" ? "mailto:" + v[0] : v[0]);
        a.appendTo(x);
    } else {
        var a = $('<a href="javascript:;"><span class="'+n+'"></span></a>');
        var m = $('<div class="modal fade" role="dialog" tabindex="-1">'
                + '<div class="modal-dialog modal-sm " role="document">'
                + '<div class="modal-content">'
                + '<div class="modal-header ">'
                + '<button type="button" class="close" data-dismiss="modal">'
                + '<span >&times;</span></button>'
                + '<h4  class="modal-title"></h4>'
                + '</div>'
                + '<div class="modal-body">'
                + '</div></div></div></div>');
        var u = $('<ul></ul>');
        var l = $('<li></li>');
        a.appendTo(x);
        m.appendTo(x);
        for(var i = 0; i < v.length; i ++) {
            var txt, url;
            if (t === "email") {
                url = "mailto:"+ v[i];
                txt = v[i];
            } else {
                url = v[i];
                txt = v[i].replace(/[?#].*/, '')
                          .replace( /.*\// , '');
            }
            var b = $('<a target="_blank"></a>');
            b.attr("href", url).text(txt);
            u.append(l.clone().append(b));
        }
        u.appendTo(m.find(".modal-body"));
        u.css("font-family", "monospace");
        m.css("text-align" , "left" /**/);
        m.find("h4").text("单击链接打开:");
        a.click(function( ) {
            m.modal("show");
        });
    }
}

/**
 * 列表填充链接包装
 */
function hsListWrapOpen(t) {
    return function (x, v, n) {
         hsListFillOpen(x, v, n, t);
    };
}

/**
 * 列表统计筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsStat (context , opts) {
    context = jQuery( context  );
    context.data("HsStat", this);
    context.addClass( "HsStat" );

    this.surl = opts.surl;
    this.curl = opts.curl;
    this.context = context;
    this.statbox = context.find(".statbox");
    this.findbox = context.find(".findbox");

    var  statobj = this;
    var  statbox = this.statbox;
    var  findbox = this.findbox;
    var  toolbox = context.find(".toolbox");

    //** 条件改变时重载图表 **/

    statbox.data("changed", statbox.is(".invisible"));

    toolbox.on("saveBack", function() {
        if (statbox.is(".invisible")) {
            statbox.data("changed", true );
        } else {
            statbox.data("changed", false);
            setTimeout(function() {
                statobj.load();
            }, 1000);
        }
    });

    findbox.on( "submit" , function() {
        if (statbox.is(".invisible")) {
            statbox.data("changed", true );
        } else {
            statbox.data("changed", false);
            setTimeout(function() {
                statobj.load();
            }, 1000);
        }
    });

    statbox.on( "change" , ":checkbox", function() {
        if ($(this).is(".checkall2")) {
            $(this).closest(".checkbox").find(".checkone2").prop("checked", false);
        } else {
            $(this).closest(".checkbox").find(".checkall2").prop("checked", false);
        }
        findbox.find(":submit:first").click();
    });
}
HsStat.prototype = {
    load: function() {
        var that    = this;
        var statbox = this.statbox;
        hsRequires("static/addons/echarts/echarts.js", function() {
            statbox.find("[data-type=counts],[data-type=statis]")
                   .each(function() {
                var sta =  $(  this  );
                if(!sta.data("echart")) {
                    var box = sta.find(".chartbox");
                    var obj = echarts.init(box[0] );
                    sta.data("echart", obj);
                }
            });

            that.statis();
            that.counts();
        });
    },

    statis: function(rb) {
        var that = this;
        var url  = this.surl;
        var context = this.context;
        var statbox = this.statbox;
        var findbox = this.findbox;

        if ( ! rb ) {
            rb = [];
            statbox.find( "[data-type=statis]" )
                   .each(function() {
                rb.push($(this).attr("data-rb"));
            });
        }
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url : url + rb.join( "" ),
            data: findbox.serialize(),
            dataType: "json",
            cache  : true,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__count__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     that.setStatisCheck(n, d);
                     that.setStatisChart(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statbox.find("[name='"+k+"']" ).val(data[k]);
                }
            }
        });
    },

    counts: function(rb) {
        var that = this;
        var url  = this.curl;
        var context = this.context;
        var statbox = this.statbox;
        var findbox = this.findbox;

        if ( ! rb ) {
            rb = [];
            statbox.find( "[data-type=counts]" )
                   .each(function() {
                rb.push($(this).attr("data-rb"));
            });
        }
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url : url + rb.join( "" ),
            data: findbox.serialize(),
            dataType: "json",
            cache  : true,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__count__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     that.setCountsCheck(n, d);
                     that.setCountsChart(n, d);
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
    },

    setStatisCheck: function(box, data) {
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
                .attr("name" , name+":on.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setCountsCheck: function(box, data) {
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
                .attr("name" , name+":in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setCountsChart: function(box, data) {
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
    },

    setStatisChart: function(box, data) {
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
};

jQuery.fn.hsStat = function(opts) {
  return this._hsModule(HsStat, opts);
};
