
function in_centra_data_upland_topic_list(context, listobj) {
    // 去掉批量操作
    context.find(".toolbox .for-choose").remove();
    context.find(".toolbox .for-checks").remove();
    context.find(".listbox [data-ft=_check]")
           .attr("data-ft", "_rowid")
           .attr("data-fn", /**/"id")
           .attr( "class" , "_rowid");

    listobj._fill_name = function(td, v) {
        $('<a href="javascript:;" class="review"></a>')
            .appendTo(td)
            .text(v);
    }
}

function in_centra_data_upland_topic_info(context, formobj) {
    var id = H$("?id", context);
    context.after($('<div></div>').hsLoad("centra/data/upland/tweet/list.html?topic_id=" + id,
    function() {
        $(this).find("button.create").text("发布评论");
        $(this).children ( "h2" ).hide(); // 隐藏标题
    }));
    context.find("button.cancel").hide(); // 隐藏返回
}
