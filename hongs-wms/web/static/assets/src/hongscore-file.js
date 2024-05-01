/**
 * 文件上传及预览用法:
 * 在表单配置区域添加:
 * data--0="_fill__file:(hsFormFillFile)"
 * 在表单选项区域添加:
 * <input name="x_url" type="hidden"/>
 * <input name="x_url" type="file" data-toggle="hsFileInit"/>
 * 图片选取预览可添加:
 * <input name="x_url" type="hidden"/>
 * <input name="x_url" type="file" data-toggle="hsViewInit" data-size="300&times;300" data-mod="keep"/>
 *
 * 2016/06/10 增加文件上传及图片预览工具
 * 2024/05/01 增加控件自动构建和设置方法
 */

(function($) {

    function _fileTemp(box) {
        var tmp = box.data("template");
        if (tmp) {
            return tmp;
        }

        tmp = box.children(".template")
              .detach()
              .removeClass( "template");
        if (tmp.size()) {
            box.data( "template", tmp );
            return tmp;
        }

        tmp = $(
            '<li  class="label">'
          +   '<a class="erase pull-right bi bi-x" href="javascript:;"></a>'
          +   '<i class="icon  pull-left "></i>'
          +   '<span  class="title"></span>'
          + '</li>'
        );
        if (box.is("[data-readonly]")) {
            tmp.addClass("label-default")
               .find(".erase, .icon" ).remove( );
        } else {
            var nam = box.data( "fn" );
            tmp.addClass("label-info")
               .find(".value").attr("name", nam);
        }
        box.data("template", tmp);
        return tmp;
    }

    function _viewTemp(box) {
        var tmp = box.data("template");
        if (tmp) {
            return tmp;
        }

        tmp = box.children(".template")
              .detach()
              .removeClass( "template");
        if (tmp.size()) {
            box.data( "template", tmp );
            return tmp;
        }

        tmp = $(
            '<li class="preview" style="overflow: hidden;">'
          +   '<a class="close" style="z-index: 1;" href="javascript:;">&times;</i>'
          + '</li>'
        );
        if (box.is("[data-readonly]")) {
            tmp.find(".close").remove( );
        }
        box.data("template", tmp);
        return tmp;
    }

    /**
     * 选取文件
     * @param {Element} box 文件列表区域
     * @param {String } src 文件路径取值
     * @returns {undefined|Element} 节点
     */
    $.fn.hsPickFile = function(box, src) {
        if (! src ) {
            return;
        }
            box = $(box );
        var inp = $(this);
        inp.after(inp.clone().val('')) ;
        var txt = /^data:/.test( src ) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        if (txt === '') { // 通过文件对象获取文件名称
            if (this[0].filez && this[0].filez.length) txt = this[0].filez[0].name;
            if (this[0].files && this[0].files.length) txt = this[0].files[0].name;
        }
        var ico = box.data("upIcon") || "bi bi-arrow-up-circle";
        var ent = _fileTemp(box).clone();

        ent.find(".icon" ).addClass(ico);
        ent.find(".title").text(txt);
        ent.attr( "title", txt );
    //  ent.data( "value", src );
        box.append(ent);
        if (! box.is("[data-readonly]")) {
            ent.append(inp);
        }

        return ent;
    };

    /**
     * 填充文件
     * @param {String } nam 文件字段名称
     * @param {String } src 文件路径取值
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFillFile = function(nam, src) {
        if (! src ) {
            return;
        }
        var box = $(this);
        var inp = $('<input type="hidden"/>').attr( 'name', nam ).val( src );
        var txt = /^data:/.test( src ) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var ico = box.data("dnIcon") || "" ;
        var ent = _fileTemp(box).clone();

        ent.find(".icon" ).addClass(ico);
        ent.find(".title").text(txt);
        ent.attr( "title", txt );
        ent.data( "value", src );
        box.append(ent);
        if (! box.is("[data-readonly]")) {
            ent.append(inp);
        }

        return ent;
    };

    //** 预览图片 **/

    /**
     * 选取预览
     * @param {Element} box 预览列表区域
     * @param {String } src 文件路径取值
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @param {boolean} k   为 true 保留, 否则截取
     * @returns {undefined|Element} 节点
     */
    $.fn.hsPickView = function(box, src, w, h, k) {
        if (! src ) {
            return;
        }
            box = $(box );
        var inp = $(this);
        inp.after(inp.clone().val('')) ;
        var txt = /^data:/.test( src ) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        if (txt === '') { // 通过文件对象获取文件名称
            if (this[0].filez && this[0].filez.length) txt = this[0].filez[0].name;
            if (this[0].files && this[0].files.length) txt = this[0].files[0].name;
        }
        var ent = _viewTemp(box).clone().css({width: w+'px', height: h+'px'});

        // 非保留和截取时为图片自身尺寸
        var cal = undefined;
        if (!k) {
            cal = function() {
                var w = this.width ;
                var h = this.height;
                var img = $( this );
                img.css({top  :  "0px", left  :  "0px"});
                ent.css({width: w+'px', height: h+'px'});
            };
        }

        var img = k === "pick"
                ? $.hsPickSnap(src, w, h, cal)
                : $.hsKeepSnap(src, w, h, cal);

    //  ent.data("value", src);
        ent.attr("title", txt);
        box.append(ent);
        ent.append(img);
        if (! box.is("[data-readonly]")) {
            ent.append(inp);
        }

        return ent;
    };

    /**
     * 填充预览
     * @param {String } nam 文件字段名称
     * @param {String } src 文件路径取值
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @param {boolean} k   为 keep 保留, 为 pick 截取
     * @returns {undefined|Element} 节点
     */
    $.fn.hsFillView = function(nam, src, w, h, k) {
        if (! src ) {
            return;
        }
        var box = $(this);
        var inp = $('<input type="hidden"/>').attr( 'name', nam ).val( src );
        var txt = /^data:/.test( src ) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var ent = _viewTemp(box).clone().css({width: w+'px', height: h+'px'});

        // 非保留和截取时为图片自身尺寸
        var cal = undefined;
        if (!k) {
            cal = function() {
                var w = this.width ;
                var h = this.height;
                var img = $( this );
                img.css({top  :  "0px", left  :  "0px"});
                ent.css({width: w+'px', height: h+'px'});
            };
        }

        var img = k === "pick"
                ? $.hsPickSnap(src, w, h, cal)
                : $.hsKeepSnap(src, w, h, cal);

        ent.data("value", src);
        ent.attr("title", txt);
        box.append(ent);
        ent.append(img);
        if (! box.is("[data-readonly]")) {
            ent.append(inp);
        }

        return ent;
    };

    //** 预览辅助 **/

    /**
     * 预载文件
     * @param {Function} cal 回调函数
     * @returns {jQuery} 当前文件节点
     */
    $.fn.hsReadFile = function(cal) {
        this.each(function() {
            var that = this;
            if (this.files ) {
            if (window.FileReader ) {
                $.each(this.files, function(i, fo) {
                    var fr = new FileReader( );
                    fr.onloadend = function(e) {
                        cal.call(that, fo , e.target.result);
                    };
                    fr.readAsDataURL ( fo );
                });
            } else {
                $.each(this.files, function(i, fo) {
                    cal.call(that, fo, fo.getAsDataURL ( ) );
                });
            }
            } else {
                cal.call(that , null , that.value);
            }
        });
        return this;
    };

    /**
     * 截取式预览
     * @param {String } src 文件真实路径
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @returns {Element}
     */
    $.hsPickSnap = function(src, w, h, b) {
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
            if (b) b.call (img);
        };
        img.src = src ;
        return  $(img);
    };

    /**
     * 保留式预览
     * @param {String } src 文件真实路径
     * @param {Number } w   预览额定宽度
     * @param {Number } h   预览额定高度
     * @returns {Element}
     */
    $.hsKeepSnap = function(src, w, h, b) {
        var img  =  new Image();
        img.onload = function() {
            var xw = img.width ;
            var xh = img.height;
            var zw = xh * w / h;
//          var zh = xw * h / w;
            if (zw < xw) {
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
            if (b) b.call (img);
        };
        img.src = src ;
        return  $(img);
    };

    /**
     * 控件构建及初始化
     */
    $(document).on("hsReady", function(e) {
        $(e.target).find("[data-toggle=hsFileInit]").each(function() {
            var inp = $(this);
            var box = inp.siblings("ul");
            if (! box.size()) {
                box = $(
                    '<div  class="form-control labelbox">'
                  +   '<ul class="repeated filebox"></ul>'
                  +   '<a href="javascript:;"></a>'
                  + '</div>'
                );
                box.insertBefore(inp).prepend(inp);
                if (! inp.is(":hidden") )
                inp.addClass("invisible");

                var lis = box.children("ul");
                var lnk = box.children("a" );

                // 选取
                lnk.attr("data-toggle","hsFile");
                lnk.text(inp.attr("placeholder") || hsGetLang("file.browse"));

                // 填充和校验
                lis.attr("data-fn"    , inp.attr("name") || "" );
                lis.attr("data-ft"    , inp.attr("data-form-ft") || "_file" );
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
        });

        $(e.target).find("[data-toggle=hsViewInit]").each(function() {
            var inp = $(this);
            var box = inp.siblings("ul");
            if (! box.size()) {
                box = $(
                    '<div  class="form-control labelbox">'
                  +   '<ul class="repeated filebox"></ul>'
                  +   '<a href="javascript:;"></a>'
                  + '</div>'
                );
                box.insertBefore(inp).prepend(inp);

                var lis = box.children("ul");
                var lnk = box.children("a" );

                // 选取
                lnk.attr("data-toggle","hsView");
                lnk.text(inp.attr("placeholder") || hsGetLang("file.browse"));

                // 填充和校验
                lis.attr("data-fn"    , inp.attr("name") || "" );
                lis.attr("data-ft"    , inp.attr("data-form-ft") || "_view" );
                lis.attr("data-mode"  , inp.attr("data-mode") || "");
                lis.attr("data-size"  , inp.attr("data-size") || "");
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
        });
    });

    /**
     * 选择文件事件处理
     */
    $(document).on("click", "[data-toggle=hsFile]",
    function( ) {
        var inp = $(this).siblings(":file");
        if (! inp.data("picked")) {
            inp.data("picked", 1);
            var box = inp.siblings("ul,ol");
            var mul = inp.prop("multiple");
            inp.on ( "change", function( ) {
                inp.hsReadFile(function(val, src) {
                   _hsSoloFile(box, false);
                    /**
                     * 多选模式下尝试自定义传递,
                     * 将文件对象绑定到扩展属性,
                     * 通过 hsToFormData 来上传.
                     */
                    if (mul) {
                        var iup = inp.clone( );
                        iup[0].filez = [ val ];
                        iup.val( "" );
                        inp.val( "" );
                        iup.hsPickFile (box, src);
                    } else {
                        inp.hsPickFile (box, src);
                    }
                    box.trigger("change"); // 转文件容器触发
                } );
                return  false;
            } );
        }
        inp.click();
    });

    /**
     * 选择图片事件处理
     */
    $(document).on("click", "[data-toggle=hsView]",
    function( ) {
        var inp = $(this).siblings(":file");
        if (! inp.data("picked")) {
            inp.data("picked", 1);
            var box = inp.siblings("ul,ol");
            var mul = inp.prop("multiple");
            var  k  = box.data("mode");
            var  w  = box.data("size");
            var  h  = w.split ("*", 2);
                 w  = h [0]; h = h [1];
            inp.on ( "change", function( ) {
                inp.hsReadFile(function(val, src) {
                   _hsSoloFile(box, false);
                    /**
                     * 多选模式下尝试自定义传递,
                     * 将文件对象绑定到扩展属性,
                     * 通过 hsToFormData 来上传.
                     */
                    if (mul) {
                        var iup = inp.clone( );
                        iup[0].filez = [ val ];
                        iup.val( "" );
                        inp.val( "" );
                        iup.hsPickView (box, src, w, h, k);
                    } else {
                        inp.hsPickView (box, src, w, h, k);
                    }
                    box.trigger("change"); // 转文件容器触发
                } );
                return  false;
            } );
        }
        inp.click();
    });

    /**
     * 文件的打开和移除
     */
    $(document).on("click", ".filebox li",
    function(x) {
        /**
         * 点击关闭按钮则删除当前文件节点
         */
        var box = $(this).closest ("ul,ol");
        if ($(x.target).is(".erase,.close")) {
            $(this).remove(/***/);
            box.trigger("change");
            _hsSoloFile(box,true);
            return;
        }

        /**
         * 暂时没有较好的办法像打开远程文件一样打开刚选择待上传的文件
         * 倒是可以通过 hsReadFile 来获取 base64 编码进而在新窗口打开
         * 但如果是较大的文件可能不太合适
         * 故干脆放弃待上传新窗口打开预览
         * 预览待上传图片用 hsView 等方法
         */
        var url = $(this).data("value");
        if (url) {
            window.open(hsFixUri(url), "_blank");
        }
        var inp = $(this).find("input");
        if (inp.attr("type") != "file") {
            url = inp.val( );
        if (url) {
            window.open(hsFixUri(url), "_blank");
        }}
    });

})(jQuery);

function _hsSoloFile(box, show) {
    var fn = box.data( "fn" ) || "" ;
    if (! box.is( "[data-repeated]" )
    &&  ! box.is( "[data-multiple]" )
    &&  ! /(\[\]|\.\.|\.$)/.test(fn)) {
        box.siblings("[data-toggle=hsFile],[data-toggle=hsView]").toggle(show);
    }
}

function hsFormFillFile(box, v, n) {
    if (! v) return;
    if (! jQuery.isArray(v)) {
        v = [ v ];
    }
    if (v.length) {
        _hsSoloFile ( box, false );
    }
    jQuery.each(v , function(i, x) {
        box.hsFillFile(n, x);
    });
}

function hsFormFillView(box, v, n) {
    if (! v) return;
    if (! jQuery.isArray(v)) {
        v = [ v ];
    }
    if (v.length) {
        _hsSoloFile ( box, false );
    }

    var k = box.data("mode");
    var w = box.data("size");
    var h = w.split ("*", 2);
        w = h [0]; h = h [1];

    jQuery.each(v , function(i, x) {
        box.hsFillView(n, x, w, h, k);
    });
}
