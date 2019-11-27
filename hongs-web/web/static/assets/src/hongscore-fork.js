
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
 * <button type="button" data-toggle="hsFork" data-target="@" data-href="xx/pick.html"> ... </button>
 *
 * 注: 2015/11/30 原 hsPick 更名为 hsFork (Hong's Foreign Key kit)
 **/

/**
 * 选择控件
 * @param {String} url 要打开的选择页地址
 * @param {jQuery} bin 在哪打开
 * @param {jQuery} box 在哪填充
 * @param {Function} fil 填充函数
 * @param {Function} fet 读取函数
 * @returns {jQuery}
 */
jQuery.fn.hsPick = function(url, bin, box, fil, fet) {
    var v    = { };
    var n    = box.attr("data-fn" ) || box.attr("name");
    var t    = box.attr("data-ft" ) || box.attr("type");
    var vk   = box.attr("data-vk" ) || "id"  ;
    var tk   = box.attr("data-tk" ) || "name";
    var mul  = box.data("repeated") || box.is( ".pickmul" )
                               || /(\[\]|\.\.|\.$)/.test(n);
    var foo  = box.closest(".HsForm").data("HsForm") || { };
    var btn  = jQuery(this);

    if (! fil) {
        do {
            fil = foo["_fill_"+ n];
            if (fil) break;

            fil = foo["_fill_"+ t];
            if (fil) break;

            fil = hsFormFillPick;
        } while (false);
    }

    if (! fet) {
        do {
            fet = foo["_fork_"+ n];
            if (fet) break;

            fet = foo["_fork_"+ t];
            if (fet) break;

            fet = hsFormForkData;
        } while (false);
    }

    // 读取已选数据
    fet.call(foo, box, v, n);

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

        // 填充数据
        fil.call   ( foo, box, v, n );
        box.trigger(    "change"    );
        return true;
    }

    function pickOpen() {
        var evt = jQuery.Event("pickOpen");
        evt.target = bin;
        box.trigger( evt, [v, n, t] );

        bin.addClass("pickbox")
        .toggleClass("pickmul", mul )
        .on("change"  , ".checkone", select)
        .on("click"   , ".commit"  , commit)
        .on("saveBack", ".create"  , create);

        var ids = Object.keys( v );
        var num = ids.length || "";
        bin.data( "pickData" , v );
        bin.find(".checkone").val ( ids );
        bin.find(".picknum" ).text( num );
    };

    function select () {
        var chk = jQuery(this);
        if (chk.closest(".HsList" ).data("HsList")._info) {
            return;
        }
        if (chk.closest(".openbox"). is ( bin ) !== true) {
            return;
        }

        var val = chk.val();
        var txt;
        var inf;

        do {
            if (! chk.prop("checked")) {
                break;
            }

            // 获取选项名称和附加信息
            txt = chk.attr( "title" );
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

        if (txt) inf = chk.hsData( ); // 其他字段信息

        if (pickItem(val, txt, inf, chk) === false) {
            chk.prop( "checked", false );
            return false;
        }

        // 已选数量
        var num = Object.keys(v).length ;
        bin.find(".picknum")
           .text( num || "");
    }

    function commit () {
        var btn = jQuery(this);
        if (btn.closest(".HsList" ).data("HsList")._info) {
            return;
        }
        if (btn.closest(".openbox"). is ( bin ) !== true) {
            return;
        }

        if (pickBack( ) === false ) {
            return false;
        }

        bin.hsClose();
        return false ;
    }

    function create () {
        var btn = jQuery(this);
        if (btn.closest(".HsList" ).data("HsList")._info) {
            return;
        }
        if (btn.closest(".openbox"). is ( bin ) !== true) {
            return;
        }

        var rst = arguments[1];
            rst = rst.info ? rst.info
              : ( rst.list ? rst.list[0] : { } );
        var val = rst[vk] || hsGetValue(rst, vk);
        var txt = rst[tk] || hsGetValue(rst, tk);

        if (pickItem(val, txt, rst) === false) {
            return false;
        }

        if (pickBack( ) === false ) {
            return false;
        }

        bin.hsClose();
        return false ;
    }

    if (bin) {
        bin =  bin  .hsOpen(url, pickOpen);
    } else {
        bin = jQuery.hsOpen(url, pickOpen);
    }
    bin.data( "rel" , btn.closest( ".openbox" )[0] );

    return bin;
};

/**
 * 表单获取选项
 * @param {jQuery} box
 * @param {Object} v
 */
function hsFormForkData(box, v) {
    if (box.is("input")) {
        var val = box.val ();
        var dat = box.data();
        var txt = box.attr("title")
               || box.data("name" );
        if (val) {
            v[val] = [txt, dat||{}];
        }
    } else
    if (box.is("ul,ol")) {
    box.find("li").each (function() {
        var row = jQuery(this);
        var inp = row.find("input");
        var val = inp.val ();
        var dat = inp.data();
        var txt = row.attr("title")
               || inp.data("name" );
        if (val) {
            v[val] = [txt, dat||{}];
        }
    });
    } else
    if (box.is("table,tbody")) {
    box.find("tr").each (function() {
        var row = jQuery(this);
        var inp = row.find("input");
        var val = inp.val ();
        var dat = inp.data();
        var txt = row.attr("title")
               || inp.data("name" );
        if (val) {
            v[val] = [txt, dat||{}];
        }
    });
    } else {
    box.find("input:hidden").each(function() {
        var inp = jQuery(this);
        var val = inp.val ();
        var dat = inp.data();
        var txt = inp.attr("title")
               || inp.data("name" );
        if (val) {
            v[val] = [txt, dat||{}];
        }
    });
    }
}

/**
 * 表单填充选项
 * @param {jQuery} box
 * @param {Object} v
 * @param {String} n
 */
function hsFormFillPick(box, v, n) {
    if (! n ) n = box.data( "fn" );
    var rol = box.data("readonly")
           || box.is  (".pickrol");
    var mul = box.data("repeated")
           || box.is  (".pickmul")
           || /(\[\]|\.\.|\.$)/.test(n);
        box.toggleClass("pickmul", mul);
    var btn = box.siblings("[data-toggle=hsPick],[data-toggle=hsFork]");

    // 表单初始化载入时需从关联数据提取选项对象
    if (this._info) {
        var ak = box.attr("data-ak") || "data";
        v = this._info[ak] || hsGetValue(this._info, ak);
        if (! v ) return ;
        if (!mul) v = [v];
    }

    // 对表单初始化数据转换为关联组件的字典格式
    if (jQuery.isArray(v)) {
        var tk = box.attr("data-tk") || "name";
        var vk = box.attr("data-vk") ||  "id" ;
        var v2 = {};
        for(var i = 0; i < v.length; i++) {
            var j = v[ i];
            var v3= j[vk] || hsGetValue(j, vk);
            var t3= j[tk] || hsGetValue(j, tk);
            if (v3 !== undefined
            &&  t3 !== undefined) {
                v2[v3] = [t3 , j];
            }
        }
        v = v2;
    } else if (! jQuery.isPlainObject(v)) {
        v = {};
    }

    function reset(btn, box) {
        var txt = btn.data("txt");
        var cls = btn.data("cls");
        btn.text(txt);
        box.val ( "");
        box.attr( "title" , ""  );
        btn.attr( "class" , cls );
    }
    function inset(btn, box, val, txt) {
        btn.text(txt);
        box.val (val);
        box.attr( "title" , txt );
        btn.addClass("btn-info" );
        btn.append('<span class="close pull-right">&times;</span>');
    }
    function doset(box, val, txt, cls) {
        var lab = jQuery('<span></span>').text(txt);
        var inp = jQuery('<input type="hidden" />').attr("name", n).val(val);
        var div = jQuery('<li class="btn '+ cls[0] +' form-control"></li>' )
           .attr  ("title" , txt)
           .append('<span class="close pull-right">&times;</span>')
           .append('<span class="glyphicon '+ cls[1] +'" ></span>')
           .append(lab)
           .append(inp);
        box.append(div);
    }

    if (box.is("input") ) {
        if (! btn.data("pickInited")) {
            btn.data("pickInited", 1);
            btn.data("txt", btn.text( /***/ ));
            btn.data("cls", btn.attr("class"));
            btn.on("click", ".close", [btn, box], function(evt) {
                var btn = evt.data[0];
                var box = evt.data[1];
                var val = box.val ( );
                delete  v [val];
                reset(box, btn);
                box.trigger("change");
                return false;
            });
        }

        if (! jQuery.isEmptyObject(v) ) {
            for(var val in v) {
                var arr  = v[val];
                var txt  = arr[0];
                inset(btn, box, val, txt);
            }
        } else {
                reset(btn, box /*reset*/);
        }
    } else {
        if (! box.data("pickInited")) {
            box.data("pickInited", 1);
            box.on("click", ".close", [btn, box], function(evt) {
                var btn = evt.data[0];
                var box = evt.data[1];
                var opt = jQuery( this ).closest( "li" );
                var val = opt.find("input:hidden").val();
                delete  v [val];
                opt.remove();
                btn. show ();
                box.trigger("change");
                return false;
            });
            box.on("click", "li.btn", [btn, box], function(evt) {
                var btn = evt.data[0];
                var box = evt.data[1];
                var opt = jQuery( this ).closest( "li" );
                var val = opt.find("input:hidden").val();
                var key = box.attr("data-vk" )||( "id" );
                var url = box.attr("data-href"  );
                var rel = box.attr("data-target");
                if (url === "-" || rel === "-"
                ||  evt.isDefaultPrevented() ) {
                    return; // 可阻止默认行为
                }
                if (url) {
                    url = hsSetParam ( url , key , val );
                    if (! rel) {
                     jQuery.hsOpen(url);
                    } else {
                        box.hsFind(rel)
                           .hsOpen(url);
                    }
                } else {
                    if (! rol && ! mul) {
                        btn.click (   );
                    }
                }
            });
        }

        if (! jQuery.isEmptyObject(v) ) {
            if (! mul) btn.hide();
        } else {
            if (! rol) btn.show();
        }

        // 按钮及图标样式
        var cls = [];
        if (box.attr('data-href')) {
            cls[0] = rol ? "btn-link" : "btn-info";
            cls[1] = rol ? "glyphicon-link" : "glyphicon-share";
        } else {
            cls[0] = rol ? "btn-text" : "btn-info";
            cls[1] = rol ? "glyphicon-link" : "glyphicon-check";
        }
        cls[0] = box.attr("data-item-class") || cls[0];
        cls[1] = box.attr("data-icon-class") || cls[1];

        box.empty();
        for(var val in v) {
            var arr  = v[val];
            var txt  = arr[0];
            doset(box, val, txt, cls);
        }
    }
}

/**
 * 列表填充选择
 * @param {jQuery} cel
 * @param {String} v
 * @param {String} n
 */
function hsListFillPick(cel, v, n) {
    var box = cel. closest (".pickbox,.openbox,.loadbox");
    var mul = box.hasClass ( "pickmul" );
    var val = box.data("pickData") || {};

    // 单选还是多选
    if (! mul) {
        box.find(".checkall").hide(/**/);
    }

    // 填充选择控件
    if (! mul) {
        HsList.prototype._fill__radio.call(this, cel, v, n );
    } else {
        HsList.prototype._fill__check.call(this, cel, v, n );
    }

    // 判断是否选中
    if (val[v] !== undefined) {
        cel.find(".checkone").prop("checked", true).change();
    }

    // 其他字段信息
    cel.find(".checkone").data(this._info);
}

(function($) {
    $(document)
    .on("click", "[data-toggle=hsPick],[data-toggle=hsFork]",
    function() {
        var url = $(this).attr("data-href") || $(this).attr("href");
        var bin = $(this).attr("data-target"); // 选择区域
        var box = $(this).attr("data-result"); // 填充区域

        if (bin) {
            bin = $(this).hsFind(bin);
        }
        if (box) {
            box = $(this).hsFind(box);
        } else
        {
            box = $(this).siblings ( "[name],[data-fn],[data-ft]" )
               .not(".form-ignored" );
        }

        $(this).hsPick(url, bin, box);
        return false;
    });
})(jQuery);

// 别名
jQuery.fn.hsFork = jQuery.fn.hsPick;
hsFormFillFork = hsFormFillPick;
hsListFillFork = hsListFillPick;
