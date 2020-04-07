/**
 * 文件上传及预览用法:
 * 在表单配置区域添加:
 * data-data-0="_fill__file:(hsFormFillFile)"
 * 在表单选项区域添加:
 * <input type="text" name="x_url" class="form-ignored invisible"/>
 * <input type="file" name="x_url" class="form-ignored invisible"/>
 * <ul data-ft="_file" data-fn="x_url" class="pickbox"></ul>
 * <button type="button" data-toggle="hsFile">Browse...</button>
 * 图片预览相应的改为 hsFormFillView, hsView
 *
 * 注: 2016/06/10 增加文件上传及图片预览工具
 */

(function($) {

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
        $(this).before($(this).clone().val(''));
            box = $(box );
        var txt = /^data:.+\/.+,/.test(src) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var cls = box.is(".pickrol" )  ?  "btn-link" : "btn-info" ;
        var lab = $('<span></span>' ).text(txt);
        var inp = $(this);
        var div = $('<li class="btn '+cls+' form-control" ></li>')
           .attr  ("title", txt)
           .append('<span class="close pull-right"      >&times;</span>')
           .append('<span class="glyphicon glyphicon-open-file"></span>')
           .append(lab)
           .append(inp);
        box.append(div);
        return div;
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
        var txt = /^data:.+\/.+,/.test(src) ? ''
                : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var cls = box.is(".pickrol" )  ?  "btn-link" : "btn-info" ;
        var lab = $('<span></span>' ).text(txt);
        var inp = $('<input  type="hidden" />')
                   .attr('name', nam).val (src);
        var div = $('<li class="btn '+cls+' form-control" ></li>')
           .attr  ("title", txt)
           .append('<span class="close pull-right"      >&times;</span>')
           .append('<span class="glyphicon glyphicon-save-file"></span>')
           .append(lab)
           .append(inp);
        box.append(div);
        return div;
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
        $(this).before($(this).clone().val(''));
            box = $(box );
        var inp = $(this);
        var txt = /^data:.+\/.+,/.test(src) ? ''
                  : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var div = $('<li class="preview"></li>').attr("title" , txt)
           .css({width: w+'px', height: h+'px', overflow: 'hidden'});

        // 非保留和截取时为图片自身尺寸
        var cal ;
        if (!k) {
            cal = function() {
                var w = this.width ;
                var h = this.height;
                var img = $( this );
                img.css({top  :  "0px", left  :  "0px"});
                div.css({width: w+'px', height: h+'px'});
            };
        }

        var img = k === "pick"
                ? $.hsPickSnap(src, w, h, cal)
                : $.hsKeepSnap(src, w, h, cal);

        div.append(inp)
           .append(img)
           .append('<a href="javascript:;" class="close">&times;</a>');
        box.append(div);
        return div;
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
        var inp = $('<input  type="hidden" />')
                   .attr('name', nam ).val(src);
        var txt = /^data:.+\/.+,/.test(src) ? ''
                  : decodeURIComponent(src.replace(/^.*[\/\\]/, ''));
        var div = $('<li class="preview"></li>').attr("title" , txt)
           .css({width: w+'px', height: h+'px', overflow: 'hidden'});

        // 非保留和截取时为图片自身尺寸
        var cal ;
        if (!k) {
            cal = function() {
                var w = this.width ;
                var h = this.height;
                var img = $( this );
                img.css({top  :  "0px", left  :  "0px"});
                div.css({width: w+'px', height: h+'px'});
            };
        }

        var img = k === "pick"
                ? $.hsPickSnap(src, w, h, cal)
                : $.hsKeepSnap(src, w, h, cal);

        div.append(inp)
           .append(img)
           .append('<a href="javascript:;" class="close">&times;</a>');
        box.append(div);
        return div;
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
     * 选择文件事件处理
     */
    $(document).on("click", "[data-toggle=hsFile]",
    function( ) {
        var inp = $(this).siblings(":file"   );
        if (! inp.data("picked")) {
            inp.data("picked", 1);
            var box = inp.siblings(".pickbox");
            inp.on("change", function( ) {
                   _hsSoloFile(box, false);
                inp.hsPickFile(box, inp.val());
            });
        }
        inp.click();
    });

    /**
     * 选择图片事件处理
     */
    $(document).on("click", "[data-toggle=hsView]",
    function( ) {
        var inp = $(this).siblings(":file"   );
        if (! inp.data("picked")) {
            inp.data("picked", 1);
            var box = inp.siblings(".pickbox");
            var mul = inp.prop("multiple");
            var  k  = box.data("mode");
            var  w  = box.data("size");
            var  h  = w.split ("*", 2);
                 w  = h [0]; h = h [1];
            var tmp = $('<input type="hidden"/>')
                  .attr("name", inp.attr("name"));
            inp.on("change", function( ) {
            inp.hsReadFile ( function( val, src ) {
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
                iup.hsPickView(box, src, w, h, k);
            } else {
                inp.hsPickView(box, src, w, h, k);
            }
                /**
                 * 因为 hsReadFile 是异步的,
                 * 插一个输入项规避校验失败;
                 * 但是如果不加延时直接移除,
                 * 还是会导致校验时检测不到.
                 */
                setTimeout ( function( ) {
                   tmp.remove();
                }, 100);
            });
                box.append(tmp);
            });
        }
        inp.click();
    });

    /**
     * 文件的打开和移除
     */
    $(document).on("click", "ul.pickbox li",
    function(x) {
        var box = $(this).closest(".pickbox");
        if (box.siblings("[data-toggle=hsFile],[data-toggle=hsView]").size() == 0) {
            return;
        }

        /**
         * 点击关闭按钮则删除当前文件节点
         */
        if ($(x.target).is( ".close" )) {
            $(this).remove();
            box.trigger( "change" );
            _hsSoloFile(box , true);
            return;
        }

        /**
         * 暂时没有较好的办法像打开远程文件一样打开刚选择待上传的文件
         * 倒是可以通过 hsReadFile 来获取 base64 编码进而在新窗口打开
         * 但如果是较大的文件可能不太合适
         * 故干脆放弃待上传新窗口打开预览
         * 预览待上传图片用 hsView 等方法
         */
        var inp = $(this).find("input");
        if (inp.attr("type") != "file") {
        var url = hsFixUri( inp.val() );
        if (url) {
            window.open(url , "_blank");
        }}
    });

})(jQuery);

function _hsSoloFile(box, show) {
    var fn = box.data("fn");
    if (! fn || box.hasClass("pickmul")) {
        return;
    }
    if (! box.data("repeated") && ! /(\[\]|\.\.|\.$)/.test( fn )) {
        box.siblings("[data-toggle=hsFile],[data-toggle=hsView]")
           .toggle( show  );
        box.removeClass("pickmul");
    } else {
        box.   addClass("pickmul");
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
