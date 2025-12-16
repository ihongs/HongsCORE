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

            var data = $(this).hsData();

            $(this).bsSuggest({
                url : data.href || null,
                data: {value: data.list || []},
                indexId : data.indexId  || 0 ,
                indexKey: data.indexKey || 0 ,
                idField : data.fieldId  || '',
                keyField: data.fieldKey || '',
                ignorecase  : hsToBool(data.ignorecase  , true),
                autoDropup  : hsToBool(data.autoDropup  , true),
                hideOnSelect: hsToBool(data.hideOnSelect, true)
            });
        });
        return  this;
    };

    $(document).on("cursor", "[data-toggle=sugginput],[data-toggle=suggest]",
    function( ) {
        $(this).hsSuggInput();
    });
})(jQuery);

