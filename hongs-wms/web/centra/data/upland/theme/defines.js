
function in_centra_data_upland_theme_list(context, listobj) {
    listobj._fill_name = function(td, v) {
        td.append($('<a href="javascript:;" class="review"></a>').text(v));
    }
}

function in_centra_data_upland_theme_info(context, formobj) {
    var id = H$("?id", context);
    context.after($('<div></div>').hsLoad("centra/data/upland/topic/list.html?theme_id=" + id,
    function() {
        $(this).find("button.create").text("发布话题");
        $(this).children ( "h2" ).hide(); // 隐藏标题
    }));
    context.find("button.cancel").hide(); // 隐藏返回
}
