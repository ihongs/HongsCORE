/* global echarts, jQuery, HsForm, HsList, HsCUID */

/**
 * 设置当前用户ID
 */
if (hsChkUri('public')) {
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
S$.remove = function(req) {
    var url = S$.src() + "/delete.act";
    return S$.send(url, req);
};
S$.browse = function(req) {
    var mod = S$();
    mod.load(undefined, hsSerialMix(mod._data, req));
};

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
    if (v && v == window.HsCUID) {
        d.show ();
    }
}

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
        inp.val(chk.val() + sel.val());
    });

    /*
    if (sel.find("[value=mtime]").size()) {
        chk.prop("checked", true);
        sel.val ( "mtime");
        inp.val ("-mtime");
    } else
    if (sel.find("[value=ctime]").size()) {
        chk.prop("checked", true);
        sel.val ( "ctime");
        inp.val ("-ctime");
    }
    */
}

/**
 * 列表属主权限控制
 */
function hsListInitMine(x, v, n) {
    x.next().on( "change", ":radio", function() {
        switch ($(this).val()) {
            case "1": x.attr("name", n + ".eq").val(HsCUID); break;
            case "2": x.attr("name", n + ".ne").val(HsCUID); break;
            default : x.attr("name", n).val(""); break;
        }

        // 为规避多余的查询项, 没有设选项字段名称, 需自行取消其他选项
        $(this).closest("label").siblings().find("input").prop("checked", false);
    });
}

/**
 * 简单页面路由处理
 * @param {jQuery|Element} context
 * @param {Object} urls
 * @return {HsPops}
 */
function HsPops (context , urls) {
    var paneBox, inState, IF, NF;
    context = jQuery( context  );
    paneBox = context.closest(".labs");
    inState = false ;
    IF = "1" ; // 处于表单中
    NF = null; // 非表单模式
    
    /**
     * 列表和查看对公共区很重要
     * 如果有给编号则打开详情页
     */
    context.hsLoad(urls.listUrl + hsSetParam(location.search, "id", null));
    $(window).on("popstate", function() {
        inState = true;
        paneBox.children().eq(2).children().hsClose();
        if (H$("@id")) {
            if (H$("@if")) {
                if (urls.formUrl) {
                    inState = true;
                    paneBox.hsOpen(urls.formUrl + location.search);
                }
            } else {
                if (urls.infoUrl) {
                    inState = true;
                    paneBox.hsOpen(urls.infoUrl + location.search);
                }
            }
        } else {
            if (H$("@if")) {
                if (urls.addsUrl) {
                    inState = true;
                    paneBox.hsOpen(urls.addsUrl + location.search);
                }
            }
        }
    });
    $(window).trigger("popstate");
    
    
    /**
     * 还需要处理内部加载的页面
     */
    paneBox.on("hsReady", ".loadbox", function() {
        if (inState) {
            inState = false;
            return;
        }

        var id ;
        var ul = location.search;
        var rl = hsFixUri($(this).data("href"));

        if (urls.infoUrl && rl.substr(0, urls.infoUrl.length) == urls.infoUrl ) {
            id = hsGetParam(rl, "id");
            ul = hsSetParam(ul, "id", id);
            ul = hsSetParam(ul, "if", NF);
            history.pushState({}, "", location.pathname + ul);
        } else
        if (urls.formUrl && rl.substr(0, urls.formUrl.length) == urls.formUrl ) {
            id = hsGetParam(rl, "id");
            ul = hsSetParam(ul, "id", id);
            ul = hsSetParam(ul, "if", IF);
            history.pushState({}, "", location.pathname + ul);
        } else
        if (urls.addsUrl && rl.substr(0, urls.addsUrl.length) == urls.addsUrl ) {
            ul = hsSetParam(ul, "id", NF);
            ul = hsSetParam(ul, "if", IF);
            history.pushState({}, "", location.pathname + ul);
        }
    });
    paneBox.on("hsClose", ".loadbox", function() {
        if (inState) {
            inState = false;
            return;
        }

        var ul = location.search;
        var rl = hsFixUri($(this).data("href"));

        if((urls.infoUrl && rl.substr(0, urls.infoUrl.length) == urls.infoUrl )
        || (urls.formUrl && rl.substr(0, urls.formUrl.length) == urls.formUrl )
        || (urls.addsUrl && rl.substr(0, urls.addsUrl.length) == urls.addsUrl)) {
            ul = hsSetParam(ul, "id", NF);
            ul = hsSetParam(ul, "if", NF);
            history.pushState({}, "", location.pathname + ul);
        }
    });
}

/**
 * 列表统计筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsCate(context, opts) {
    context = $(context);

    var  statBox = context.find(".statbox");
    var  findBox = context.find(".findbox");
    var  toolBox = context.find(".toolbox");

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this.murl  = opts.murl;
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
HsCate.prototype = {
    load: function() {
        this.amount();
        this.acount();
    },

    amount: function(rb) {
        var that = this;
        var url  = this.murl;
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
                rst = rst.data || rst.info || {};
                for (var k in rst) {
                     if (k == "__count__") continue;
                     var n  = statBox.find("[data-name='"+k+"']");
                     if (0 == n.size( )  ) continue;
                     var d  = rst[k];
                     that.setAmountCheck(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statBox.find("[name='"+k+"']" ).val(data [k]);
                }

                statBox.find(".checkbox").each(function() {
                    if ($(this).find(":checked"  ).size() === 0 ) {
                        statBox.find(".checkall2").prop("checked", true);
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
                rst = rst.data || rst.info || {};
                for (var k in rst) {
                     if (k == "__count__") continue;
                     var n  = statBox.find("[data-name='"+k+"']");
                     if (0 == n.size( )  ) continue;
                     var d  = rst[k];
                     that.setAcountCheck(n, d);
                }

                var list = context.data( "HsList" );
                var data = hsSerialDic (list._data);
                for (var k in data) {
                    statBox.find("[name='"+k+"']" ).val(data [k]);
                }

                statBox.find(".checkbox").each(function() {
                    if ($(this).find(":checked"  ).size() === 0 ) {
                        statBox.find(".checkall2").prop("checked", true);
                    }
                });
            }
        });
    },

    setAmountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.empty();

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
            if (v[0] == "" || v[2] == 0) continue;
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
        var box2  = box.empty();

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
            if (v[0] == "" || v[2] == 0) continue;
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
  return this._hsModule(HsCate, opts);
};
