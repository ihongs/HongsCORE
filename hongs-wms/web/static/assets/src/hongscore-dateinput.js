/**
 * Datetime 国际化及启动器
 * Huang Hong <ihongs@live.cn>
 * @param $ jQuery
 */
(function($){
    if (!$.fn.datetimepicker) {
        return;
    }

    $.fn.datetimepicker.dates.en = {
        months      : hsGetLang("date.LM"),//["一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月", "十二月"],
        monthsShort : hsGetLang("date.SM"),//["一","二","三","四","五","六","七","八","九","十","十一","十二"],
        days        : hsGetLang("date.LE"),//["星期日","星期一","星期二","星期三","星期四","星期五","星期六"],
        daysShort   : hsGetLang("date.SE"),//["日","一","二","三","四","五","六"],
        daysMin     : hsGetLang("date.SE"),//["日","一","二","三","四","五","六"],
        meridiem    : hsGetLang("time.Sa"),//["AM","PM"],
        weekStart   : hsGetLang("week.start"),//1
        today       : hsGetLang("date.today"),//今天
        format      : _hs2bsDF(hsGetLang("datetime.format")),//yyyy-mm-dd HH:ii
        suffix      : []
    };

    $.fn.hsDateInput = function() {
        $(this).each ( function() {
            if ($(this).data("datetimepicker")) {
                return;
            }

            var that = $(this);
            var attr;
            var opts;

            var mrep = function(v) {
                if (!/^(\{.*\})$/.test( v ) ) {
                        v  = '{'+v+'}' ;
                }
                return  eval('('+v+')');
            };

            // 基础配置
            attr = that.attr( "data-config" );
            if (attr) {
                opts = mrep.call(this , attr);
            } else {
                opts = {};
            }
            if (opts.autoclose === undefined) {
                opts.autoclose  =  true;
            }
            if (opts.todayBtn  === undefined) {
                opts.todayBtn   =  true;
            }
            if (opts.todayHighlight === undefined) {
                opts.todayHighlight  =  true;
            }

            // 获取格式
            attr = that.attr("data-format" );
            if (attr) {
                opts.format = _hs2bsDF(attr);
            } else
            if (that.is(".input-date")) {
                opts.format = _hs2bsDF(hsGetLang("date.format"));
            } else
            if (that.is(".input-time")) {
                opts.format = _hs2bsDF(hsGetLang("time.format"));
            } else
            {
                opts.format = _hs2bsDF(hsGetLang("datetime.format"));
            }

            // 获取位置
            attr = that.attr("data-position");
            if (attr) {
                opts.pickerPosition  =  attr ;
            }

            // 根据格式选择视图
            if (/p/i.test(opts.format)) {
                opts.showMeridian = true;
            }
            if (/i/ .test(opts.format)) {
                opts.startView = 0;
                opts.minView = 0;
                if (!/[md]/.test(opts.format)) {
                if (!/[Hh]/.test(opts.format)) {
                    opts.maxView = 0;
                } else {
                    opts.maxView = 1;
                } }
            } else
            if (/h/i.test(opts.format)) {
                opts.startView = 1;
                opts.minView = 1;
                if (!/[md]/.test(opts.format)) {
                    opts.maxView = 1;
                }
            } else
            if (/d/ .test(opts.format)) {
                opts.startView = 2;
                opts.minView = 2;
            } else
            if (/m/ .test(opts.format)) {
                opts.startView = 3;
                opts.minView = 3;
            } else
            if (/y/ .test(opts.format)) {
                opts.startView = 4;
                opts.minView = 4;
            }

            that.datetimepicker( opts );
            that.datetimepicker("show");
        });
        return  this;
    };

    $(document).on("focus", "[data-toggle=dateinput],[data-toggle=datetimepicker]",
    function( ) {
        $(this).hsDateInput();
    });
})(jQuery);

/**
 * HongsCORE日期格式转Bootstrap日期格式
 * @param {String} format
 * @return {String)
 */
function _hs2bsDF(format) {
  return format.replace(/a/g , 'P')
               .replace(/m/g , 'i')
               .replace(/M/g , 'm')
               // 交换 H h
               .replace(/H/g , 'x')
               .replace(/h/g , 'H')
               .replace(/x/g , 'h');
}

/**
 * Bootstrap日期格式转HongsCORE日期格式
 * @param {String} format
 * @return {String)
 */
function _bs2hsDF(format) {
  return format.replace(/m/g , 'M')
               .replace(/i/g , 'm')
               .replace(/P/gi, 'a')
               // 交换 H h
               .replace(/H/g , 'x')
               .replace(/h/g , 'H')
               .replace(/x/g , 'h');
}
