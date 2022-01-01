/**
 * 日期选择组添加方法:
 * <input type="text" name="xtime" data-toggle="hsDate" data-type="timestamp" data-fl="v || new Date().getTime() / 1000"/>
 * 可包裹在输入框组内,
 * 此输入框将被替换掉.
 *
 * 注: 2017/06/22 增加简单日期选择控件
 */

(function($) {

    function _addzero(num, len) {
        num = num ? num : "";
        num = num.toString();
        var gth = num.length;
        for (var i = gth; i < len; i ++) {
            num = "0" + num ;
        }
        return num;
    }

    function _fixdate(box, dat) {
        var m = dat.getMonth( ) + 1;
        var y = dat.getFullYear ( );
        var b = box.find("[data-date=d]");
        var d = b.val( );
        if (m == 2 ) {
            if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) {
                if (d > 29) b.val(29).change();
                b.find("option:gt(28)")
                 .hide();
            } else {
                if (d > 28) b.val(28).change();
                b.find("option:gt(27)")
                 .hide();
            }
        } else {
            if ((m < 7 && m % 2 == 0) || (m > 7 && m % 2 == 1)) {
                if (d > 30) b.val(30).change();
                b.find("option:gt(29)")
                 .hide();
            } else {
                b.find("option:gt(27)")
                 .show();
            }
        }
    }

    function _setdate(box, dat) {
        box.find("[data-date]").each(function() {
            var flg = $( this ).data( "date" );
            switch (flg) {
                case 'La':
                case 'Sa':
                    $(this).val(dat.getHours() > 11 ? 1 : 0);
                    break;
                case 'LM':
                case 'SM':
                    $(this).val(dat.getMonth());
                    break;
                case 'M':
                    $(this).val(dat.getMonth() + 1);
                    break;
                case 'y':
                    $(this).val(dat.getFullYear() );
                    break;
                case 'Sy':
                    $(this).val(dat.getFullYear() - 2000);
                    break;
                case 'd':
                    $(this).val(dat.getDate( ));
                    break;
                case 'H':
                    $(this).val(dat.getHours());
                    break;
                case 'k':
                    $(this).val(dat.getHours() + 1 );
                    break;
                case 'K':
                    $(this).val(dat.getHours() % 12);
                    break;
                case 'h':
                    $(this).val(dat.getHours() % 12) + 1;
                    break;
                case 'm':
                    $(this).val(dat.getMinutes());
                    break;
                case 's':
                    $(this).val(dat.getSeconds());
                    break;
                case 'S':
                    $(this).val(dat.getMilliseconds());
                    break;
            }
        });
        return dat;
    }

    function _getdate(box, dat) {
        var pm = box.find("[data-date=La],[data-date=Sa]").val() == '1';
        box.find("[data-date]").each(function() {
            var flg = $( this ).data( "date" );
            var val = parseInt($(this).val( ));
            // 值被清空时则自动选中第一个日历项
            if (isNaN( val )) {
                var opt;
                opt = $(this).children().eq(0);
                val = parseInt(opt.val());
                opt.prop("selected",true);
            }
            switch (flg) {
                case 'LM':
                case 'SM':
                    dat.setMonth(val);
                    break;
                case 'M':
                    dat.setMonth(val - 1);
                    break;
                case 'y':
                    dat.setFullYear(val);
                    break;
                case 'Sy':
                    dat.setFullYear(val + 2000);
                    break;
                case 'd':
                    dat.setDate(val);
                    break;
                case 'H':
                    dat.setHours(val);
                    break;
                case 'k':
                    dat.setHours(val - 1);
                    break;
                case 'K':
                    dat.setHours(val + (pm ? 12 : 0));
                    break;
                case 'h':
                    dat.setHours(val + (pm ? 11 :-1));
                    break;
                case 'm':
                    dat.setMinutes(val);
                    break;
                case 's':
                    dat.setSeconds(val);
                    break;
                case 'S':
                    dat.setMilliseconds(val);
                    break;
            }
        });
        return dat;
    };

    function _makeGroups(grp) {
        var len = grp.length;
        var flg = grp.substring(0, 1);
        switch (flg) {
            case 'a':
                if (len >= 4) {
                    return _makeChoose("La", hsGetLang("date.La"));
                }
                else {
                    return _makeChoose("Sa", hsGetLang("date.Sa"));
                }
            case 'M':
                if (len >= 4) {
                    return _makeChoose("LM", hsGetLang("date.LM"));
                }
                if (len == 3) {
                    return _makeChoose("SM", hsGetLang("date.SM"));
                }
                else {
                    return _makeSelect( "M", 1, 12, len);
                }
            case 'y':
                if (len >= 4) {
                    return _makeSelect( "y", 1949, 2099, len);
                }
                else {
                    return _makeSelect("Sy", 0, 99, len);
                }
            case 'd':
                return _makeSelect(flg, 1, 31, len);
            case 'H':
                return _makeSelect(flg, 0, 23, len);
            case 'k':
                return _makeSelect(flg, 1, 24, len);
            case 'K':
                return _makeSelect(flg, 0, 11, len);
            case 'h':
                return _makeSelect(flg, 1, 12, len);
            case 'm':
                return _makeSelect(flg, 0, 59, len);
            case 's':
                return _makeSelect(flg, 0, 59, len);
            case 'S':
                return _makeSelect(flg, 0,999, len);
        }
    }

    function _makeChoose(flg, arr) {
        var s = '<select class="form-control datebox-'+flg+'" data-date="'+flg+'">';
        for(var j = arr.length, i = 0; i < j; i ++) {
            var v = arr[i];
            s += '<option value="'+ i +'">'+ v +'</option>';
        }
        s += '</select>';
        return $(s);
    }

    function _makeSelect(flg, min, max, len) {
        var s = '<select class="form-control datebox-'+flg+'" data-date="'+flg+'">';
        for(  ; min <= max  ; min ++  ) {
            var v  = _addzero(min, len);
            s += '<option value="'+min+'">'+ v +'</option>';
        }
        s += '</select>';
        return $(s);
    }

    function _makeAddons(txt) {
        var spn = $('<span class="input-group-addon"></span>');
        spn.text(txt.replace(/^'|'$/g , ""));
        return spn ;
    }

    function _makeInputs(fmt) {
        var box = $('<div class="input-group datebox"></div>');
        var pat = /([MdyHkKhmsSa]+|[^MdyHkKhmsSa']+|'.*?')/g;
        var wrd = /^[MdyHkKhmsSa]+$/;
        var grp ;
        while ((grp = pat.exec(fmt))!= null) {
            grp = grp[0];
            if (wrd.test(grp)) {
                box.append(_makeGroups(grp));
            } else {
                box.append(_makeAddons(grp));
            }
        }
        box.append(_makeAddons("").css("width", "100%")); // 占位撑长
        return box;
    }

    /**
     * 日期时间选择, 用下拉列表构建
     */
    $.fn.hsDate = function() {
        if (this.data("linked")) {
            return; // 跳过已初始化
        }

        var fmt = this.data("format") || hsGetLang("datetime.format");
        var inp = this;
        var box ;

        do {
            /**
             * 如果有查找到组件标记
             * 说明用的是自定义选项
             * 就不需要再构建选项了
             */
            box = inp.siblings().filter("[data-widget=hsDate]");
            if (box.size() != 0) {
                break;
            }
            box = inp.parent(  ).filter("[data-widget=hsDate]");
            if (box.size() != 0) {
                break;
            }

            // 也可以指定对象的类型
            // 从当前语言配置中提取
            switch (fmt) {
                case "time":
                    fmt = hsGetLang("time.format");
                    break;
                case "date":
                    fmt = hsGetLang("date.format");
                    break;
                case "datetime" : case "" : case null :
                    fmt = hsGetLang("datetime.format");
            }

            inp.addClass("invisible");
            box = _makeInputs ( fmt );
            box.insertBefore  ( inp );

            /**
             * 当前时间和取消设置等
             */
            box.find('.input-group-addon')
               .last()
               .css ("text-align","right")
               .html('<span class="today bi bi-hi-time"></span>'
                    +'<span class="clear bi bi-hi-delete"></span>');

            /**
             * 输入框组是不可嵌套的
             * 如果已经在输入框组里
             * 需要把选项向上提一层
             */
            if (inp.parent().is(".input-group")) {
                inp.before(box.contents());
                box.remove();
                box = inp.parent( );
                box.addClass ( "datebox" );
            }
        }
        while (false);

        box.addClass("datebox");
        box.attr("data-widget" , "hsDate");
        box.data("linked", inp);
        inp.attr("data-toggle" , "hsDate");
        inp.data("linked", box);

        inp.trigger ("change" );
    };

    /**
     * 日期时间输入, 用原生控件构建
     */
    $.fn.hsTime = function() {
        if (this.data("linked")) {
            return; // 跳过已初始化
        }

        var that = this;
        var type = this.attr("type"   );
        var kind = this.data("type"   );
        var frmt = this.data("format" );
        var patt = this.attr("pattern");
        var fset = this.data("offset" );
            fset = parseInt (fset || 0);

        // 没指定则按类型的标准处理
        if (!frmt || !patt) {
            if (type == "time") {
                frmt = "HH:mm" ;
                patt = "\\d{1,2}:\\d{1,2}" ;
            } else
            if (type == "date") {
                frmt = "yyyy-MM-dd";
                patt = "\\d{2,4}-\\d{1,2}-\\d{1,2}";
            } else
            {
                frmt = "yyyy-MM-ddTHH:mm";
                patt = "\\d{2,4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}";
            }
            this.data("format" , frmt);
            this.attr("pattern", patt);
            this.attr("placeholder" , frmt );
        }

        // 取表单项, 没有则添加一个
        var hide = this.next("input:hidden");
        if (! hide.size() ) {
            hide = this.prev("input:hidden");
        }
        if (! hide.size() ) {
            hide = $ ('<input type="hidden" class="form-ignored"/>');
            hide.attr("name", this.attr("name"));
            this.attr("name", "");
            // 避免在 input-group 中影响圆边角
            if (! this.index( ) ) {
                this.after (hide);
            } else {
                this.before(hide);
            }
        }
        this.data("linked", hide);
        hide.data("linked", this);

        // 互联互动
        this.on("change", function(e) {
            var v = $(this).val();
            if (v == "") {
                hide.val("");
                return ;
            }

            // 解析失败设为空
            var d = hsPrsDate(v, frmt);
            var t = d.getTime();
            if (isNaN(t)
            ||  0 === t) {
                hide.val("");
                return ;
            }

            // 可选择精确到秒
            if (kind == "timestamp"
            ||  kind == "datestamp") {
                t = Math.floor(t/1000);
            }   t = fset + t; // 偏移

            hide.val(t);

            // 自定事件可微调
            hide.trigger($.Event("change", {Date: d}));
        });
        hide.on("change", function(e) {
            var v = $(this).val();
            if (v == "") {
                that.val("");
                return ;
            }

            // 跳过自定的事件
            if (e.Date ) {
                return ;
            }

            // 需要转换为毫秒
            if (kind == "timestamp"
            ||  kind == "datestamp") {
                v = v * 1000;
            }
            var d = hsFmtDate(v, frmt);

            that.val(d);
        });

        // 初始同步
        if (this.val()) {
            this.change();
        } else
        if (hide.val()) {
            hide.change();
        }
    };

    // 处理大小月及闰年时日期的变化
    $(document).on("change", "[data-date=M],[data-date=SM],[data-date=LM]", function() {
        var box = $(this).closest(".datebox");
        var dat = new Date();

        // 不用 _getdate, 因 04/31 是 05/01
        var y = box.find(".datebox-y" ).val();
        if (y !== undefined) {
            dat.setFullYear( y );
        } else {
            y = box.find(".datebox-Sy").val();
        if (y !== undefined) {
            dat.setFullYear( y + 2000 );
        }
        }   dat.setMonth($(this).val( ) - 1 );

        _fixdate(box, dat);
    });

    // 控件组的值更新后联动日期输入
    $(document).on("change", "[data-widget=hsDate]", function(evt) {
        if ($(evt.target).data("toggle")=='hsDate') {
            return; // 跳过表单输入
        }
        if (! $(this).data("linked")) {
            return; // 跳过未初始化
        }

        var val , dat;
        var box = $(this);
        var inp = box.data("linked");
        var fmt = inp.data("format");
        var typ = inp.data( "type" );
        var off = inp.data("offset");
        var dat = new Date(1970, 0, 1, 0, 0, 0, 0);
            dat = _getdate(box, dat);
            off = parseInt(off || 0);

        /**
         * 偏移以便补充精度
         * 如在选日期需要到 23:59:59
         */
        if (typ == "timestamp"
        ||  typ == "datestamp") {
            off = off * 1000;
        }
        dat.setTime(dat.getTime() + off);

        switch (fmt) {
            case "time":
                fmt = hsGetLang("time.format");
                break;
            case "date":
                fmt = hsGetLang("date.format");
                break;
            case "datetime":
                fmt = hsGetLang("datetime.format");
                break;
            default : if (! fmt)
                fmt = hsGetLang("datetime.format");
        }

        switch (typ) {
            case "time":
            case "date":
                val = dat.getTime();
                break;
            case "timestamp":
            case "datestamp":
                val = dat.getTime() / 1000;
                val = Math.floor (  val  );
                break;
            default:
                val = hsFmtDate(dat , fmt);
        }

        inp.val(val);

        // 自定事件可微调
        inp.trigger($.Event("change", {Date: dat}));
    });

    // 表单项的值更改后联动日期控件
    $(document).on("change", "[data-toggle=hsDate]", function(evt) {
        if ($(evt.target).data("toggle")!='hsDate') {
            return; // 跳过非表单项
        }
        if (! $(this).data("linked")) {
            return; // 跳过未初始化
        }
        if (evt.Date) {
            return; // 跳过自定事件
        }

        var inp = $(this);
        var box = inp.data("linked");
        var fmt = inp.data("format");
        var typ = inp.data( "type" );
        var val = inp.val (   );
        var num = parseInt(val);

        switch (fmt) {
            case "time":
                fmt = hsGetLang("time.format");
                break;
            case "date":
                fmt = hsGetLang("date.format");
                break;
            case "datetime":
                fmt = hsGetLang("datetime.format");
            default : if (! fmt)
                fmt = hsGetLang("datetime.format");
        }

        /**
         * 根据 type 确定精度
         * 缺失则将选项置为空
         */
        if (!isNaN(num)
        && (typ == "timestamp"
        ||  typ == "datestamp")) {
            val =  num * 1000  ;
        }
        if (! val ) {
            box.find( "select" )
               .val ( "" );
            inp.val ( "" );
            return;
        }

        var dat = hsPrsDate(val, fmt);
        _setdate( box, dat );
        _fixdate( box, dat );
    });

    // 设为当前时间
    $(document).on("click", ".datebox .today", function() {
        var box = $(this).closest(".datebox");
        var dat = new Date();
        _setdate( box, dat );
        _fixdate( box, dat );
        box.change();
    });

    // 清除所有选项
    $(document).on("click", ".datebox .clear", function() {
        var sel = $(this).closest(".datebox").find("select");
        var inp = $(this).closest(".datebox").data("linked");
        sel.val ("");
        inp.val ("");
        inp.change();
    });

    // 加载就绪后自动初始化日期控件
    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=hsDate]").each(function() {
            $(this).hsDate();
        });
        $(this).find("[data-toggle=hsTime]").each(function() {
            $(this).hsTime();
        });
    });

})(jQuery);
