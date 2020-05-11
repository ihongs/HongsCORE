
function hsUserMove(treebox, listbox) {
    var top_ids = {};

    // 顶层需禁止操作
    treebox.on("treeSelect", ".tree-node>table", function(ev, id) {
        if (top_ids[id]) {
            if (id == 0)ev.preventDefault(); // 受限时原始顶层不要打开用户列表
            treebox.find(".for-select")
                   .prop("disabled", true );
        } else {
            treebox.find(".for-select")
                   .prop("disabled", false);
        }
    });

    listbox.on("loadOver", function(evt, rst, mod) {
        var did = hsGetParam(mod._url , "dept_id");

        // 非管辖范围提示
        if (top_ids[did]) {
            listbox.find(".create" )
                   .prop("disabled", true );
            listbox.find(".pagebox .alert")
                   .text("您可以管理左侧的下级分组中的用户, 或向其下添加新的分组.");
            listbox.find(".findbox .input-search")
                   .attr("placeholder", "可以搜全部用户哦");
            listbox.find(".listbox").addClass("on-top");
        } else
        if (did === "0" ) {
            listbox.find(".findbox .input-search")
                   .attr("placeholder", "可以搜全部用户哦");
            listbox.find(".listbox").addClass("on-top");
        }

        // 拖拽用户
        listbox.find(".listbox tbody tr td:nth-child(3)"
                   +",.listbox tbody tr td:nth-child(4)")
        .draggable({
            opactiy: 0.5,
            revert: "invalid",
            helper: function() {
                var tr  = $(this).closest("tr");
                var uid = tr.find("td:eq(0) input").val();
                var img = tr.find("td:eq(2)").clone();
                var txt = tr.find("td:eq(3)").clone();
                var dis = tr.data("dept_ids");
                return $('<table data-type="user"></table>')
                        .data("user_id" , uid)
                        .data("dept_id" , did)
                        .data("dept_ids", dis)
                        .append($('<tr></tr>')
                                  .append(img)
                                  .append(txt)
                        );
            }
        });
    });

    treebox.on("loadOver", function(evt, rst, mod) {
        // 非管辖范围处理
        if (rst.scope == 0
        &&  rst._pid  == 0 ) {
            var  pid  ;
            for(var i = 0; i < rst.list.length; i ++) {
                var d = rst.list[i]["id"];
                if (i < 1) pid = d;
                top_ids[d] = true ;
            }
            top_ids[ "0" ] = true ;
            mod.select (pid);
            mod.toggle (pid);
            mod.getNode("0").children("table").hide();
        }

        // 拖拽分组
        treebox.find(".tree-node .tree-name" )
        .draggable({
            opacity: 0.5,
            revert: "invalid",
            helper: function() {
                var txt = $(this).clone();
                return $('<table data-type="dept"></table>')
                        .append($('<tr></tr>').append(txt) );
            }
        });

        // 分组接收
        treebox.find(".tree-node table" )
        .droppable({
            accept: function(item) {
                return ! item.is(".tree-name")
                    || ! $.contains(item.closest(".tree-node").get(0), $(this).get(0));
            },
            drop: function(ev, ui) {
                var pid = $(this).parent().closest(".tree-node").attr("id").substring(10);
                if (ui.helper.data("type") == "dept") {
                    var did = ui.draggable.closest(".tree-node").attr("id").substring(10);
                    var req = { id : did, pid : pid };
                    $.hsMask({
                            "mode" : "warn",
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
                    var del = hsUserDel_;
                    $.hsMask({
                            "mode" : "warn",
                            "class": "alert-success",
                            "title": "您需要将用户移动到新部门, 还是移补或增挂到新部门?",
                            "text" : "移动操作将取消此用户与前部门的关联, 移补操作退出当前部门但保留其他关联, 增挂就是他可以既在新部门又在旧部门."
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

}

function hsUserLoad(url, data) {
    if (!url ) url  = this._url ;
    if (!data) data = this._data;
    // 搜索
    if (this.listBox.is(".on-top") ) {
        var rd = hsSerialArr( data );
        var wd = hsGetSeria(rd,"wd");
        if (wd) {
           url = hsSetParam(url, "dept_id", "");
        } else {
           url = hsSetParam(url, "dept_id","0");
        }
    }
    HsList.prototype.load.call(this, url, data);
}

function hsUserSend(btn, msg, url, data) {
    // 移出
    if (btn.is(".remove")) {
        var uid = data.val();
        var dis = data.closest("tr")
                 .data ( "dept_ids")
                 .slice( 0 );
        var did = hsGetParam(this._url, "dept_id");

        hsUserDel_(dis, did);
        if (dis.length == 0) {
            this.warn("这是此用户唯一的部门, 不能再移除了.");
            return;
        }

        data = new HsSerialDic({id: uid, "depts..dept_id": dis});
    }
    HsList.prototype.send.call(this, btn, msg, url, data);
}

function hsUserDel_(arr, val) {
    for(var i = 0; i < arr.length; i ++) {
        if (arr[i] === val) {
            arr.splice(i,1);
            break;
        }
    }
}
