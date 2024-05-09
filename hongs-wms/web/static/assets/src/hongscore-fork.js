
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
 * <input name="xx_id" type="hidden" data-topple="hsForkInit" data-ln="xx" data-vk="id" data-tk="name" data-pick-href="xx/pick.html" data-pick-target="@" data-link-href="xx/info.html" data-link-target="@"/>
 *
 * 子表单加载构建用法:
 * 在表单配置区域添加:
 * data--1="_feed__form:(hsFormFeedFart)"
 * data--2="_fill__form:(hsFormFillFart)"
 * 在表单分组区域添加:
 * <div class="form-subs" data-ft="_form" data-fn="xxxx" data-href="xxxx/form.html"></div>
 * <div class="row form-sub-add hide">
 *   <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
 *     <button type="button" class="btn btn-default" data-toggle="hsFormSubAdd">添加</button>
 *   </div>
 * </div>
 * <div class="row form-sub-del hide">
 *   <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
 *     <button type="button" class="btn btn-default" data-toggle="hsFormSubDel">删除</button>
 *   </div>
 * </div>
 *
 * 2015/11/30 原 hsPick 更名为 hsFork (Hong's Foreign Key kit)
 * 2024/04/20 增加子表单加载绑定设置方法
 * 2024/05/01 增加控件自动构建和设置方法
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
    var btn  = jQuery(this);
    if (bin) bin = btn.hsFind(bin);
    if (box) box = btn.hsFind(box);

    var v    = { };
    var n    = box.attr("data-fn") || "";
    var t    = box.attr("data-ft") || "";
    var vk   = box.attr("data-vk") || "id";
    var tk   = box.attr("data-tk") || "name";
    var at   = box.attr("data-at");
    var mul  = box.is( ".pickmul")
            || box.is("[data-multiple]")
            || box.is("[data-repeated]")
            || /(\[\]|\.\.|\.$)/.test(n);
    var foo  = box.closest(".HsForm").data("HsForm") || {};

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

jQuery.fn.hsPickInit = function() {
    var  $  = jQuery ;
    var inp = $(this);
    var box = inp.siblings("ul");
    if (! box.size()) {
        box = $(
            '<div  class="form-control labelbox">'
          +   '<ul class="repeated forkbox"></ul>'
          +   '<a href="javascript:;"></a>'
          + '</div>'
        );
        box.insertBefore(inp).prepend(inp);
        if (! inp.is(":hidden") )
        inp.addClass("invisible");

        var lis = box.children("ul");
        var lnk = box.children("a" );

        // 选取
        lnk.attr("data-toggle", "hsFork");
        lnk.attr("data-href"  , inp.attr("data-pick-href"  ) || "");
        lnk.attr("data-target", inp.attr("data-pick-target") || "");
        lnk.text( inp.attr("placeholder") || hsGetLang ("fork.select") );

        // 填充和校验
        lis.attr("data-fn"    , inp.attr("name"   ) || "");
        lis.attr("data-ln"    , inp.attr("data-ln") || "");
        lis.attr("data-vk"    , inp.attr("data-vk") || "");
        lis.attr("data-tk"    , inp.attr("data-tk") || "");
        lis.attr("data-at"    , inp.attr("data-at") || "");
        lis.attr("data-ft"    , inp.attr("data-form-ft"    ) || "_fork");
        lis.attr("data-href"  , inp.attr("data-link-href"  ) || "");
        lis.attr("data-target", inp.attr("data-link-target") || "");
        if (inp.attr("data-minrepeat")) {
            lis.attr("data-minrepeat" , inp.attr("data-maxrepeat"));
        }
        if (inp.attr("data-maxrepeat")) {
            lis.attr("data-maxrepeat" , inp.attr("data-maxrepeat"));
        }

        // 必选多选和只读等
        if (inp.is("[required]")) {
            lis.attr("data-required", "required");
        }
        if (inp.is("[multiple]")) {
            lis.attr("data-multiple", "multiple");
        }
        if (inp.is("[readonly]")) {
            lis.attr("data-readonly", "readonly");
            box.find("a").remove( );
        }

        inp.removeAttr("required" );
        inp.removeAttr("placeholder");
        inp.removeAttr("data-toggle");
    }
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
    var rol = box.is( ".pickrol" )
           || box.is("[data-readonly]");
    var mul = box.is( ".pickmul" )
           || box.is("[data-multiple]")
           || box.is("[data-repeated]")
           || /(\[\]|\.\.|\.$)/.test(n);

    var tmp = box.data("template");
    if (! tmp) {
        tmp = box.children(".template")
              .detach ( )
              .removeClass( "template");
        if (! tmp.size()) {
            //throw new Error("hsFormFillPick: template not found");
            tmp = jQuery(
                '<li  class="label">'
              +   '<a class="erase pull-right bi bi-x" href="javascript:;"></a>'
              +   '<span  class="title"></span>'
              +   '<input class="value" type="hidden"/>'
              + '</li>'
            );
            if (! rol) {
                var nam = box.data( "fn" );
                tmp.addClass("label-info")
                   .find(".value").attr("name", nam);
            } else {
                tmp.addClass("label-default")
                   .find(".value, .erase").remove( );
            }
        }
        box.data("template",tmp);
    }
    box.children().not("[data-toggle]").remove();

    var btn = box.children("[data-toggle=hsPick],[data-toggle=hsFork]");
    if (btn.size() === 0) {
        btn = box.siblings("[data-toggle=hsPick],[data-toggle=hsFork]");
    }

    // 表单初始化载入时需从关联数据提取选项对象
    if (this._info) {
        var ln = box.attr("data-ln") || "data";
        var av = this._info[ln] || hsGetValue(this._info, ln);
        if (v == av && n == ln) { // 关联键同名
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

    if (! jQuery.isEmptyObject(v) ) {
        if (! mul) btn.hide();
    } else {
        if (! rol) btn.show();
    }

    // 逐条写入已选项, .erase,.title,.value 用于标识删除,名称,取值
    for(var val in v) {
        var arr  = v[val];
        var txt  = arr[0];
        var ent  = tmp.clone().appendTo(box);
        ent.find(".title").text(txt);
        ent.find(".value").val (val);
        ent.attr( "title", txt );
    }

    // 初始化绑定事件, 处理删除和查看
    if (! box.data("pickInited")) {
        box.data("pickInited", 1);
        box.on("click", ".erase,.close", [btn, box], function(evt) {
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

/**
 * 子表单登记选项
 * @param {Element} box
 * @param {Object} v
 * @param {String} n
 */
function hsFormFeedPart(box, v, n) {
    if (! n ) n = box.data( "fn" );
    var rol = box.data("readonly");
    var mul = box.data("repeated")
        || !! box.data("multiple")
        || /(\[\]|\.\.|\.$)/.test(n);
    n = n.replace(/(\[\]|\.)$/ , "");

    var htm = box.siblings(".form-sub");
    var btr = box.siblings(".form-sub-del");
    var btn = box.siblings(".form-sub-add");
        htm . detach();
    if (rol || ! mul ) {
        btn . remove();
        btr . remove();
        btn = jQuery();
        btr = jQuery();
    } else {
        btn.show( ).removeClass("hide");
        btn.find("[data-toggle=hsFormSubAdd]").data("docket", box);
    }

    if (htm.size()) {
        box.data("html", htm);
    }
    box.data("name", n);
    box.data("enfo", v);
}

/**
 * 子表单填充数据
 * @param {Element} box
 * @param {Object} v
 * @param {String} n
 */
function hsFormFillPart(box, v, n) {
    if (! n ) n = box.data( "fn" );
    var rol = box.data("readonly");
    var mul = box.data("repeated")
        || !! box.data("multiple")
        || /(\[\]|\.\.|\.$)/.test(n);
    n = n.replace(/(\[\]|\.)$/ , "");

    var htm = box.find(".form-sub.hide");
    var btr = box.siblings(".form-sub-del");
    var btn = box.siblings(".form-sub-add");
        htm . detach();
    if (rol || ! mul ) {
        btn . remove();
        btr . remove();
        btn = jQuery();
        btr = jQuery();
    } else {
        btn.show( ).removeClass("hide");
        btn.find("[data-toggle=hsFormSubAdd]").data("docket", box);
    }

    var  m  = box.data("enfo")|| {  };
    var  o  = box.data("form")|| this;
    var add = box.data("add") || function(k, w) {
        var mod = jQuery.extend( {},o );
        var htx =  htm  . clone( true );
        var btx =  btr  . clone( true );
        htx.show( ).removeClass("hide");
        btx.show( ).removeClass("hide");

        // 字段增加前缀
        htx.find(".form-field,.form-group,[data-fn],[data-ln],input[name],select[name],textarea[name]")
           .each(function() {
            var l, inp = jQuery(this);
            l = inp.attr("name");
            if (l) {
                l = k + "." + l ;
                inp.attr("name", l);
            }
            l = inp.data("name");
            if (l) {
                l = k + "." + l ;
                inp.data("name", l);
                inp.attr("data-name", l);
            }
            l = inp.data( "fn" );
            if (l) {
                l = k + "." + l ;
                inp.data( "fn" , l);
                inp.attr( "data-fn" , l);
            }
            l = inp.data( "ln" );
            if (l) {
                l = k + "." + l ;
                inp.data( "ln" , l);
                inp.attr( "data-ln" , l);
            }
        });

        // 数据增加前缀
        var enf = {};
        hsSetValue(enf, k, m);
        var inf = {};
        hsSetValue(inf, k, w);

        // 填充数据
        mod.context= htx ;
        mod.formBox= htx ;
        btx.appendTo(htx);
        htx.appendTo(box);
        htx.hsReady (   );
        mod.fillEnfo(enf);
        mod.fillInfo(inf);
    };
    var set = box.data("set") || function(n, v) {
      var i = box.data("idx") || 0;
        if (! mul) {
            v = v || {};
            add(n , v );
            box.data("idx", i);
        } else {
            v = v || [];
            for(var j = 0 ; j < v.length ; j ++) {
                add(n +"."+ j , v[j]);
                box.data("idx", i ++);
            }
        }
    };
    var put = function (n, v) {
        // 模板为松散多块的需要进行包裹
        if (htm.size() !== 1) {
            htm = jQuery('<div></div>').append(htm);
        }

        // 模板内部存在删除按钮则不再加
        var btx = htm.find("[data-toggle=hsFormSubDel]");
        if (btx.size() !== 0) {
            btr . remove();
            btr = jQuery();
        }

        // 外部有指定样式则进行替换绑定
        var cls = box.data("subClass");
        if (cls) {
            htm.attr("class",cls);
        }
        var sty = box.data("subStyle");
        if (sty) {
            htm.attr("style",sty);
        }

        htm.addClass("form-sub" );
        box.addClass("form-subs");
        box.data("html", htm);
        set(n, v);
    };

    box.data("add" , add);
    box.data("set" , set);
    box.data("name",  n );
//  box.data("info",  v );

    if (box.data("href")) {
        jQuery.hsAjax({
            url  : box.data("href"),
            async: true ,
            cache: true ,
            type : "get",
            dataType: "html",
            success : function(dom) {
                dom = jQuery  (dom);

                // 特殊情况可完全由外部定制
                htm = dom.find(".form-subs").first();
                if (htm.size( )) {
                    box.removeClass ("form-subs");
                    box.addClass ("loadbox");
                    box.data("info", v);
                    box.append(dom);
                    dom.hsReady ( );
                    btn.remove();
                    btr.remove();
                    return;
                }

                // 一般情况仅取子表单一部分
                htm = dom.find(".form-body").first();
                htm = htm.size() ? htm : dom;
                htm.removeClass("form-body");
                put(n, v);
            }
        });
    } else
    if (box.data("html")) {
        htm = box.data("html");
        htm = jQuery  ( htm  );
        put(n, v);
    } else {
        put(n, v);
    }
}

/**
 * 子表单常规校验
 * @param {Element} box
 */
function hsFormTestPart(box) {
    var m = 0;
    if (box.data("required")) {
        if (m === box.children().size()) {
            return this.getError(box, "form.required");
        }
    }
    m = parseInt(box.data("maxrepeat") || 0);
    if (m > 0) {
        if (m < box.children().size()) {
            return this.getError(box, "form.gt.maxrepeat", [m]);
        }
    }
    m = parseInt(box.data("minrepeat") || 0);
    if (m > 0) {
        if (m > box.children().size()) {
            return this.getError(box, "form.gt.minrepeat", [m]);
        }
    }
    return true;
}

(function($) {
    $(document)
    .on("hsReady", function(e) {
        $(e.target)
        .find("[data-toggle=hsPickInit],[data-toggle=hsForkInit]")
        .each(function() {
            $(this).hsPickInit();
        });
    })
    .on("click", "[data-toggle=hsPick],[data-toggle=hsFork]",
    function() {
        var url = $(this).data( "href" )
               || $(this).attr( "href" );
        var bin = $(this).data("target"); // 选择区域
        var box = $(this).data("docket"); // 填充区域

        if (bin) {
            bin = $(this).hsFind(bin);
        }
        if (box) {
            box = $(this).hsFind(box);
        } else {
            box = $(this).siblings("[data-fn],[data-ft]");
        if (! box.size()) {
            box = $(this). parent ( );
        }}

        $(this).hsPick(url, bin, box);
        return false;
    })
    .on("click", "[data-toggle=hsFormSubAdd]",
    function() {
        var box = $(this).data("docket"); // 填充区域
        if (box) {
            box = $(this).hsFind(box);
        } else {
            box = $(this).siblings("[data-fn],[data-ft]");
        if (! box.size()) {
            box = $(this). parent ( );
        }}

        var  n  = box.data("fn" ) || "";
        var  i  = box.data("idx") || 0 ;
        var add = box.data("add");
             n  = n.replace(/(\[\]|\.)$/, ""); // 去掉数组后缀
        if (add) {
            add( n +"."+ i, { } );
            box.data("idx", i+1 );
            box.trigger("change");
        } else {
            throw new Error ("hsFormSub add function required!");
        }
    })
    .on("click", "[data-toggle=hsFormSubDel]",
    function() {
        var subs = $(this).closest(".form-subs");
        var sub  = $(this).closest(".form-sub" );
        sub .remove ( /****/ );
        subs.trigger("change");
    });
})(jQuery);

// 别名
hsFormFillFork = hsFormFillPick;
hsListFillFork = hsListFillPick;
jQuery.fn.hsFork = jQuery.fn.hsPick;
jQuery.fn.hsForkInit = jQuery.fn.hsPickInit;
