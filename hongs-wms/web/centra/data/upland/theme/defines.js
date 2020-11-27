
function in_centra_data_upland_theme_list(context, listobj) {
    context.find(".toolbox .create").text("新建主题");

    // 去掉批量操作
    context.find(".toolbox .for-choose").remove();
    context.find(".toolbox .for-checks").remove();
    context.find(".listbox [data-ft=_admin]").remove();
    context.find(".listbox [data-ft=_check]")
           .attr("data-ft", "_rowid")
           .attr("data-fn", /**/"id")
           .attr( "class" , "_rowid");

    // 点击标题打开
    listobj._fill_name = function(td, v) {
        $('< a href=" " class="review"></ a>')
            .appendTo(td)
            .text(v);
    }
}

function in_centra_data_upland_theme_info(context, formobj) {
    context.find(".btns-group" ).hide();
    context.on("loadOver", function(evt, rst) {
        var adm = H$("!centra/data/upland/admin");
        var uid = H$('%HsCUID' );
        var tid = rst.info.id   ;
        var own = rst.info.owner;
        var grd = rst.info.grade;

        // 主题管理标识
        if (! adm && own ) {
            $.each ( own , function() {
                if ( uid == this.id ) {
                     adm =  true;
                }
            });
        }
        window._THEME_ADMIN_ = adm;

        // 加入话题列表, 重建操作按钮
        var box = context.children('.loadbox');
        if (box.size() === 0) {
            box = $('<div class="loadbox"></div>')
                . appendTo(context);
            box.on("loadOver", function(e) {
                e.stopPropagation(); // 避免引起上级重载
            });
            box.on("click", ".cancel2", function() {
                context.hsFind("@").hsClose();
            });
            box.on("click", ".delete2", function() {
                $.hsAjax({
                    url : "centra/data/upland/theme/delete?id=" + tid,
                    success: function (sd) {
                        if (sd.ok) {
                            context.hsFind("@").hsClose();
                        }
                    }
                });
            });
            box.on("click", ".update2", function() {
                context.hsFind("@").hsOpen("centra/data/upland/theme/form.html?id=" + tid,
                function( ) {
                    $(this).on("saveBack", function(ev, sd) {
                        if (sd.ok) {
                            formobj.load();
                        }
                    });
                });
            });
            box.on("click", ".reveal2", function() {
                context.hsFind("@").hsOpen("centra/data/upland/theme/snap.html?id=" + tid,
                function( ) {
                    $(this).on("sendBack", function(ev, sd) {
                        if (sd.ok) {
                            formobj.load();
                        }
                    });
                });
            });
        }
        box.hsLoad("centra/data/upland/topic/list.html?theme_id=" + tid,
        function( ) {
            $(this).children( "h1,h2" ).hide( );
            $(this).find(".toolbox .btn-group")
            .empty ()
            .append('<button type="button" class="btn btn-default cancel2">'
                  + '返回'
                  + '</button>'
            )
            .append('<button type="button" class="btn btn-default create ">'
                  + '新建话题'
                  + '</button>'
            )
            .append('<button type="button" class="btn btn-default manage2 dropdown-toggle" data-toggle="dropdown">'
                  + '<span class="caret"></span>'
                  + '</button>'
            )
            .append('<ul class="dropdown-menu">'
                  + '<li><a href="javascript:;" class="update2">修改当前主题</a></li>'
                  + '<li><a href="javascript:;" class="delete2">删除当前主题</a></li>'
                  + '<li><a href="javascript:;" class="reveal2">浏览主题历史</a></li>'
                  + '</ul>'
            );
            if (! adm && grd != 1 ) {
                $(this).find("button.create" ).remove();
            }
            if (! adm ) {
                $(this).find("button.manage2").remove();
            }
        });
    });
}
