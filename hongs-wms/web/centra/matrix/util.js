
function getTypeName(widgets, type) {
    if (type == "datetime" || type == "time") type = "date";
    return widgets.find( "[data-type='"+type+"']"   ).find("span").first().text();
}
function getTypeItem(widgets, type) {
    if (type == "datetime" || type == "time") type = "date";
    return widgets.find( "[data-type='"+type+"']"   ).clone();
}
function getTypePane(context, type) {
    if (type == "datetime" || type == "time") type = "date";
    return context.find(".widget-pane-"+type).first().clone();
}

function setItemType(input, type) {
    input = jQuery(input);
    if (type == input.attr("type")) {
        return  input;
    }
    var oldAttrs = input[0].attributes;
    var newInput = jQuery('<input type="'+type+'"/>');
    for(var i = 0; i < oldAttrs.length; i ++ ) {
        var attrValue = oldAttrs[i].nodeValue;
        var attrName  = oldAttrs[i].nodeName ;
        if (attrName != "type") {
            newInput.attr(attrName, attrValue);
        }
    }
    input.before(newInput);
    input.remove( );
    return newInput;
}

function getFormInfo(id, func) {
    $.ajax({
        url : hsFixUri("centra/matrix/form/info.act?rb=id,name&id="+id),
        type: "GET",
        dataType: "JSON",
        success : function(rs) {
            if  (     rs.info) {
                func (rs.info);
            }
        }
    });
}

function prsDataList(s) {
    var a = [];
    var x = s.split(/[\r\n]/);
    for(var i = 0; i < x.length; i ++) {
        var z = $.trim(x[i]);
        if (! z.length ) {
            continue;
        }
        // 键值
        z = z.split ( "::" );
        var v = $.trim(z[0]);
        var t = z.length > 1
              ? $.trim(z[1])
              : v;
        a.push([v, t]);
    }
    return a;
}
function strDataList(a) {
    var x = [];
    for(var i = 0; i < a.length; i ++) {
        var v = a[i][0];
        var t = a[i][1];
        if (t != v) {
            v += "::" + t;
        }
        x.push ( v);
    }
    return x.join("\r\n");
}

var COLS_PATT_BASE = /^[a-z0-9\-_.:]+$/;    // 当作为标签属性时可以使用, 没有引起问题的特殊字符
var COLS_PATT_DATA = /^data-/;              // 以 data 开头不用再加前缀, 如 data-tk, data-ak 等
var COLS_PATT_DATN = /^data-\d+$/;          // 数字结尾要从属性值里取键
var COLS_PATT_DATL = /^data-(?=\W|...)/;    // 长属性在写回时要去掉前缀
var COLS_PATT_DATS = /^data-(?!f[tn]$)/;    // fn和ft很特殊需要专门处理

/**
 * 打开设置时载入字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function loadConf(modal, field) {
    var fd = field.find("[data-fn], [name]").first();
    var az = fd[0].attributes;
    var tb = modal.find(".detail-set tbody");
    var tp = tb.find(".hide");
    var uz = {};

    // 基础设置
    modal.find(".simple-set")
         .find("input,select,textarea")
         .each(function( ) {
        var name = $(this).attr("name") || $(this).attr("data-fn");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
            uz [attr] = true;
            if (attr === "datalist") {
                var x =  "" ;
                field.find(name).find("option").each(function() {
                    var s = $(this).prop("selected");
                    var t = $(this).text();
                    var v = $(this).val ();
                    if (s) {
                        x +=   "!!";
                    }
                    if (t !== v) {
                        x += v+"::";
                    }
                    x += t + "\r\n";
                });
                field.find(name).find( "input").each(function() {
                    var s = $(this).prop( "checked");
                    var t = $(this).next().text();
                    var v = $(this).val ();
                    if (s) {
                        x +=   "!!";
                    }
                    if (t !== v) {
                        x += v+"::";
                    }
                    x += t + "\r\n";
                });
                $(this).val(x);
            } else
            if ($(this).is(":checkbox")) {
                if (/^data-/.test(attr)) {
                    $(this).prop("checked", field.find(name).attr(attr) ? true : false);
                } else {
                    $(this).prop("checked", field.find(name).prop(attr));
                }
            } else
            if (attr === "type") {
                $(this).val(field.find(name).attr(attr)).change();
            } else
            if (attr !== "text") {
                $(this).val(field.find(name).attr(attr));
            } else
            {
                $(this).val(field.find(name).text());
            }
        } else {
                $(this).val(field.find(name).text());
        }

        if ($(this).is("[name='textarea|data-type']")) {
            $(this).change();
        }
    });

    // 高级设置
    var fc, fn;
    for(var i = 0; i < az.length; i ++) {
        var x = az[i];
        if (! COLS_PATT_DATS.test( x.name ) || uz[ x.name ]) {
            continue;
        }
        var tr = tp.clone().appendTo(tb).removeClass("hide");
        var pv =  x.value;
        var pn =  x.name ;
        if (COLS_PATT_DATN.test(pn)) {
            var j = pv.indexOf("|");
            pn = pv.substring (0,j);
            pv = pv.substring (1+j);
        } else
        if (COLS_PATT_DATL.test(pn)) {
            pn = pn.substring ( 5 );
        }
        tr.find("[name=param_name]" ).val(pn);
        tr.find("[name=param_value]").val(pv);
        // 给下方关联用
        if (pn == "conf") fc = pv;
        if (pn == "form") fn = pv;
    }

    // 关联设置
    var ul = modal.find(".simple-set [data-ft=_pick]");
    if (fn && ul.size ()) getFormInfo(fn, function(rs) {
        var ds = {};
        ds[ rs.id ] = [rs.name];
        hsFormFillPick(ul, ds );
    });
}

/**
 * 确定设置时写入字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function saveConf(modal, field) {
    var uz = { "data-fn" : true, "data-ft" : true };
    var fd = field.find("[data-fn],[name]").first();
    var az = fd[0].attributes;
    var I  = 0;

    // 基础设置
    modal.find(".simple-set")
         .find("input,select,textarea")
         .each(function( ) {
        var name = $(this).attr("name") || $(this).attr("data-fn");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
            uz [attr] = true;
            if (attr === "datalist") {
                var x = $(this).val().split(/[\r\n]/);
                var n = field.find (name).empty( );
                for(var i = 0; i < x.length; i ++) {
                    var z = $.trim(x[i]);
                    if (! z.length ) {
                        continue;
                    }
                    // 选中
                    var s = null;
                    if (z.substring(0 , 2)==='!!') {
                        z = z.substring(2);
                        s = true;
                    }
                    // 键值
                    z = z.split ( "::" );
                    var v = $.trim(z[0]);
                    var t = z.length > 1
                          ? $.trim(z[1])
                          : v;
                    // 选项
                    if (n.is("select") ) {
                        var o = $('<option></option>');
                        o.prop("selected", s === true);
                        o.text(t).val(v).appendTo( n );
                    } else {
                        var o = $('<input type="'+(n.is(".checkbox")?"checkbox":"radio")+'"/>');
                        var w = $( '<label></label>' );
                        var s = $(  '<span></span>'  );
                        o.prop( "checked", s === true);
                        o.attr("name", n.data( "fn" ));
                        o.val (v).appendTo( w );
                        s.text(t).appendTo( w );
                        w.appendTo( n );
                    }
                }
            } else
            if ($(this).is(":checkbox")) {
            if ($(this).prop("checked")) {
                field.find(name).attr(attr, $(this).val());
            } else {
                field.find(name).removeAttr(attr);
            }
            } else
            if (attr === "type") {
               setItemType(field.find(name),$(this).val());
               fd = field.find("[data-fn],[name]").first(); // 改变类型会重建节点， 重新获取以规避问题
            } else
            if (attr !== "text") {
                field.find(name).attr(attr, $(this).val());
            } else
            {
                field.find(name).text($(this).val());
            }
        } else {
                field.find(name).text($(this).val());
        }
    });

    // 高级设置
    modal.find(".detail-set tr")
         .not ( ".hide" )
         .each(function() {
        var n = $(this).find("[name=param_name]" ).val();
        var v = $(this).find("[name=param_value]").val();
        if (! n ) {
            return;
        }
        if (! COLS_PATT_BASE.test( n )) {
            v = (n + "|" + v);
            n = "data-"+(I++);
        } else
        if (! COLS_PATT_DATA.test( n )) {
            n = "data-"  + n ;
        }
        fd.removeAttr(n);
        fd.attr ( n , v);
        uz[n] = true;
    });

    // 清理遗留
    for(var i = 0; i < az.length; i ++) {
        var n = az[i]["name"];
        if (/**/uz[n]
        ||  ! COLS_PATT_DATA.test( n )) {
            continue;
        }
        fd.removeAttr(n);
    }

    // 关联路径
    if (fd.is("[data-ft=_fork]")) {
        fd.siblings(  "button"  )
          .attr("data-href", fd.attr("data-al"))
          .attr("data-hrel", fd.attr("data-at"));
    }
}

/**
 * 从表单获取字段
 * @param {Array} fields
 * @param {jQuery} area
 */
function gainFlds(fields, area) {
    area.find(".form-group").each(function() {
        var label = $(this).find("label span,legend span").first();
        var input = $(this).find(   "[name],[data-fn]"   ).first();
        var text  = label.text();
        var hint  = input.attr("placeholder");
        var name  = input.attr("name") || input.attr("data-fn");
        var type  = input.attr("type") || input.prop("tagName");
        var required = input.prop("required") || input.data("required") ? "true" : "";
        var repeated = input.prop("multiple") || input.data("repeated") ? "true" : "";

            type  = type.toLowerCase ( );
        if (type == "ul"
        ||  type == "div"
        ||  type == "textarea") {
            type  = $(this).data("type");
        }

        var params = {
            "__name__": name,
            "__type__": type,
            "__text__": text,
            "__hint__": hint,
            "__required__": required ,
            "__repeated__": repeated
        };

        if (/^\-/.test(name)) {
            params["__name__"] = "-" ;
        } else
        if (/^@$/.test(name)) {
            delete params["__type__"];
            delete params["__text__"];
            delete params["__hint__"];
            delete params["__required__"];
            delete params["__repeated__"];
        }

        var a = input.get(0).attributes;
        for(var i = 0; i < a.length; i ++) {
            var v = a[i].nodeValue;
            var k = a[i].nodeName;
            if ("data-" === k.substr(0,5)) {
                if (k === "data-fn"
                ||  k === "data-ft"
                ||  k === "data-required"
                ||  k === "data-repeated") {
                    continue ;
                }
//              if (k === "data-datalist") {
//                  v = prsDataList    (v);
//                  v = JSON.stringify (v);
//              }
                if (COLS_PATT_DATN.test(k)) {
                    var j = v.indexOf("|");
                    k = v.substring(0 , j);
                    v = v.substring(1 + j);
                } else
                if (COLS_PATT_DATL.test(k)) {
                    k = k.substring(5    );
                }
                params[k] = v;
            } else {
                if (k !== "min"
                &&  k !== "max"
                &&  k !== "step"
                &&  k !== "pattern") {
                    continue ;
                }
                params[k] = v;
            }
        }
        if (input.is("select") ) {
            var datalist = [];
            var selected = [];
            input.find("option").each(function() {
                datalist.push([$(this).val(), $(this).text()]);
                if ($(this).prop("selected")) {
                selected.push( $(this).val());
                }
            });
            input.change( );
            params["datalist"] = JSON.stringify(datalist);
            params["selected"] = JSON.stringify(selected);
        } else
        if (input.is(".check") ) {
            var datalist = [];
            var selected = [];
            input.find( "input").each(function() {
                datalist.push([$(this).val(), $(this).next().text()]);
                if ($(this).prop( "checked")) {
                selected.push( $(this).val());
                }
            });
            input.change( );
            params["datalist"] = JSON.stringify(datalist);
            params["selected"] = JSON.stringify(selected);
        }

        fields.push(params);
    });
}

/**
 * 向表单设置字段
 * @param {Array} fields
 * @param {jQuery} area
 * @param {jQuery} wdgt
 */
function drawFlds(fields, area, wdgt, pre, suf) {
    for(var I = 0, i = 0; i < fields.length; i ++) {
        var field = fields[i];
        var name  = field['__name__'];
        var type  = field["__type__"];
        var text  = field["__text__"];
        var hint  = field["__hint__"];
        var required = field["__required__"];
        var repeated = field["__repeated__"];

        // 表单高级配置
        if (name == "@") {
            type  = "_";
            text  = getTypeItem(wdgt , type).find("label span").text();
        }
        // 内部缺省字段, 禁止自行设置
        if ((type == "number" && (name == "ctime" || name == "mtime"))
        || ( type == "hidden" && (name == "cuser" || name == "muser" || name == "id"))) {
            continue;
        }

        if (pre) {
            name  = pre + name;
        }
        if (suf) {
            name  = name + suf;
        }
        var group = getTypeItem(wdgt , name);
        if (group.size() == 0) {
            group = getTypeItem(wdgt , type);
        }
        if (group.size() == 0) {
            group = getTypeItem(wdgt , "-" ).show();
        }
        var label = group.find("label span,legend span").first( );
        var input = group.find(   "[data-fn],[name]"   ).first( );
            label.text( text );
        if (type == "date" || type == "time" || type == "datetime"
        ||  input.is("input[type='']")) {
            input = setItemType(input, type);
        }
        if (input.is("ul,div")) {
            input.attr("data-fn", name);
            input.attr("data-required", required);
            input.attr("data-repeated", repeated);
        } else {
            input.attr("name"   , name);
        if (group.attr("data-type") !== "_"
        &&  group.attr("data-type") !== "-") {
            input.attr("placeholder", hint );
            input.prop("required" , ! ! required);
            input.prop("multiple" , ! ! repeated);
        }}

        for(var k in field) {
            // 双下划线为字段标准属性, 上面已处理
            // 表单配置(_)和自定字段(-)需特殊处理
            if (k === "__name__"
            ||  k === "__type__") {
                continue;
            }
            if (group.attr("data-type") === "_" ) {
            if (k === "__rule__") {
                continue;
            }} else
            if (group.attr("data-type") === "-" ) {
            if (k === "__text__") {
                continue;
            }} else
            if (/^__/.test(  k  )
            && !input.attr("data-"+ k)) {
                continue;
            }

            if (k === "selected") {
                continue; // 下方一并处理
            }
            if (k === "datalist") {
                if (input.is( "input")) {
//                  var datalist = JSON.parse(field["datalist"]) || [];
//                  input.attr("data-datalist", strDataList(datalist));
                    input.attr("data-datalist", field[k] );
                } else
                if (input.is("select")) {
                    input.empty();
                    var datalist = JSON.parse(field["datalist"]) || [];
                    var selected = JSON.parse(field["selected"]) || [];
                    for(var j = 0; j < datalist.length; j ++ ) {
                        var a = datalist[j];
                        var o = $("<option></option>");
                        o.val(a[0]).text(a[1]).appendTo(input);
                    }
                    input.val(selected);
                } else {
                    input.empty();
                    var datalist = JSON.parse(field["datalist"]) || [];
                    var selected = JSON.parse(field["selected"]) || [];
                    var ct = input.is(".radio") ? "radio" : "checkbox";
                    for(var j = 0; j < datalist.length; j ++ ) {
                        var a = datalist[j];
                        var o = $('<input type="' + ct + '">');
                        var w = $('<label></label>');
                        var s = $( '<span></span>' );
                        o.attr("name" , input.attr("data-fn"));
                        o.val (a[0]).appendTo(w);
                        s.text(a[1]).appendTo(w);
                        w.appendTo(input);
                    }
                    input.find(":"+ct).val(selected);
                }
                continue;
            }

            if (k === "min"
            ||  k === "max"
            ||  k === "step"
            ||  k === "pattern") {
                input.attr(k, field[k]);
            } else
            if (COLS_PATT_DATA.test(k)) {
                input.attr(k, field[k]);
            } else
            if (COLS_PATT_BASE.test(k)) {
                input.attr("data-"+ k, field[k]);
            } else
            {
                input.attr("data-"+(I++), k+"|"+field[k]);
            }

            // 关联参数以 data- 打头
            if (k === "data-al" ) {
                input.siblings("button").attr("data-href", field[k]);
            } else
            if (k === "data-at" ) {
                input.siblings("button").attr("data-hrel", field[k]);
            }
        }
        area.append(group);
    }
}

$.fn.hsCols = function() {
    var context = $(this);
    var records = context.find(".record-form"); // 记录表单
    var widgets = context.find(".widget-form"); // 控件表单
    var targets = context.find(".target-form"); // 目标表单
    var targetz = context.find(".target-area"); // 目标区域
    var modal = context.find(".modal");
    var index = 0;
    var field ;

    // 字段排序
    targetz.sortable({
        items : ".form-group",
        sort  : function() {
            $(this).removeClass("ui-state-default");
        }
    });
    // 属性排序
    modal.find(".detail-set table").sortable({
        items : "tr",
        sort  : function() {
            $(this).removeClass("ui-state-default");
        }
    });

    // 字段加载
    records.on("loadOver", function(evt, rst) {
        if (!rst.info || !rst.info.conf) {
            return;
        }
        var conf = rst.info.conf;
        if (! $.isArray(conf)) {
            conf = eval(conf);
        }
        drawFlds(conf, targetz, widgets);
    });

    // 字段保存
    context.on("click", ".commit", function() {
        var conf = [];
        gainFlds(conf, targetz, widgets);
            conf = JSON.stringify (conf);
        records.find("[name=conf]").val(conf);
        records.find(":submit").click( );
    });

    // 删除字段
    targetz.on("click", ".glyphicon-remove-sign", function() {
        $(this).closest(".form-group").remove();
    });

    // 添加字段
    widgets.on("click", ".glyphicon-plus-sign", function() {
        // 预定字段不能重复添加
        var group = $(this).closest(".form-group");
        if (group.is(".base-field")) {
            group = targetz.find("[data-type='"+group.attr("data-type")+"']");
        if (group.size( )  !==  0  ) {
            $.hsWarn("此字段不可重复添加, 请检查已设字段!", "warning");
            return;
        }}

        field = $(this).closest(".form-group")
                .clone( ).appendTo ( targetz );

        // 立即打开自定字段设置
        var input = field.find("[name='-'],[data-fn='-']");
        if (input.size()) {
            index ++;
            input.filter("[name='-']"   )
                 .attr  ( "name"   , "-" + index);
            input.filter("[data-fn='-']")
                 .attr  ( "data-fn", "-" + index);
            field.find  (".glyphicon-info-sign" ).click( );
        }
    });

    // 打开设置
    targetz.on("click", ".glyphicon-info-sign", function() {
        field = $(this).closest(".form-group");
        var tabs =  modal.find ( ".nav:first");
        var type =  field.attr ( "data-type" );
        var name = getTypeName (widgets, type);
        var pane = getTypePane (context, type);
        if (pane.size() === 0) {
            pane = getTypePane (context, "-" );
        }

        modal.find("h4").text(name);
        modal.find( ".simple-set" ) // 基础设置区
             .empty( ).append(pane);
        modal.find( ".detail-set" ) // 详细设置区
             .find("tr").not(".hide").remove();

        // 表单设置只显示高级, 分隔栏仅需基础设置
        if (type == '_') {
            tabs.find("li:eq(1) a").click();
            tabs.find("li:eq(0)"  ).hide( );
        } else
        if (type == 'legend') {
            tabs.find("li:eq(0) a").click();
            tabs.find("li:eq(1)"  ).hide( );
        } else
        {
            tabs.find("li:eq(0) a").click();
            tabs.find("li"        ).show( );
        }

        loadConf(modal, field);
        modal.modal( "show"  );
    });
    
    // 完成设置
    modal.find("form").submit(function() {
        modal.modal( "hide"  );
        saveConf(modal, field);
    });

    // 添加属性
    modal.on("click", ".add-param", function() {
        var tb = $(this).prev( ).find("tbody");
        var tr = tb.find( ".hide" ).clone();
        tr.appendTo(tb).removeClass("hide");
    });
    
    // 删除属性
    modal.on("click", ".del-param", function() {
        $(this).closest("tr").remove();
    });

    // 日期类型
    modal.on("change", "[name='input|type']", function() {
        var type = $(this).val();
        $(this).closest(".form-group")
               .next   (".form-group")
               .find   ("input")
               .each   (function( ) {
            setItemType(this, type);
        });
    });

    // 文本类型, 富文本、代码等
    modal.on("change", "[name='textarea|data-type']", function() {
        var type = $(this).val();
        $(this).closest(".form-group")
               .next   (".form-group")
               .toggle (type==="code")
               .val    (type);
    });

    // 关联选项
    modal.on("pickBack", function(ev, items) {
        var tb = modal.find(".detail-set tbody");
        var tp =    tb.find(".hide");
        if (items) {
            for(var k in items) {
                items  = items[k][1];
                break;
            }
            for(var k in items) {
                var v  = items[k];
                var tr = tb
                   .find   ("[name=param_name]")
                   .filter (function() {return $(this).val() == k;})
                   .closest( "tr" );
                if (tr.size() == 0) {
                    tr = tp.clone().appendTo(tb).removeClass("hide");
                }
                tr.find("[name=param_name]" ).val(k);
                tr.find("[name=param_value]").val(v);
            }
        }
    });
};

//** 单元和表单互移动 **/

function hsFormMove(treebox, listbox) {
    listbox.on("loadOver", function(evt, rst, mod) {
        var uid = hsGetParam(mod._url , "unit_id");

        listbox.find(".listbox tbody tr td:nth-child(3)")
        .draggable({
            opactiy: 0.5,
            revert: "invalid",
            helper: function() {
                var tr  = $(this).closest("tr");
                var fid = tr.find("td:eq(0) input").val();
            //  var img = tr.find("td:eq(2)").clone();
                var txt = tr.find("td:eq(2)").clone();
                return $('<table data-type="form"></table>')
                        .data("form_id" , fid)
                        .data("unit_id" , uid)
                        .append($('<tr></tr>')
                                //.append(img)
                                  .append(txt)
                        );
            }
        });
    });

    treebox.on("loadOver", function(evt, rst, mod) {
        treebox.find(".tree-node .tree-name" )
        .draggable({
            opacity: 0.5,
            revert: "invalid",
            helper: function() {
                var txt = $(this).clone();
                return $('<table data-type="unit"></table>')
                        .append($('<tr></tr>').append(txt) );
            }
        });

        treebox.find(".tree-node table" )
        .droppable({
            accept: function(item) {
                return ! item.is(".tree-name")
                    || ! $.contains(item.closest(".tree-node").get(0), $(this).get(0));
            },
            drop: function(ev, ui) {
                var pid = $(this).parent().closest(".tree-node").attr("id").substring(10);
                if (ui.helper.data("type") == "unit") {
                    var did = ui.draggable.closest(".tree-node").attr("id").substring(10);
                    var req = { id : did, pid : pid };
                    $.hsMask({
                            "mode" : "warn",
                            "class": "alert-success",
                            "title": "您确定将此单元移到新单元下吗?",
                            "text" : "移动后导航结构发生改变, 可能还会影响到顶部菜单."
                        },
                        {
                            "label": "移动",
                            "click": function() {
                                $.ajax({
                                    url     : hsFixUri("centra/matrix/unit/save.act"),
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
                    var fid = ui.helper.data("form_id");
                    var req = {id: fid, "unit_id": pid};
                    $.hsMask({
                            "mode" : "warn",
                            "class": "alert-success",
                            "title": "您确定将此表单移到新单元下吗?",
                            "text" : "移动后导航结构发生改变, 可能还会影响到顶部菜单."
                        },
                        {
                            "label": "移动",
                            "click": function() {
                                $.ajax({
                                    url     : hsFixUri("centra/matrix/form/save.act"),
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
