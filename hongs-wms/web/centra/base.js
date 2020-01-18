/* global jQuery, HsCUID, HsForm, HsList, hsFormFillFork, hsListFillFork, echarts */

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
 * 返回键联动导航条
 */
$(window).on("popstate", function(ev) {
    ev = ev.originalEvent;
    if (!ev || !ev.state || !ev.state.crumb) { return; }
    $("#main-context>.breadcrumb>.back-crumb:visible>a").click();
    history.pushState({crumb: true}, null, null);
}); history.pushState({crumb: true}, null, null);

/**
 * 筛选重置事件处理
 */
$(document).on("reset", ".HsList .findbox", function() {
    var findbox = $(this);
    findbox.find( "[data-ft=_fork]" ).each( function() {
        hsFormFillFork( $(this), {} );
    });
    setTimeout(function() {
        findbox.find(":submit")
               .first().click();
    } , 500);
});

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
        complete: function   (rst) {
            rzt = hsResponse (rst , 3);
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
S$.remove = function(req) {
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
function hsListDollFilt(x, v, n) {
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

    return HsForm.prototype._doll__select.call(this, x, v, n);
}
var hsListPrepFilt = hsListDollFilt; // 兼容

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
         hsListFillOpen(x, v, t);
    };
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
    var cho = $('<label><input type="checkbox" name="_list_cols" />'
              + '<span style="display:inline-block"></span></label>');

    ths.each(function() {
        if ($(this).is(".dont-hide,._check,._admin")) {
            return;
        }
        var ch = cho.clone().appendTo(chs);
        ch.find("span" ).text( $(this).text(/**/) );
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
 * 复制列表中的内容
 */
function hsCopyListData(box) {
    // 检查接口
    if (! window.getSelection
    ||  ! document.execCommand
    ||  ! document.createRange ) {
        $.hsWarn("浏览器无法复制\r\n请使用较新版的 Chrome/Safari/Firefox, 或某些双核/多核浏览器的极速模式.", "warning");
        return;
    }

    // 复制提示
    var msk = $.hsMask({
        mode : "warn",
        glass: "alert-default",
        text : "这将整理原始数据, 清理表格中的内容, 提取完整的日期/时间, 补全图片/链接等网址, 以便您复制到 Office 等软件中查阅.",
        title: "正在组织列表原始数据, 请稍等..."
    }, {
        glass: "btn-primary",
        label: "复制"
    }, {
        glass: "btn-default",
        label: "取消"
    });
    var mok = function() {
        msk.removeClass ("alert-info")
           .addClass ("alert-success");
        msk.find(".alert-footer .btn")
           .prop( "disabled" , false );
        msk.find(".alert-title" /**/ )
           .text("已经备好列表原始数据, 请复制吧!");
    };
    msk.find(".alert-footer button").prop("disabled", true);
    msk.find(".alert-footer button").eq(0).click(function() {
        msk.prev().show().children().hsCopy();
        $.hsNote("复制成功, 去粘贴吧!", "success");
    });

    // 复制表格
    var div = $('<div></div>').insertBefore(msk);
    var tab = $('<table></table>').appendTo(div);
    var thd = $('<thead></thead>').appendTo(tab);
    var tbd = $('<tbody></tbody>').appendTo(tab);
    var tr  = $('<tr></tr>');
    var th  = $('<td></th>');
    var td  = $('<td></td>');
    div.attr("style" , "height:1px; display:none; overflow:auto;");
    tab.attr("style" , "margin:1px;")
       .attr("class" , "table table-bordered");

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
            td2.attr( "style" , "white-space: pre-line;" );
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

    // 就绪
    mok ();
}

/**
 * 清理内容中的标签
 * 以供表格复制中用
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
        this.warn(msg, "warning", func, funx)
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
    if (msg) {
        this.warn(msg, "warning", func, null)
            .find(".alert-body").append(memo)
            .trigger("shown.bs.modal");
    } else {
        func();
    }
}

/**
 * 列表高级搜索支持
 */
function hsFindWithWord(url, data) {
    var word = this.context.find(".findbox [name=wd]").val();
    if (data && word && /^\?.+=/.test(word)) {
        word = hsSerialDic(word);
        data = hsSerialDic(data);
               delete data["wd"];
        data = hsSerialMix(data, word);
    }
    HsList.prototype.load.call(this, url, data);
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
    this.murl  = opts.murl;
    this.curl  = opts.curl;
    this.eurl  = opts.eurl;

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
            if ($(this).data("extr")) eb.push($(this).data("extr"));

            $(this).find(".checkbox").hide();
            $(this).find(".chartbox").hide();
            $(this).find(".alertbox").show()
             .children().text($(this).data("text")+" 统计中...");
        });

        var dat;
        dat = hsSerialDic(findBox);
        url = hsSetParam (url, "rb.", rb);
        url = url + eb.join( "&" );

        $.hsAjax({
            url : url,
            data: dat,
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.info || {};
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

                    if (dat && dat [k] ) {
                        n.find("[name='"+ k +"']").val ( dat [k] );
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
        var listBox =itemBox.find(".checkbox");

        // 检查接口
        if (! window.getSelection
        ||  ! document.execCommand
        ||  ! document.createRange ) {
            $.hsWarn("浏览器无法复制\r\n请使用较新版的 Chrome/Safari/Firefox, 或某些双核/多核浏览器的极速模式.", "warning");
            return;
        }

        // 复制提示
        var msk = $.hsMask({
            mode : "warn",
            glass: "alert-default",
            text : "为了降低网络延迟, 图表按从多到少排, 仅取前 20 条; 而此功能将调取完整统计数据, 以便您复制到 Office 等软件中查阅.",
            title: "正在获取完整统计数据, 请稍等..."
        }, {
            glass: "btn-primary",
            label: "复制"
        }, {
            glass: "btn-default",
            label: "取消"
        });
        var mok = function() {
            msk.removeClass ("alert-info")
               .addClass ("alert-success");
            msk.find(".alert-footer .btn")
               .prop( "disabled" , false );
            msk.find(".alert-title" /**/ )
               .text("已经取得完整统计数据, 请复制吧!");
        };
        msk.find(".alert-footer button").prop("disabled", true);
        msk.find(".alert-footer button").eq(0).click(function() {
            var div = listBox.children( "div" );
            var tab = /**/div.children("table");
            div.show(  );
            tab.hsCopy();
            div.hide(  );
            $.hsNote("复制成功, 去粘贴吧!", "success");
        });

        // 重复拷贝
        var div = listBox.children( "div" );
        if (div.size( )) {
            mok( );
            return;
        }

        var fx  = itemBox.attr("data-text");
        var fn  = itemBox.attr("data-name");
        var ft  = itemBox.attr("data-type");
        var url = ft==="amount"? this.murl
              : ( ft==="acount"? this.curl
              :                  this.eurl);
        var dat = this.findBox.serialize( );

        url = hsSetParam(url, "rb", fn ); // 单字段
        url = hsSetParam(url, "rn", "0"); // 不分页

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
                if (rst.info && rst.info[fn]) {
                        var a = rst.info[fn];
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
            if (v[0] ==  "" )  continue;
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
            if (v[0] ==  "" )  continue;
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
  return this._hsModule(HsStat, opts);
};

jQuery.fn.hsCopy = function() {
    if (! window.getSelection
    ||  ! document.execCommand
    ||  ! document.createRange ) {
        throw new Error("hsCopy: Copy is not supported");
    }

    var rng = document.createRange();
    var sel = window.getSelection( );
    sel.removeAllRanges();
    for ( var i = 0; i < this.length; i ++) {
        try {
            rng.selectNodeContents(this[i]);
            sel.addRange(rng);
        } catch (e) {
            rng.selectNode/*Text*/(this[i]);
            sel.addRange(rng);
        }
    }
    document.execCommand("Copy");
};
