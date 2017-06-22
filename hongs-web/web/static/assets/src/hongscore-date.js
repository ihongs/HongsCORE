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
        var flg = grp.substr(0, 1);
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
                    return _makeSelect( "y", 1970, 2099, len);
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
        spn.text ( txt );
        return spn ;
    }

    function _makeInputs(fmt) {
        var box = $('<div class="input-group datebox" data-widget="hsDate"></div>');
        var pat = /(\w+|\W+|'.*?')/g;
        var wrd = /^\w+$/;
        var grp ;
        while ((grp = pat.exec(fmt)) != null) {
            grp = grp[0];
            if (wrd.test(grp)) {
                box.append(_makeGroups(grp));
            } else {
                box.append(_makeAddons(grp));
            }
        }
        return box;
    }

    $.fn.hsDate = function() {
        if (this.data("hsDate")) {
            return;
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

            inp.addClass("invisible");
            box = _makeInputs ( fmt );
            box.insertBefore  ( inp );

            /**
             * 输入框组是不可嵌套的
             * 如果已经在输入框组里
             * 需要把选项向上提一层
             */
            if (inp.parent().is(".input-group")) {
                inp.before(box.contents( ));
                box.remove();
                box = inp.parent();
                box.addClass("datebox");
                box.data("widget","hsDate");
            }
        }
        while (false);

        inp.data("hsDate", box);
        box.data("hsDate", inp);
        inp.trigger( "change" );
    };

    // 处理大小月及闰年时日期的变化
    $(document).on("change", "[data-date=M],[data-date=SM],[data-date=LM]", function() {
        var y = $(this).closest(".input-group")
                .find("[data-date=y],[data-date=Sy]").val();
        var d = $(this).closest(".input-group")
                .find("[data-date=d]");
        var m = $(this).val( );
        if (y < 100) y += 2000;
        if (m == 2) {
            if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) {
                d.find("option:gt(28)")
                 .prop("selected", false)
                 .hide();
            } else {
                d.find("option:gt(27)")
                 .prop("selected", false)
                 .hide();
            }
        } else {
            if ((m < 7 && m % 2 == 0) || (m > 7 && m % 2 == 1)) {
                d.find("option:gt(29)")
                 .prop("selected", false)
                 .hide();
            } else {
                d.find("option:gt(27)")
                 .show();
            }
        }
    });

    // 控件组的值更新后联动日期输入
    $(document).on("change", "[data-widget=hsDate]", function(evt) {
        if ($(evt.target).data("toggle")=='hsDate') {
            return; // 跳过表单输入
        }
        if (! $(this).data("hsDate")) {
            return; // 跳过未初始化
        }

        var box = $(this);
        var inp = box.data("hsDate");
        var typ = inp.data( "type" );
        var fmt = inp.data("format") || hsGetLang("datetime.format");
        var dat = _getdate(box, new Date(0));
        var val ;

        switch (typ) {
            case "time":
                val = dat.getTime();
                break;
            case "timestamp":
                val = dat.getTime() / 1000;
                break;
            default:
                val = hsFmtDate(dat , fmt);
        }

        var evt = $.Event("change", {"date": dat});
        inp.val    (val);
        inp.trigger(evt);
    });

    // 表单项的值更改后联动日期控件
    $(document).on("change", "[data-toggle=hsDate]", function(evt) {
        if ($(evt.target).data("toggle")!='hsDate') {
            return; // 跳过非表单项
        }
        if (! $(this).data("hsDate")) {
            return; // 跳过未初始化
        }

        var inp = $(this);
        var box = inp.data("hsDate");
        var typ = inp.data( "type" );
        var fmt = inp.data("format") || hsGetLang("datetime.format");
        var val = inp.val (   );
        var num = parseInt(val);

        // 外部可以指定仅仅精确到秒
        if (! isNaN(num) && (typ== "timestamp" || typ== "datestamp")) {
            val = num * 1000;
        }

        _setdate(box, hsPrsDate(val, fmt));
    });

    // 加载就绪后自动初始化日期控件
    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=hsDate]").each(function() {
            $(this).hsDate();
        });
    });

})(jQuery);
