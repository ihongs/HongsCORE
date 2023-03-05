
function in_centra_data_upland_tweet(context) {
    // 去掉额外功能
    context.find(".bi-hi-manual").closest("li").hide();
    context.find(".bi-hi-reveal").closest("li").hide();
}

function in_centra_data_upland_tweet_list(context, listobj) {
    var req = listobj._data;
    context.find(".toolbox .create").text("发表评论")
        .removeClass("create")
        .addClass("create2")
        .click(function() {
            listobj.open(this, null, "centra/data/upland/tweet/form_init.html", req);
        });

    // 去掉批量操作
    context.find(".toolbox .for-choose").remove();
    context.find(".toolbox .for-checks").remove();
    context.find(".listbox [data-ft=_admin]").remove();
    context.find(".listbox [data-ft=_check]")
           .attr("data-ft", "_rowid")
           .attr("data-fn", /**/"id")
           .attr( "class" , "_rowid");

    // 增加评论操作
    context.find(".listbox thead tr").append(
        '<th data-fn="_" data-ft="_admin" class="_admin _amenu">'
      + '<a href="javascript:;" class="retort" title="回复评论">'
      + '<i class="bi bi-hi-create"></i>'
      + '</a>'
      + '<a href="javascript:;" class="delete" title="删除评论">'
      + '<i class="bi bi-hi-remove"></i>'
      + '</a>'
      + '</th>'
    );
    listobj._fill__admin = function(td) {
        HsList.prototype._fill__admin.call(this, td);
        if (!window._THEME_ADMIN_) {
            td.find("a").addClass("disabled")
                     .removeClass( "delete" );
        }
    };
}
