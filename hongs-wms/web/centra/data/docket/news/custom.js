
document.write('<script type="text\/javascript" src="centra\/data\/docket\/ueditor\/config.js"><\/script>');
document.write('<script type="text\/javascript" src="static\/addons\/ueditor\/ueditor.all.min.js"><\/script>');

function in_centra_data_docket_news_list(context) {
    context.find  ('[data-fn="statist.name"]')
           .before('<th data-fn="statist.impress_count" data-fl="v||0">浏览</th>')
           .before('<th data-fn="statist.comment_count" data-fl="v||0">评论</th>')
           .before('<th data-fn="statist.dissent_count" data-fl="v||0">举报</th>')
           .before('<th data-fn="statist.endorse_count" data-fl="v||0">赞</th>')
           .before('<th data-fn="statist.undorse_count" data-fl="v||0">踩</th>')
           .before('<th data-fn="statist.endorse_score" data-fl="v||0">得分</th>')
           .remove();
}

function in_centra_data_docket_news_form(context) {
    var loadbox = context.closest(".loadbox");

    // 启用编辑器
    context.find("[name=body]")
           .after('<textarea cols="50" name="body" id="body-html"></textarea>')
           .remove();
    context.on("loadOver", function(evt, rst) {
        self.AE = UE.getEditor( "body-html" );
    });
    loadbox.on("hsClose" , function() {
        if (self.AE) {
            self.AE.destroy();
        }
    });
}

