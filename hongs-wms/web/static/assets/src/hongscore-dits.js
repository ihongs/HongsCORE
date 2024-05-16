/**
 * 标签输入组添加方法:
 * <input type="text" name="tags" data-toggle="hsDits"/>
 * 多项选择组添加方法:
 * <select name="sels" data-toggle="hsSels">
 *    <option value=""></option>
 *    <option value="1">选项1</option>
 *    <option value="2">选项2</option>
 * </select>
 * 将自动创建操作控件,
 * 亦可自定义操作控件.
 */

(function($) {

    function _fetch(lsp, s) {
        var a = [];
        lsp.find(".value").each(function() {
            a.push($(this).val());
        });
        if (s) {
            a = a.join(s);
        }
        return  a;
    }
    function _exist(lsp, v) {
        var n = true;
        lsp.find(".value").each(function() {
            if (v == $(this).val()) {
                n  =   false;
                return false;
            }
        });
        return !n;
    }
    function _label(sel, v) {
        var t = null;
        sel.children().each(function() {
            if (v == $(this).val()) {
                t  = $(this).text();
                return false;
            }
        });
        return  t || v;
    }

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

                // 如 input 有 datalist 也挪进来
                var dl = inp. attr ("list");
                if (dl) {
                    dl = $("#" + dl);
                if (box.siblings(dl).size()) {
                    box. append (dl);
                }}

                // 汇总或者占位
                jnp.attr(  "name"  , fn);

                // 接收取值数据
                lsp.attr("data-fn" , fn);
                lsp.data("fill", function(x, v) { hsDitsFill(this, v); });
                lsp.data("feed", function(x, v) { hsDitsFeed(this, v); });

                // 设置校验参数
                if (inp.prop("required" )) {
                    lsp.attr("data-required" , "required");
                }
                if (inp.data("minrepeat")) {
                    lsp.attr("data-minrepeat", inp.data("minrepeat"));
                }
                if (inp.data("maxrepeat")) {
                    lsp.attr("data-maxrepeat", inp.data("maxrepeat"));
                }

                // 清理原有属性, 仅被用作输入
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

        // 切词键, 默认为回车, 半角逗号和分号, 全角顿号和逗号和分号
        if (! ends) {
            ends = [13, 44, 59, 0x3001, 0xff0c, 0xff1b];
        } else {
            ends = ends.split(',');
            for (var i = 0; i < ends.length; i ++) {
                ends[i] = parseInt(ends[i]);
            }
        }

        // 拼接符, 默认为半角逗号
        if (! join && (lsp.is("[data-join]") || inp.is("[data-join]"))) {
            join = ",";
        }

        // 检查和补充默认条目模板
        if (tmp.size()) {
            tmp.removeClass("template");
        } else {
            tmp  = $('<li  class="label label-info">'
                 +     '<a class="erase bi bi-x pull-right" href="javascript:;"></a>'
                 +     '<span  class="title"></span>'
                 +     '<input class="value" type="hidden"/>'
                 +   '</li>');
            if (! join) {
                tmp.find(".value").attr("name", fn);
            }
        }

        function set(a) {
            if (! $.isArray( a )) {
                a = a.split(join);
            }
            var c = 0;
            var e = 0;

            // 清空待写
            if (lsp.is(":empty")) {
                e = 1;
            }
            lsp.empty();

            // 整理取值
            var w = { };
            lsp.find(".value").each(function() {
                w[$(this).val()] = true;
            });

            for(var i = 0; i < a.length; i ++) {
                var v = a [i];
                if (!unstrip) {
                    v = $.trim(v || "");
                }
                if (!unstint) {
                if ( w[ v ] ) {
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

                w [v] = true;
                c ++;
            }

            if (c > 0 || e > 0) {
                // 合并取值, 触发事件
                if (join) {
                    jnp.val(_fetch(lsp, join));
                }
                lsp.trigger( "change" , true );
            }

            return c;
        }

        function add(v) {
            if (!unstrip) {
                v = $.trim(v || "");
            }
            if (!unstint) {
            if (_exist(lsp,v)) {
                return 0;
            }}
            if (!v) {
                return 0;
            }

            // 添加标签
            var tag = tmp.clone();
            tag.attr( "title", v);
            tag.find(".title").text(v);
            tag.find(".value").val (v);
            lsp.append ( tag );

            // 合并取值, 触发事件
            if (join) {
                jnp.val(_fetch(lsp, join));
            }
            lsp.trigger( "change" , true );

            return 1;
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
            add(val);

            // 阻止事件往外扩散触发提交
            e.stopPropagation( );
            return false ;
        }

        // 方便外部操作
        lsp.data("get", function( ) {
            return _fetch(lsp,join);
        });
        lsp.data("set", function(v) {
            set(v);
        });
        lsp.data("add", function(v) {
            add(v);
        });

        lsp.on("click", ".erase", function() {
            $(this).closest("li").remove();
            if (join) {
                jnp.val(_fetch(lsp, join));
            }
            lsp.trigger( "change" , true );
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
                add(v);
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
            set(v);
        }
    };

    $.fn.hsSels = function(v) {
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
                  +   '<input class="value" type="hidden"/>'
                  +   '<ul class="repeated labelbox ditsbox">'
                  +   '</ul>'
                  + '</div>'
                );
                box.insertBefore(inp).append(inp);
                lsp = this.siblings(".ditsbox");
                jnp = this.siblings(".value");
                var fn = inp. attr ("name");

                // 汇总或者占位
                jnp.attr(  "name"  , fn);

                // 接收取值数据
                lsp.attr("data-fn" , fn);
                lsp.data("fill", function(x, v) { hsSelsFill(this, v); });
                lsp.data("feed", function(x, v) { hsSelsFeed(this, v); });

                // 设置校验参数
                if (inp.prop("required" )) {
                    lsp.attr("data-required" , "required");
                }
                if (inp.data("minrepeat")) {
                    lsp.attr("data-minrepeat", inp.data("minrepeat"));
                }
                if (inp.data("maxrepeat")) {
                    lsp.attr("data-maxrepeat", inp.data("maxrepeat"));
                }

                // 清理原有属性, 仅被用作输入
                inp.removeAttr (  "name"  );
                inp.removeAttr ("required");
                inp.removeAttr ("multiple");
                inp.   addClass(  "input" );
                inp.removeClass("form-field form-control");
            }
        }

        var tmp  = lsp.children( ".template" ).detach(); // 模板, 移出
        var fn   = lsp.data( "fn" ) || inp.attr("name"); // 字段名
        var join = lsp.data("join") || inp.data("join"); // 拼接符
        var unstrip = lsp.is("[data-unstrip]") || inp.is("[data-unstrip]"); // 不清理前后空格
        var unstint = lsp.is("[data-unstint]") || inp.is("[data-unstint]"); // 不进行排重处理

        // 拼接符, 默认为半角逗号
        if (! join && (lsp.is("[data-join]") || inp.is("[data-join]"))) {
            join = ',';
        }

        // 检查和补充默认条目模板
        if (tmp.size()) {
            tmp.removeClass("template");
        } else {
            tmp  = $('<li  class="label label-info">'
                 +     '<a class="erase bi bi-x pull-right" href="javascript:;"></a>'
                 +     '<span  class="title"></span>'
                 +     '<input class="value" type="hidden"/>'
                 +   '</li>');
            if (! join) {
                tmp.find(".value").attr("name", fn);
            }
        }

        function set(a) {
            if (! $.isArray( a )) {
                a = a.split(join);
            }
            var c = 0;
            var e = 0;

            // 清空待写
            if (lsp.is(":empty")) {
                e = 1;
            }
            lsp.empty();

            // 整理取值
            var w = { };
            lsp.find(".value").each(function() {
                w[$(this).val()] = true;
            });

            // 整理文本
            var m = { };
            inp.find("option").each(function() {
                m[$(this).val()] = $(this).text();
            });

            for(var i = 0; i < a.length; i ++) {
                var v = a [i];
                if (!unstrip) {
                    v = $.trim(v || "");
                }
                if (!unstint) {
                if ( w[ v ] ) {
                    continue;
                }}
                if (!v) {
                    continue;
                }

                // 获取文本
                var t = m[v] || v;

                // 添加标签
                var tag = tmp.clone();
                tag.attr( "title", t);
                tag.find(".title").text(t);
                tag.find(".value").val (v);
                lsp.append ( tag );

                w [v] = true;
                c ++;
            }

            if (c > 0 || e > 0) {
                // 合并取值, 触发事件
                if (join) {
                    jnp.val(_merge(lsp, join));
                }
                lsp.trigger( "change" , true );
            }

            return c;
        }
        function add(v) {
            if (!unstrip) {
                v = $.trim(v || "");
            }
            if (!unstint) {
            if (_exist(lsp,v)) {
                return 0;
            }}
            if (!v) {
                return 0;
            }

            // 获取文本
            var t = _label(inp,v);

            // 添加标签
            var tag = tmp.clone();
            tag.attr( "title", t);
            tag.find(".title").text(t);
            tag.find(".value").val (v);
            lsp.append ( tag );

            // 合并取值, 触发事件
            if (join) {
                jnp.val(_fetch(lsp, join));
            }
            lsp.trigger( "change" , true );

            return 1;
        }

        // 方便外部操作
        lsp.data("get", function( ) {
            return _fetch(lsp,join);
        });
        lsp.data("set", function(v) {
            set(v);
        });
        lsp.data("add", function(v) {
            add(v);
        });

        lsp.on("click", ".erase", function(e) {
            $(this).closest("li").remove();
            if (join) {
                jnp.val(_fetch(lsp, join));
            }
            lsp.trigger( "change" , true );
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
                add(v);
            }
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
            set(v);
        }
    };

    // 加载就绪后自动初始化
    $(document).on("hsReady", function(e) {
        $(e.target).find("[data-toggle=hsDits]").each(function() {
            $(this).hsDits();
        });
        $(e.target).find("[data-toggle=hsSels]").each(function() {
            $(this).hsSels();
        });
    });
})(jQuery);

function hsDitsFill(box , v) {
    $(box).hsDits(v);
}

function hsDitsFeed(box , v) {
    $(box).hsDits( );

    if (! v || v.length < 1) {
        return v;
    }
    box = $(box);
    if (! box.is(".input") ) {
        box = box.siblings(".input");
    }

    // 清理空值
    var w = [];
    for(var i = 0; i < v.length; i ++) {
        var u = v [i];
        if (! u[0]) {
            continue ;
        }
        w.push (u);
    }

    HsForm.prototype._feed__datalist(box , w);
}

function hsSelsFill(box , v) {
    $(box).hsSels(v);
}

function hsSelsFeed(box , v) {
    $(box).hsDits( );

    if (! v || v.length < 1) {
        return v;
    }
    box = $(box);
    if (! box.is(".input") ) {
        box = box.siblings(".input");
    }

    // 补充空值
    var w = [];
    if (! box.find("option[value='']").size()) {
        w.push(["", ""]);
    }

    // 清理空值
    for(var i = 0; i < v.length; i ++) {
        var u = v [i];
        if (! u[0]) {
            continue ;
        }
        w.push (u);
    }

    HsForm.prototype._feed__datalist(box , w);
}
