/* global echarts, jQuery, HsForm, HsList, HsCUID */

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
 * 列表填充分页按钮
 */
function hsListFillMore(page) {
    HsList.prototype.fillPage.call(this, page);
    this.pageBox.find(".page-count").remove( );
    this.pageBox.find(".pagination").removeClass("pull-left");
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
    d.toggle( v && v == HsCUID );
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
 * 列表预设排序选项
 */
function hsListInitSort(x, v, n) {
    var inp = x;
    var sel = x.next().find("select").eq(0);
    var chk = x.next().find("select").eq(1);
    // 没有可排序的字段就不显示此项
    if (sel.children().size() < 2) {
        sel.closest(".form-group").remove();
        return;
    }
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
 * 列表统计筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsCate (context , opts) {
    context = jQuery( context  );
    context.data("HsCate", this);
    context.addClass( "HsCate" );

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
HsCate.prototype = {
    load: function() {
        this.statis();
        this.counts();
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
                     if (k == "__total__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     that.setStatisCheck(n, d);
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
                     if (k == "__total__") continue;
                     var d  = rst.info[k];
                     var n  = statbox.find("[data-name='"+k+"']");
                     that.setCountsCheck(n, d);
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
                .attr("name" , name+":in.")
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
