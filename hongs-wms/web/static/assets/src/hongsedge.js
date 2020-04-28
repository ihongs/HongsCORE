/* global H$, jQuery, HsCUID, HsForm, HsList, hsFormFillFork, hsListFillFork, echarts */

//** 资源扩展功能 **/

H$.inst = function() {
    var context = $(".HsList,.HsForm").filter(":visible:first");
    return context.data("HsList")
        || context.data("HsForm");
};
H$.src  = function() {
    return (H$.inst()._url || location.pathname)
    //  .replace(/\#.*/, '')
        .replace(/\?.*/, '')
        .replace(/\/[^\/]+$/, '');
};
H$.load = function(req) {
    var mod = H$.inst();
    mod.load(undefined, hsSerialMix(mod._data, req));
};
H$.send = function(url, req) {
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

//** 一般事件功能 */

/**
 * 设置当前用户ID, 登录时写入
 */
if (hsChkUri('public')) {
    window.HsCUID = H$('%HsCUID');
    window.HsCUST = H$('%HsCUST');
} else {
    window.HsCUID = null ;
    window.HsCUST = null ;
    H$ ( '%HsCUID', null);
    H$ ( '%HsCUST', null);
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
         hsFillOpenLink(x, v, t);
    };
}
function hsFormWrapOpen(t) {
    return function (x, v) {
         hsFillOpenLink(x, v, t);
    };
}

jQuery.hsCanCopy = function() {
    return window.clipboardData
      || ( window.getSelection
      &&   document.execCommand
      &&   document.createRange );
};
jQuery.fn.hsCopy = function() {
    if (window.clipboardData) {
        clipboardData.setData("Text", $(this).prop("outerHTML"));
    } else
    if (window.getSelection
    &&  document.execCommand
    &&  document.createRange) {
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
    } else
    {
        throw new Error("hsCopy: Copy is not supported");
    }
};
