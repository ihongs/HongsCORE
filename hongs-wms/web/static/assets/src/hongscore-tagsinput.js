/**
 * Tags 国际化及初始化
 * Huang Hong <ihongs@live.cn>
 * @param $ jQuery
 */
(function($) {
    if (!$.fn.tagsinput) {
        return;
    }

    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=tagsinput]").each(function() {
            if ($(this).data("tagsinput")) {
                return;
            }
            
            /**/$(this).tagsinput();
            if ($(this).data("fl")) {
                $(this).data("fl", "$.isArray(v) ? v.join(',') : v");
            }
        });
    });
})(jQuery);
