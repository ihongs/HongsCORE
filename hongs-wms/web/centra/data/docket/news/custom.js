
function in_centra_data_docket_news_list(context) {
    context.find  ('[data-fn="statist.name"]')
           .before('<th data-fn="statist.impress_count" data-fl="v||0">浏览</th>')
           .before('<th data-fn="statist.comment_count" data-fl="v||0">评论</th>')
           .before('<th data-fn="statist.dissent_count" data-fl="v||0">举报</th>')
           .before('<th data-fn="statist.endorse_count" data-fl="v||0">赞</th>')
           .before('<th data-fn="statist.undorse_count" data-fl="v||0">踩</th>')
           .before('<th data-fn="statist.endorse_score" data-fl="v||0">得分</th>')
           .remove();
   
   // 查看直接打开文章页面
   var listObj = context.data("HsList");
   listObj.open = function(btn, box, url, req) {
       if (btn.is(".review") ) {
           var id = hsGetSeria ( hsSerialArr( url ), "id" );
           window.open(hsFixUri("public/docket/news/" + id), "_blank");
           return;
       }
       HsList.prototype.open.call(this, btn, box, url, req);
   };
}

function in_centra_data_docket_news_form(context) {
    var text = context.find("[data-type=html]");
    var area = text.parent();
    var help = area.next(  ).insertBefore(area);
    text.height("25em");
    help.removeClass("col-sm-3").addClass("col-sm-6");
    area.removeClass("col-sm-6").addClass("col-sm-8").addClass("col-sm-offset-2");
}
