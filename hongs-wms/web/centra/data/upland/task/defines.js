
function in_centra_data_upland_task(context) {
    // 去掉额外功能
    context.find(".bi-hi-manual").closest("li").hide();
    context.find(".bi-hi-reveal").closest("li").hide();
}

function in_centra_data_upland_task_list(context, listobj) {
    context.find(".toolbox .create").text("新建任务");

    // 去掉批量操作
    context.find(".toolbox .for-choose").remove();
    context.find(".toolbox .for-checks").remove();
    context.find(".listbox [data-ft=_admin]").remove();
    context.find(".listbox [data-ft=_check]")
           .attr("data-ft", "_rowid")
           .attr("data-fn", /**/"id")
           .attr( "class" , "_rowid");

    // 点击标题打开
    listobj._fill_name = function(td, tt) {
        $('<a href="javascript:;" class="recite"></a>')
            .appendTo(td)
            .text    (tt);
    }
}

function in_centra_data_upland_task_info(context, formobj) {
    if (context.is(".base-info,.review-info,.recite-info"))
    context.one("loadOver", function(evt, rst) {
        var adm = H$("!centra/data/upland/lead") || window._THEME_ADMIN_;
        var uid = H$('%HsCUID' );
        var tid = rst.info.id   ;
        var pid = rst.info.theme_id;
        var own = rst.info.owner;
        var grd = rst.info.grade;

        // 网址附加上ID
        location.hash = "#"+ pid +"/"+ tid;
        formobj.loadBox.parent().on("hsShow", function() {
        location.hash = "#"+ pid +"/"+ tid;
        });

        // 话题管理标识
        if (! adm && own ) {
            $.each ( own , function() {
                if ( uid == this.id ) {
                     adm =  true;
                }
            });
        }
        window._TOPIC_ADMIN_ = adm;

        // 建立分栏页签
        var tabs = $(
            '<ul class="nav nav-tabs board">'
          + '<li class="active"><a href="javascript:;">任务</a></li>'
          + '<li><a href="javascript:;" data-href="centra/data/upland/tick/list.html?topic_id='+tid+'">评论列表</a></li>'
          + '</ul>'
        );
        var labs = $(
            '<div></div>'
        );
        var pane  = $(
            '<div></div>'
        );
        tabs.insertBefore(context);
        labs.insertBefore(context);
        labs.append(context);
        labs.append(pane);
        tabs.hsTabs(labs);

        // 追加操作按钮
        if (adm) {
            context.find(".form-foot .btn-toolbar")
                .append('<div class="btn-group">'
                    + '<button type="button" class="update2 btn btn-default">修改任务</button>'
                    + '<button type="button" class="reveal2 btn btn-default">历史记录</button>'
                    + '<button type="button" class="delete2 btn btn-default"><span class="text-danger">删除</span></button>'
                + '</div>')
                .on("click", ".update2", function() {
                    context.hsFind("@").hsOpen("centra/data/upland/task/form.html?id=" + tid,
                    function( ) {
                        $(this).on("saveBack", function(ev, sd) {
                            if (sd.ok) {
                                formobj.load();
                            }
                        });
                    });
                })
                .on("click", ".reveal2", function() {
                    context.hsFind("@").hsOpen("centra/data/upland/task/snap.html?id=" + tid,
                    function( ) {
                        $(this).on("sendBack", function(ev, sd) {
                            if (sd.ok) {
                                formobj.load();
                            }
                        });
                    });
                })
                .on("click", ".delete2", function() {
                    $.hsWarn("确定要删除这个任务吗?", "warning",
                        function() {
                            $.hsAjax({
                                url : "centra/data/upland/task/delete.act?id=" + tid,
                                success: function (sd) {
                                    if (sd.ok) {
                                        context.trigger("saveBack")
                                            .hsFind("@").hsClose( );
                                    }
                                }
                            });
                        } ,
                        function() {
                            // Cancel
                        });
                });
        }
    });
}

function in_centra_data_upland_task_form(context, formobj) {
    context.find("textarea[name=body]").css({height: "30em", maxHeight: "90em"});
}