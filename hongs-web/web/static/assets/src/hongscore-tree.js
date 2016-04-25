
/* global self */

/**
 * 树型组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsTree(opts, context) {
    context = jQuery (context);
    context.data("HsTree", this);
    context.addClass( "HsTree" );

    var loadBox  = context.closest(".loadbox");
    var treeBox  = context.find   (".treebox");
    var findBox  = context.find   (".findbox");
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var sendUrls = hsGetValue(opts, "sendUrls");
    var openUrls = hsGetValue(opts, "openUrls");
    var linkUrls = hsGetValue(opts, "linkUrls");

    // 数据的节点属性的键
    this.idKey   = hsGetValue(opts, "idKey"  , "id"  );
    this.pidKey  = hsGetValue(opts, "pidKey" , "pid" );
    this.nameKey = hsGetValue(opts, "nameKey", "name");
    this.noteKey = hsGetValue(opts, "noteKey");
    this.typeKey = hsGetValue(opts, "typeKey");
    this.cnumKey = hsGetValue(opts, "cnumKey");

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
    this._url  = "";
    this._pid  = "";

    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0, 1)
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        }
    }

    if (loadUrl) {
        loadUrl = hsFixPms(loadUrl, loadBox);
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
            sid = that.getId (n);
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
            n = a[0];
            u = a[1];
            m = a[2];
            break;
        case 2:
            n = a[0];
            u = a[1];
            m = undefined;
            break;
        default:
            return;
        }

        if (typeof(n) === "string") {
            context.on("click", n, [n, m, u], sendHand);
        } else if (n) {
            n.on("click", [n, m, u], sendHand);
        }
    });

    //** 打开服务 **/

    function openHand(evt) {
        var n = jQuery(this);
        var m = evt.data[1];
        var u = evt.data[2];

        switch (m) {
            case "{CONTEXT}": m = context; break;
            case "{LOADBOX}": m = loadBox; break;
            case "{AUTOBOX}": m = '@';
            default: if ( m ) m = n.hsFind(m);
        }

        var tip = n.closest(".tooltip");
        if (tip.length) {
            n   = tip.data ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call( that, n, m );
            return;
        }

        if (0 <= u.indexOf("{ID}")) {
            var sid;
            if (0 <= jQuery.inArray(treeBox[0], n.parents())) {
                sid = that.getId (n);
            }
            else {
                sid = that.getSid( );
            }
            if (sid == null) return ;

            u  = u.replace("{ID}", encodeURIComponent( sid ));
        }

        u = hsFixPms(u, loadBox);
        that.open( n, m, u );
    }

    if (openUrls) jQuery.each(openUrls, function(i, a) {
        switch (a.length) {
        case 3:
            n = a[0];
            u = a[1];
            m = a[2];
            break;
        case 2:
            n = a[0];
            u = a[1];
            m = undefined;
            break;
        default:
            return;
        }

        if (typeof(n) === "string") {
            context.on("click", n, [n, m, u], openHand);
        } else if (n) {
            n.on("click", [n, m, u], openHand);
        }
    });

    //** 选中打开 **/

    treeBox.on("click", ".tree-node td.tree-hand", function() {
        that.toggle(jQuery(this).closest(".tree-node"));
    });
    treeBox.on("click", ".tree-node td:not(.tree-hand)", function() {
        that.select(jQuery(this).closest(".tree-node"));
    });

    if (linkUrls) {
        treeBox.on("select", function(evt, id) {
            for(var i = 0; i < linkUrls.length; i ++) {
                var u = linkUrls[i][1];
                u = u.replace('{ID}', encodeURIComponent(id));
                u = hsFixPms (   u  , loadBox   );
                jQuery( linkUrls[i][0]).hsLoad(u);
            }
        });
    }

    //** 顶级节点 **/

    var  rootBox = jQuery('<div class="tree-node tree-root" id="tree-node-'
                 +rootInfo.id+'"></div>')
                 .appendTo(treeBox);
    this.fillInfo(rootInfo,rootBox);
    this.select  (rootInfo.id);

    //** 搜索服务 **/

    if (findBox.length) {
        findBox.on("submit", function() {
            that.find( loadUrl , this );
            return true;
        });
    }

    //** 立即加载 **/

    if (loadUrl) {
        this.load(rootInfo.id, loadUrl, []);
    }
}
HsTree.prototype = {
    load     : function(pid, url, data) {
        if (pid ) this._pid  = pid;
        if (url ) this._url  = url;
        if (data) this._data = hsSerialArr(data);
      hsSetSeria( this._data, this.pidKey, this._pid );
        jQuery.hsAjax({
            "url"       : this._url ,
            "data"      : this._data,
            "type"      : "POST",
            "dataType"  : "json",
            "funcName"  : "load",
            "async"     : false,
            "cache"     : false,
            "context"   : this,
            "success"   : function(rst) {
                this.loadBack(rst, pid);
            }
        });
    },
    loadBack : function(rst, pid) {
        rst = hsResponObj(rst);
        if (rst.ok === false) return;

        this.treeBox.trigger("loadBack", [rst, pid, this]);

        if (rst.list) this.fillList( rst.list, pid );
        if (this.treeBox.find(
        "#tree-node-" + this.getSid()).size() === 0) {
            this.select ( pid );
        }

        this.treeBox.trigger("loadOver", [rst, pid, this]);
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
            }
            else {
                nod.find("table:first").empty( );
                pid2 = this.getPid(nod);
                lst2 = nod.closest(".tree-list");
                if (pid2 != pid) lsts[pid2] = lst2;
            }
            nod.prependTo(lst);
            this.fillInfo(list[i], nod);
        }
        for(i in lsts) {
            this.fillCnum(list.length, lsts[i]);
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
            if (n)
                nod.addClass("tree-fold");
        }
        else {
                nod.addClass("tree-fold");
        }

        if (! t) t = "info";
        if (typeof(this["_fill_"+t]) !== "undefined") {
            this["_fill_"+t].call(this, tab, info);
        }

        tab.prependTo(nod);
    },
    fillCnum : function(cnum, lst) {
        var nod = lst.closest (".tree-node");
        var arr = lst.children(".tree-node");

        if (typeof(cnum) === "undefined")
            cnum  = arr.length;
        if (cnum != arr.length)
            for (var i = arr.length-1; i > cnum-1; i --) {
                jQuery(arr[i]).remove();
            }

        if (cnum != 0)
            nod.find(".tree-cnum").text(cnum.toString());
        else {
            nod.find(".tree-cnum").hide();
            nod.removeClass("tree-fold")
               .removeClass("tree-open");
        }
    },

    find     : function(url, data) {
    },
    findBack : function(rst) {
    },

    send     : function(btn, msg, url, data) {
        var that = this;
        var func = function() {
        var dat2 = jQuery.extend({}, hsSerialObj(url), hsSerialObj(data||{}));
        jQuery.hsAjax({
            "url"       : url,
            "data"      : data,
            "type"      : "POST",
            "dataType"  : "json",
            "funcName"  : "send",
            "async"     : false,
            "cache"     : false,
            "context"   : that,
            "trigger"   : btn,
            "success"   : function(rst) {
                this.sendBack(btn, rst, dat2);
            }
        });
        } ;
        if (msg) {
            this.warn(msg, function() {
                func();
            } , null );
        } else {
            /**/func();
        }
    },
    sendBack : function(btn, rst, data) {
        rst = hsResponObj(rst, true);
        if (rst.ok) {
            if (rst.msg) {
                this.note(rst.msg, "succ");
            }
        } else {
            if (rst.msg) {
                this.warn(rst.msg, "warn");
            } else {
                this.warn(hsGetLang('.error.unkwn'), 'warn');
            }
            return;
        }

        var evt = jQuery.Event( "sendBack" );
        btn.trigger(evt , [rst, data, this]);
        if (evt.isDefaultPrevented()) return;

        if (data[this.idKey ] !== undefined) {
            this.load(this.getPid(data[this.idKey])); // 更新
        }
        if (data[this.pidKey] !== undefined) {
            this.load(/* Moved */ data[this.pidKey]); // 移动
        }
    },

    open     : function(btn, box, url, data) {
        var that = this;
        var dat2 = jQuery.extend({}, hsSerialObj(url), hsSerialObj(data||{}));
        if (box) {
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
        var that = this;
        btn.trigger("openBack", [box, data, this]);

        box.on("saveBack", function(evt, rst, rel) {
            var ext = jQuery.Event( "saveBack" );
            ext.relatedTarget = evt.target;
            ext.relatedHsInst = rel /****/;
            btn.trigger(evt , [rst, data, that]);
            if (evt.isDefaultPrevented()) return;

            if (data[that.idKey ] !== undefined) {
                that.load(that.getPid(data[that.idKey])); // 修改
            } else {
                that.load(that.getSid(/* Current id */)); // 添加
            }
        });
    },

    note : function() {
        jQuery.hsNote.apply(self, arguments);
    },
    warn : function() {
        jQuery.hsWarn.apply(self, arguments);
    },

    select   : function(id) {
        var nod = this.getNode(id);
            id  = this.getId (nod);
        nod.children("table").trigger("select", [ id, this ] );

        this.treeBox.find(".tree-curr")
            .removeClass ( "tree-curr");
            nod.addClass ( "tree-curr");
    },
    toggle   : function(id) {
        var nod = this.getNode(id);
            id  = this.getId (nod);
        nod.children("table").trigger("toggle", [ id, this ] );

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
            return this.getId(id.attr("id"));
        else  if  (id)
            return id.toString().substr(10 );
        else
            return "";
    },
    getPid   : function(id) {
        return this.getId(this.getPnode(id));
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
    return this._hsModule(HsTree, opts);
};

(function($) {
    // 当选中非根节点时, 开启工具按钮, 否则禁用相关按钮
    $(document)
    .on( "select" , ".HsTree .tree-node>table",
    function(ev, id, obj) {
        var box = obj.context;
        var rid = obj.getRid( );
        box.find(".for-select").prop("disabled", rid == id);
    });
})(jQuery);
