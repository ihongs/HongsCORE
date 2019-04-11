/* global echarts, jQuery, HsForm, HsList, hsListFillFork */

/**
 * 设置当前用户ID
 */
if (hsChkUri('centra')) {
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
S$.browse = function(req) {
    var mod = S$();
    mod.load(undefined, hsSerialMix(mod._data, req));
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
 * 当多个时显示列表
 */
function hsListFillOpen(x, v, t) {
    if (!v || !v.length) {
        return ;
    }
    if (!$.isArray( v )) {
        v = [v];
    }

    var n ;
    switch  (t) {
        case "email": n = "glyphicon glyphicon-envelope"; break;
        case "image": n = "glyphicon glyphicon-picture" ; break;
        case "video": n = "glyphicon glyphicon-play"    ; break;
        case "audio": n = "glyphicon glyphicon-play"    ; break;
        case "file" : n = "glyphicon glyphicon-file"    ; break;
        default     : n = "glyphicon glyphicon-link"    ; break;
    }

    var a = $('<a><span class="'+n+'" ></span></a>');
    var k = $('<a target="_blank" ></a>');
    var b = $('<b class="'+n+'"></span>');
    var l = $('<li class="label label-info "></li>');
    var u = $('<ul class="labelbox repeated"></ul>');
    for(var i = 0; i < v.length; i ++) {
        var txt, url;
        if (t === "email") {
            url = "mailto:"+v[i] ;
            txt = v[i];
        } else {
            url = hsFixUri (v[i]);
            txt = v[i].replace(/[?#].*/, '')
                      .replace( /.*\// , '');
            txt = decodeURIComponent ( txt );
        }
        u.append(l.clone().append(b.clone())
         .append(k.clone().attr("href", url)
                          .text(  txt  ) ) );
    }
    x.addClass("dont-check"); // 点链接不选中
    x.data("node", u);        // 可被复制使用

    if (v.length > 1) {
        a.appendTo(x);
        a.attr("href","javascript:;");
        a.click(function() {
            var m = $.hsMask({"title": "点击可打开..."});
            m.find(".modal-body"  ).append(u);
            m.find(".modal-footer").remove( );
        });
    } else {
        a.appendTo(x);
        a.attr( "target" , "_blank" );
        if (t === "email") {
            a.attr("href", "mailto:"+v[0] );
        } else {
            a.attr("href", hsFixUri (v[0]));
        }
    }
}
function hsListWrapOpen(t) {
    return function (x, v) {
         hsListFillOpen(x, v, t);
    };
}

/**
 * 复制列表中的内容
 */
function hsCopyListData(box) {
    // 检查接口
    if (! window.getSelection
    ||  ! document.createRange
    ||  ! document.execCommand ) {
        $.hsWarn("浏览器无法复制\r\n请使用较新版的 Chrome,Firefox 或以它们为核心的其他浏览器.", "warning");
        return;
    }

    // 复制表格
    var div = $('<div style="height:1px;overflow:auto;"></div>').insertBefore(box);
    var tab = $('<table></table>').appendTo(div);
    var tbd = $('<tbody></tbody>').appendTo(tab);
    var tr  = $('<tr></tr>');
    var th  = $('<td></th>');
    var td  = $('<td></td>');
    tab.attr("class", "table");
    box.find("thead:first tr").each(function() {
        var tr2 = tr.clone().appendTo(tbd);
        $(this).find("th").each(function() {
            var th0 = $(this);
            if (th0.is(".dont-copy,._check,._amenu")) {
                return;
            }
            var th2 = th.clone().appendTo(tr2);
            th2.text(th0.text());
        });
    });
    box.find("tbody:first tr").each(function() {
        var tr2 = tr.clone().appendTo(tbd);
        $(this).find("td").each(function() {
            var td0 = $ (this);
            if (td0.is(".dont-copy,._check,._amenu")) {
                return;
            }
            var td2 = td.clone().appendTo(tr2);
            if (td0.data("copy")) {
                td0.data("copy").call(this , td2[0]);
            } else
            if (td0.data("node")) {
                td2.append(td0.data("node"));
            } else
            if (td0.data("html")) {
                td2.html(td0.data("html"));
            } else
            if (td0.data("text")) {
                td2.text(td0.data("text"));
            } else
            if (td0.attr("title")) {
                td2.text(td0.attr("title"));
            } else
            {
                td2.text(td0.text());
            }
        });
    });

    // 复制内容
    var rng = document.createRange();
    var sel = window.getSelection( );
    sel.removeAllRanges();
    try {
        rng.selectNodeContents(tab[0]);
        sel.addRange(rng);
    } catch (e) {
        rng.selectNode /****/ (tab[0]);
        sel.addRange(rng);
    }
    document.execCommand("Copy");
    div.remove();

    $.hsNote("复制成功, 去粘贴吧!", "success");
}

/**
 * 列表带备注的发送
 */
function hsSendWithMemo(btn, msg, url, data) {
    var that = this;
    var memo = jQuery('<input type="text" name="memo" class="form-control" placeholder="请输入操作备注(选填)"/>');
    var func = function() {
    var dat1 = jQuery.extend({}, hsSerialDat(data), {memo:memo.val()});
    var dat2 = jQuery.extend({}, hsSerialDat(url ), hsSerialDat(data));
    that.ajax({
        "url"       : url ,
        "data"      : dat1,
        "type"      : "POST",
        "dataType"  : "json",
        "funcName"  : "send",
        "cache"     : false,
        "context"   : that,
        "trigger"   : btn ,
        "success"   : function(rst) {
            this.sendBack(btn, rst, dat2);
        }
    });
    } ;
    if (!msg) {
        func( );
    } else {
        this.warn(msg, "warning", func, null)
            .find(".alert-body").append(memo)
            .trigger("shown.bs.modal");
    }
}

/**
 * 列表统计筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsStat(context, opts) {
    context = $(context);

    var  statBox = context.find(".statbox");
    var  findBox = context.find(".findbox");
    var  toolBox = context.find(".toolbox");

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this.aurl  = opts.aurl;
    this.curl  = opts.curl;

    var  that  = this;

    //** 条件改变时重载图表 **/

    statBox.data("changed", statBox.is(".invisible"));

    toolBox.on("saveBack", function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    findBox.on( "submit" , function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    statBox.on( "change" , "input,select", function() {
        if ($(this).is(".checkall2")) {
            $(this).closest(".checkbox").find(".checkone2").prop("checked", false);
        } else
        if ($(this).is(".checkone2")) {
            $(this).closest(".checkbox").find(".checkall2").prop("checked", false);
        }
        findBox.find(":submit").first().click();
    });
}
HsStat.prototype = {
    load: function() {
        var that    = this;
        var statBox = this.statBox;
        hsRequires("static/addons/echarts/echarts.js", function() {
            statBox.find("[data-type=acount],[data-type=amount]")
                   .each(  function  () {
                var sta =  $(  this  );
                if(!sta.data("echart")) {
                    var box = sta.find(".chartbox");
                    var obj = echarts.init(box[0] );
                    sta.data("echart", obj);
                }
            });

            that.amount();
            that.acount();
        });
    },

    amount: function(rb) {
        var that = this;
        var url  = this.aurl;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;

        if ( ! rb ) {
            rb = [];
            statBox.find( "[data-type=amount]" )
                   .each(function() {
                rb.push($(this).attr("data-rb"));
            });
        }
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url : url + rb.join( "" ),
            data: findBox.serialize(),
            dataType: "json",
            cache  : true,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__count__") continue;
                     var d  = rst.info[k];
                     var n  = statBox.find("[data-name='"+k+"']");
                     that.setAmountCheck(n, d);
                     that.setAmountChart(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statBox.find("[name='"+k+"']" ).val(data [k]);
                }

                statBox.find(".checkbox").each(function() {
                    if ($(this).find(":checked"  ).size() === 0 ) {
                        $(this).find(".checkall2").prop("checked", true);
                    }
                });
            }
        });
    },

    acount: function(rb) {
        var that = this;
        var url  = this.curl;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;

        if ( ! rb ) {
            rb = [];
            statBox.find( "[data-type=acount]" )
                   .each(function() {
                rb.push($(this).attr("data-rb"));
            });
        }
        if (rb.length == 0) {
            return;
        }

        $.ajax({
            url : url + rb.join( "" ),
            data: findBox.serialize(),
            dataType: "json",
            cache  : true,
            success: function(rst) {
                for (var k in rst.info) {
                     if (k == "__count__") continue;
                     var d  = rst.info[k];
                     var n  = statBox.find("[data-name='"+k+"']");
                     that.setAcountCheck(n, d);
                     that.setAcountChart(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statBox.find("[name='"+k+"']" ).val(data [k]);
                }

                statBox.find(".checkbox").each(function() {
                    if ($(this).find(":checked"  ).size() === 0 ) {
                        $(this).find(".checkall2").prop("checked", true);
                    }
                });
            }
        });
    },

    setAmountCheck: function(box, data) {
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

    setAcountCheck: function(box, data) {
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

    setAcountChart: function(box, data) {
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

        var pieOpts = {
            series: [{
                data: pData,
                type: "pie"
            }],
            xAxis : [{
                show: false
            }],
            yAxis : [{
                show: false
            }]
        };
        var barOpts = {
            series: [{
                data: bData,
                type: "bar",
                barMaxWidth: 10,
                itemStyle: { normal: {
                    barBorderRadius: [5, 5, 0, 0]
                } }
            }],
            xAxis : [{
                data: xData,
                show: true,
                type: "category"
            }],
            yAxis : [{
                show: true,
                type: "value"
            }]
        };
        var opts = {
            grid : {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            tooltip: {},
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myBar: {
                        show: true,
                        icon: 'M6.7,22.9h10V48h-10V22.9zM24.9,13h10v35h-10V13zM43.2,2h10v46h-10V2zM3.1,58h53.7',
                        title: '柱状图',
                        onclick: function () {
                            chart.setOption(barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M56.3,20.1 C52.1,9,40.5,0.6,26.8,2.1C12.6,3.7,1.6,16.2,2.1,30.6 M3.7,39.9c4.2,11.1,15.8,19.5,29.5,18 c14.2-1.6,25.2-14.1,24.7-28.5',
                        title: '饼视图',
                        onclick: function () {
                            chart.setOption(pieOpts);
                        }
                    }
                }
            }
        };

        if (box.data("plot") == "pie") {
            $.extend( opts , pieOpts );
        } else {
            $.extend( opts , barOpts );
        }

        chart.resize();
        chart.setOption(opts);
    },

    setAmountChart: function(box, data) {
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

        var pieOpts = {
            series: [{
                radius : [ 0, 50],
                data: pData1,
                type: "pie"
            }, {
                radius : [60, 80],
                data: pData2,
                type: "pie"
            }],
            xAxis : [{
                show: false
            }],
            yAxis : [{
                show: false
            }]
        };
        var barOpts = {
            series: [{
                data: bData1,
                type: "bar",
                barMaxWidth: 10,
                itemStyle: { normal: {
                    barBorderRadius: [5, 5, 0, 0]
                } }
            }, {
                data: bData2,
                type: "bar",
                barMaxWidth: 10,
                itemStyle: { normal: {
                    barBorderRadius: [5, 5, 0, 0]
                } }
            }],
            xAxis : [{
                data: xData,
                show: true,
                type: "category"
            }],
            yAxis : [{
                show: true,
                type: "value"
            }, {
                show: true,
                type: "value",
                position: "right"
            }]
        };
        var opts = {
            grid : {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            tooltip: {},
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myBar: {
                        show: true,
                        icon: 'M6.7,22.9h10V48h-10V22.9zM24.9,13h10v35h-10V13zM43.2,2h10v46h-10V2zM3.1,58h53.7',
                        title: '柱状图',
                        onclick: function () {
                            chart.setOption(barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M56.3,20.1 C52.1,9,40.5,0.6,26.8,2.1C12.6,3.7,1.6,16.2,2.1,30.6 M3.7,39.9c4.2,11.1,15.8,19.5,29.5,18 c14.2-1.6,25.2-14.1,24.7-28.5',
                        title: '饼视图',
                        onclick: function () {
                            chart.setOption(pieOpts);
                        }
                    }
                }
            }
        };

        if (box.data("plot") == "pie") {
            $.extend( opts , pieOpts );
        } else {
            $.extend( opts , barOpts );
        }

        chart.resize();
        chart.setOption(opts);
    }
};

jQuery.fn.hsStat = function(opts) {
  return this._hsModule(HsStat, opts);
};
