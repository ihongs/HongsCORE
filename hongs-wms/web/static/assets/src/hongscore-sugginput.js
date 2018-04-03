/**
 * Suggest 国际化及启动器
 * Huang Hong <ihongs@live.cn>
 * @param $ jQuery
 */
(function($){
    if (!$.fn.bsSuggest) {
        return;
    }
    
    $.fn.hsSuggInput = function() {
        $(this).each ( function() {
            if ($(this).data("bsSuggest")) {
                return;
            }

            $(this).bsSuggest({
                url: $(this).data("href")
            });
        });
        return  this;
    };
    
    $(document).on("cursor", "[data-toggle=sugginput],[data-toggle=suggest]",
    function( ) {
        $(this).hsSuggInput();
    });
})(jQuery);

