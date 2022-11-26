
/**
 * 列表组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsList(context, opts) {
    context = jQuery (context);

    var loadBox  = context.closest(".loadbox");
    var listBox  = context.find   (".listbox");
    var pageBox  = context.find   (".pagebox");
    var findBox  = context.find   (".findbox");
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var sendUrls = hsGetValue(opts, "sendUrls");
    var openUrls = hsGetValue(opts, "openUrls");
    var loadDat  = hsGetValue(opts, "loadData");

    // 排序, 分页等参数
    this.sortKey = hsGetValue(opts, "sortKey", hsGetConf("ob.key", "ob"));
    this.pageKey = hsGetValue(opts, "pageKey", hsGetConf("pn.key", "pn"));
    this.rowsKey = hsGetValue(opts, "rowsKey", hsGetConf("rn.key", "rn"));
    this.rowsNum = hsGetValue(opts, "rowsNum", hsGetConf("rows.per.page", 20));
    this.pugsNum = hsGetValue(opts, "pugsNum", hsGetConf("pugs.for.page", 5 ));

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
            u.call(that, n, m);
            return;
        }

        var c = that.getIds(n);
        if (c == null) return ;

        u = hsFixPms(u, loadBox);
        that.send   (n, m, u, c);
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

        var t = n.closest(".tooltip");
        if (t.length) {
            n = t.data   ( "trigger");
        }
        if (typeof(u) === "function") {
            u.call(that, n, m);
            return;
        }

        var c = that.getIds(n);
        if (c == null) return ;

        u = hsFixPms(u, loadBox);
        that.open   (n, m, u, c);
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

    //** 搜索服务 **/

    if (findBox.length) {
        findBox.on("submit", function() {
            that.load(null ,
            hsSerialMix(loadDat, findBox)
            ); return false;
        });
    }

    //** 立即加载 **/

    if (loadUrl) {
        this.load(
            hsFixPms   (loadUrl, loadBox),
            hsSerialMix(loadDat, findBox)
        );
    }
}
HsList.prototype = {
    load     : function(url, data) {
        data =  hsSerialObj (data) ;
        if (url ) this._url  = url ;
        if (data) this._data = data;
        this.ajax({
            "url"      : this._url ,
            "data"     : this._data,
            "type"     : "POST",
            "dataType" : "json",
            "funcName" : "load",
            "cache"    : false,
            "global"   : false,
            "context"  : this,
            "complete" : this.loadBack
        });
    },
    loadBack : function(rst) {
        rst = hsResponse(rst, 1);

        this.listBox.trigger("loadBack", [rst, this]);

        var list = rst["list"] || [];
        var page = rst["page"] || {};

        // page state 取值: 0 错误, 1 正常, 2 非精确数量
        if (page[this._ps_key] === undefined) {
            page[this._ps_key] = rst.list.length ? 1 : 0;
        }
        if (page.msg === undefined) {
            page.mag = rst.msg;
        }

        this.fillList(list);
        this.fillPage(page);

        this.listBox.trigger("loadOver", [rst, this]);
    },
    fillList : function(list) {
        var ths, th, tb, tr, td, i, j, n, t, v, f;
        ths = this.listBox.find("thead th,thead td" );
        tb  = this.listBox.find("tbody"); tb.empty( );

        // 排序
        var sn = hsGetSeria (this._data,this.sortKey);
        if (! sn) {
            sn = hsGetParam (this._url ,this.sortKey);
        }
        if (jQuery.isArray(sn)) {
            sn = sn . join(",");
        }

        for (i = 0; i < ths .length; i ++) {
            th = jQuery(ths[i]);

            // 填充句柄
            f = th.data( "fl" );
            if (f && typeof f != "function") {
                try {
                    f = eval('(null||function(list,v,n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse list data-fl error: "+e);
                }
                th.data("fl",f);
            }

            // 排序处理
            if (th.hasClass("sortable")) {
                n  = th.data("fn");
                t  = th.data("ft");
                this._fill__sort(th, sn, n, t);
            }
        }

        this._list = list;
        for (i = 0; i < list.length; i ++) {
        this._info = list[i];

                tr = jQuery('<tr></tr>').appendTo(tb);
            for (j = 0; j < ths .length; j ++) {
                td = jQuery('<td></td>').appendTo(tr);
                th = jQuery(ths[j]);
                td.attr( "class" , th.attr("class") );
                td.attr( "style" , th.attr("style") );
                td.data(th.data( ));

                n  = th.data("fn");
                t  = th.data("ft");
                f  = th.data("fl");
                if (n !== undefined) {
                    v  =  hsGetValue ( list[i], n);
                if (v === undefined) {
                    v  =  list[i][n];
                }} else {
                    v  =  undefined ;
                }
                if (t === undefined) {
                    t  =  "_review" ;
                }

                // 调节
                if (f) {
                    v  = f.call(td[0], this, v, n);
                }
                // 填充
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

                if (td.is("input,select,textarea")) {
                    td.val (v);
                } else {
                    td.text(v);
                }
            }
        }

        delete this._list;
        delete this._info;
    },
    fillPage : function(page) {
        var that = this;
        var listBox = this.listBox;
        var pageBox = this.pageBox;
        if (page[this._ps_key] != 0) {
            pageBox.show();
            listBox.show();
        }  else
        if (pageBox.size()) {
            pageBox.show();
            listBox.hide();

            // 显示警示区
            if (page[this._pc_key] > 0 ||  page[this._rc_key] > 0) {
                pageBox.empty( ).append('<div class="alert alert-warning" style="width: 100%;">'
                           + (page.msg || this._above_err || hsGetLang('list.above')) + '</div>');
                setTimeout(function( ) {
                    hsSetSeria(that._data, that.pageKey);
                    that.load( ); // 返回第一页
                }, 5000);
            } else {
                pageBox.empty( ).append('<div class="alert alert-warning" style="width: 100%;">'
                           + (page.msg || this._empty_err || hsGetLang('list.empty')) + '</div>');
            }
            return;
        } else {
            // 弹出警示框
            if (page[this._pc_key] > 0 ||  page[this._rc_key] > 0) {
                jQuery.hsWarn(page.msg || this._above_err || hsGetLang('list.above') , "warning" ,
                function ( ) {
                    hsSetSeria(that._data, that.pageKey);
                    that.load( ); // 返回第一页
                });
            } else {
                jQuery.hsWarn(page.msg || this._empty_err || hsGetLang('list.empty') , "warning");
            }
            return;
        }

        this.pageBox.empty();

        var i, r, p, t, pmin, pmax;
        r = page[this.rowsKey] ? parseInt(page[this.rowsKey]) : this.rowsNum;
        p = page[this.pageKey] ? parseInt(page[this.pageKey]) : 1;
        t = page[this._pc_key] ? parseInt(page[this._pc_key]) : 0;
        if (t < 1) { // 用总数量算总页数
        t = page[this._rc_key] ? parseInt(page[this._rc_key]) : 0;
        t = Math.ceil( t / r );
        }
        pmin = p - Math.floor(this.pugsNum / 2);
        if (pmin < 1) pmin = 1;
        pmax = pmin + this.pugsNum - 1;
        if (pmax > t) pmax = t;
        pmin = pmax - this.pugsNum + 1;
        if (pmin < 1) pmin = 1;

        var pbox = jQuery('<ul class="pagination pull-left"></ul>').appendTo(this.pageBox);
        var qbox = jQuery('<em class="page-text pull-right"></em>').appendTo(this.pageBox);
        var nums = pbox; //jQuery('<ul class="pagination pull-left "></ul>').appendTo(this.pageBox);
        var btns = pbox; //jQuery('<ul class="pagination pull-right"></ul>').appendTo(this.pageBox);

        if (page[this._ps_key] == 2) {
            qbox.text(hsGetLang("list.page.unfo", page));
        } else {
            qbox.text(hsGetLang("list.page.info", page));
        }
///     if (t >  this.pugsNum) {
            qbox.append(jQuery('<a href="javascript:;" class="bi bi-hi-page"></a>'));
///     }

        if (1 < p) {
            btns.append(jQuery('<li class="page-prev"><a href="javascript:;" data-pn="'+(p-1)+'" title="'+hsGetLang("list.prev.pagi")+'">&laquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-prev disabled"><a href="javascript:;" title="'+hsGetLang("list.prev.page")+'">&laquo;</a></li>'));
        }

        for(i = pmin; i < pmax + 1; i ++) {
            nums.append(jQuery('<li class="'+(i != p ?'page-link':'page-curr active')+'"><a href="javascript:;" data-pn="'+i+'">'+i+'</a></li>'));
        }

        if (t > p) {
            btns.append(jQuery('<li class="page-next"><a href="javascript:;" data-pn="'+(p+1)+'" title="'+hsGetLang("list.next.pagi")+'">&raquo;</a></li>'));
        } else {
            btns.append(jQuery('<li class="page-next disabled"><a href="javascript:;" title="'+hsGetLang("list.next.page")+'">&raquo;</a></li>'));
        }

        var tm = null;
        var go = function(p) {
            if ( tm ) clearTimeout(tm);
            hsSetSeria(that._data, that.pageKey, p);
            that.load ( );
        };
        var to = function(p) {
            if ( tm ) clearTimeout(tm);
            tm = setTimeout(function() {
                go(p);
            } , 500 );
        };
        this.pageBox.find(".page-link a").on("click", function(ev) {
            go(jQuery(this).attr("data-pn"));
            ev.preventDefault();
        });
        this.pageBox.find(".page-prev a").on("click", function(ev) {
            to(jQuery(this).attr("data-pn"));
            ev.preventDefault();
        });
        this.pageBox.find(".page-next a").on("click", function(ev) {
            to(jQuery(this).attr("data-pn"));
            ev.preventDefault();
        });
        this.pageBox.find(".page-prev a").on("dblclick", function(ev) {
            go(1);
            ev.preventDefault();
        });
        this.pageBox.find(".page-next a").on("dblclick", function(ev) {
            go(t);
            ev.preventDefault();
        });
        this.pageBox.find(".page-text a").on("click", function(ev) {
            that.gotoPage(p, r);
            ev.preventDefault();
        });
    },
    gotoPage : function(pn, rn) {
        var that = this;
        jQuery.hsMask({
            title: '请输入页码和条数',
            mode : "warn",
            html : '<form autocomplete="off" novalidate="novalidate">'
                 + '<span> 跳转到第 </span><input type="number" name="pn" value="'+pn+'" class="form-control" style="display:inline-block;width:6em;" step="1"  min="1"  '   +   '/><span> 页, </span>'
                 + '<span> 平均每页 </span><input type="number" name="rn" value="'+rn+'" class="form-control" style="display:inline-block;width:6em;" step="10" min="10" max="100"/><span> 条. </span>'
                 + '<button type="submit" class="invisible"></button>'
                 + '</form>'
        }, {
            glass: 'btn-primary',
            label: hsGetLang("ensure"),
            click: function() {
                var fo = jQuery(this).closest(".alert-content").find("form");
                var rn = parseInt(fo.find("[name='rn']").val());
                var pn = parseInt(fo.find("[name='pn']").val());
                if (isNaN(pn) || isNaN(rn) || pn < 1 || rn < 10 || rn > 100) {
                    alert("输入错误, 页码需大于或等于1, 行数需10到100之间!");
                    return false;
                }
                jQuery(this).closest(".modal").find(":submit").click();
            }
        }, {
            glass: 'btn-default',
            label: hsGetLang("cancel")
        }).submit(function(evt) {
            evt.preventDefault( );
            evt.stopPropagation();
            jQuery(this).closest(".modal").modal("hide");
            var rn = $(this).find( "[name=rn]" ).val(  );
            var pn = $(this).find( "[name=pn]" ).val(  );
            that._data = hsSerialArr(that._data);
            that._url  = hsSetParam (that._url , that.rowsKey, rn);
            /* SetDat */ hsSetSeria (that._data, that.pageKey, pn);
            that.load();
        });
    },
    _ps_key  : "state" ,
    _pc_key  : "total" ,
    _rc_key  : "count" ,

    send     : function(btn, msg, url, data) {
        btn = jQuery(btn);
        var that = this ;
        var func = function() {
        var dat2 = jQuery.extend({}, hsSerialDat(url), hsSerialDat(data));
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

        this.load();
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
        var dat2 = jQuery.extend({}, hsSerialDat(url), hsSerialDat(data));
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

            that.load( );
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

    getAll   : function() {
        var cks = this.context.find(".checkone").filter(":checked");
        if (cks.length === 0) {
            alert(hsGetLang("list.get.all"));
            return null;
        }
        else {
            return cks ;
        }
    },
    getOne   : function() {
        var cks = this.context.find(".checkone").filter(":checked");
        if (cks.length !== 1) {
            alert(hsGetLang("list.get.one"));
            return null;
        }
        else {
            return cks ;
        }
    },
    getRow   : function(o) {
        var chk = jQuery(o).closest("tr,.itembox", this . listBox )
               .find(".checkone,.row-id,[data-fn=id],[data-ft=id]")
               .first ();

        // 规避未选中无取值的问题
        if (chk.is  (".checkone")
        &&  chk.is  (":checkbox,:radio")) {
            chk.not (":checked" )
               .prop( "checked" , true  )
               .change();
            chk.closest ( "tr,.itembox" )
               .siblings( "tr,.itembox" )
               .find(":checked.checkone")
               .prop( "checked" , false )
               .change();
        }

        return chk;
    },
    getIds   : function(o) {
        o = jQuery(o);
        if (o.hasClass("for-checks")) {
            return this.getAll( );
        } else
        if (o.hasClass("for-choose")) {
            return this.getOne( );
        } else
        {
            return this.getRow(o);
        }
    },

    // /** 填充函数 **/

    _fill__review : function(td, v, n) {
        if (v === undefined
        ||  v === null) {
            return;
        }

        var inp;

        // 链接,图片,视频,音频
        inp = ! td.is ("a,img,video,audio")
            ? td.find ("a,img,video,audio")
            : td;
        if (inp.length) {
            var u = ! v ? v : hsFixUri( v );
            inp.filter("a:empty").text( v );
            inp.filter("a").attr("href",u );
            inp.filter("a.a-email").attr("href", "mailto:"+v);
            inp.filter("a.a-tel").attr("href", "tel:"+v);
            inp.filter("a.a-sms").attr("href", "sms:"+v);
            inp.filter("img,video,audio").attr("src", u);
            return;
        }

        // 表单项
        inp = ! td.is ("input,select,textarea")
            ? td.find ("input,select,textarea")
            : td;
        if (inp.length) {
            if (inp.is(":radio,:checkbox")
            && ! jQuery.isArray(v)) {
                v = [v];
            }
            inp.val (v);
            return;
        }

        // 格式化
        v = this._fill__format (td, v, n );

        // 多个值
        if (jQuery.isArray(v)) {
            v = v .join(", ");
        }

        return  v ;
    },
    _fill__format : function(td, v, n) {
        if (v === undefined) return v;
        var f = td.data("format");
        if (f === undefined) return v;
        var a = [f] ;
        if (! jQuery.isArray(v) ) {
            v = [v] ;
        }
        Array.prototype.push.apply(a, v);
        return hsFormat.apply(window, a);
    },

    _fill__admin : function(td, v, n) {
        this.listBox.find( 'thead th,thead td' ).eq(td.index( ))
            .find(".invisible").clone().removeClass("invisible")
            .appendTo(td);
        jQuery(td).find("input:hidden").val( v );
        return false;
    },
    _fill__check : function(td, v, n) {
        jQuery('<input type="checkbox" class="checkone"/>')
            .attr("name" , n).val( v)
            .appendTo(td);
        return false;
    },
    _fill__radio : function(td, v, n) {
        jQuery('<input type="radio" class="checkone"/>')
            .attr("name" , n).val( v)
            .appendTo(td);
        return false;
    },
    _fill__rowid : function(td, v, n) {
        jQuery('<input type="hidden" class="row-id" />')
            .attr("name" , n).val( v)
            .appendTo(td);
        return false;
    },
    _fill__email : function(td, v, n) {
        if (! v) return v;
        n = "mailto:" + v;
        jQuery('<a target="_blank"></a>')
            .attr("href" , n).text(v)
            .appendTo(td);
        return false;
    },
    _fill__ulink : function(td, v, n) {
        if (! v) return v;
        v = hsFixUri( v );
        jQuery('<a target="_blank"></a>')
            .attr("href" , v).text(v)
            .appendTo(td);
        return false;
    },
    _fill__nlink : function(td, v, n) {
        // 把 a.b[c][] 改为 a-b-c
        n = n.replace(/(\[|\.)/, '-')
             .replace(/(\]|\-$)/, '');
        jQuery('<a href="javascript:;"></a>')
            .addClass("a-"+n).text(v)
            .appendTo(td);
        return false;
    },
    _fill__htime : function(td, v, n) {
        if (v === undefined) return v;
        var d1  =  new  Date();
        var d2  =  hsPrsDate(v, hsGetLang("datetime.format"));
        td.attr("title", this._fill__datetime (td, v, n));
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
        if (v === undefined) return v;
        var d1  =  new  Date();
        var d2  =  hsPrsDate(v, hsGetLang("date.format"));
        td.attr("title", this._fill__datetime (td, v, n));
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
        if (v === undefined) return v;
        return hsFmtDate(v, td.data("format") || hsGetLang("datetime.format"));
    },
    _fill__date : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, td.data("format") || hsGetLang("date.format"));
    },
    _fill__time : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, td.data("format") || hsGetLang("time.format"));
    },
    _fill__html : function(td, v, n) {
        if (v === undefined) return v;
        if (v === null ) v = "" ;
        jQuery(td).html( v );
    },
    _fill__text : function(td, v, n) {
        if (v === undefined) return v;
        if (v === null ) v = "" ;
        jQuery(td).text( v );
    },

    _fill__sort : function(th, s, n) {
        if (th.find(".sort-ico").size() === 0) {
            var  that = this ;
            th.click( function() {
                var s = null ;
                if (!!th.hasClass("sort-asc")) {
                    s = that._sort_esc(th, n);
                } else
                if (! th.hasClass("sort-esc")) {
                    s = that._sort_asc(th, n);
                }
                hsSetSeria(that._data, that.sortKey, s);
            //  hsSetSeria(that._data, that.pageKey, 1);
                that.load ( );
            });
            th.append('<span class="sort-ico"></span>');
        }

        th .removeClass("sort-asc sort-esc");
        if (s === this._sort_asc(th, n)) {
            th.addClass("sort-asc");
        } else
        if (s === this._sort_esc(th, n)) {
            th.addClass("sort-esc");
        }
    },
    _sort_asc : function(th, n) {
        return th.data("ob") || n;
    },
    _sort_esc : function(th, n) {
        return th.data("od") || (
            this._desc === "-"
          ? this._desc + this._sort_asc(th, n)
          : this._sort_asc(th, n) + this._desc
        );
    },
    _desc : "!"
};

/**
 * 列表填充分页按钮
 */
function hsListFillMore(page) {
    HsList.prototype.fillPage.call(this, page);
    this.pageBox.find(".page-text" ).remove( );
    this.pageBox.find(".pagination").removeClass("pull-left" );
}

/**
 * 列表填充分页按钮
 */
function hsListFillLess(page) {
    HsList.prototype.fillPage.call(this, page);
    this.pageBox.find(".page-text" ).remove( );
    this.pageBox.find(".page-link" ).remove( );
    this.pageBox.find(".page-curr" ).remove( );
    this.pageBox.find(".pagination").removeClass("pull-left" )
                                    .removeClass("pagination")
                                    .   addClass("pager"     );
    this.pageBox.find(".page-prev" ).addClass("previous")
                .find( "a" ).text(hsGetLang('list.prev.page'));
    this.pageBox.find(".page-next" ).addClass("next"    )
                .find( "a" ).text(hsGetLang('list.next.page'));
}

/**
 * 这是比 fillPage 更简单的上下页导航方式
 * 需替代 fillPage 时, 在初始化参数中加入
 * fillPage: hsListFillNext
 * @param {type} page
 * @return {undefined}
 */
function hsListFillNext(page) {
    if (page[this._ps_key] == "0") {
    if (page[this._pc_key] == "0"
    ||  page[this._rc_key] == "0") {
        this.warn(this._empty_err || hsGetLang('list.empty'), "warning");
        return;
    } else {
        this.warn(this._above_err || hsGetLang('list.above'), "warning");
        return;
    }}

    var r = page[this.rowsKey] ? parseInt(page[this.rowsKey]) : this.rowsNum;
    var p = page[this.pageKey] ? parseInt(page[this.pageKey]) :  1  ;
    var t = page[this._pc_key] ? parseInt(page[this._pc_key])
        : ( page[this._rc_key] == 0 ? p - 1
        : ( page[this._rc_key] == r ? p + 1
        : p ) );

    // 添加翻页按钮
    var btn = this.pageBox.find("[data-pn]");
    if (btn.size() === 0) {
        this.pageBox
        .append(jQuery(
            '<ul class="pager"></ul>'
        )
        .append(jQuery(
            '<li class="page-prev"><a href="javascript:;" data-pn="">'+hsGetLang('list.prev.page')+'</a></li>'
        ))
        .append(jQuery(
            '<li class="page-curr active"><a href="javascript:;"></a></li>'
        ))
        .append(jQuery(
            '<li class="page-next"><a href="javascript:;" data-pn="">'+hsGetLang('list.next.page')+'</a></li>'
        )));
        btn = this.pageBox.find("[data-pn]");
    }

    // 设置页码参数
    var pag = this.pageBox.find(".page-curr a");
    if (pag.size() === 0) {
        pag = this.pageBox.find(".page-curr"  );
    }
    if (page[this._ps_key] == "2") {
        pag.text(   p   );
    } else {
        pag.text(p+"/"+t);
    }
    if (p > 1) {
        pag = btn.closest(".page-prev").removeClass("disabled");
        pag.filter("[data-pn]").attr("data-pn", p-1);
        pag.find  ("[data-pn]").attr("data-pn", p-1);
    } else {
        pag = btn.closest(".page-prev").   addClass("disabled");
        pag.filter("[data-pn]").attr("data-pn", 1  );
        pag.find  ("[data-pn]").attr("data-pn", 1  );
    }
    if (p < t) {
        pag = btn.closest(".page-next").removeClass("disabled");
        pag.filter("[data-pn]").attr("data-pn", p+1);
        pag.find  ("[data-pn]").attr("data-pn", p+1);
    } else {
        pag = btn.closest(".page-next").   addClass("disabled");
        pag.filter("[data-pn]").attr("data-pn", t  );
        pag.find  ("[data-pn]").attr("data-pn", t  );
    }

    // 翻页点击事件
    if (! this.pageBox.data("inited")) {
        this.pageBox.data("inited", 1);
        var that = this;
        this.pageBox.on("click", "[data-pn]", function(evt) {
            evt.preventDefault ( );
            var po = jQuery (this);
            var pn = po.data("pn");
            var pk = that.pageKey ;
            if (po.prop( "disabled" )) {
                return;
            }
            hsSetSeria(that._data, pk, pn);
            that.load();
        });
    }
}

/**
 * 这是比 fillList 更简单的卡片式展现方式
 * 需替代 fillList 时, 在初始化参数中加入
 * fillList: hsListFillItem
 * @param {array} list 返回的列表数据
 */
function hsListFillItem(list) {
    var that = this;
    var tt, tr, td, n, t, v, f;

    // 条目模板
    if (! this.itemBox ) {
          this.itemBox = this.context.find(".itembox:hidden:first");
    }
    tt  = this.itemBox ;

    // _keep_prev 无论何种情况都不清空之前的列表
    // _keep_void 当前数据为空时不清空之前的列表
    if (! this._keep_prev
    ||  !(this._keep_void && list.length == 0 )) {
        this.listBox.children().not(tt).remove();
    }

    this._list = list;
    for (var i = 0 ; i < list.length ; i ++) {
        this._info = list[i];
        tr = tt.clone();
        tr.find("[data-fn],[data-ft],[data-fl]").each(function() {
            td = jQuery (this);
            n  = td.data("fn");
            t  = td.data("ft");
            f  = td.data("fl");
            if (n !== undefined) {
            v  = hsGetValue(list[i], n);
            if (v === undefined) {
                v  =  list[i][n];
            }} else {
                v  =  undefined ;
            }

            // 解析填充方法
            if (f && typeof f != "function") {
                try {
                    f = eval('(null||function(list,v,n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse list data-fl error: "+e);
                }
                td.data("fl", f);
            }

            // 调整
            if (f) {
                v  = f.call(td[0], that, v, n);
            }
            // 填充
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

            td.text(v);
        });
        tr.css( "display", "" );
        this.listBox.append(tr);
    }

        delete this._list;
    if (typeof(this._info) !== "undefined") {
        delete this._info;
    }
}

jQuery.fn.hsList = function(opts) {
    return this.hsBind(HsList, opts);
};

(function($) {
    $(document)
    .on("mouseover", ".table-responsive-revised th,.table-responsive-revised td",
    function(evt) {
        if ($(this).children().not("a,b,i,sub,sup,span").size()) {
            return;
        }
        if (! $(this).is("._title")) {
        if (! $(this).attr("title" )
        &&  ! $(this).data("title")) {
            $(this).addClass("_title");
        } else {
            return;
        }}
        // 当内容省略时鼠标悬浮可见更多
        if (this.offsetWidth < this.scrollWidth) {
            var tt = $( this ).text();
            $(this).attr("title", tt);
        } else {
            $(this).attr("title", "");
        }
    })
    .on("mouseout" , ".table-responsive-revised th,.table-responsive-revised td",
    function(evt) {
        if ($(this).children().not("a,b,i,sub,sup,span").size()) {
            return;
        }
        if ($(this).is("._title")) {
            $(this).attr("title", "");
        }
    })
    .on("click" , ".listbox tbody td",
    function(evt) {
        // 工具按钮有三类, 打开|打开选中|发送选中
        // 打开选中只能选一行, 发送选中可以选多行
        // 复选框太小不便操作, 当点击表格列时单选
        if ($(evt.target).is("a,input,textarea,button,select,option")
        ||  $(this).is(".dont-check,._admin,._amenu")) {
            return;
        }
        var tr = $(this).closest("tr");
        var ck = tr.find (".checkone");
        if (ck . is(":disabled")) {
            return;
        }
        ck.prop( "checked" , ! ck.prop("checked") )
          .change();
        if ( this !== ck.closest("td")[0] ) {
        tr.closest ("tbody")
          .find(".checkone:checked")
          .not (":disabled").not(ck)
          .prop( "checked" , false )
          .change();
        }
    })
    .on("change", ".HsList .checkone",
    function() {
        var box = $(this).closest(".HsList");
        var ckd = $(this).prop   ("checked");
        var max = box.find(".checkone").not(":disabled").length;
        var min = box.find(".checkone").not(":disabled")
                                    .filter(":checked" ).length;
        var chd = max && max == min ? true
              : ( min && min != max ? null
              :   false );
        $(this).closest("tr").toggleClass("active", ckd);
        box.find( ".checkall" ).prop("choosed" ,    chd);
        box.find(".for-choose").prop("disabled", 1!=min);
        box.find(".for-checks").prop("disabled", 0==min);
        if ($(this).is (":radio") && ckd) {
            $(this).closest("tr").siblings()
                   .removeClass ( "active" );
        }
    })
    .on("change", ".HsList .checkall",
    function() {
        var box = $(this).closest(".HsList");
        var ckd = $(this).prop   ("checked");
        var cks = box.find(".checkone").not(":disabled");
        $( cks).prop("checked", ckd).trigger( "change" );
        $(this).prop("choosed", ckd);
    })
    .on("loadOver", ".HsList .listbox",
    function() {
        var box = $(this).closest(".HsList");
        box.find(".for-choose, .for-checks")
                             .prop("disabled", true);
        box.find(".checkall").prop("checked", false)
                             .prop("choosed", false);
    });
})(jQuery);
