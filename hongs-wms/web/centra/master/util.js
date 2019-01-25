
function hsUserMove(treebox, listbox) {
    listbox.on("loadOver", function(evt, rst, mod) {
        var did = hsGetParam(mod._url , "dept_id");
        listbox.find(".listbox tbody tr")
        .draggable({
            opactiy: 0.5,
            revert: "invalid",
            helper: function() {
                var uid = $(this).find("td:eq(0) input").val  ();
                var img = $(this).find("td:eq(1) span" ).clone();
                var txt = $(this).find("td:eq(2)"      ).text ();
                var dis = $(this).data("dept_ids"      );
                return $('<div data-type="user"></div>')
                        .data("user_id" , uid)
                        .data("dept_id" , did)
                        .data("dept_ids", dis)
                        .append(img)
                        .append(txt);
            }
        });
    });

    treebox.on("loadOver", function(evt, rst, mod) {
        treebox.find(".tree-node" /***/ )
        .draggable({
            opacity: 0.5,
            revert: "invalid",
            helper: function() {
                return $('<div data-type="dept"></div>')
                        .text( $(this).children("table")
                        .find( ".tree-name" ).text( )  );
            }
        });

        treebox.find(".tree-node table" )
        .droppable({
            drop: function(ev, ui) {
                var pid = $(this).parent().attr("id").substring(10);
                if (ui.helper.data("type") == "dept") {
                    var did = ui.draggable.attr("id").substring(10);
                    var req = { id : did, pid : pid };
                    $.hsView({
                            "alert": "static",
                            "class": "alert-success",
                            "title": "您确定将此部门移到新部门下吗?"
                        },
                        {
                            "label": "移动",
                            "click": function() {
                                $.ajax({
                                    url     : hsFixUri("centra/master/dept/save.act"),
                                    data    : hsSerialArr(req),
                                    type    : "POST",
                                    dataType: "JSON",
                                    cache   : false,
                                    global  : false,
                                    success : function(rst) {
                                        rst = hsResponse(rst);
                                        if (rst.ok) {
                                            var mod = treebox.find(".HsTree").data("HsTree");
                                            mod.load(mod.getPid(did));
                                            mod.load(pid);
                                        }
                                    }
                                });
                            }
                        },
                        {
                            "label": "取消",
                            "class": "btn-link"
                        }
                    );
                } else {
                    var uid = ui.helper.data("user_id" );
                    var did = ui.helper.data("dept_id" );
                    var dis = ui.helper.data("dept_ids").slice(0);
                    $.hsView({
                            "alert": "static",
                            "class": "alert-success",
                            "title": "您需要将用户移动到新部门, 还是移补或增挂到新部门?",
                            "notes": "移动操作将取消此用户与前部门的关联, 移补操作退出当前部门但保留其他关联, 增挂就是他可以既在新部门又在旧部门."
                        },
                        {
                            "label": "移动",
                            "click": function() {
                                var diz = [pid];
                                var req = new HsSerialDic({id: uid, "depts..dept_id": diz});
                                $.ajax({
                                    url     : hsFixUri("centra/master/user/save.act"),
                                    data    : hsSerialArr(req),
                                    type    : "POST",
                                    dataType: "JSON",
                                    cache   : false,
                                    global  : false,
                                    success : function(rst) {
                                        rst = hsResponse(rst);
                                        if (rst.ok) {
                                            var mod = listbox.find(".HsList").data("HsList");
                                            mod.load();
                                        }
                                    }
                                });
                            }
                        },
                        {
                            "label": "移补",
                            "click": function() {
                                del( dis, did );
                                dis.push( pid );
                                var req = new HsSerialDic({id: uid, "depts..dept_id": dis});
                                $.ajax({
                                    url     : hsFixUri("centra/master/user/save.act"),
                                    data    : hsSerialArr(req),
                                    type    : "POST",
                                    dataType: "JSON",
                                    cache   : false,
                                    global  : false,
                                    success : function(rst) {
                                        rst = hsResponse(rst);
                                        if (rst.ok) {
                                            var mod = listbox.find(".HsList").data("HsList");
                                            mod.load();
                                        }
                                    }
                                });
                            }
                        },
                        {
                            "label": "增挂",
                            "click": function() {
//                              del( dis, did );
                                dis.push( pid );
                                var req = new HsSerialDic({id: uid, "depts..dept_id": dis});
                                $.ajax({
                                    url     : hsFixUri("centra/master/user/save.act"),
                                    data    : hsSerialArr(req),
                                    type    : "POST",
                                    dataType: "JSON",
                                    cache   : false,
                                    global  : false,
                                    success : function(rst) {
                                        rst = hsResponse(rst);
                                        if (rst.ok) {
                                            var mod = listbox.find(".HsList").data("HsList");
                                            mod.load();
                                        }
                                    }
                                });
                            }
                        },
                        {
                            "label": "取消",
                            "class": "btn-link"
                        }
                    );
                }
            }
        });
    });

    function del(arr, val) {
        for(var i = 0; i < arr.length; i ++) {
            if (arr[i] === val) {
                arr.splice(i,1);
                break;
            }
        }
    }
}