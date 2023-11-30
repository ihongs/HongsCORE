
/* global HsList */

/**
 * 组合选择工具之用法:
 * 在选择列表配置添加:
 * data--0="_fill__fork:(hsListFillFork)"
 * 在选择列表头部添加:
 * <td data-ft="_fork"><input type="checkbox" class="checkall" title="XX"/></td>
 * 在表单配置区域添加:
 * data--0="_fill__fork:(hsFormFillFork)"
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
    var at   = box.attr("data-at" );
    var mul  = box.is  (".pickmul")
         || !! box.data("repeated")
         || !! box.data("multiple")
         || /(\[\]|\.\.|\.$)/.test(n);
    var foo  = box.closest(".HsForm").data("HsForm") || {};
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

            // 单选直接结束
            pickBack();
        } else {
            if (txt !== undefined)
                v[val]= [txt, inf];
            else
                delete v[val];

            // 显示已选数量
            var l = Object.keys(v).length ;
            bin.find(".picknum").text( l );
        }
        return true;
    }

    function pickBack() {
        var evt = jQuery.Event("pickBack");
        evt.target = bin;
        box.trigger( evt, [v, n, t] );
        if (evt.isDefaultPrevented()) {
            return false;
        }

        bin.hsClose( );
        fil. call  ( foo, box, v, n );
        box.trigger("change", [v, n]);
        return true;
    }

    function pickOpen() {
        var bin = jQuery(this); // 还没赋值
        var evt = jQuery.Event("pickOpen");
        evt.target = bin;
        box.trigger( evt, [v, n, t] );

        bin.addClass("picksel")
        .toggleClass("pickmul", mul )
        .on("change"  , ".checkone", select)
        .on("click"   , ".commit"  , commit)
        .on("saveBack", ".create"  , create);

        var ids = Object.keys( v );
        var num = ids.length ;
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

        var ckd = chk.prop("checked");
        var val = chk.val ();
        var txt;
        var inf;

        if (ckd) do {
            // 获取选项名称和附加信息
            txt = chk.attr("title") || "" ;
            inf = chk.hsData();

            // 尝试从当前行的列中获取
            var thd = chk.closest("table").find("thead");
            var tds = chk.closest( "tr"  ).find( "td"  );
            var idx;
            var tit;

            idx = thd.find("[data-fn='"+tk+"']").index();
            if (idx !== -1) tit = tds.eq(idx).text();
            if (tit) {txt = tit ; break;}

            idx = thd.find("[data-ft='"+tk+"']").index();
            if (idx !== -1) txt = tds.eq(idx).text();
            if (tit) {txt = tit ; break;}
        }
        while (false);

        if (pickItem(val, txt, inf, chk) === false ) {
            return false;
        }
    }

    function create () {
        var btn = jQuery(this);
        if (btn.closest(".HsList" ).data("HsList")._info) {
            return;
        }
        if (btn.closest(".openbox"). is ( bin ) !== true) {
            return;
        }

        var evt = arguments[0];
        var rst = arguments[1];

        if (rst.list) {
            rst = rst.list [0];
        } else
        if (rst.info) {
            rst = rst.info ;
        } else
        if (rst.id && at ) {
            /**
             * 新方案创建接口仅返回id
             * 需调用详情接口获取数据
             */
            $.hsAjax({
                url : hsSetParam(at, vk, rst.id ),
                cache : false,
                rsync : false,
                dataType:  "json"  ,
                complete:  function (rst) {
                    rst = hsResponse(rst);
                    create.call(btn[0], evt, rst);
                }
            });
            return;
        } else {
            return;
        }

        var val = rst[vk] || hsGetValue(rst, vk );
        var txt = rst[tk] || hsGetValue(rst, tk );

        if (pickItem(val, txt, rst) === false ) {
            return false;
        }
    }

    function commit () {
        var btn = jQuery(this);
        if (btn.closest(".HsList" ).data("HsList")._info) {
            return;
        }
        if (btn.closest(".openbox"). is ( bin ) !== true) {
            return;
        }

        pickBack ( );
        return false;
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
    var rol = box.is  (".pickrol")
        || !! box.data("readonly");
    var mul = box.is  (".pickmul")
        || !! box.data("repeated")
        || !! box.data("multiple")
        || /(\[\]|\.\.|\.$)/.test(n);
    var btn = box.siblings("[data-toggle=hsPick],[data-toggle=hsFork]");
    if (btn.size() === 0) {
        btn = box.children("[data-toggle=hsPick],[data-toggle=hsFork]");
    }

    box.toggleClass ("pickmul", mul);

    // 表单初始化载入时需从关联数据提取选项对象
    if (this._info) {
        var ak = box.attr("data-ak") || "data";
        var av = this._info[ak] || hsGetValue(this._info, ak);
        if (v == av && n == ak) { // 关联键同名
            if (! v ) return ;
            if (!mul) v = [v];
        } else
        if (! jQuery.isPlainObject(v) ) {
            v  = av ;
            if (! v ) return ;
            if (!mul) v = [v];
        }
    }

    // 对表单初始化数据转换为关联组件的字典格式
    if (jQuery.isArray(v)) {
        var tk = box.attr("data-tk") || "name";
        var vk = box.attr("data-vk") ||  "id" ;
        var v2 = {};
        for(var i = 0; i < v.length; i++) {
            var j = v[ i];
            if (!jQuery.isPlainObject(j)
            &&  !jQuery.isArray      (j)) {
                continue; // 规避关联被删除只剩 ID 的情况
            }
            var v3= j[vk] || hsGetValue(j, vk);
            var t3= j[tk] || hsGetValue(j, tk);
            if (v3 !== undefined
            &&  t3 !== undefined) {
                v2[v3] = [t3 , j];
            }
        }
        v = v2;
    } else
    if (! v ) {
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
        var tms = "&times;";
        jQuery('<li class="' + cls[0] + '"></li>')
           .append(jQuery('<span class="' + cls[1] + '"></span>' ))
           .append(jQuery('<span class="close"></span>').html(tms))
           .append(jQuery('<span class="title"></span>').text(txt))
           .append(jQuery('<input type="hidden">').attr("name", n).val(val))
           .attr  ("title" , txt)
           .appendTo(box);
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
            box.on("click", "li:not([data-toggle])", [btn, box], function(evt) {
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
            cls[0] = rol ? "btn btn-link"  : "btn btn-info" ;
            cls[1] = rol ? "bi bi-hi-fork" : "bi bi-hi-fork";
        } else {
            cls[0] = rol ? "btn btn-text"  : "btn btn-info" ;
            cls[1] = rol ? "bi bi-hi-fork" : "bi bi-hi-fork";
        }
        cls[0] = box.attr("data-item-class") || cls[0];
        cls[1] = box.attr("data-icon-class") || cls[1];

        box.children().not("[data-toggle]" ).remove( );
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
    var box = cel. closest (".picksel" );
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

/**
 * 列表跨页全选
 * @param {HsList} listObj
 * @param {Number} bn 起始页码, 默认 1
 * @param {Number} qn 最多页数, -1 不限
 * @param {Number} rn 单页数量, -1 不设
 * @param {Function} pf 进度回调函数(页码, 总数)
 */
function hsListPickMore(listObj, bn, qn, rn, pf) {
    if (rn === undefined) rn = -1;
    if (qn === undefined) qn = -1;
    if (pf === undefined) pf = function() {};

    var vk  = listObj._id_key ||  "id" ;
    var tk  = listObj._tt_key || "name";
    var url = listObj._url ;
    var dat = listObj._data;
    var div = jQuery('<div class="invivisible"></div>');
    var chk = jQuery('<input type="checkbox" class="checkone" checked="checked"/>');

    listObj.context.append(div);
    dat = hsSerialMix ({}, dat);

    var sel = function(pn) {
        if (rn > -1) {
            dat[listObj.rowsKey] = rn;
        }   dat[listObj.pageKey] = pn;

        listObj.ajax({
            "url"      : url,
            "data"     : dat,
            "type"     : "POST",
            "dataType" : "json",
            "funcName" : "pick",
            "async"    : true ,
            "cache"    : false,
            "global"   : false,
            "context"  : listObj,
            "complete" : function(rst) {
                try {
                    rst = hsResponse(rst);
                } catch (e) {
                    pf ( 0 , 0 );
                    div.remove();
                    return ;
                }

                if (! rst.ok
                ||  ! rst.list
                ||  ! rst.list.length) {
                    pn = pn - 1 ;
                    pf ( pn,pn );
                    div.remove();
                    return ;
                }

                for(var i = 0; i < rst.list.length; i ++) {
                    var a = rst.list [i];
                    var c = chk.clone( );
                    c.data(a);
                    c.attr("value", a[vk]);
                    c.attr("title", a[tk]);
                    c.appendTo(div);
                    c.trigger ( "change" );
                    c.remove  (   );
                }

                var gn = rst.page && rst.page.total || 0;
                if (gn > 0 && gn <= pn) {
                    pf ( pn,pn );
                    div.remove();
                    return ;
                }
                if (qn > 0 && qn <= pn) {
                    pf ( pn,pn );
                    div.remove();
                    return ;
                }

                if (gn > 0 && qn > 0) {
                    gn = Math.min(gn, qn);
                } else {
                    gn = Math.max(gn, qn);
                }

                pf (pn, gn);
                sel(pn + 1);
            }
        });
    };
    sel(bn > 0 ? bn : 1);
}

(function($) {
    $(document)
    .on("click", "[data-toggle=hsPick],[data-toggle=hsFork]",
    function() {
        var url = $(this).data( "href" )
               || $(this).attr( "href" );
        var bin = $(this).data("target"); // 选择区域
        var box = $(this).data("result"); // 填充区域

        if (bin) {
            bin = $(this).hsFind(bin);
        }
        if (box) {
            box = $(this).hsFind(box);
        } else
        {
            box = $(this).siblings("[name],[data-fn],[data-ft]")
               .not(".form-ignored" );
        if (! box.size()) {
            box = $(this). parent ( );
        }}

        $(this).hsPick(url, bin, box);
        return false;
    });
})(jQuery);

// 别名
jQuery.fn.hsFork = jQuery.fn.hsPick;
hsFormFillFork = hsFormFillPick;
hsListFillFork = hsListFillPick;
