/* global jQuery, Element, HsForm, HsList, echarts */

function HsSift(context, opts) {
    context = $(context);

    var  siftBox = context.find(".siftbox");
    var  findBox = context.find(".findbox");

    this.context = context;
    this.siftBox = siftBox;
    this.findBox = findBox;
    this._url  = opts._url || opts.loadUrl ;

    var  that  = this;

    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0,1 )
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        }
    }

    //** 事件处理 **/

    siftBox.data("fetched", siftBox.is(".invisible"));

    siftBox.on("change", "[data-sift=fn]", function() {
        var fno  = $(this);
        var fro  = siftBox.find("[data-sift=fr]");
        var name = fno.data("name");
        var kind = fno.data("kind");
        if (that["_feed_"+name]) {
            that["_feed_"+name].call(that, fno, fro);
        } else
        if (that["_feed_"+kind]) {
            that["_feed_"+kind].call(that, fno, fro);
        } else {
            throw new Error("HsSift: Can not find feed function for "+name+"("+kind+")");
        }
    });
    siftBox.on("click" , "[data-sift=fv]", function() {
        var btn  = $(this);
        var fno  = siftBox.find("[data-sift=fn]");
        var fro  = siftBox.find("[data-sift=fr]");
        if ("is" == fro.val()) { // 空值判断
            that["_fill__is"].call(that, fno, fro, btn);
            return;
        }
        var name = fno.data("name");
        var kind = fno.data("kind");
        if (that["_fill_"+name]) {
            that["_fill_"+name].call(that, fno, fro, btn);
        } else
        if (that["_fill_"+kind]) {
            that["_fill_"+kind].call(that, fno, fro, btn);
        } else {
            throw new Error("HsSift: Can not find fill function for "+name+"("+kind+")");
        }
    });
    siftBox.on("click" , "[data-sift=fs]", function() {
        that.addList(that.getList(), $(this).data("name"));
    });
}
HsSift.prototype = {
    load: function() {
        var that = this;
        $.hsAjax({
            url : this._url,
            type: "get",
            dataType: "json",
            cache   :  true ,
            async   :  true ,
            success : function(rst) {
                rst = hsResponse(rst);
                if (rst && rst.ok ) {
                    that.feed(rst.enfo || {});
                }
            }
        });
    },
    feed: function(enfo) {
        var that = this;
        this.siftBox.find("select[data-fn],datalist[data-fn]").each(function() {
            var x = $(this);
            var n = x.data("fn");
            var v = hsGetValue(enfo, n);
            if (v) {
                that._feed__datalist(x, v, n);
            }
        });
    },
    _feed__datalist: HsForm.prototype._feed__datalist,

    mask: function(title, input, back) {
        var that = this;
        var form = $(
            '<form onsubmit="return false">'
          +   '<div class="invisible"><button type="submit"></button></div>'
          + '</form>'
        );
        var mask = $.hsMask({
            title: title,
            node : form
        }, {
            label: "确定",
            glass: "btn-primary",
            click: function() {
                form.find(":submit").click();
            }
        }, {
            label: "取消",
            glass: "btn-link"
        });
        form.append(input);
        form.submit(function() {
            back.call(that, form);
            mask.hsClose();
            return false;
        });
    },

    cntList: function(lst) {
        var pre = lst.data("name") || "ar";
        var lzt = lst.children();
        var   i =  0 ;
        var   j = -1 ;
        for(; i < lzt.length(); i ++ ) {
            var sup;
            var sub  = $(lzt[i]);
            if (sub.is(".sift-group")) {
                sub  = sub.find(".sift-list").first();
                sup  = sub.data("name") || "";
            } else {
                sub  = sub.fidn("input:hidden");
                sup  = sub.attr("name") || "";
            }
            if (pre == sup.substr(0,pre.length)) {
                sup  = sub.substr(0+pre.length);
                var p  = sup.indexOf('.');
                if (p !== -1) {
                    p  = sup.substr (0,p);
                    p  =  parseInt  (  p);
                    if (j < p) {
                        j = p;
                    }
                }
            }
        }
        return j + 1;
    },
    getList: function() {
        var lst = this.siftBox.find(".sift-group.active").find(".sift-list:first");
        if (! lst.size() ) {
            lst = this.siftBox.find(".sift-group:first" ).find(".sift-list:first");
        }
        return lst;
    },
    addList: function(lst, rel) {
        var sub = this.listTmp.clone();
        var pre = lst.data("name") || "ar";
        var cnt = lst.data("size") ||  0  ;
            cnt = parseInt( cnt  );
        var key = pre +"."+ cnt +"."+ rel ;
        sub.find(".sift-list").first()
           .attr( "data-name" , key  );
        lst.append(sub);
        lst.data("size", cnt + 1 );
        return sub;
    },
    addItem: function(lst, fno, fro, txt, val) {
        var tag = this.itemTmp.clone();
        var pre = lst.data("name") || "ar";
        var cnt = lst.data("size") ||  0  ;
            cnt = parseInt( cnt  );
        var key = pre +"."+ cnt +"."+ fno.val() +"."+ fro.val();
        var inp = $('<input type="hidden"/>');
        tag.find(".sift-fn").text(fno.text());
        tag.find(".sfit-fr").text(fro.text());
        tag.find(".sift-fv").text(txt);
        inp.attr("name",key).val (val);
        lst.append(tag);
        lst.data("size", cnt + 1 );
        return tag;
    },
    setRels: function(fro, enf) {
        this._feed__datalist(fro.empty(),enf);
    },

    _feed__is: function(fno, fro) {
        this.setRels(fno, fro, [
            ["is", "为"]
        ]);
    },
    _feed__string: function(fno, fro) {
        switch (fno.data("for")) {
        case "serial": this.setRels(fro, [
            ["is", "为"],
            ["eq", "等于"],
            ["ne", "不等于"],
            ["sp", "匹配"],
            ["ns", "不匹配"]
        ]); break;
        case "search": this.setRels(fro, [
            ["is", "为"],
            ["sp", "匹配"],
            ["ns", "不匹配"]
        ]); break;
        default: this.setRels(fro, [
            ["is", "为"],
            ["eq", "等于"],
            ["ne", "不等于"]
        ]);
        }
    },
    _feed__number: function(fno, fro) {
        this.setRels(fro, [
            ["is", "为"],
            ["gt", "大于"],
            ["ge", "大或等于"],
            ["lt", "小于"],
            ["le", "小或等于"]
        ]);
    },
    _feed__date: function(fno, fro) {
        this.setRels(fro, [
            ["is", "为"],
            ["gt", "大于"],
            ["ge", "大或等于"],
            ["lt", "小于"],
            ["le", "小或等于"]
        ]);
    },
    _feed__enum: function(fno, fro) {
        this.setRels(fro, [
            ["is", "为"],
            ["eq", "等于"],
            ["ne", "不等于"]
        ]);
    },
    _feed__fork: function(fno, fro) {
        this.setRels(fro, [
            ["is", "为"],
            ["eq", "等于"],
            ["ne", "不等于"]
        ]);
    },

    _fill__is: function(fno, fro) {
        this.mask(
            fno.text()+" "+fro.text(),
          + '<select class="form-control">'
          +   '<option value="none">空</option>'
          +   '<option value="not-none">非空</option>'
          + '</select>',
            function(form) {
                var sel = form.find("select");
                var val =  sel. val();
                var txt =  sel.find("option").filter(function( ) {
                    return $(this).val() === val;
                }).text() || val;
                this.addItem(this.getList(), fno, fro, val, txt);
            }
        );
    },
    _fill__string: function(fno, fro) {
        this.mask(
            fno.text()+" "+fro.text(),
            '<input type="search" class="form-control"/>',
            function(form) {
                var val = form.find("input").val();
                this.addItem(this.getList(), fno, fro, val, val);
            }
        );
    },
    _fill__number: function(fno, fro) {
        this.mask(
            fno.text()+" "+fro.text(),
            '<input type="number" class="form-control"/>',
            function(form) {
                var val = form.find("input").val();
                this.addItem(this.getList(), fno, fro, val, val);
            }
        );
    },
    _fill__date: function(fno, fro) {
        var inp = $('<input type="hidden" data-topple="hsDate"/>');
        var fmt = fno.data("format");
        var fst = fno.data("offset");
        var typ = fno.data("type"  );

        // 转换预置格式
        switch (fmt) {
            case "time":
                fmt = hsGetLang("time.format");
                break;
            case "date":
                fmt = hsGetLang("date.format");
                break;
            case "datetime" : case "" : case null :
                fmt = hsGetLang("datetime.format");
        }

        inp.attr("data-format", fmt);
        inp.attr("data-offset", fst);
        inp.attr("data-type"  , typ);

        this.mask(
            fno.text()+" "+fro.text(),
            inp,
            function() {
                var val =  inp.val ();
                var txt = hsFmtDate(val, fmt);
                this.addItem(this.getList(), fno, fro, val, txt);
            }
        ).hsReady();
    },
    _fill__enum: function(fno, fro) {
        var nam = fno.data("ln") || fno.val();
        var sel = this.formBox.find("select[data-name='"+nam+"']").clone();
        this.mask(
            fno.text()+" "+fro.text(),
            sel,
            function() {
                var val =  sel.val ();
                var txt =  sel.find("option[value='" + val + "']").text( );
                this.addItem(this.getList(), fno, fro, val, txt);
            }
        ).hsReady();
    },
    _fill__fork: function(fno, fro, btn) {
        var ln  = fno.data("ln") || "";
        var vk  = fno.data("vk") || "id";
        var tk  = fno.data("tk") || "name";
        var url = fno.data( "href" ) || "";
        var bin = fno.data("target") || "";
        var box = jQuery('<div></div>').attr({"data-ln": ln, "data-vk": vk, "data-tk": tk});

        var that = this;
        btn.hsPick( url, bin, box, function (btn, v) {
            for(var val in v) {
                var arr  = v[val];
                var txt  = arr[0];
                that.addItem(this.getList(), fno, fro, val, txt);
            }
        });
    }
};

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
    this._url  = opts._url || opts.loadUrl ;

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

            that.fill(statBox.find(".stat-group[data-name]"));
        });
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;
        var url     = this._url;

        var rb  = [];
        var eb  = [];

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("type")) eb.push($(this).data("name")+".rb="+$(this).data("type"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).find(".checkbox").hide();
            $(this).find(".chartbox").hide();
            $(this).find(".alertbox").show()
             .children().text($(this).data("text")+" 统计中...");
        });

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
        url = hsSetParam (    url, "rb", rb.join(","));
        dat = hsSerialMix({}, dat, "?" + eb.join("&"));
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");
                    var t  =  n.attr("data-type");

                    if (n.size() === 0) {
                        continue;
                    }
                    if (d.length === 0) {
                        continue;
                    }

                    n.find(".alertbox").hide();
                    n.find(".checkbox").show();
                    n.find(".chartbox").show();

                    if (t === "range"
                    ||  t === "tally") {
                        that.setAmountCheck(n , d);
                        that.setAcountChart(n , d);
                    } else
                    if (t === "total") {
                        that.setAmountCheck(n , d);
                        that.setAmountChart(n , d);
                    } else
                    {
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
        var context = this.context;
        var findBox = this.findBox;
        var listBox = itemBox.find(".checkbox");

        // 检查接口
        if (! $.hsCanCopy()) {
            $.hsWarn("浏览器无法拷贝\r\n请使用较新版的 Chrome/Firefox/Safari/Edge, 或某些双核/多核浏览器的极速模式.", "warning");
            return;
        }

        // 复制提示
        var set = [{
            mode : "warn",
            glass: "alert-default",
            text : "为了降低网络延迟, 图表按从多到少排, 仅取部分; 这将会调取完整统计数据, 以便导入 Office/WPS 等应用中.",
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
            $.hsNote( set[0].note , "success" );
        });

        // 重复拷贝
        var div = listBox.children( "div" );
        if (div.size( )) {
            mok( );
            return;
        }

        var fn  = itemBox.attr("data-name");
        var fp  = itemBox.attr("data-prms");
        var fx  = itemBox.attr("data-text");
        var ft  = itemBox.attr("data-type");
        var url = this._url;

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
        url = hsSetParam (    url, "rn", 0 );
        url = hsSetParam (    url, "rb", fn);
        dat = hsSerialMix({}, dat, "?" + fp);
        delete dat[ fn + ".rn" ];
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        // 读取全部
        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
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
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + (v[3] ? ","+ v[3] : "") + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".at.")
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
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
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
    this._url  = opts._url || opts.loadUrl ;

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
        that.fill(statBox.find(".stat-group[data-name]"));
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;
        var url     = this._url;

        var rb  = [];
        var eb  = [];

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("type")) eb.push($(this).data("name")+".rb="+$(this).data("type"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).empty(); // 清空待写入选项
        });

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
        url = hsSetParam (    url, "rb", rb.join(","));
        dat = hsSerialMix({}, dat, "?" + eb.join("&"));
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");
                    var t  =  n.attr("data-type");

                    if (n.size() === 0 ) {
                        continue;
                    }
                    if (d.length === 0 ) {
                        continue;
                    }

                    if (t === "range"
                    ||  t === "tally"
                    ||  t === "total") {
                        that.setAmountCheck(n , d);
                    } else
                    {
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
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + (v[3] ? ","+ v[3] : "") + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".at.")
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
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
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
