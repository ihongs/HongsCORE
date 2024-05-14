/**
 * 标签输入组添加方法:
 * <input type="hidden" name="tags" data-toggle="hsDits"/>
 * 将自动创建操作控件,
 * 亦可自定义操作控件.
 */

(function($) {
    $.fn.hsDits = function(v) {
        if (this.data("linked")) {
            if (v !== undefined) {
                if (this.is(".ditsbox")) {
                    this.data("set")(v);
                } else {
                    this.data("linked")
                        .data("set")(v);
                }
            }
            return; // 跳过已初始化
        }

        var box; // 外框
        var lsp; // 标签容器, .ditsbox
        var inp; // 输入控件, .input
        var jnp; // 取值占位, .value

        if (this.is(".ditsbox")) {
            lsp = this;
            box = this.parent();
            inp = this.siblings(".input");
            jnp = this.siblings(".value");
        } else {
            inp = this;
            box = this.parent();
            jnp = this.siblings(".value");
            lsp = this.siblings(".ditsbox");

            if (! lsp.size()) {
                box = $(
                    '<div  class="multiple form-control">'
                  +   '<input class="value" type="hidden"  />'
                  +   '<ul class="repeated labelbox ditsbox">'
                  +     '<li  class="label label-info template">'
                  +       '<a class="erase bi bi-x pull-right" href="javascript:;"></a>'
                  +       '<span  class="title"></span>'
                  +       '<input class="value" type="hidden" />'
                  +     '</li>'
                  +   '</ul>'
                  + '</div>'
                );
                box.insertBefore(inp).append(inp);
                lsp = this.siblings(".ditsbox");
                jnp = this.siblings(".value");
                var fn = inp. attr ("name");

                // 只读无需输入
                if (inp.is("[readonly],[data-readonly]")) {
                    // 移除控件, 无需 input 相关的操作
                    box.find(".input").remove();
                    box.find(".value").remove();
                    box.find(".erase").remove();
                    inp = jQuery();
                    jnp = jQuery();
                } else
                // 添加隐藏字段
                if (inp.is("[multiple],[data-multiple]")) {
                    // 分散取值, 不将 value 串联到一起
                    lsp.find(".value")
                       .attr("name", fn);
                    jnp.attr("name", fn);
                    lsp.attr("data-unjoin" , "unjoin");
                    // 校验相关, 无需 input 校验和填充
                    if (inp.prop("required" )) {
                        lsp.attr("data-required" , "required");
                    }
                    if (inp.data("minrepeat")) {
                        lsp.attr("data-minrepeat", inp.data("minrepeat"));
                    }
                    if (inp.data("maxrepeat")) {
                        lsp.attr("data-maxrepeat", inp.data("maxrepeat"));
                    }
                } else
                {
                    // 合并取值, 会将 value 串联到一起
                    jnp.attr("name", fn);
                    // 校验相关, 无需 input 校验和填充
                    if (inp.prop("required" )) {
                        lsp.attr("data-required" , "required" );
                    }
                    if (inp.data("minrepeat")) {
                        lsp.attr("data-minrepeat", inp.data("minrepeat"));
                    }
                    if (inp.data("maxrepeat")) {
                        lsp.attr("data-maxrepeat", inp.data("maxrepeat"));
                    }
                }

                // 接收取值数据
                lsp.attr("data-fn" , fn);
                lsp.data("fill", function(x, v) { hsDitsFill(this, v); });

                inp.removeAttr (  "name"  );
                inp.removeAttr ("required");
                inp.removeAttr ("multiple");
                inp.   addClass(  "input" );
                inp.removeClass("form-field form-control");
            }
        }

        var tmp  = lsp.children( ".template" ).detach(); // 模板, 移出
        var fn   = lsp.data( "fn" ) || inp.attr("name"); // 字段名
        var ends = lsp.data("ends") || inp.data("ends"); // 切词键
        var join = lsp.data("join") || inp.data("join"); // 拼接符
        var unstrip = lsp.is("[data-unstrip]") || inp.is("[data-unstrip]"); // 不清理前后空格
        var unstint = lsp.is("[data-unstint]") || inp.is("[data-unstint]"); // 不进行排重处理
        var unjoin  = lsp.is("[data-unjoin]" ) || inp.is("[data-multiple],[multiple]"); // 多值字段不拼接

        if (! tmp.size()) {
            throw new Error("hsDits temp not exists");
        }
        tmp.removeClass ( "template invisible hide" );

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

        function merge(s) {
            var a = [];
            lsp.find("input").each(function() {
                a.push($(this).val());
            });
            return a.join(s);
        }
        function exist(v) {
            var n = true;
            lsp.find("input").each(function() {
                if (v == $(this).val()) {
                    n  =   false;
                    return false;
                }
            });
            return !n;
        }
        function inlet(a) {
            if (! $.isArray( a )) {
                a = a.split(join);
            }

            var c = false;
            for(var i = 0; i < a.length; i ++) {
                var v = a [i];
                if (!unstrip) {
                    v = $.trim(v || "");
                }
                if (!unstint) {
                if (exist(v)) {
                    continue;
                }}
                if (!v) {
                    continue;
                }

                // 添加标签
                var tag = tmp.clone();
                tag.attr( "title", v);
                tag.find(".title").text(v);
                tag.find(".value").val (v);
                lsp.append ( tag );

                c = true;
            }

            if (c) {
                // 触发事件, 合并取值
                if (join) {
                    jnp.val(merge(join));
                    jnp.trigger("change", true );
                } else {
                    lsp.trigger("change", false);
                }
            }
        }
        function inlay(v) {
            if (!unstrip) {
                v = $.trim(v || "");
            }
            if (!unstint) {
            if (exist(v)) {
                return;
            }}
            if (!v) {
                return;
            }

            // 添加标签
            var tag = tmp.clone();
            tag.attr( "title", v);
            tag.find(".title").text(v);
            tag.find(".value").val (v);
            lsp.append ( tag );

            // 触发事件, 合并取值
            if (join) {
                jnp.val(merge(join));
                jnp.trigger("change", true );
            } else {
                lsp.trigger("change", false);
            }
        }
        function input(e) {
            var cod = e.keyCode;
            var val = inp.val();

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
            }
            while (false);

            inp.val( "" );
            inlay ( val );

            // 阻止事件往外扩散触发提交
            e.stopPropagation();
            return false ;
        }

        // 方便外部操作
        lsp.data("set", function(v) {
            lsp.empty( );
            if (v) {
                inlet(v);
            }
        });
        lsp.data("add", function(v) {
            if (v) {
                inlay(v);
            }
        });

        lsp.on("click", ".erase", function(e) {
            $(this).closest( "li" ).remove( );
        });

        // 只读模式时输入框可能缺失
        // 此时并不需要绑定事件监听
        if (inp.size()) {

        // 改变即确认, 直接设置所选的值
        inp.on("change" , function( ) {
            var v;
            v = $(this).val(  );
            if (v) {
                $(this).val("");
                inlay(v);
            }
        });

        // 规避回车被表单截获而触发提交
        if ($.inArray(13, ends) >= 0)
        inp.on("keydown", function(e) {
            if (13 === e.keyCode) {
                return input(e);
            }
        });
        // 监听按键检查是否有输入切词符
        inp.on( "keyup" , function(e) {
                return input(e);
        });

        // 点击控件空白处将聚焦到输入框
        box.on( "click focus", function(e) {
            if (box.is (e.target)
            ||  lsp.is (e.target)) {
                return inp.focus();
            }
        });

        } // End if has inp

        lsp.data("linked", inp);
        inp.data("linked", lsp);

        // 初始值
        if (v === undefined) {
            v = inp.val();
            inp.val( "" );
        }
        if (v) {
            inlet(v);
        }
    };

    // 加载就绪后自动初始化
    $(document).on("hsReady", function(e) {
        $(e.target).find("[data-toggle=hsDits]").each(function() {
            $(this).hsDits();
        });
    });
})(jQuery);

function hsDitsFill(box , v) {
    $(box).hsDits(v);
}
