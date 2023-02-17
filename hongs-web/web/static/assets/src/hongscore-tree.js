
/**
 * 树型组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsTree(context, opts) {
    context = jQuery (context);

    var loadBox  = context.closest(".loadbox");
    var treeBox  = context.find   (".treebox");
    var findBox  = context.find   (".findbox");
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var sendUrls = hsGetValue(opts, "sendUrls");
    var openUrls = hsGetValue(opts, "openUrls");
    var linkUrls = hsGetValue(opts, "linkUrls");
    var loadDat  = hsGetValue(opts, "loadData");

    // 数据的节点属性的键
    this.idKey   = hsGetValue(opts, "idKey"  , "id"  );
    this.pidKey  = hsGetValue(opts, "pidKey" , "pid" );
    this.nameKey = hsGetValue(opts, "nameKey", "name");
    this.noteKey = hsGetValue(opts, "noteKey");
    this.typeKey = hsGetValue(opts, "typeKey");
    this.cnumKey = hsGetValue(opts, "cnumKey");
    this.rowsKey = hsGetValue(opts, "rowsKey" , hsGetConf("rn.key", "rn"));

    // 根节点信息
    var rootInfo = {
            id   : hsGetValue(opts, "rootId"  , hsGetConf("tree.root.id", "0")),
            name : hsGetValue(opts, "rootName", hsGetLang("tree.root.name")),
            note : hsGetValue(opts, "rootNote", hsGetLang("tree.root.note")),
            type : "__ROOT__"
        };

    this.context = context;
    this.loadBox = loadBox;
    this.treeBox = treeBox;
    this._pid  = "";
    this._url  = "";
    this._data = [];

    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0, 1)
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        }
    }

    var that = this;
    var m, n, u;

    //** 发送服务 **/

    function sendHand(evt) {
        var n = jQuery(this);
        var m = evt.data[1];
        var u = evt.data[2];

        var tip = n.closest(".tooltip");
        if (tip.length) {
            n   = tip.data ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call( that, n, m );
            return;
        }

        var sid;
        if (-1 != jQuery.inArray(treeBox[0], n.parents())) {
            sid = that.getCid(n);
        } else {
            sid = that.getSid( );
        }
        if (sid == null) return ;

        var dat = {};
        dat[that.idKey] =  sid  ;
        u = hsFixPms(u, loadBox);
        that.send(n, m, u, dat );
    }

    if (sendUrls) jQuery.each(sendUrls, function(i, a) {
        switch (a.length) {
        case 3:
            u = a[0];
            n = a[1];
            m = a[2];
            break;
        case 2:
            u = a[0];
            n = a[1];
            m = undefined;
            break;
        default:
            return;
        }

        if (typeof(n) !== "string" || /^[@%\^\-+~>*#]/.test(n)) {
            context.hsFind(n).on("click", [n, m, u], sendHand);
        } else {
            context.on("click", n, [n, m, u], sendHand);
        }
    });

    //** 打开服务 **/

    function openHand(evt) {
        var n = jQuery(this);
        var m = evt.data[1];
        var u = evt.data[2];

        var tip = n.closest(".tooltip");
        if (tip.length) {
            n   = tip.data ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call( that, n, m );
            return;
        }

        var sid;
        if (-1 != jQuery.inArray(treeBox[0], n.parents())) {
            sid = that.getCid(n);
        } else {
            sid = that.getSid( );
        }
        if (sid == null) return ;

        var dat = {};
        dat[that.idKey] =  sid  ;
        u = hsFixPms(u, loadBox);
        that.open(n, m, u, dat );
    }

    if (openUrls) jQuery.each(openUrls, function(i, a) {
        switch (a.length) {
        case 3:
            u = a[0];
            n = a[1];
            m = a[2];
            break;
        case 2:
            u = a[0];
            n = a[1];
            m = undefined;
            break;
        default:
            return;
        }

        if (typeof(n) !== "string" || /^[@%\^\-+~>*#]/.test(n)) {
            context.hsFind(n).on("click", [n, m, u], openHand);
        } else {
            context.on("click", n, [n, m, u], openHand);
        }
    });

    //** 选中打开 **/

    treeBox.on("click", ".tree-node td.tree-name", function() {
        that.select(jQuery(this).closest(".tree-node"));
    });
    treeBox.on("click", ".tree-node td.tree-hand", function() {
        that.toggle(jQuery(this).closest(".tree-node"));
    });
    treeBox.on("dblclick", ".tree-node td:not(.tree-hand)", function() {
        that.toggle(jQuery(this).closest(".tree-node"));
    });

    if (linkUrls) {
        treeBox.on( "treeSelect" , function( ev , id ) {
            for(var i = 0; i < linkUrls.length; i ++ ) {
                var u = linkUrls[i][0];
                var m = linkUrls[i][1];
                u = u.replace ('{ID}', encodeURIComponent(id));
                u =  hsFixPms (u , loadBox);
                treeBox.hsFind(m).hsLoad(u);
            }
        });
    }

    //** 顶级节点 **/

    this.fillInfo(
         rootInfo,
         jQuery('<div/>').appendTo(treeBox)
          .attr("id", "tree-node-"+rootInfo.id)
          .attr("class", "tree-node tree-root")
    );

    //** 立即加载 **/

    if (loadUrl) {
        this.load(
            rootInfo.id,
            hsFixPms   (loadUrl, loadBox),
            hsSerialMix(loadDat, findBox)
        );
    }
}
HsTree.prototype = {
    load     : function(pid, url, data) {
        data =  hsSerialObj (data) ;
        if (pid ) this._pid  = pid ;
        if (url ) this._url  = url ;
        if (data) this._data = data;
        if (undefined === hsGetParam(this._url, this.rowsKey)) {
            this._url  =  hsSetParam(this._url, this.rowsKey, "0");
        }   this._data =  hsSetSeria(this._data, this.pidKey, this._pid);
        this.ajax({
            "url"      : this._url ,
            "data"     : this._data,
            "type"     : "POST",
            "dataType" : "json",
            "funcName" : "load",
            "cache"    : false,
            "global"   : false,
            "context"  : this,
            "complete" : function(rst) {
               this.loadBack(rst, pid);
            }
        });
    },
    loadBack : function(rst, pid) {
        rst = hsResponse(rst);
        if (rst.ok === false) return;

        rst._pid = pid; // 先登记待用

        this.treeBox.trigger("loadBack", [rst, this]);

        this.fillList(rst.list || [], pid);

        this.treeBox.trigger("loadOver", [rst, this]);

        /**
         * 删除后重载会致丢失选中
         * 此时需选中此节点的上级
         */
        var sid = this.getSid();
        var rid = this.getRid();
        if (this.treeBox.find("#tree-node-"+sid).size() === 0) {
        if (this.treeBox.find("#tree-node-"+pid).size() === 0) {
            this.select(rid);
        } else {
            this.select(pid);
        }}
    },
    fillList : function(list, pid) {
        var lst, nod, i, id;
        nod = this.treeBox.find("#tree-node-"+pid);
        lst = nod .children(".tree-list:first");
        if (list.length == 0) {
            nod.removeClass("tree-fold")
               .removeClass("tree-open");
            lst.remove();
            return;
        }
        nod.addClass("tree-open");

        if (lst.length == 0 ) {
            lst = jQuery('<div class="tree-list"></div>');
            lst.appendTo(nod);
        }

        var pid2, lst2, lsts = {}; lsts[pid] = lst;
        for(i = list.length -1; i > -1; i --) {
            id  = hsGetValue(list[i], this.idKey);
            nod = this.treeBox.find("#tree-node-"+id);
            if (nod.length == 0) {
                nod = jQuery('<div class="tree-node"></div>');
                nod.attr("id", "tree-node-"+id );
            } else {
                nod.find("table:first").empty( );
                pid2 = this.getPid(nod);
                lst2 = nod.closest(".tree-list");
                if (pid2 != pid) lsts[pid2]=lst2;
            }
            nod.prependTo(lst);
            this.fillInfo(list[i], nod);
        }
        for(i in lsts) {
            this.fillCnum(list.length , lsts[i]);
        }
    },
    fillInfo : function(info, nod) {
        var tab = jQuery('<table><tbody><tr>'
            + '<td class="tree-hand"><span class="caret"></span></td>'
            + '<td class="tree-name"></td>'
            + '<td class="tree-cnum"></td>'
            + '</tr></tbody></table>');
        var n, t;

        n = hsGetValue(info, this.nameKey);
        tab.find(".tree-name").text(n);
        if (typeof(this.noteKey) !== "undefined") {
            n = hsGetValue(info , this.noteKey);
            nod.find(".tree-name").attr("title", n);
        }
        if (typeof(this.typeKey) !== "undefined") {
            t = hsGetValue(info , this.typeKey);
            nod.addClass("tree-type-" + t);
        }
        if (typeof(this.cnumKey) !== "undefined") {
            n = hsGetValue(info , this.cnumKey);
            tab.find(".tree-cnum").text(n);
        if (n) {
            nod.addClass("tree-fold");
        }}
        else {
            nod.addClass("tree-fold");
        }

        tab.prependTo(nod);
    },
    fillCnum : function(cnum, lst) {
        var nod = lst.closest (".tree-node");
        var arr = lst.children(".tree-node");

        if (typeof(cnum) === "undefined")
            cnum  = arr.length;
        if (cnum != arr.length) {
            for (var i = arr.length-1; i > cnum-1; i --) {
                jQuery (arr[i]).remove( );
            }
        }

        if (cnum != 0) {
            nod.find(".tree-cnum").text(cnum.toString());
        } else {
            nod.find(".tree-cnum").hide();
            nod.removeClass( "tree-fold")
               .removeClass( "tree-open");
        }
    },

    send     : function(btn, msg, url, data) {
        btn = jQuery(btn);
        var that = this ;
        var func = function() {
        var dat2 = jQuery.extend({}, hsSerialDat(url), hsSerialDat(data||{}));
        that.ajax({
            "url"       : url ,
            "data"      : data,
            "type"      : "POST",
            "dataType"  : "json",
            "funcName"  : "send",
            "cache"     : false,
            "global"    : false,
            "context"   : that,
            "trigger"   : btn ,
            "complete"  : function(rst) {
                this.sendBack(btn, rst, dat2);
            }
        });
        } ;
        if (!msg) {
            func( );
        } else {
            this.warn( msg , "warning", func, null);
        }
    },
    sendBack : function(btn, rst, data) {
        btn = jQuery(btn);
        rst = hsResponse(rst, 1);
        if (rst.ok) {
            if (rst.msg) {
                this.note(rst.msg, "success");
            }
        } else {
            if (rst.msg) {
                this.warn(rst.msg, "warning");
            } else {
                this.warn(hsGetLang('error.undef'));
            }
            return;
        }

        var evt = jQuery.Event( "sendBack" );
        btn.trigger(evt , [rst, data, this]);
        if (evt.isDefaultPrevented()) return;

        if (data[this.pidKey] !== undefined) {
            this.load(/* Moved */ data[this.pidKey]); // 移动
        }
        if (data[this.idKey ] !== undefined) {
            this.load(this.getPid(data[this.idKey])); // 更新
        }
    },

    open     : function(btn, box, url, data) {
        // 如果 URL 里有 {ID} 则替换之
        if ( -1 != url.indexOf("{ID}")) {
            var i, idz, ids = [ /**/ ];
            idz  = hsSerialArr( data );
            for(i=0; i<idz.length; i++) {
                ids.push(encodeURIComponent(idz[i].value));
            }
            url  = url.replace("{ID}" , ids.join(  ","  ));
            data = undefined;
        }

        btn = jQuery(btn);
        var that = this ;
        var dat2 = jQuery.extend({}, hsSerialDat(url), hsSerialDat(data||{}));
        if (box) {
            // 外部打开
            if (box instanceof String && /^_/.test(box)) {
                url = hsSetPms(url, data);
                window . open (url, box );
                return ;
            }
            // 内部打开
            box = btn.hsFind(box);
            box.hsOpen(url, data, function() {
               that.openBack(btn, jQuery( this ), dat2 );
            })
            .data("rel", btn.closest(".loadbox").get(0));
        } else {
         jQuery.hsOpen(url, data, function() {
               that.openBack(btn, jQuery( this ), dat2 );
            });
        }
    },
    openBack : function(btn, box, data) {
        btn = jQuery(btn);
        var that = this ;
        btn.trigger("openBack", [box, data, this]);

        box.on("saveBack", function(evt, rst, rel) {
            var ext = jQuery.Event( "saveBack" );
            ext.relatedTarget = evt.target;
            ext.relatedHsInst = rel /****/;
            btn.trigger(ext , [rst, data, that]);
            if (ext.isDefaultPrevented()) return;

            if (data[that.pidKey] !== undefined) {
                that.load(/* Saved */ data[that.pidKey]); // 更新
            } else
            if (data[that.idKey ] !== undefined) {
                that.load(that.getPid(data[that.idKey])); // 修改
            } else
            {
                that.load(that.getSid(/* Current id */)); // 添加
            }
        });
    },

    ajax : function() {
        return jQuery.hsAjax.apply(window, arguments);
    },
    note : function() {
        return jQuery.hsNote.apply(window, arguments);
    },
    warn : function() {
        return jQuery.hsWarn.apply(window, arguments);
    },

    select   : function(id) {
        var nod = this.getNode(id);
            id  = this.getId (nod);
        var evt = jQuery.Event("treeSelect");
        nod.children("table").trigger(evt, [id, this]);
        if (evt.isDefaultPrevented()) return;

        this.treeBox.find(".tree-curr")
            .removeClass ( "tree-curr");
            nod.addClass ( "tree-curr");
    },
    toggle   : function(id) {
        var nod = this.getNode(id);
            id  = this.getId (nod);
        var evt = jQuery.Event("treeToggle");
        nod.children("table").trigger(evt, [id, this]);
        if (evt.isDefaultPrevented()) return;

        var lst = nod.children(".tree-list");
            lst.toggle();
        if (lst.size( )) {
            nod.removeClass("tree-open")
               .removeClass("tree-fold");
            lst.is(":visible")?
               nod.addClass("tree-open"):
               nod.addClass("tree-fold");
        } else {
            this.load(id);
        }
    },

    getId    : function(id) {
        if (typeof(id) === "object")
            return this.getId ( id.attr("id"));
        else  if  (id)
            return id.toString().substring(10);
        else
            return "";
    },
    getPid   : function(id) {
        return this.getId(this.getPnode(id));
    },
    getCid   : function(nd) {
        return this.getId(jQuery(nd).closest(".tree-node"));
    },
    getSid   : function() {
        return this.getId(this.treeBox.find(".tree-curr"));
    },
    getRid   : function() {
        return this.getId(this.treeBox.find(".tree-root"));
    },
    getNode  : function(id) {
        if (typeof(id) === "object")
            return id.closest(".tree-node" );
        else
            return this.treeBox.find( "#tree-node-" + id );
    },
    getPnode : function(id) {
        return this.getNode(id).parent().closest(".tree-node");
    }
};

jQuery.fn.hsTree = function(opts) {
    return this.hsBind(HsTree, opts);
};

(function($) {
    $(document)
    .on("treeSelect", ".HsTree .tree-node>table",
    function(evt, nid, obj) {
        // 当选中非根节点时,
        // 工具按钮设为可用,
        // 否则禁用相关按钮.
        var box = obj.context  ;
        var rid = obj.getRid( );
        box.find(".for-select").prop("disabled", rid == nid);
    })
    .on("selectstart", ".HsTree",
    function(evt) {
        // 阻止选中节点文字,
        // 避免双击选中文字.
        evt.preventDefault(   );
    });
})(jQuery);
