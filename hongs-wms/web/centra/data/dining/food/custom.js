
function in_centra_data_dining_food_form(context) {
    // 添加套餐选择列表
    var id      = H$("#id", context);
    var loadUrl = 'centra/data/dining/item_edit_list.html?unit=food&unit_id='+id;
    var typeInp = context.find   ("[name=type]");
    var typeBox = typeInp.closest(".form-group");
    var itemBox = $(
          '<div class="row form-group">'
        + '<div class="col-sm-6 col-sm-offset-3" data-load="'+loadUrl+'">'
        + '</div>'
        + '</div>'
    );
    itemBox.insertAfter(typeBox).hsReady();
    
    // 选择套餐时显示列表
    context.on("change", "[name=type]", function() {
        if ($.inArray($(this).val(), ["套餐", "组合"]) !== -1) {
            itemBox.show();
        } else {
            itemBox.hide();
        }
    });
}