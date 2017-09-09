
/* global HsList */

/**
 * 组合选择工具之用法:
 * 在选择列表配置添加:
 * data-data-0="_fill__fork:(hsListFillFork)"
 * 在选择列表头部添加:
 * <td data-ft="_fork"><input type="checkbox" class="checkall" title="XX"/></td>
 * 在表单配置区域添加:
 * data-data-0="_fill__fork:(hsFormFillFork)"
 * 在表单选项区域添加:
 * <ul data-ft="_fork" data-fn="xx_id" data-ak="xx" data-vk="id" data-tk="name" class="pickbox"></ul>
 * <button type="button" data-toggle="hsFork" data-target="@" data-href="/xx/fork.html">选择</button>
 *
 * 注: 2015/11/30 原 hsPick 更名为 hsFork (Hong's Foreign Key kit)
 **/

/**
 * 选择控件
 * @param {String} url 要打开的选择页地址
 * @param {jQuery} bin 在哪打开
 * @param {jQuery} box 在哪填充
 * @param {Function} fil 填充函数
 * @param {Function} fet 加载函数
 * @returns {jQuery}
 */
jQuery.fn.hsPick = function(url, bin, box, fil, fet) {
    var v    = { };
    var n    = box.attr("data-fn" ) || box.attr("name");
    var t    = box.attr("data-ft" ) || ""    ;
    var vk   = box.attr("data-vk" ) || "id"  ;
    var tk   = box.attr("data-tk" ) || "name";
    var mul  = box.data("repeated") || /(\[\]|\.\.|\.$)/.test(n);
    var frm  = box.closest( ".HsForm" ).data( "HsForm")  ||  { };
    var btn  = jQuery(this);

    if (! fil) {
        do {
            fil = frm["_fill_"+ n];
            if (fil) break;

            fil = frm["_fill_"+ t];
            if (fil) break;

            fil = hsFormFillPick;
        } while (false);
    }

    if (fet) {
        fet(box, v, n );
    } else
    if (box.is("input")) {
        var val = box.val( );
        var txt = btn.text();
        if (val) {
            v[val] = txt;
        }
    } else
    if (box.is("ul,ol")) {
        box.find("li").each(function() {
            var opt = jQuery(this);
            var val = opt.find(".pickval").val ();
            var txt = opt.find(".picktxt").text();
            v[val] = txt;
        });
    }

    function pickItem(val, txt, inf, chk ) {
        var evt = jQuery.Event("pickItem");
        evt.target = chk;
        box.trigger( evt, arguments );
        if (evt.isDefaultPrevented()) {
            return false;
        }

        if (! mul) {
            for( var  key  in  v )
                delete v[key];
            if (txt !== undefined)
                v[val]= [txt, inf];
        } else {
            if (txt !== undefined)
                v[val]= [txt, inf];
            else
                delete v[val];
        }
        return true;
    }

    function pickBack() {
        // 未选警告
        if (jQuery.isEmptyObject(v)) {
            var msg = box.data("unpickedError");
            if (msg != "!") {
                msg = msg?msg: "fork.unpicked" ;
                if (! confirm (hsGetLang(msg))) {
                    return;
                }
            }
        }

        var evt = jQuery.Event("pickBack");
        evt.target = bin;
        box.trigger( evt, [v, n, t] );
        if (evt.isDefaultPrevented()) {
            return false;
        }

        fil.call(frm, box, v, n, "fork");
        box.trigger("change");
        return true;
    }

    function pickOpen() {
        var bin = jQuery(this);
        bin.data("pickData", v)
           .addClass("pickbox")
        .toggleClass("pickmul", mul)
        .on("change"  , ".checkone", select)
        .on("click"   , ".ensure"  , ensure)
        .on("saveBack", ".create"  , create);
        // 初始选中
        bin.find(".checkone").val(Object.keys(v));
    };

    function select() {
        var chk = jQuery(this);
        if (chk.closest(".HsList").data("HsList")._info) {
            return;
        }
        if (chk.closest(".openbox").is ( bin ) == false) {
            return;
        }

        var val = chk.val();
        var txt;
        var inf;

        do {
            if (! chk.prop("checked")) {
                break;
            }

            // 获取代表名称
            txt = chk.attr( "title" );

            // 获取附加信息
            inf = chk.hsData( );

            if (txt) break;

            // 选项中没有指定则尝试从当前行的其他位置获取
            var thd = chk.closest("table").find("thead");
            var tds = chk.closest( "tr"  ).find( "td"  );
            var idx;

            idx = thd.find("[data-fn='"+tk+"']").index();
            if (idx != -1) txt = tds.eq(idx).text();
            if (txt) break;

            idx = thd.find("[data-ft='"+tk+"']").index();
            if (idx != -1) txt = tds.eq(idx).text();
            if (txt) break;

            idx = thd.find(".name" ).index();
            if (idx != -1) txt = tds.eq(idx).text();
        }
        while (false);

        if (pickItem(val, txt, inf, chk) === false) {
            chk.prop("checked", false);
            return false;
        }
    }

    function ensure() {
        var btn = jQuery(this);
        if (! btn.closest(".openbox").is(bin)) {
            return;
        }

        if (! pickBack()) {
            return false;
        }

        bin.hsClose();
        return false ;
    }

    function create(evt, rst ) {
        var btn = jQuery(this);
        if (! btn.closest(".openbox").is(bin)) {
            return;
        }

        if (! rst || ! rst.info) {
            return false;
        }
        if (! pickItem(rst.info[vk], rst.info[tk], rst.info)) {
            return false;
        }
        if (! pickBack()) {
            return false;
        }

        bin.hsClose();
        return false ;
    }

    if (bin) {
        bin =  bin  .hsOpen(url);
    } else {
        bin = jQuery.hsOpen(url);
    }
    pickOpen.call ( bin );
    bin.data("rel", btn.closest(".openbox")[0]);

    return bin;
};

/**
 * 表单填充选项
 * @param {jQuery} box
 * @param {Object} v
 * @param {String} n
 * @param {String} t
 * @returns {undefined}
 */
function hsFormFillPick(box, v, n, t) {
    var btn = box.siblings("[data-toggle=hsPick],[data-toggle=hsFork]");
    var mul = ! ! box.data( "repeated" ) || /(\[\]|\.\.|\.$)/.test( n );

    if (t == "info") {
        if (! v ) return ;
        var tn = box.attr("data-ak") || n.replace(/_id$/, "");
        v = hsGetValue(this._info, tn);
        if (! v ) return ;
        if (!mul) v = [v];
    }
    if (jQuery.isArray(v)) {
        var vk = box.attr("data-vk") ||  "id" ;
        var tk = box.attr("data-tk") || "name";
        var x  = {};
        for(var i = 0; i < v.length; i++) {
            var j = v[i];
            if (j[vk] !== undefined
            &&  j[tk] !== undefined) {
              x[j[vk]] = [j[tk] , j];
            }
        }
        v = x ;
    } else if (! jQuery.isPlainObject(v)) {
        v = {};
    }

    function reset(btn, box) {
        var txt = btn.data("txt");
        var cls = btn.data("cls");
        box.val ( "");
        btn.text(txt);
        btn.attr( "class" , cls );
    }
    function inset(btn, box, val, txt) {
        box.val (val);
        btn.text(txt);
        btn.addClass("btn-info" );
        btn.append('<a href="javascript:;" class="close">&times;</a>');
    }
    function putin(btn, box, val, txt) {
        box.append(jQuery('<li class="btn btn-info form-control"></li>').attr( "title", txt )
           .append(jQuery('<input class="pickval" type="hidden"/>').attr("name", n).val(val))
           .append(jQuery( '<span class="picktxt"></span>' ).text (  txt  ))
           .append(jQuery( '<span class="close pull-right">&times;</span>'))
        );
    }
    function puton(btn, box, val, txt) {
        box.append(jQuery('<li class="btn btn-link form-control"></li>').attr( "title", txt )
           .append(jQuery('<input class="pickval" type="hidden"/>').attr("name", n).val(val))
           .append(jQuery( '<span class="picktxt"></span>' ).text (  txt  ))
        );
    }

    if (box.is("input") ) {
        if (! btn.data("pickInited")) {
            btn.data("pickInited", 1);
            btn.data("txt", btn.text( ) );
            btn.data("cls", btn.attr("class"));
            btn.on("click", ".close", box, function(evt) {
                var btn = jQuery(evt.delegateTarget);
                var box = evt.data;
                reset( box , btn );
                box.trigger("change");
                return false ;
            });
        }

        if (!jQuery.isEmptyObject(v)) {
        for(var val in v) {
            var arr  = v[val];
            var txt  = arr[0];
            inset(btn, box, val, txt);
        }} else {
            reset(btn, box  );
        }
    } else if ( ! box.is(".pickrol")) {
        if (! box.data("pickInited")) {
            box.data("pickInited", 1);
            box.on("click", ".close", btn, function(evt) {
                var opt = jQuery(this).closest("li");
                var val = opt.find(":hidden").val( );
                var btn = evt.data;
                delete v[val];
                btn.show (  );
                opt.remove( );
                box.trigger("change");
                return false ;
            });
            if (! mul) {
                box.on("click", null, btn, function(evt) {
                    evt.data.click();
                });
            }
        }

        box.empty().toggleClass("pickmul", mul);

        if (!jQuery.isEmptyObject(v)) {
            btn.hide();
        } else if (! mul) {
            btn.show();
        }
        for(var val in v) {
            var arr  = v[val];
            var txt  = arr[0];
            putin(btn, box, val, txt);
        }
    } else {
        for(var val in v) {
            var arr  = v[val];
            var txt  = arr[0];
            puton(btn, box, val, txt);
        }
    }
}

/**
 * 列表填充选择
 * @param {jQuery} cel
 * @param {String} v
 * @param {String} n
 * @returns {undefined}
 */
function hsListFillPick(cel, v, n) {
    var box = cel.closest (".pickbox");
    var mul = box.hasClass( "pickmul");
    var val = box.data("pickData")||{};

    // 单选还是多选
    if (! mul) {
        box.find( ".checkall" ).hide();
    }

    // 填充选择控件
    if (! mul) {
        HsList.prototype._fill__radio.call( this, cel, v, n );
    } else {
        HsList.prototype._fill__check.call( this, cel, v, n );
    }

    // 判断是否选中
    if (val[v] !== undefined) {
        cel.find(".checkone").prop("checked", true).change( );
    }
}

(function($) {
    $(document)
    .on("click", "[data-toggle=hsPick],[data-toggle=hsFork]",
    function() {
        var url = $(this).attr("data-href") || $(this).attr("href");
        var bin = $(this).attr("data-target");
        var box = $(this).attr("data-result");

        // 选择区域
        if (bin) {
            bin = $(this).hsFind(bin);
        } else {
            bin = $(this).hsFind("@");
        }

        // 填充区域
        if (box) {
            box = $(this).hsFind(box);
        } else {
            box = $(this).siblings("[name],[data-fn]").not(".form-ignored");
        }

        $(this).hsPick(url, bin, box);
        return false;
    });
})(jQuery);

// 别名
jQuery.fn.hsFork = jQuery.fn.hsPick;
hsFormFillFork = hsFormFillPick;
hsListFillFork = hsListFillPick;
