
/* global self */

/**
 * 列表组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsList(opts, context) {
    context = jQuery (context);
    context.data("HsList", this);
    context.addClass( "HsList" );

    var loadBox  = context.closest(".loadbox");
    var listBox  = context.find   (".listbox");
    var pageBox  = context.find   (".pagebox");
    var findBox  = context.find   (".findbox");
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var sendUrls = hsGetValue(opts, "sendUrls");
    var openUrls = hsGetValue(opts, "openUrls");

    // 排序, 分页等参数
    this.sortKey = hsGetValue(opts, "sortKey", hsGetConf("ob.key", "ob"));
    this.pageKey = hsGetValue(opts, "pageKey", hsGetConf("pn.key", "pn"));
    this.pagsKey = hsGetValue(opts, "pagsKey", hsGetConf("gn.key", "gn"));
    this.rowsKey = hsGetValue(opts, "rowsKey", hsGetConf("rn.key", "rn"));
    this.pagsNum = hsGetValue(opts, "pagsKey", "");

    // 逐层解析分页数量
    var arr = hsSerialMix(hsSerialArr(loadUrl), hsSerialArr(loadBox));
    this.pagsNum = hsGetSeria(arr, this.pagsKey) || this.pagsNum;
    this.rowsNum = hsGetSeria(arr, this.rowsKey);
    if (this.pagsNum == "") {
        this.pagsNum = hsGetConf("pags.for.page", 5 );
    }
    if (this.rowsNum == "") {
        this.pagsNum = hsGetConf("pags.for.page", 20);
    }

    this.context = context;
    this.loadBox = loadBox;
    this.listBox = listBox;
    this.pageBox = pageBox;
    this._url  = "";
    this._data = [];

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

        var t = n.closest(".tooltip");
        if (t.length) {
            n = t.data   ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call(n, m, that);
            return;
        }

        var cks;
        if (-1 != jQuery.inArray(listBox[0], n.parents())) {
            cks = that.getRow(n);
        } else {
            cks = that.getAll( );
        }
        if (cks == null) return ;

        u = hsFixPms(u, loadBox);
        that.send(n, m, u, cks );
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

        var t = n.closest(".tooltip");
        if (t.length) {
            n = t.data   ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call(n, m, that);
            return;
        }

        if (0 <= u.indexOf("{ID}")) {
            var sid;
            if (0 <= jQuery.inArray(listBox[0], n.parents())) {
                sid = that.getRow(n);
            }
            else {
                sid = that.getOne( );
            }
            if (sid == null) return ;
            sid = sid.val( );

            u  = u.replace("{ID}", encodeURIComponent( sid ));
        }

        u = hsFixPms(u, loadBox);
        that.open(n, m, u /**/ );
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

    //** 搜索服务 **/

    if (findBox.length) {
        findBox.on("submit", function() {
            that.load( loadUrl , this );
            return false;
        });
    }

    //** 立即加载 **/

    if (loadUrl) {
        this.load(loadUrl, findBox);
    }
}
HsList.prototype = {
    load     : function(url, data) {
        if (url ) this._url  = url;
        if (data) this._data = hsSerialArr(data);
        jQuery.hsAjax({
            "url"       : this._url ,
            "data"      : this._data,
            "type"      : "POST",
            "dataType"  : "json",
            "funcName"  : "load",
            "async"     : false,
            "cache"     : false,
            "context"   : this,
            "success"   : this.loadBack
        });
    },
    loadBack : function(rst) {
        rst = hsResponObj(rst);
        if (rst.ok === false) return;

        this.listBox.trigger("loadBack", [rst, this]);

        if (rst.list) this.fillList(rst.list);
        if (rst.page) this.fillPage(rst.page);
        else { // 无分页
            if ( rst.list && rst.list.length) {
                this.fillPage({uncertain: 1});
            } else {
                this.fillPage({err: 1});
            }
        }

        this.listBox.trigger("loadOver", [rst, this]);
    },
    fillList : function(list) {
        var ths, th, tb, tr, td, i, j, n, t, v, f;
        ths = this.listBox.find("thead th,thead td" );
        tb  = this.listBox.find("tbody"); tb.empty( );

        // 排序
        var sn = hsGetSerias(this._data,this.sortKey);
        if (sn.length == 0) {
            sn = hsGetParam (this._url ,this.sortKey);
        } else {
            sn = sn[0];
        }

        for (i = 0; i < ths .length; i ++) {
            th = jQuery(ths[i]);

            // 填充句柄
            f = th.data( "fl" );
            if (f && typeof f != "function") {
                try {
                    f = eval('(function(list, v, n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse list data-fl error: " + e);
                }
                th.data("fl",f);
            }

            // 排序处理
            if (th.hasClass("sortable")) {
                if (th.find(".sort-ico").size() == 0) {
                    var that = this;
                    th.append('<span class="sort-ico"></span>');
                    th.click(function( ) {
                        var td = jQuery ( this );
                        var fn = td.attr("data-ob") || td.attr("data-fn");
                        var sn = "";
                        if ( td.hasClass("sort-a-z")) {
                            sn = "-"+fn;
                        } else
                        if (!td.hasClass("sort-z-a")) {
                            sn =     fn;
                        }
                        hsSetSeria(that._data, that.sortKey, sn );
                        that.load();
                    });
                }
                var fn = th.attr("data-ob") || th.attr("data-fn");
                 th.removeClass("sort-a-z sort-z-a");
                if (sn ==     fn) {
                    th.addClass("sort-a-z");
                } else
                if (sn == '-'+fn) {
                    th.addClass("sort-z-a");
                }
            }
        }

        for (i = 0; i < list.length; i ++) {
            tr = jQuery('<tr></tr>');
            tb.append(tr);

            this._info = list[i];
            for (j = 0; j < ths .length; j ++) {
                th = jQuery(ths[j]);
                td = jQuery('<td></td>');
                td.appendTo(tr);
                td.attr("class", th.attr("class"));

                n  = th.attr("data-fn");
                t  = th.attr("data-ft");
                v  = th.data("fv");
                f  = th.data("fl");

                // 取值
                if (n) {
                    var x  =  hsGetValue(list[i], n);
                    if (x === undefined) {
                        x  =  list[i][n];
                    }
                    if (x) {
                        v  =  x;
                    }
                }

                // 填充
                if (f) {
                    v  = f.call(td, this, v, n);
                } else
                if (n && this["_fill_"+n] !== undefined) {
                    v  = this["_fill_"+n].call(this, td, v, n);
                } else
                if (t && this["_fill_"+t] !== undefined) {
                    v  = this["_fill_"+t].call(this, td, v, n);
                }
                // 无值不理会
                if (! v && v !== 0 && v !== "") {
                    continue;
                }

                td.text( v );
            }
        }

        if (typeof(this._info) !== "undefined") {
            delete this._info;
        }
    },
    fillPage : function(page) {
        switch (page.err) {
            case  2 :
            case "2":
                hsSetSeria(this._data, this.pageKey, 1);
                this.pageBox.empty().append('<div class="alert alert-warning">'+hsGetLang('list.outof')+'</div>');
                this.listBox.hide( );
                var that = this;
                setTimeout(function() {
                    that.load();
                }, 5000);
                return;
            case  1 :
            case "1":
                this.pageBox.empty().append('<div class="alert alert-warning">'+hsGetLang('list.empty')+'</div>');
                this.listBox.hide( );
                return;
            default :
                this.pageBox.empty();
                this.listBox.show( );
        }

        var i, p, t, pmin, pmax, that = this;
        p = page.page ? parseInt(page.page) : 1 ;
        t = page.pagecount ? parseInt(page.pagecount): 1;
        pmin = p - Math.floor( this.pagsNum / 2 );
        if (pmin < 1) pmin = 1;
        pmax = pmin + this.pagsNum - 1;
        if (pmax > t) pmax = t;
        pmin = pmax - this.pagsNum + 1;
        if (pmin < 1) pmin = 1;

        var pbox = jQuery('<ul class="pagination"></ul>').appendTo(this.pageBox);
        var btns = pbox;//jQuery('<ul class="pagination pull-left" ></ul>').appendTo(this.pageBox);
        var nums = pbox;//jQuery('<ul class="pagination pull-right"></ul>').appendTo(this.pageBox);

        if (1 < p) {
            btns.append(jQuery('<li class="page-prev"><a href="javascript:;" data-pn="'+(p-1)+'" title="'+hsGetLang("list.prev.page")+'">&lsaquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-prev disabled"><a href="javascript:;" title="'+hsGetLang("list.prev.page")+'">&lsaquo;</a></li>'));
        }

        if (1 < pmin) {
            btns.append(jQuery('<li class="page-home"><a href="javascript:;" data-pn="'+1+'" title="'+1+'">&laquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-home disabled"><a href="javascript:;" title="'+1+'">&laquo;</a></li>'));
        }

        for(i = pmin; i < pmax + 1; i ++) {
            nums.append(jQuery('<li class="page-link'+(i === p ? ' active' : '')+'"><a href="javascript:;" data-pn="'+i+'" title="'+i+'">'+i+'</a></li>'));
        }

        if (t > pmax) {
            btns.append(jQuery('<li class="page-last"><a href="javascript:;" data-pn="'+t+'" title="'+t+'">&raquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-last disabled"><a href="javascript:;" title="'+t+'">&raquo;</a></li>'));
        }

        if (t > p) {
            btns.append(jQuery('<li class="page-next"><a href="javascript:;" data-pn="'+(p+1)+'" title="'+hsGetLang("list.next.page")+'">&rsaquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-next disabled"><a href="javascript:;" title="'+hsGetLang("list.next.page")+'">&rsaquo;</a></li>'));
        }

        // 页码不确定则不在末页显示为更多
        if (page.uncertain && t == pmax + 1) {
            nums.find(".page-last").addClass("page-more").find("a").html("&hellip;");
        }

        this.pageBox.show();//.addClass("clearfix");
        this.pageBox.find("[data-pn="+p+"]").addClass("page-curr");
        this.pageBox.find("[data-pn]").on("click", function( evt ) {
            hsSetSeria(that._data, that.pageKey, jQuery(this).attr("data-pn"));
            evt.preventDefault();
            that.load();
        });
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

        this.load();
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
            btn.trigger(ext , [rst, data, that]);
            if (ext.isDefaultPrevented()) return;

            that.load( );
        });
    },

    note : function() {
        jQuery.hsNote.apply(self, arguments);
    },
    warn : function() {
        jQuery.hsWarn.apply(self, arguments);
    },

    getRow   : function(o) {
        return jQuery(o)
                .closest("tr,.itembox")
                .find   ( ".checkone" )
                .filter ( ":checkbox,:radio,:hidden" );
    },
    getAll   : function() {
        var cks = this.context.find(".checkone").filter(":checked");
        if (cks.length == 0) {
            alert(hsGetLang("list.get.all"));
            return null;
        }
        else {
            return cks ;
        }
    },
    getOne   : function() {
        var cks = this.context.find(".checkone").filter(":checked");
        if (cks.length != 1) {
            alert(hsGetLang("list.get.one"));
            return null;
        }
        else {
            return cks ;
        }
    },

    // /** 填充函数 **/

    _fill__admin : function(td, v, n) {
        var th = this.listBox.find('thead [data-fn="'+n+'"]');
        td.append(th.find(".invisible").clone().removeClass("invisible")).hsInit();
        return false;
    },
    _fill__check : function(td, v, n) {
        jQuery('<input type="checkbox" class="input-checkbox checkone"/>')
            .attr("name", n).val(v)
            .appendTo(td);
        return false;
    },
    _fill__radio : function(td, v, n) {
        jQuery('<input type="radio" class="input-radio checkone"/>')
            .attr("name", n).val(v)
            .appendTo(td);
        return false;
    },
    _fill__alink : function(td, v, n) {
        jQuery('<a href="javascript:;"></a>')
            .addClass("a-"+n).text(v)
            .appendTo(td);
        return false;
    },

    _fill__htime : function(td, v, n) {
        var d1  =  new Date ();
        var d2  =  hsPrsDate(v, hsGetLang("datetime.format"));
        if (d1.getFullYear() == d2.getFullYear()
        &&  d1.getMonth() == d2.getMonth()
        &&  d1.getDate( ) == d2.getDate()) {
            return hsGetLang("time.today", [
                   hsFmtDate(v, hsGetLang("time.format"))
            ]);
        }
        else {
            return hsFmtDate(v, hsGetLang("date.format"));
        }
    },
    _fill__hdate : function(td, v, n) {
        var d1  =  new Date ();
        var d2  =  hsPrsDate(v, hsGetLang("date.format"));
        if (d1.getFullYear() == d2.getFullYear()
        &&  d1.getMonth() == d2.getMonth()
        &&  d1.getDate( ) == d2.getDate()) {
            return hsGetLang("date.today");
        }
        else {
            return hsFmtDate(v, hsGetLang("date.format"));
        }
    },
    _fill__datetime : function(td, v, n) {
        return hsFmtDate(v, hsGetLang("datetime.format"));
    },
    _fill__date : function(td, v, n) {
        return hsFmtDate(v, hsGetLang("date.format"));
    },
    _fill__time : function(td, v, n) {
        return hsFmtDate(v, hsGetLang("time.format"));
    },
    _fill__html : function(td, v, n) {
        td.html(v); return false;
    }
};

/**
 * 这是比 fillList 更简单的卡片式展现方式
 * 需替代 fillList 时, 在初始化参数中加入
 * fillList: hsListFillItem
 * @param {array} list 返回的列表数据
 */
function hsListFillItem(list) {
    var that = this;
    var cb   = this.context.find(".itembox:hidden:first");
    var tr, td, n, t, f, v;
    this.listBox.children().not(cb).remove();
    for (var i = 0 ; i < list.length ; i ++) {
        this._info = list[i];
        tr = cb.clone();
        tr.find("[data-fn],[data-ft],[data-fl]").each(function() {
            td = jQuery(this);
            n = td.attr("data-fn");
            t = td.attr("data-ft");
            f = td.data("fl");
            v = td.data("fv");

            // 解析填充方法
            if (f && typeof f != "function") {
                try {
                    f = eval('(function(list, v, n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse list data-fl error: " + e);
                }
                td.data( "fl", f );
            }

            // 取值
            if (n) {
                var x  =  hsGetValue(list[i], n);
                if (x === undefined) {
                    x  =  list[i][n];
                }
                if (x) {
                    v  =  x;
                }
            }

            // 填充
            if (f) {
                v  = f.call(td, that, v, n);
            } else
            if (n && that["_fill_"+n] !== undefined) {
                v  = that["_fill_"+n].call(that, td, v, n);
            } else
            if (t && that["_fill_"+t] !== undefined) {
                v  = that["_fill_"+t].call(that, td, v, n);
            }
            // 无值不理会
            if (! v && v !== 0 && v !== "") {
                return;
            }

            if (td.is("input,select,textarea")) {
                td.val (v);
            } else {
                td.text(v);
            }
        });
        tr.show( ).appendTo( this.listBox );
    }
    if (typeof(this._info) !== "undefined")
        delete this._info;
}

jQuery.fn.hsList = function(opts) {
    return this._hsModule(HsList, opts);
};

(function($) {
    $(document)
    .on("click", ".listbox tbody td",
    function(evt) {
        // 当点击表格列时单选
        // 工具按钮有三类, 打开|打开选中|发送选中
        // 打开选中只能选一行, 发送选中可以选多行
        // 但是复选框太小不方便操作, 故让点击单元格即单选该行, 方便操作
        if ( $(evt.target).is("a,input,select,textarea") )
            return;
        var tr = $(this).closest("tr");
        var ck = tr.find(".checkone" );
        if (this  !=  ck.closest("td")[0])
            tr.closest("tbody").find(".checkone").not(ck )
                               .prop( "checked"  , false );
        ck.prop("checked", ! ck.prop( "checked")).change();
    })
    .on("change", ".HsList .checkone",
    function() {
        var box = $(this ).closest(".HsList" );
        var siz = box.find(".checkone").length;
        var len = box.find(".checkone:checked").length;
        var ckd = siz && siz === len ? true : (len && len !== siz ? null : false);
        box.find(".for-choose").prop("disabled", len !== 1);
        box.find(".for-checks").prop("disabled", len === 0);
        box.find(".checkall").prop("choosed", ckd);
    })
    .on("change", ".HsList .checkall",
    function() {
        var box = $(this ).closest(".HsList" );
        var ckd = $(this/**/).prop("checked" );
                  $(this/**/).prop("choosed", ckd);
        box.find(".checkone").prop("checked", ckd).trigger("change");
    })
    .on("loadOver", ".HsList .listbox",
    function() {
        var box = $(this ).closest(".HsList" );
        box.find(".for-choose,.for-checks").prop("disabled" , true );
        box.find(".checkall").prop("choosed", false);
        box.find(".checkone").change();
    });
})(jQuery);
