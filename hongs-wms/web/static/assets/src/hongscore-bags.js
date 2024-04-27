/**
 * 标签输入组添加方法:
 *  <input type="hidden" name="tags" data-toggle="hsBags"/>
 * 将自动创建操作控件,
 * 亦可自定义操作控件.
 */

(function($) {

    function _exists(box, val) {
        var non = true;
        box.find(".bags-value").each(function() {
            if (val ==  $(this).text()) {
                non  = false;
                return false;
            }
        });
        return !non;
    }

    function _merges(box, sep) {
        var vas = [];
        box.find(".bags-value")
           .each( function() {
            vas.push($(this).text());
        });
        return vas.join(sep);
    }

    $.fn.hsBags = function() {
        if (this.data("linked")) {
            return; // 跳过已初始化
        }

        var inp , box ;
        if (! this.is("input,textarea")) {
            box = this;
            inp = this.siblings("input,textarea");
        } else {
            inp = this;
            box = this.siblings(".bagsbox,div,ul,ol");

            // 创建操作控件
            if (! box.size()) {
                var fn = inp.attr("name");
                var ft = inp.attr("type");
                var fl = "hsFormFillBags.call(form, this, v, n)"; // 直接填充, 不从 input 读取
                if (! ft || ft == "hidden") {
                    ft = "text";
                }

                box = $(
                    '<div class="bagsbox form-control">'
                  +   '<div class="bags-label label label-info hide">'
                  +     '<i class="bags-erase bi-x" ></i>'
                  +     '<span class="bags-value"></span>'
                  +   '</div>'
                  +   '<input class="bags-input" type="'+ft+'"/>'
                  + '</div>'
                );
                if (! inp.is(":hidden") )
                inp.addClass("invisible");
                box.insertAfter(inp);

                // 添加隐藏字段
                // 指明了不拼接, 或未设拼接符, 而又要求多值;
                // 当通过自定义控件模式构建时, 则需自行处理.
                if (inp.data("unjoin")
                || (! inp.data("join")
                &&  inp.attr("multiple"))) {
                    $('<input type="hidden" />')
                       .appendTo(box.find(".bags-label"))
                       .attr("name"     , fn);
                    box.attr("data-fn"  , fn);
                    box.attr("data-fill", fl);
                    box.data("unjoin" , true);
                    // 校验相关, input 无 form-field 将不填充不校验
                    inp.removeClass("form-field");
                    if (inp.prop("required" )) {
                        box.attr("data-required" , "required");
                    }
                    if (inp.data("minrepeat")) {
                        box.attr("data-minrepeat", inp.data("minrepeat"));
                    }
                    if (inp.data("maxrepeat")) {
                        box.attr("data-maxrepeat", inp.data("maxrepeat"));
                    }
                }
            }
        }

        var enp  = box.find(".bags-input");
        var tmp  = box.find(".bags-label.hide").detach();
        var fn   = box.data( "fn" ) || inp.attr("name"); // 字段名
        var ends = box.data("ends") || inp.data("ends"); // 切词键
        var join = box.data("join") || inp.data("join"); // 拼接符
        var unjoin  = box.data("unjoin" ) || inp.data("unjoin" ); // 不拼接
        var unstrip = box.data("unstrip") || inp.data("unstrip"); // 不清理前后空格
        var unstint = box.data("unstint") || inp.data("unstint"); // 不进行排重处理

        if (! tmp.size()) {
            throw new Error(".bags-label not exists");
        }
        if (! enp.size()) {
            throw new Error(".bags-input not exists");
        }

        // 拼接符, 默认为半角逗号
        if (! join && ! unjoin) {
            join = ',';
        }

        // 切词键, 默认为回车, 半角逗号和分号, 全角顿号和逗号和分号
        if (! ends) {
            ends = [13, 44, 59, 0x3001, 0xff0c, 0xff1b];
        } else {
            ends = ends.split(',');
            for (var i = 0; i < ends.length; i ++) {
                ends[i] = parseInt(ends[i]);
            }
        }

        function inlay(v) {
            do {
                if (!v) {
                    break;
                }
                if (!unstrip) {
                    v = $. trim (v);
                }
                if (!unstint) {
                if (_exists(box, v)) {
                    break;
                }}

                // 添加标签
                var tag = tmp.clone();
                    tag.removeClass ("hide")
                       .insertBefore( this );
                    tag.find(".bags-value" )
                       .text(v);
                    tag.find("input")
                       .val (v);

                // 触发事件, 合并取值
                if (join) {
                    inp.val(_merges(box , join));
                    inp.trigger("change", true );
                } else {
                    box.trigger("change");
                }
            } while (false);

            $(this).val("");
        }
        function input(e) {
            var cod = e.keyCode;
            var val = $(this).val();

            do {
                if ($.inArray(cod, ends) !== -1) { // 针对回车和半角字符等
                    if (val) {
                        cod = val.charCodeAt(val.length - 1);
                        if ($.inArray(cod, ends) !== -1) {
                            val = val.substring(0, val.length - 1);
                        }
                    }
                } else {
                    if (val) {
                        cod = val.charCodeAt(val.length - 1);
                        if ($.inArray(cod, ends) !== -1) {
                            val = val.substring(0, val.length - 1);
                            break;
                        }
                    }
                    return ;
                }
            } while (false);

            inlay.call(this, val);
            e.stopPropagation ( );
            return false;
        }
        function erase( ) {
            $(this).closest(".bags-label").remove();
        }
        function clear( ) {
            $(this). find  (".bags-label").remove();
        }
        function focus( ) {
            $(this). find  (".bags-input").focus ();
        }

        box.on("click", ".bags-erase", function(e) {
            return erase.call(this,e);
        });
        box.on("keyup", ".bags-input", function(e) {
            return input.call(this,e);
        });
        box.on("blur" , ".bags-input", function(e) {
            // 失焦即确认, 防止填完不回车的
            var v = $(this).val();
            if (v) {
                $(this).val( "" );
                return inlay.call(this,v);
            }
        });
        box.on("keydown keypress", function(e) {
            // 阻止回车被表单截获而触发提交
            if (13 === e.keyCode && -1 !== $.inArray(13, ends)) {
                var that = $(this).find( ".bags-input" ) [ 0 ];
                return input.call(that,e);
            }
        });
        box.on("click focus", function(e) {
            // 点击控件空白处将聚焦到输入框
            if (this === e.target) {
                return focus.call(this,e);
            }
        });
        inp.on("change", function(e, x) {
            // 常规添加、无值或不拼接均跳过
            var v = $(this).val();
            if (x || !v || !join) {
                return;
            }   v = v.split(join);
            // 逐条加入
            clear.call(box);
            for (var i = 0; i < v.length; i ++) {
                inlay.call(enp, v[i]);
            }
        });

        // 方便外部操作
        box.data("add", function(v) {
            inlay.call(enp, v);
        });
        box.data("set", function(v) {
            clear.call(box);
            for (var i = 0; i < v.length; i ++) {
                inlay.call(enp, v[i]);
            }
        });

        box.data("linked", inp);
        inp.data("linked", box);
        inp.trigger( "change" );
    };

    // 加载就绪后自动初始化
    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=hsBags]").each(function() {
            $(this).hsBags();
        });
    });

})(jQuery);

function hsFormFillBags(box , v) {
    var set = $(box).data("set");
    if (set) {
        set(v || []);
    } else {
        throw new Error("hsBags not inited", box);
    }
}
