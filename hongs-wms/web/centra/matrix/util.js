
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

function setItemType(input, type) {
    input = jQuery(input);
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
                    var o = $('<option></option>');
                    o.prop("selected", s === true);
                    o.text(t).val(v).appendTo( n );
                }
            } else
            if ($(this).is(":checkbox")) {
                if ( COLS_PATT_DATA .test(attr)) {
                    field.find(name).attr(attr, $(this).prop("checked") ? "true" : "" );
                } else {
                    field.find(name).prop(attr, $(this).prop("checked"));
                }
            } else
            if (attr === "type") {
               setItemType(field.find(name), $(this).val());
            } else
            if (attr !== "text") {
                field.find(name).attr(attr , $(this).val());
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
        if (! n || !v ) {
            return;
        }
        if (!COLS_PATT_BASE.test(n)) {
            v = (n + "|" + v);
            n = "data-"+(I++);
        } else
        if (!COLS_PATT_DATA.test(n)) {
            n = "data-"+ n ;
        }
        fd.removeAttr(n);
        fd.attr(n, v);
        uz[n] = true ;
    });

    // 清理遗留
    for(var i = 0; i < az.length; i ++) {
        var n = az[i].name ;
        var v = az[i].value;
        if (!COLS_PATT_DATA.test(n)
        || (v &&uz[n]) ) {
            continue ;
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
        var input = $(this).find(   "[data-fn],[name]"   ).first();
        var text  = label.text();
        var name  = input.attr("name") || input.attr("data-fn");
        var type  = input.attr("type") || input.prop("tagName").toLowerCase();
        var required = input.prop("required") || input.data("required") ? "true" : "";
        var repeated = input.prop("multiple") || input.data("repeated") ? "true" : "";
        var params   = {};

        if (name == "@") {
            type  = "" ;
            text  = "" ;
        } else
        if (name.substr(0, 1) == "-") {
            name  = "" ;
        }

        if (type =="ul") {
            type  = $(this).data("type");
        }
        if (type =="image") {
            params["__rule__"] = "Thumb";
        }

        var a = input.get(0).attributes ;
        for(var i = 0; i < a.length; i ++) {
            var k = a[i].nodeName ;
            var v = a[i].nodeValue;
            if (v !== ""
            &&  k.substr(0,5) === "data-") {
                if (k === "data-fn"
                ||  k === "data-ft"
                ||  k === "data-required"
                ||  k === "data-repeated") {
                    continue;
                }
//              if (k === "data-datalist") {
//                  v = JSON.stringify(prsDataList(v));
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
            }
        }
        if (input.is("select")) {
            var datalist = [];
            var selected = [];
            input.find("option").each(function() {
                datalist.push([$(this).val(), $(this).text()]);
                if ($(this).prop("selected")) {
                selected.push( $(this).val());
                }
            });
            input.change();
            params["datalist"] = JSON.stringify(datalist);
            params["selected"] = JSON.stringify(selected);
        }
        fields.push($.extend({
            "__text__": text,
            "__name__": name,
            "__type__": type,
            "__required__": required,
            "__repeated__": repeated
        }, params));
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
        var text  = field["__text__"];
        var name  = field['__name__'];
        var type  = field["__type__"];
        var required = field["__required__"];
        var repeated = field["__repeated__"];

        // 表单高级配置
        if (name == "@") {
            type  = "_";
            text  = getTypeItem(wdgt , type).find("label span").text();
        }
        // 内部缺省字段, 禁止自行设置
        if ((type == "number" && (name == "ctime" || name == "mtime"))
        || ( type == "hidden" && (name == "cuid"  || name == "muid" || name == "id"))) {
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
        if (input.is( "ul" ) ) {
            input.attr("data-fn", name);
            input.attr("data-required", required);
            input.attr("data-repeated", repeated);
        } else {
            input.attr("name"   , name);
            input.prop("required" , ! ! required);
            input.prop("multiple" , ! ! repeated);
        }

        for(var k in field) {
            if (/^__/.test( k ) ) {
                continue;
            }
            if (k === "selected") {
                continue;
            }
            if (k === "datalist") {
                if (input.is("input")) {
//                  var datalist = JSON.parse(field["datalist"]) || [];
//                  input.attr("data-datalist", strDataList(datalist));
                    input.attr("data-datalist", field[k] );
                } else {
                    input.empty();
                    var datalist = JSON.parse(field["datalist"]) || [];
                    var selected = JSON.parse(field["selected"]) || [];
                    for(var j = 0; j < datalist.length; j ++ ) {
                        var a = datalist[j];
                        var o = $("<option></option>");
                        o.val(a[0]).text(a[1]).appendTo(input);
                    }
                    input.val(selected);
                }
                continue;
            }

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

    // 模拟提交
    targets.find("[name=validate]").data("validate", function() {
        return false;
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
    targets.on("click", ".ensure", function() {
        var conf = [];
        gainFlds(conf, targetz, widgets);
        records.find("[name=conf]").val(JSON.stringify(conf));
        records.find(":submit").click();
    });

    // 删除字段
    targetz.on("click", ".glyphicon-remove-sign", function() {
        $(this).closest(".form-group").remove();
    });
    // 添加字段
    widgets.on("click", ".glyphicon-plus-sign", function() {
        // 预定字段不能重复添加
        var item = $(this).closest(".form-group");
        if (item.is(".base-field")) {
            var name = item.attr("data-type");
            if (targetz.find("[data-type='"+ name +"']").size()) {
                $.hsWarn("预定字段不可重复添加, 请检查已设字段");
                return;
            }
        }

        field = $(this).closest(".form-group").clone();
        targetz.append( field);

        // 立即打开自定字段设置
        var input = field.find ("[name='-']" );
        if (input.size() != 0) {
            input.attr("name", "-"+(index ++));
            field.find(".glyphicon-info-sign").click();
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
        modal.find( ".simple-set" ) // 基础设置区域
             .empty( ).append(pane);
        modal.find( ".detail-set" ) // 详细设置区域
             .find("tr").not(".hide").remove();

        // 表单设置只显示高级, 其他情况点到基础设置
        if (type == '_') {
            tabs.find("li:eq(1) a").click();
            tabs.find("li:eq(0)"  ).hide( );
        } else {
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
        listbox.find(".listbox tbody tr")
        .draggable({
            opactiy: 0.5,
            revert: "invalid",
            helper: function() {
                var fid = $(this).find("td:eq(0) input").val ();
                var txt = $(this).find("td:eq(1)"      ).text();
                return $('<div data-type="form"></div>')
                        .data("form_id" , fid)
                        .data("unit_id" , uid)
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
                return $('<div data-type="unit"></div>')
                        .text( $(this).children("table")
                        .find( ".tree-name" ).text( )  );
            }
        });

        treebox.find(".tree-node table" )
        .droppable({
            drop: function(ev, ui) {
                var pid = $(this).parent().attr("id").substring(10);
                if (ui.helper.data("type") == "unit") {
                    var did = ui.draggable.attr("id").substring(10);
                    var req = { id : did, pid : pid };
                    $.hsWarn(
                        "您确定将此单元移到新单元下吗?",
                        "移动后导航结构发生改变, 可能还会影响到顶部菜单.",
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
                    $.hsWarn(
                        "您确定将此表单移到新单元下吗?",
                        "移动后导航结构发生改变, 可能还会影响到顶部菜单.",
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
