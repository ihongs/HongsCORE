
/* global HsList */

/**
 * 组合选择工具之用法:
 * 在选择列表配置添加:
 * data-data-0="_fill__fork:(hsForkFillList)"
 * 在选择列表头部添加:
 * <td data-ft="_fork"><input type="checkbox" class="checkall"/></td>
 * 在表单配置区域添加:
 * data-data-0="_fill__fork:(hsForkFillForm)"
 * 在表单选项区域添加:
 * <ul data-ft="_fork" data-fn="my_example" required="required"></ul>
 * <button type="button" data-toggle="hsFork" data-target="@" data-href="my_example/fork.html">Pick It!</button>
 *
 *注: 2015/11/30 原 hsPick 更名为 hsFork(Hong's Foreign Key kit)
 **/

/**
 * 选择控件
 * @param {String} url 要打开的选择页地址
 * @param {jQuery} tip 在哪打开
 * @param {jQuery} box 在哪填充
 * @param {Function} fil 填充函数
 * @returns {jQuery}
 */
jQuery.fn.hsPick = function(url, tip, box, fil) {
    if (fil == undefined
    &&  typeof url == "function") {
        fil  = url;
        url  = tip;
        tip  = null;
    } else if (url == undefined ) {
        url  = tip;
        tip  = null;
    }

    var v    = { };
    var n    = box.attr("name")||box.attr("data-fn");
    var vk   = box.attr("data-vk") ||  "id" ;
    var tk   = box.attr("data-tk") || "name";
    var form = box.closest(".HsForm").data("HsForm");
    var mul  = /(\[\]|\.\.|\.$)/.test(n);
    var btn  = jQuery(this);

    if (! fil) {
        do {
            fil = form["_fill_"+ n];
            if (fil) break;

            var t;

            t = box.attr("data-ft");
            fil = form["_fill_"+ t];
            if (fil) break;

            t = box.attr("data-fn");
            fil = form["_fill_"+ t];
            if (fil) break;

            fil = hsFormFillPick;
        } while (false);
    }

    if (box.is("input")) {
        var val = box.val( );
        var txt = btn.text();
        if (val) {
            v[val] = txt;
        }
    } else {
        box.find("li").each(function() {
            var opt = jQuery(this);
            var val = opt.find(".pickval").val ();
            var txt = opt.find(".picktxt").text();
            v[val] = txt;
        });
    }

    function pickItem(val, txt) {
        var evt = jQuery.Event("pickItem");
        box.trigger( evt, arguments );
        if (evt.isDefaultPrevented()) {
            return false;
        }

        if (! mul) {
            for( var  key  in  v )
                delete v[key];
            if (txt !== undefined)
                v[val] = txt ;
        } else {
            if (txt !== undefined)
                v[val] = txt ;
            else
                delete v[val];
        }
        return true;
    }

    function pickBack() {
        // 未选警告
        if (jQuery.isEmptyObject(v)) {
            var msg  = box.data("unpickedError" );
            if (msg != "!") {
                alert( msg  ?  hsGetLang(  msg  ):
                      hsGetLang("fork.unpicked"));
                return;
            }
        }

        var evt = jQuery.Event("pickBack");
        box.trigger(evt, [v, tip]);
        if (evt.isDefaultPrevented()) {
            return false;
        }

        fil.call(form, box , v, n, "data");
        box.trigger("change");
        return true;
    }

    function pickOpen() {
        var tip = jQuery(this);
        tip.data("pickData", v)
           .addClass("pickbox")
        .toggleClass("pickmul", mul)
        .on("change"  , ".checkone", checks)
        .on("click"   , ".ensure"  , ensure)
        .on("saveBack", ".create"  , create);
        // 初始选中
        tip.find(".checkone").val(Object.keys(v));
    };

    function checks() {
        var chk = jQuery(this);
        if (chk.closest(".HsList").data("HsList")._info) {
            return;
        }
        if (chk.closest(".openbox").is ( tip ) == false) {
            return;
        }

        var val = chk.val();
        var txt;
        var inf;

        do {
            if (! chk.prop("checked") ) break;

            // 获取额外数据
            inf = chk.data();
            if (! inf  ) {
                inf = chk.attr("data-data");
                if (inf) {
                    inf = eval('('+inf+')');
                }  else  {
                    inf = null;
                }
            }

            txt = chk.attr("data-name");
            if (txt) break;
            txt = chk.closest("tr").find(".name").text();
            if (txt) break;

            var thd = chk.closest("table").find("thead");
            var tds = chk.closest( "tr"  ).find( "td"  );
            var idx;

            idx = thd.find("[data-ft=name]").index();
            if (idx != -1) txt = tds.eq(idx).text( );
            if (txt) break;
            idx = thd.find("[data-fn=name]").index();
            if (idx != -1) txt = tds.eq(idx).text( );
        }
        while (false);

        if (pickItem( val, txt, inf  ) === false) {
            chk.prop("checked", false);
            return false;
        }
    }

    function ensure() {
        var btn = jQuery(this);
        if (! btn.closest(".openbox").is(tip)) {
            return;
        }

        if (! pickBack()) {
            return false;
        }

        tip.hsClose();
        return false ;
    }

    function create(evt, rst) {
        var btn = jQuery(this);
        if (! btn.closest(".openbox").is(tip)) {
            return;
        }

        if (! rst || ! rst.info /*||!rst.info[vk]*/) {
            return false;
        }
        if (! pickItem(rst.info[vk], rst.info[tk]) ) {
            return false;
        }
        if (! pickBack()) {
            return false;
        }

        tip.hsClose();
        return false ;
    }

    if (tip) {
        tip =    tip.hsOpen(url, undefined, pickOpen);
        tip.data( "rel", btn.closest(".openbox")[0] );
    } else {
        tip = jQuery.hsOpen(url, undefined, pickOpen);
    }

    return tip;
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
    // 注意: 绑定当前函数用于选择后的填充
    box.data("pickFunc", hsFormFillPick);

    var btn = box.siblings("[data-toggle=hsPick],[data-toggle=hsFork]");
    var mul = /(\[\]|\.\.|\.$)/.test(n); // a[b][]|a[][b]|a.b.|a..b 均表示多选

    if (t == "info") {
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
            x[j[vk]] = j[tk];
        }
        v = x ;
    } else if (! jQuery.isPlainObject(v)) {
        v = {};
    }

    if (box.is("input") ) {
        function reset(box, btn) {
            var txt = btn.data("txt");
            var cls = btn.data("cls");
            box.val ( "");
            btn.text(txt);
            btn.attr( "class" , cls );
        }
        function inset(box, btn, val, txt) {
            box.val (val);
            btn.text(txt);
            btn.addClass("btn-success");
            btn.append('<a href="javascript:;" class="close">&times;</a>');
        }

        if (! btn.data("pickInited"))  {
            btn.data("pickInited", 1);
            btn.data("txt", btn.text( ) );
            btn.data("cls", btn.attr("class"));
            btn.on("click", ".close", box, function(evt) {
                var btn = jQuery(evt.delegateTarget);
                var box = evt.data;
                reset(box, btn);
                box.trigger("change");
                return false;
            });
        }

        if (jQuery.isEmptyObject(v)) {
            reset(box, btn);
        } else
        for(var val in v) {
            var txt  = v[val];
            inset(box, btn, val,txt);
        }
    } else {
        if (! box.data("pickInited"))  {
            box.data("pickInited", 1);
            box.on("click", ".close", btn, function(evt) {
                var opt = jQuery(this).closest("li");
                var val = opt.find(":hidden").val( );
                var btn = evt.data;
                delete v[val];
                opt.remove( );
                btn.show  ( );
                box.trigger("change");
                return false ;
            });
            if (! mul) {
                box.on("click", null, btn, function(evt) {
                    evt.data.click();
                });
            }
        }

        if (jQuery.isEmptyObject(v)) {
            btn.show();
        } else if (! mul) {
            btn.hide();
        }

        box.empty();
        for(var val in v) {
            var txt  = v[val];
            box.append(jQuery('<li class="btn btn-success form-control"></li>').attr("title", txt )
               .append(jQuery('<input class="pickval" type="hidden"/>').attr( "name", n ).val(val))
               .append(jQuery('<span  class="picktxt"></span>').text(   txt   ))
               .append(jQuery('<span  class="close pull-right">&times;</span>'))
            );
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
    var box = cel.closest(".pickbox");
    var mul = box.hasClass("pickmul");
    var dat = box.data("pickData") || {};

    // 单选还是多选
    if (! mul) {
        box.find(".checkall").hide( );
    }

    // 填充选择控件
    if (! mul) {
        HsList.prototype._fill__radio.call( this, cel, v, n );
    } else {
        HsList.prototype._fill__check.call( this, cel, v, n );
    }

    // 判断是否选中
    if (dat[v] !== undefined) {
        cel.find(".checkone").prop("checked", true).change( );
    }
}

(function($) {
    $(document)
    .on("click", "[data-toggle=hsPick],[data-toggle=hsFork]",
    function() {
        var url = $(this).attr("data-href") || $(this).attr("href");
        var box = $(this).attr("data-target");
        if (box) {
            box = $(this)._hsTarget(box);
        } else {
            box =   null ;
        }

        // 填充区域
        var inp = $(this).attr("data-fill-area");
        if (inp) {
            inp = inp.charAt(0) == ">" ? $(inp , this) : $(inp);
        } else {
            inp = $(this).siblings("[name],[data-fn]");
        }

        // 填充函数
        var fun = $(this).attr("data-fill-func");
        if (fun) {
            fun = eval ( '(' + fun + ')' );
        }

        $(this).hsPick(url, box, inp, fun);
        return false;
    });
})(jQuery);

// 2015/11/30 原 hsPick 更名为 hsFork
jQuery.fn.hsFork = jQuery.fn.hsPick;
hsFormFillFork = hsFormFillPick;
hsListFillFork = hsListFillPick;
hsForkFillForm = hsFormFillPick;
hsForkFillList = hsListFillPick;

/**
 * 文件上传辅助工具
 * @param {jQuery.class} $
 */
(function($) {
    /**
     * 文件校验
     * @param {Object} cnf
     */
    $.fn.hsFileVali = function(cnf) {
        // 以后补充
    };

    //** 选择文件 **/

    /**
     * 选取文件
     * @param {Element} box 文件列表区域
     * @param {String } src 文件路径取值
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFilePick = function(box, src) {
        if (! src) {
            return ;
        }
        var txt = src.replace(/^.*[\/\\]/, '');
        var inp =$(this).clone( );
        var grp =$(this).parent();
            box =$(box );
        var div =$('<li class="btn btn-success form-control"></li>').attr("title" , txt )
           .append(this)
           .append('<span class="glyphicon glyphicon-file"></span>')
           .append($('<span  class="picktxt"></span>').text( txt ) )
           .append('<span  class="close pull-right">&times;</span>');
        box.append(div );
        grp.append(inp );
        inp.val("");
        return div ;
    };

    /**
     * 填充文件
     * @param {String } nam 文件字段名称
     * @param {String } src 文件路径取值
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFillFile = function(nam, src) {
        if (! src) {
            return ;
        }
        var txt = src.replace(/^.*[\/\\]/, '');
        var box =$(this);
        var div =$('<li class="btn btn-success form-control"></li>').attr("title" , txt )
           .append($('<input class="pickval" type="hidden"/>').attr('name',nam).val(src))
           .append('<span class="glyphicon glyphicon-file"></span>')
           .append($('<span  class="picktxt"></span>').text( txt ) )
           .append('<span  class="close pull-right">&times;</span>');
        box.append(div );
        return div ;
    };

    //** 预览图片 **/

    /**
     * 预载文件
     * @param {Function} cal 回调函数
     * @returns {jQuery} 当前文件节点
     */
    $.fn.hsFileLoad = function(cal) {
        this.each(function() {
            var that = this;
            if (window.FileReader) {
                var fr = new FileReader( );
                fr.onloadend = function(e) {
                    cal.call(that, e.target.result);
                };  cal.call(that);
                $.each( this.files, function(i, fo) {
                    fr.readAsDataURL( fo );
                });
            } else
            if (this.getAsDataURL) {
                cal.call(that, that.getAsDataURL());
            } else {
                cal.call(that, that.value);
            }
        });
        return this;
    };

    /**
     * 预览文件
     * @param {Element} box 预览列表区域
     * @param {String } src 文件路径取值
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFileView = function(box, src, w, h) {
        if (! src) {
            return ;
        }
            box =$(box );
        var grp =$(this).parent();
        var inp =$(this).clone().val("");
        var img =$.hsMakeView(src, w, h);
        img.css("positoin" , "absolute");
        var div =$('<li class="preview"></li>').css({
            width:w+'px', height:h+'px', overflow:'hidden', position:'relative'
        } ).append(this)
           .append(img )
           .append('<a href="javascript:;" class="close pull-right">&times</a>');
        box.append(div );
        grp.append(inp );
        return div ;
    };

    /**
     * 填充预览
     * @param {String } nam 文件字段名称
     * @param {String } src 文件路径取值
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFillView = function(nam, src, w, h) {
        if (! src) {
            return ;
        }
        var box =$(this);
        var img =$.hsMakeView(src, w, h);
        img.css("positoin" , "absolute");
        var div =$('<li class="preview"></li>').css({
            width:w+'px', height:h+'px', overflow:'hidden', position:'relative'
        } ).append($('<input type="hidden" />').attr( "name", nam ).val( src ) )
           .append(img )
           .append('<a href="javascript:;" class="close pull-right">&times</a>');
        box.append(div );
        return div ;
    };

    /**
     * 构建预览
     * @param {String } src 文件真实路径
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @returns {Element}
     */
    $.hsMakeView = function(src, w, h) {
        var img  =  new Image();
        img.onload = function() {
            var xw = img.width ;
            var xh = img.height;
            var zw = xh * w / h;
//          var zh = xw * h / w;
            if (zw > xw) {
                // 宽度优先
                img.width   = w;
                img.height  = xh * w / xw;
                $(img).css("top" , (((h - img.height) / 2)) + "px");
            } else {
                // 高度优先
                img.height  = h;
                img.width   = xw * h / xh;
                $(img).css("left", (((w - img.width ) / 2)) + "px");
            }
        };
        img.src = src ;
        return  $(img);
    };

    /**
     * 移除文件事件处理
     */
    $(document).on("click", ".forkbox .close,.pickbox .close,.preview .close",
    function( ) {
        $(this).parent().remove();
    });
})(jQuery);
