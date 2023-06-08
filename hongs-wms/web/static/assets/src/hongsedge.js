/* global jQuery, Element, H$, HsCUID, HsForm, HsList, hsFormFillFork, hsListFillFork, echarts */

//** 资源扩展功能 **/

H$.uid  = function() {
    if (window.HsCUID === undefined) {
        if (hsGetAuth('centra', false)
        ||  hsGetAuth('centre', false)
        ||  hsGetAuth('public', false)) {
            window.HsCUID = H$('%HsCUID');
        } else {
            window.HsCUID = null ;
        }
    }
    return  window.HsCUID ;
};
H$.mod  = function() {
    var context = $(".HsList,.HsForm")
            .filter(":visible:first" );
    return context.data("HsList")
        || context.data("HsForm");
};
H$.src  = function() {
    return (H$.mod()._url || location.pathname)
    //  .replace(/\#.*/, '')
        .replace(/\?.*/, '')
        .replace(/\/[^\/]+$/, '');
};
H$.load = function(req) {
    var mod = H$.mod();
    mod.load(undefined, hsSerialMix(mod._data, req));
};
H$.send = function(url, req) {
    var rzt;
    $.hsAjax({
        url     :  url,
        data    :  req,
        type    : "post",
        dataType: "json",
        async   : false ,
        cache   : false ,
        global  : false ,
        complete: function  (rst) {
            rzt = hsResponse(rst, 3);
        }
    });
    return  rzt ;
};
H$["search"] = function(req) {
    var url = H$.src() + "/search.act";
    return H$.send(url, req);
};
H$["create"] = function(req) {
    var url = H$.src() + "/create.act";
    return H$.send(url, req);
};
H$["update"] = function(req) {
    var url = H$.src() + "/update.act";
    return H$.send(url, req);
};
H$["delete"] = function(req) {
    var url = H$.src() + "/delete.act";
    return H$.send(url, req);
};

//** 组件扩展功能 */

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

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this.murl  = opts.murl;
    this.curl  = opts.curl;
    this.eurl  = opts.eurl;

    var  that  = this;

    //** 条件改变时重载图表 **/

    statBox.data("changed", statBox.is(".invisible"));

    context.on("saveBack sendBack", function() {
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

        hsRequires("static/addons/echarts/echarts.min.js", function() {
            statBox.find(".stat-group")
                   .each(  function  () {
                var sta =  $(  this  );
                if(!sta.data("echart")) {
                    var box = sta.find(".chartbox");
                    var obj = echarts.init(box[0] );
                    sta.data("echart", obj);
                }
            });

            that.fill(statBox.find("[data-type=ecount]"));
            that.fill(statBox.find("[data-type=acount]"));
            that.fill(statBox.find("[data-type=amount]"));
        });
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;

        var rb  = [];
        var eb  = [];
        var ft  = itemBox.attr ("data-type");
        var url = ft==="amount"? this.murl
              : ( ft==="acount"? this.curl
              :                  this.eurl );

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).find(".checkbox").hide();
            $(this).find(".chartbox").hide();
            $(this).find(".alertbox").show()
             .children().text($(this).data("text")+" 统计中...");
        });

        // 统计字段
        url = hsSetParam(url, "rb.", rb) + eb.join("&");

        // 筛选数据
        var dat;
        var mod;
        do {
            mod = context.data("HsList");
            if (mod) {
                dat = mod._data;
                break;
            }

            mod = context.data("HsTree");
            if (mod) {
                dat = mod._data;
                break;
            }

            dat = findBox;
        }
        while (false);

        // 重建数据, 删除可能冲突的参数
        dat = hsSerialMix({}, dat);
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];

        $.hsAjax({
            url : url,
            data: dat,
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");

                    if (n.size() === 0) {
                        continue;
                    }
                    if (d.length === 0) {
                        continue;
                    }

                    n.find(".alertbox").hide();
                    n.find(".checkbox").show();
                    n.find(".chartbox").show();

                    if (ft === "amount") {
                        that.setAmountCheck(n , d);
                        that.setAmountChart(n , d);
                    } else {
                        that.setAcountCheck(n , d);
                        that.setAcountChart(n , d);
                    }

                    k = n.find(".checkone2").attr( "name" );
                    k = _hsGetDkeys(k) [ 0 ]; // 规避数组结尾
                    if (dat && dat [k] ) {
                        n.find(".checkone2").val ( dat[k] );
                    } else {
                        n.find(".checkall2").prop("checked", true);
                    }
                }

                itemBox.each(function() {
                    $(this).find(".alertbox:visible")
                     .children().text($(this).data("text")+" 无统计值!");
                });
            }
        });
    },

    copy: function(itemBox) {
        var listBox = itemBox.find(".checkbox");

        // 检查接口
        if (! $.hsCanCopy()) {
            $.hsWarn("浏览器无法拷贝\r\n请使用较新版的 Chrome/Firefox/Safari/Edge, 或某些双核/多核浏览器的极速模式.", "warning");
            return;
        }

        var fn  = itemBox.attr("data-name");
        var fx  = itemBox.attr("data-text");
        var ft  = itemBox.attr("data-type");
        var url = ft==="amount"? this.murl
              : ( ft==="acount"? this.curl
              :                  this.eurl);
        var rn  = hsGetParam(url, "rn", 20);

        // 复制提示
        var set = [{
            mode : "warn",
            glass: "alert-default",
            text : "为了降低网络延迟, 图表按从多到少排, 仅取前 "+rn+" 条; 这将会调取完整统计数据, 以便导入 Office/WPS 等应用中.",
            title: "正在获取完整统计数据, 请稍等...",
            topic: "已经取得完整统计数据, 请拷贝吧!",
            note : "拷贝成功, 去粘贴吧!"
        }, {
            glass: "btn-primary copies",
            label: "拷贝"
        }, {
            glass: "btn-default",
            label: "取消"
        }];
        var msk = $.hsMask.apply( $, set );
        var mok = function() {
            msk.removeClass ("alert-info")
               .addClass ("alert-success");
            msk.find(".alert-footer .btn")
               .prop( "disabled" , false );
            msk.find(".alert-title" /**/ )
               .text( set[0].topic );
            return msk;
        };
        msk.find(".alert-footer button").prop("disabled", true );
        msk.find(".alert-footer button.copies").click(function() {
            var div = listBox.children( "div" );
            var tab = /**/div.children("table");
            div.show(  );
            tab.hsCopy();
            div.hide(  );
            $.hsNote( set[1].note , "success" );
        });

        // 重复拷贝
        var div = listBox.children( "div" );
        if (div.size( )) {
            mok( );
            return;
        }

        var dat = this.findBox.serialize( );
        url = hsSetParam(url, "rb", fn ); // 单字段
        url = hsSetParam(url, "rn", "0"); // 不分页

        // 读取全部
        $.hsAjax({
            url : url,
            data: dat,
            dataType : "json",
            cache  : true,
            success: function(rst) {
                // 构建表格
                var div = $('<div></div>').appendTo(listBox);
                var tab = $('<table></table>').appendTo(div);
                var thd = $('<thead></thead>').appendTo(tab);
                var tbd = $('<tbody></tbody>').appendTo(tab);
                var tr  = $('<tr></tr>');
                var th  = $('<td></th>');
                var td  = $('<td></td>');
                div.attr("style", "height:1px; display:none; overflow:auto;");
                tab.attr("style", "margin:1px;")
                   .attr("class", "table table-bordered");

                // 表头
                var tr2 = tr.clone().appendTo ( thd );
                    th.clone().appendTo(tr2).text(fx);
                    th.clone().appendTo(tr2).text("数量");
                if (ft === "amount") {
                    th.clone().appendTo(tr2).text("求和");
                    th.clone().appendTo(tr2).text("最小");
                    th.clone().appendTo(tr2).text("最大");
                }

                // 内容
                if (rst.enfo && rst.enfo[fn]) {
                        var a = rst.enfo[fn];
                    for(var i = 0; i < a.length; i ++) {
                        var b = a [i];
                        if (b[1]===null) b[1] = '#'+b[0];
                        tr2 = tr.clone().appendTo( tbd );
                    for(var j = 1; j < b.length; j ++) {
                        var c = b [j];
                        td.clone().appendTo(tr2).text(c);
                    }}
                }

                mok ();
            }
        });
    },

    setAmountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "" )  continue;
            if (v[1] == null)  v[1] = "#"+ v[0] ;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ", "+ v[3] +")" );
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".rg.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "" )  continue;
            if (v[1] == null)  v[1] = "#"+ v[0] ;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountChart: function(box, data) {
        var that  = this;
        var chart = box.data("echart");
        var xData = [];
        var bData = [];
        var pData = [];
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
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
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
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
                        icon: 'M85.312 938.688H1024V1024H0V0h85.312v938.688zM256 341.312h85.312V768H256V341.312zM512 128h85.312v640H512V128z m256 213.312h85.312V768H768V341.312z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '柱状图',
                        onclick: function() {
                            box.data("plot", "bar");
                            chart.setOption(chart._barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M85.312 512a426.688 426.688 0 0 0 844.8 85.312H426.688V93.888A426.816 426.816 0 0 0 85.312 512zM512 0v512h512a512 512 0 1 1-512-512z m506.816 438.848H585.152V5.184a512.32 512.32 0 0 1 433.664 433.664z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '饼视图',
                        onclick: function() {
                            box.data("plot", "pie");
                            chart.setOption(chart._pieOpts);
                        }
                    },
                    myTab: {
                        show: true,
                        icon: 'M512 1024A512 512 0 1 1 512 0a512 512 0 0 1 0 1024z m0-85.312A426.688 426.688 0 1 0 512 85.312a426.688 426.688 0 0 0 0 853.376zM320 576a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '导出',
                        onclick: function() {
                            that.copy(box );
                        }
                    }
                }
            }
        };

        chart._pieOpts = pieOpts;
        chart._barOpts = barOpts;

        if (box.data("plot") == "pie") {
            $.extend( opts , pieOpts );
        } else {
            $.extend( opts , barOpts );
        }

        chart.resize();
        chart.setOption(opts);
    },

    setAmountChart: function(box, data) {
        var that  = this;
        var chart = box.data("echart");
        var xData = [];
        var bData1 = [];
        var bData2 = [];
        var pData1 = []; // Count
        var pData2 = []; // Sum
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
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
            }, {
                show: false
            }]
        };
        var barOpts = {
            series: [{
                data: bData1,
                type: "bar",
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
                } }
            }, {
                data: bData2,
                type: "bar",
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
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
                        icon: 'M85.312 938.688H1024V1024H0V0h85.312v938.688zM256 341.312h85.312V768H256V341.312zM512 128h85.312v640H512V128z m256 213.312h85.312V768H768V341.312z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '柱状图',
                        onclick: function() {
                            box.data("plot", "bar");
                            chart.setOption(chart._barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M85.312 512a426.688 426.688 0 0 0 844.8 85.312H426.688V93.888A426.816 426.816 0 0 0 85.312 512zM512 0v512h512a512 512 0 1 1-512-512z m506.816 438.848H585.152V5.184a512.32 512.32 0 0 1 433.664 433.664z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '饼视图',
                        onclick: function() {
                            box.data("plot", "pie");
                            chart.setOption(chart._pieOpts);
                        }
                    },
                    myTab: {
                        show: true,
                        icon: 'M512 1024A512 512 0 1 1 512 0a512 512 0 0 1 0 1024z m0-85.312A426.688 426.688 0 1 0 512 85.312a426.688 426.688 0 0 0 0 853.376zM320 576a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '导出',
                        onclick: function() {
                            that.copy(box );
                        }
                    }
                }
            }
        };

        chart._pieOpts = pieOpts;
        chart._barOpts = barOpts;

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
    return this.hsBind(HsStat, opts);
};

/**
 * 列表分类筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsCate(context, opts) {
    context = $(context);

    var  statBox = context.find(".statbox");
    var  findBox = context.find(".findbox");

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this.murl  = opts.murl;
    this.curl  = opts.curl;
    this.eurl  = opts.eurl;

    var  that  = this;

    //** 条件改变时重载图表 **/

    statBox.data("changed", statBox.is(".invisible"));

    context.on("saveBack sendBack", function() {
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
HsCate.prototype = {
    load: function() {
        var that    = this;
        var statBox = this.statBox;
        that.fill(statBox.find("[data-type=ecount]"));
        that.fill(statBox.find("[data-type=acount]"));
        that.fill(statBox.find("[data-type=amount]"));
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;

        var rb  = [];
        var eb  = [];
        var ft  = itemBox.attr ("data-type");
        var url = ft==="amount"? this.murl
              : ( ft==="acount"? this.curl
              :                  this.eurl );

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).empty(); // 清空待写入选项
        });

        // 统计字段
        url = hsSetParam(url, "rb.", rb) + eb.join("&");

        // 筛选数据
        var dat;
        var mod;
        do {
            mod = context.data("HsList");
            if (mod) {
                dat = mod._data;
                break;
            }

            mod = context.data("HsTree");
            if (mod) {
                dat = mod._data;
                break;
            }

            dat = findBox;
        }
        while (false);

        // 重建数据, 删除可能冲突的参数
        dat = hsSerialMix({}, dat);
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];

        $.hsAjax({
            url : url,
            data: dat,
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");

                    if (n.size() === 0 ) {
                        continue;
                    }
                    if (d.length === 0 ) {
                        continue;
                    }

                    if (ft === "amount") {
                        that.setAmountCheck(n , d);
                    } else {
                        that.setAcountCheck(n , d);
                    }

                    k = n.find(".checkone2").attr( "name" );
                    k = _hsGetDkeys(k) [ 0 ]; // 规避数组结尾
                    if (dat && dat[k] ) {
                        n.find(".checkone2").val ( dat[k] );
                    } else {
                        n.find(".checkall2").prop("checked", true);
                    }
                }

                // 隐藏空的统计块
                itemBox.filter(":empty").closest(".stat-group").hide();
            }
        });
    },

    setAmountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        if (data.length == 0) {
            box.closest(".form-group").hide();
            return;
        } else {
            box.closest(".form-group").show();
        }

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部");
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".rg.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        if (data.length == 0) {
            box.closest(".form-group").hide();
            return;
        } else {
            box.closest(".form-group").show();
        }

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部");
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            label = $('<label></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    }
};
jQuery.fn.hsCate = function(opts) {
    return this.hsBind(HsCate, opts);
};

/**
 * 简单页面路由处理
 * @param {jQuery|Element} context
 * @param {Object} urls
 * @return {HsPops}
 */
function HsPops (context, urls) {
    context = jQuery( context );

    // 反转映射
    var urlz  = { };
    jQuery.each(urls, function( k , v ) {
        urlz[v] = k ;
    });

    context.on("hsReady" , ".loadbox" , function () {
        if (!$(this).parent().parent().is(context)) {
            return;
        }

        var base, srch, hash;

        // 去掉路径前缀
        base = hsFixUri("" );
        href = hsFixUri($( this ).data( "href" ));
        if (href.length <= base.length
        ||  href.substr(0, base.length) !== base) {
            return;
        }
        href = href.substr(base.length);

        // 提取页面路径
        var p  = href.indexOf('?');
        if (p >= 0 ) {
            srch = href.substring(1+ p);
            href = href.substring(0, p);
        }

        hash = urlz[href] || '';
        if (hash) {
            hash  = '#' + hash ;
        if (srch) {
            hash += '&' + srch ;
        }} else {
            return;
        }

        history.replaceState({}, '', location.pathname + location.search + hash);
    });
    context.on("hsClose" , ".loadbox" , function () {
        if (!$(this).parent().parent().is(context)) {
            return;
        }

        history.replaceState({}, '', location.pathname + location.search);
    });

    // 打开内页
    var hash = /^#(\w+)(&.+)?/.exec(location.hash);
    if (hash) {
        var href = urls [hash[1]];
        if (href) {
            href = href+(hash[2] || '' );
            href = href.replace('&','?');
            var timo = function() {
                var tabs = context.data("tabs");
                if (tabs && tabs.size()) {
                    tabs.hsOpen(href /**/);
                } else {
                    setTimeout (timo, 500);
                }
            }
            timo(context);
        }
    }
}
jQuery.fn.hsPops = function(opts) {
    return this.hsBind(HsPops, opts);
};

/**
 * 交换占位节点对象
 * 用于动态隐藏字段
 * 规避隐藏后的校验
 * @param {jQuery|Element} sup
 * @param {jQuery|Element} sub
 * @returns {HsSwap}
 */
function HsVeil (sup, sub) {
    this.sup = jQuery(sup);
    this.sub = jQuery(sub);
    this.sup.data("sub", sub);
    this.sub.data("sup", sup);
}
HsVeil.prototype = {
    show: function() {
        if (this.sup.parent().size() === 0) {
            this.sup.insertBefore(this.sub);
            this.sub.detach();
        }
    },
    hide: function() {
        if (this.sub.parent().size() === 0) {
            this.sub.insertBefore(this.sup);
            this.sup.detach();
        }
    },
    toggle: function() {
        if (this.sup.parent().size() === 0) {
            this.show();
        } else {
            this.hide();
        }
    }
};
jQuery.fn.hsToggleInput = function(show) {
    $(this).each(function() {
        var inst = $(this).hsBind(HsVeil, function() {
            var sup = $(this);
            var sub = $(this).data('hsVeilNode');
            if (sub === undefined) {
            var nam = $(this).data('hsVeilName');
                // 默认为空值的 input hidden
                sub = $('<input class="form-ignored" type="hidden"/>');
                sub.attr ("name", nam
                    || sup.attr( "data-fn" )
                    || sup.attr( "name"    )
                    || sup.find("[data-fn]").attr("data-fn")
                    || sup.find("[name]"   ).attr("name"   )
                );
            }
            return  sub;
        });
        if (show === undefined) {
            inst.toggle();
        } else
        if (show) {
            inst.show();
        } else {
            inst.hide();
        }
    });
    return  this;
};
jQuery.fn.hsShowInput = function() {
    return  this. hsToggleInput (true);
};
jQuery.fn.hsHideInput = function() {
    return  this. hsToggleInput (null);
};
jQuery.fn.hsHideValue = function(data) {
    data = hsSerialArr(data);
    var hide = $('<input type="hidden" class="form-ignored"/>');
    this.empty( );
    for(var i = 0; i < data.length; i ++) {
        var item = data [i];
        var node = hide.clone();
        node.attr("name" , item.name );
        node.attr("value", item.value);
        node.appendTo(this);
    }
    return  this;
};
// 兼容旧版
jQuery.fn.hsVeil = function(hide) { this.hsToggleInput(hide !== undefined ? ! hide : hide) };
jQuery.fn.hsHide = jQuery.fn.hsHideValue;

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
    n = n.replace(/\.(\w\w)$/, "");
    v = this._info[n];
    return v;
}

/**
 * 列表预置过滤选项
 */
function hsListDollFilt(x, v, n) {
    n = n.replace(/^ar\.\d\./, "");
    n = n.replace(/\.(\w\w)$/, "");
    v = this._enfo[n];

    /**
     * 列表的初始条件是空的
     * 如果选项没有设置空值
     * 需要补充一个空的选项
     */
    var vk = x.attr("data-vk") || 0;
    var tk = x.attr("data-tk") || 1;
    var ek = x.find("[value='']").size() == 0;
    if (ek)
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

    return HsForm.prototype._doll__select.call(this, x, v, n);
}

/**
 * 填充打开链接方式
 * 当多个时显示列表
 */
function hsFillOpenLink(x, v, t) {
    if (!v || !v.length) {
        return ;
    }
    if (!$.isArray( v )) {
        v = [v];
    }

    var n ;
    switch  (t) {
        case "email": n = "bi-hi-email"; break;
        case "image": n = "bi-hi-image"; break;
        case "video": n = "bi-hi-video"; break;
        case "audio": n = "bi-hi-audio"; break;
        case "file" : n = "bi-hi-file" ; break;
        default     : n = "bi-hi-link" ; break;
    }

    var a = $('<a><span class="'+n+'"></span></a>');
    var e = $('<a target="_blank"></a>');
    var l = $('<li></li>');
    var u = $('<ul></ul>');
    for(var i = 0; i < v.length; i ++) {
        var txt, url;
        if (t === "email") {
            url = "mailto:"+v[i] ;
            txt = v[i];
        } else {
            url = hsFixUri (v[i]);
            txt = v[i].replace(/[?#].*/,'')
                      .replace( /.*\// ,'');
            txt = decodeURIComponent( txt );
        }
        u.append(l.clone().append(e.clone()
         .attr("href", url ).text( txt ) ));
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
         hsFillOpenLink(x, v, t);
    };
}
function hsFormWrapOpen(t) {
    return function (x, v) {
         hsFillOpenLink(x, v, t);
    };
}

/**
 * 点击打开显示全文
 * 适用列表多行文本
 */
function hsFillListMore(x, v) {
    if (! v) {
        return;
    }

    var s = v;
    var n = x.data("title" );
    var t = x.data( "type" );
    var d = $('<div></div>');
    var a = $('<a href="javascript:;"></ a>');

    if (! n) {
        n = x.closest("td").index( );
        n = x.closest("table")
          .find("thead th,thead td")
          .eq  (n)
          .text( );
    }

    // 截取文字, 并留存原始文本以便拷贝
    if (t ===  "html") {
        s = $('<div></div>').html(s).text();
        x.data("html", v);
    } else {
        x.data("text", v);
    }
        s = s.replace(/\s{2,}/g,' ');
        s = $.trim( s );
    if (s.length > 100) {
        s = s.substr(0, 100) + "...";
    }

    // 填充表格, 设置提示
    a.appendTo(d).text(  /***/   s );
    d.appendTo(x).attr( "title", s );
    d.css({
        "overflow"      : "hidden",
        "text-overflow" : "ellipsis",
        "white-space"   : "nowrap",
        "width"         : "100%"
    });

    // 查看全文
    a.click(function () {
        if (t === "html") {
            $.hsMask({
                html  : v,
                title : n,
                space : "pre-wrap"
            });
        } else {
            $.hsMask({
                text  : v,
                title : n,
                space : "pre-wrap"
            });
        }
    });
}

//** 后台标准功能 **/

/**
 * 跨页选择所有的行
 */
function hsPickListMore(box, obj) {
    if (! obj) obj = box.closest(".HsList").data("HsList");

    $.hsWarn(
        "确定跨页全选吗?\r\n当数据量较大时, 可能需要些时间, 期间无法执行其他操作.",
        function() {
            setTimeout (function() {
                var bar = $.hsWait("选择中, 请稍等...");
                hsListPickMore(obj, -1, -1, function(pn, tn) {
                    if (pn !== tn) {
                        bar.prog(pn / tn, tn <= 0 ? "已选 "+pn+"页" : "已选 "+pn+"页, 共 "+tn+"页");
                    } else {
                        bar.hide();
                        box.find(":checkbox.checkone").prop("checked", true);
                        box.find(":checkbox.checkall").prop("checked", true);
                    }
                });
            } , 0);
        },
        function() {
        }
    );
}

/**
 * 存取列表隐藏的列
 */
function hsSaveListCols(box, id) {
    var tbl = box.is("table")?box:box.find("table:first");
    var ths = tbl.children("thead").children().children();
    var fs  = H$("%list-cols-"+ id);

    // 读取
    if (fs) {
        fs = fs.split(",");
        for(var i = 0; i < fs.length; i ++) {
            var f = fs[i] ;
            if (f) {
                ths.filter("[data-fn='"+f+"']")
                   .not   (".dont-hide")
                   .addClass( "hidden" );
            }
        }
    }

    // 存储
    box.on("hideCols", function() {
        fs = [];
        ths.filter(".hidden")
           .not(".dont-hide")
           .each( function( ) {
             fs.push ($(this).data("fn"));
        });
        fs = fs.join (",");
        if (!fs) fs = null;
        H$ ("%list-cols-" + id, fs);
    });
}

/**
 * 设置列表隐藏的列
 */
function hsHideListCols(box) {
    var tbl = box.is("table")?box:box.find("table:first");
    var ths = tbl.children("thead").children().children();
    var chs = $('<form class="checkbox" onsubmit="return false"></form>');
    var cho = $('<label><input type="checkbox" name="rb."><span></span></label>');

    ths.each(function() {
        if ($(this).is(".dont-hide,._admin,._check,._radio,._rowid")) {
            return;
        }
        var ch = cho.clone().appendTo(chs);
        ch.find("span" ).text( $(this).text()||' ');
        ch.find("input").val ( $(this).data("fn") );
        ch.find("input").prop("checked", !$(this).is(".hidden"));
    });

    $.hsMask({
        title: "请选择要显示的列"
    }, {
        label: "确定",
        glass: "btn-primary",
        click: function() {
            var trs = tbl.children("thead,tbody,tfoot").children();
            chs.find("input").each(function() {
                var ck = $(this).prop("checked");
                var fn = $(this).val (/*field*/);
                var fi = ths.filter('[data-fn="'+fn+'"]').index( );
                trs.each(function() {
                    $(this).children().eq(fi).toggleClass("hidden", ! ck );
                });
            });
            box.trigger("hideCols");
        }
    }, {
        label: "关闭",
        glass: "btn-default"
    }, {
        label: "全选",
        glass: "btn-link pull-left",
        click: function(evt) {
            chs.find(":checkbox").prop("checked", true );
            evt.preventDefault( );
            return false;
        }
    }, {
        label: "全否",
        glass: "btn-link pull-left",
        click: function(evt) {
            chs.find(":checkbox").prop("checked", false);
            evt.preventDefault( );
            return false;
        }
    }).find(".modal-body")
      .append(chs);
}

/**
 * 拷贝列表中的内容
 * 后可跟对话框参数, 参数同  $.hsMask
 * 如首参为数据对象, 结构为: {cols: [{label: "标签", fn: "字段", ft: "类型", fl: "填充方法"}], list: [{col1: "", col2: ""}]}
 */
function hsCopyListData(box) {
    // 检查接口
    if (! $.hsCanCopy()) {
        $.hsWarn("浏览器无法拷贝\r\n请使用较新版的 Chrome/Firefox/Safari/Edge, 或某些双核/多核浏览器的极速模式.", "warning");
        return;
    }

    // 复制提示
    var set ;
    var def = [{
        mode : "warn",
        glass: "alert-default",
        text : "这将整理原始数据, 清理表格中的内容, 提取完整的日期/时间, 补全图片/链接等网址, 以便导入 Office/WPS 等应用中.",
        title: "正在组织列表原始数据, 请稍等...",
        topic: "已经备好列表原始数据, 请拷贝吧!",
        note : "拷贝成功, 去粘贴吧!"
    }, {
        glass: "btn-primary copies",
        label: "拷贝"
    }, {
        glass: "btn-default",
        label: "取消"
    }];
    if (arguments.length > 1) {
        set = [].slice.call(arguments, 1);
        set[0] = $.extend(def[0], set[0]);
    } else {
        set = def;
    }
    var msk = $.hsMask.apply( $, set );
    var mok = function() {
        msk.removeClass ("alert-info")
           .addClass ("alert-success");
        msk.find(".alert-footer .btn")
           .prop( "disabled" , false );
        msk.find(".alert-title" /**/ )
           .text( set[0].topic );
        return msk;
    };
    msk.find(".alert-footer button").prop("disabled", true );
    msk.find(".alert-footer button.copies").click(function() {
        msk.prev().show().children().hsCopy();
        $.hsNote( set[0].note , "success" );
    });

    // 复制表格
    var div = $('<div></div>').insertBefore(msk);
    var tab = $('<table></table>').appendTo(div);
    var thd = $('<thead></thead>').appendTo(tab);
    var tbd = $('<tbody></tbody>').appendTo(tab);
    var tr  = $('<tr></tr>');
    var th  = $('<th></th>');
    var td  = $('<td></td>');
    div.attr("style", "height: 1px; display: none; overflow: auto;");
    tab.attr("style", "margin: 1px; white-space: pre-line;");
    tab.attr("class", "table table-bordered table-copylist");

    if (box instanceof jQuery || box instanceof Element) {
        box = $(box);

        // 表头
        box.find("thead:first tr").each(function() {
            var tr2 = tr.clone().appendTo(thd);
            $(this).find("th").each(function() {
                var th0 = $(this);
                if (th0.is(".dont-copy,._check,._admin,:hidden")) {
                    return;
                }
                var th2 = th.clone().appendTo(tr2);
                th2.text(th0.text());
            });
        });

        // 内容
        box.find("tbody:first tr").each(function() {
            if (!$(this).is(".active")) return;
            var tr2 = tr.clone().appendTo(tbd);
            $(this).find("td").each(function() {
                var td0 = $ (this);
                if (td0.is(".dont-copy,._check,._admin,:hidden")) {
                    return;
                }
                var td2 = td.clone().appendTo(tr2);
                if (td0.data("node" )) {
                    td2.html(hsTidyHtmlTags(td0.data("node")));
                } else
                if (td0.data("html" )) {
                    td2.html(hsTidyHtmlTags(td0.data("html")));
                } else
                if (td0.data("text" )) {
                    td2.text(td0.data("text" ));
                } else
                if (td0.attr("title")) {
                    td2.text(td0.attr("title"));
                } else
                {
                    td2.html(hsTidyHtmlTags(td0.html()));
                }
            });
        });

        /**
         * 规避将非数字类型的数字文本解析为数字
         * mso-number-format: '\@' 仅在 IE 有效
         * vnd.ms-excel.numberformat: '@' 非 IE 有效
         */
        tbd.find("td").not(".numerial")
           .attr("style", "mso-number-format:'\\@';vnd.ms-excel.numberformat:'@';");
    } else {
        /**
         * 拷贝原始数据
         * 以便导出表格
         */
        var cols = box.cols || [];
        var list = box.list || [];
        var mod  = new HsList({
            context : div
        });
        mod.listBox = tab;

        // 表头
            var tr2 = tr.clone().appendTo(thd);
        for(var i = 1; i < cols.length; i ++ ) {
            var th2 = th.clone().appendTo(tr2);
            var col = cols[i];
            if (typeof col === "string"
            ||  typeof col === "number") {
                col = { fn : col };
            }
            th2.attr("class", col.glass);
            th2.attr("style", col.style);
            th2.text( col. label || "" );
            th2.data( col  );
        }

        // 内容
        mod.fillList( list );

        /**
         * 规避将非数字类型的数字文本解析为数字
         * mso-number-format: '\@' 仅在 IE 有效
         * vnd.ms-excel.numberformat: '@' 非 IE 有效
         */
        tbd.find("td").not(".numerial")
           .attr("style", "mso-number-format:'\\@';vnd.ms-excel.numberformat:'@';");
    }

    // 就绪
    return mok();
}

/**
 * 清理内容中的标签
 * 以供表格拷贝中用
 */
function hsTidyHtmlTags(htm) {
    if (!htm) return "";
    if (typeof htm === "number") {
        return htm + "";
    }
    if (typeof htm === "object") {
        htm = $(htm).html();
    }
    htm = htm.replace(/\s+/gm, " "); // 合并空字符
    htm = htm.replace(/<\/?(p|br|hr|ul|ol|li|div|pre)(\/|\s.*?)?>/igm, "\r\n" ); // 块转为换行
    return    $.trim( htm );
}

/**
 * 表单带备注的保存
 */
function hsSaveWithMemo(msg) {
    return function( ) {
    var args = arguments;
    var data = args[ 1 ];
    var that = this;
    var memo = jQuery('<input type="text" name="memo" class="form-control" placeholder="选填, 请输入操作备注, 如: 补充简介信息"/>');
    var funx = function() {
        delete that._waiting;
    } ;
    var func = function() {
        data.append("memo",memo.val());
      HsForm.prototype.save.apply(that, args);
    } ;
    if (msg) {
        this.warn(msg, "default", func, funx)
            .find(".alert-body").append(memo)
            .trigger("shown.bs.modal");
    } else {
        func();
    }
    } ;
}

/**
 * 列表带备注的发送
 */
function hsSendWithMemo(btn, msg, url, data) {
    var that = this;
    var memo = jQuery('<input type="text" name="memo" class="form-control" placeholder="选填, 请输入操作备注, 如: 清理重复数据"/>');
    var func = function() {
    var dat1 = jQuery.extend({}, hsSerialDat(data), {memo:memo.val()});
    var dat2 = jQuery.extend({}, hsSerialDat(url ), hsSerialDat(data));
    that.ajax({
        "url"      : url ,
        "data"     : dat1,
        "type"     : "POST",
        "dataType" : "json",
        "funcName" : "send",
        "cache"    : false ,
        "context"  : that,
        "trigger"  : btn ,
        "success"  : function(rst) {
            if (this.saveBack) {
                this.saveBack(rst);
            } else {
                this.sendBack(btn, rst, dat2);
            }
        }
    });
    } ;
    if (msg) {
        this.warn(msg, "warning", func, null)
            .find(".alert-body").append(memo)
            .trigger("shown.bs.modal");
    } else {
        func();
    }
}

/**
 * 打开操作之后重载
 */
function hsOpenThenLoad(btn, box, url, data) {
    var formbox = this.formBox;
    var loadbox = this.loadBox;
    loadbox.hsFind(box).hsOpen(url, data, function() {
        var box = jQuery(this);
        box.on("saveBack", function( evt, rst, obj ) {
            // 继续外传事件
            formbox.trigger(evt, [rst,obj]);
            if (evt.isDefaultPrevented( ) ) {
                return;
            }
            // 重载当前板块
            var url  = loadbox.data("href");
            var data = loadbox.data("data");
            loadbox.hsLoad(url,data);
        });
    });
}

/**
 * 列表高级搜索支持
 */
function hsLoadWithWord(url, data) {
    var word = this.context.find(".findbox [name=wd]").val();
    if (data && word && /^\?.+=/.test(word)) {
        word = hsSerialDic(word);
        data = hsSerialDic(data);
               delete data["wd"];
        data = hsSerialMix(data, word);
    }
    HsList.prototype.load.call(this, url, data);
}

//** 前台标准功能 **/

/**
 * 列表填充卡片图标
 */
function hsListFillLogo(d, v) {
    if (!v) return ;
    d.css ( {
        "background-size"    : "cover",
        "background-repeat"  : "no-repeat",
        "background-position": "center center",
        "background-image"   : "url(" + v + ")"
    });
}

/**
 * 依据权限开放编辑
 */
function hsListShowEdit(d, v) {
    if (v) {
        if (v.uid !== undefined) {
            v = v.uid;
        } else
        if (v. id !== undefined) {
            v = v. id;
        }
    }
    if (v && v == H$.uid()) {
        d.show ();
    }
}

/**
 * 列表属主权限控制
 */
function hsListInitMine(x, v, n) {
    var uid = H$.uid();
    x.next().on( "change", ":radio", function() {
        switch ($(this).val()) {
            case "1": x.attr("name", n+".eq").val(uid); break;
            case "2": x.attr("name", n+".ne").val(uid); break;
            default : x.attr("name", n).val(""); break;
        }

        // 为规避多余的查询项, 没有设选项字段名称, 需自行取消其他选项
        $(this).closest("label").siblings().find("input").prop("checked", false);
    });
}

/**
 * 列表预设排序选项
 */
function hsListInitSort(x, v, n) {
    var inp = x;
    var sel = x.next().find("select").eq(0);
    var chk = x.next().find("select").eq(1);

    chk.addClass ("invisible");
    x.next().change(function() {
        if (sel.find("option").first().prop("selected") /**/) {
            chk.find("option").first().prop("selected", true);
            chk.toggleClass("invisible", true );
        } else {
            chk.toggleClass("invisible", false);
        }
        if (chk.val() === "-") {
            inp.val(chk.val() + sel.val());
        } else {
            inp.val(sel.val() + chk.val());
        }
    });

    /*
    if (sel.find("[value=mtime]").size()) {
        chk.prop("checked", true);
        sel.val ("mtime" );
        inp.val ("mtime!");
    } else
    if (sel.find("[value=ctime]").size()) {
        chk.prop("checked", true);
        sel.val ("ctime" );
        inp.val ("ctime!");
    }
    */
}

//** 其他 **/

/**
 * 自适滚动(兼容旧版)
 * @see $.fn.hsRoll
 */
function hsFlexRoll(part, body, setHeight, minHeight) {
    return $(part).hsRoll(body, setHeight, minHeight);
}

/**
 * 暗黑模式
 * @param mode 0:系统, 1:开启, 2:定时, 3:关闭
 * @param time 开启时间范围, 格式为: HHmmHHmm
 */
function hsDarkMode(mode, time) {
    var root = $(document.documentElement);
    if (window._HsDarkTime) {
        clearInterval (window._HsDarkTime);
    }

    switch (mode) {
        case "2" :
            if (! time || ! /^\d{8}$/.test(time)) {
                throw new Error("hsDarkMode: Wrong dark time format: " + time);
            }

            root.removeClass("dusk-mode");

            var b = time.substring(0 , 4);
            var e = time.substring(4 , 8);
            var c = b > e
                ? function (t) {
                    return (t >= b || t < e);
                }
                : function (t) {
                    return (t >= b && t < e);
                };

            function change( ) {
                var d = new  Date  ( );
                var h = d.getHours ( );
                var m = d.getMinutes();
                if (c ((h * 100) + (m < 10 ? m * 10 : m)) ) {
                    root.   addClass("dark-mode");
                } else {
                    root.removeClass("dark-mode");
                }
            }

            change( );

            // 模式定时器, 每分钟检测
            window._HsDarkTime = setInterval(change, 60000);

            break;
        case "1" :
            root.removeClass("dusk-mode")
                   .addClass("dark-mode");
            break;
        case "0" :
            root.removeClass("dark-mode")
                   .addClass("dusk-mode");
            break;
        default  :
            root.removeClass("dark-mode")
                .removeClass("dusk-mode");
            break;
    }
}

(function($) {

/**
 * 自动启用暗黑模式
 */
if (!$(document.documentElement).hasClass( "deny-dark" )) {
    hsDarkMode(H$("%HsDarkMode")||"0", H$("%HsDarkTime")||"18000600");
}

/**
 * 筛选重置事件处理
 */
$(document).on("reset", ".HsList .findbox", function() {
    var findbox = $(this);
    findbox.find("[data-ft=_fork]"). each ( function() {
        hsFormFillFork($(this), {});
    });
    findbox.find("[data-fn].repeated.labelbox .label"  ).remove();
    findbox.find("[data-fn].bootstrap-tagsinput .label").remove();
    setTimeout(function() {
        findbox.find(":submit").first().click();
    } , 500);
});

/**
 * 关闭按钮事件处理
 */
$(document).on("click", ".cancel,.recant" , function() {
    var b0  = $(this).closest(".dont-close")
                     .hsFind ("@");
    var b1  = $(this).hsFind ("@");
    if (b0 != b1) {
        b1.hsClose( );
    }
});

/**
 * 新开页导航滚到底
 */
$(document).on("hsReady", "#main-context>.laps.labs>*>.openbox", function(ev) {
    if ( ev.target != this ) return ;
    var nav = $("#main-context>.laps.tabs");
    var end = nav.prop  (  "scrollWidth"  );
    nav.scrollLeft(end || 0);
});

/**
 * 返回键联动导航条
 */
$(window).on("popstate", function(ev) {
    var ov  = ev.originalEvent;
    if (! ov || ! ov.state || ! ov.state.crumb) return;
    var li  = $("#main-context>.laps.tabs>li.active" );
    if (! li.is(".home-crumb")) {
        li.hsClose( );
    }
    history.pushState({crumb: true}, null, null);
});
history.pushState({crumb: true}, null, null);
history.pushState({crumb: true}, null, null);

})(jQuery);
