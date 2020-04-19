/**
 * Tags 国际化及初始化
 * Huang Hong <ihongs@live.cn>
 * @param $ jQuery
 */
(function($) {
    if (!$.fn.tagsinput) {
        return;
    }

    $.fn.hsTagsInput = function() {
        $(this).each ( function() {
            if ($(this).data("tagsinput")) {
                return;
            }

            $(this).tagsinput({
                trimValue  : true,
                confirmKeys: [ 13,44,59, 0xff0c,0xff1b ]
            });

            // 微调样式以融入环境
            // 并阻止回车导致提交
            $(this).siblings ( ".bootstrap-tagsinput"  )
                   .attr("class", $(this).attr("class"))
                   .addClass (  "bootstrap-tagsinput"  )
                   .attr("style", $(this).attr("style"))
                   .show()
                   .on("keypress",function(e) {
                        if (e.keyCode === 13) {
                            e.stopPropagation();
                            return false;
                        }
                    });

            // 表单项改变时需重建
            $(this).on( "change" ,function(e) {
                var ti = $(this).data("tagsinput");
                $('.tag' , ti.$container).remove();
                $('option' , ti.$element).remove();
                while (ti.itemsArray.length  >  0) {
                    ti.itemsArray.pop(  );
                }
                ti.add($(this).val(), !0);
            });
        });
        return  this;
    };

    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=tagsinput]").hsTagsInput();
    });
})(jQuery);
