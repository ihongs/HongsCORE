
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

function getFormInfo(id) {

}

/**
 * 打开设置时载入字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function loadConf(modal, field) {
    var set = {};

    modal.find(".simple-set")
         .find("input,select,textarea")
         .each(function( ) {
        var name = $(this).attr("name") || $(this).attr("data-fn");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
            set[attr] = true;
            if (attr === "datalist") {
                var x = "";
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
    });

    var az = field.find("[data-fn], [name]")[0].attributes;
    var tb = modal.find(".detail-set tbody");
    var tp = tb.find(".hide");
    for(var i = 0; i < az.length; i ++) {
        var x = az[i];
        if (! /^data-(?!fn|ft)/.test(x.name) || set[x.name]) {
            continue;
        }
        var tr = tp.clone().appendTo(tb).removeClass("hide");
        var pv =  x.value;
        var pn =  x.name ;
        if (/^data-.{3,}/.test(pn)) {
            pn = pn.substring ( 5 );
        }
        tr.find("[name=param_name]" ).val(pn);
        tr.find("[name=param_value]").val(pv);
    }
}

/**
 * 确定设置时写入字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function saveConf(modal, field) {
    var set = {"data-fn": true, "data-ft": true};

    modal.find(".simple-set")
         .find("input,select,textarea")
         .each(function( ) {
        var name = $(this).attr("name") || $(this).attr("data-fn");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
            set[attr] = true;
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
                if (/^data-/.test(attr)) {
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
    var fd = field.find("[data-fn], [name]").first();
    var tr = modal.find(".detail-set tr").not( ".hide" );
    var a  = fd[0].attributes;
    tr.each(function() {
        var n = $(this).find("[name=param_name]" ).val();
        var v = $(this).find("[name=param_value]").val();
        if (!/^data-/.test(n)) {
            n = "data-"+n ;
        }
        fd.attr(n, v);
        set[n] = true;
    });
    for(var i = 0; i < a.length; i ++) {
        var n = a[i].name ;
        var v = a[i].value;
        if (!/^data-/.test(n)) {
            continue;
        }
        if (v != '' && set[n]) {
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

        if ($(this).is("[data-type=fork" )) {
            type = "fork" ;
        } else
        if ($(this).is("[data-type=image")) {
            type = "image";
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
                if (/^data-.{3,}/.test(k)) {
                    k = k.substring(5);
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
    for(var i = 0; i < fields.length; i ++) {
        var field = fields[i];
        var text  = field["__text__"];
        var name  = field['__name__'];
        var type  = field["__type__"];
        var required = field["__required__"];
        var repeated = field["__repeated__"];

        // 表单高级配置
        if (name == "@") {
            type  = "_";
            text  = getTypeItem( wdgt, type )
                          .find("label span")
                          .first( ).text ( );
        }
        // 内部缺省字段, 禁止自行设置
        if (type == "hidden" || (type == "number"
        && (name == "ctime"  ||  name == "mtime"))) {
            continue;
        }

        if (pre) {
            name  = pre + name;
        }
        if (suf) {
            name  = name + suf;
        }
        var group = getTypeItem(wdgt, name);
        if (group.size() == 0) {
            group = getTypeItem(wdgt, type);
        }
        if (group.size() == 0) {
            continue;
        }
        var label = group.find("label span,legend span").first();
        var input = group.find(   "[data-fn],[name]"   ).first();
        label.text(text);
        if (type == "datetime" || type == "time") {
            setItemType(input, type);
        }
        if (input.is( "[name]" )) {
            input.attr("name", name);
        }
        if (input.is( "[data-fn]" )) {
            input.attr("data-fn", name);
        }

        if (input.is("ul")) {
            input.attr("data-required", required);
            input.attr("data-repeated", repeated);
        } else {
            input.prop("required" , ! ! required);
            input.prop("multiple" , ! ! repeated);
        }

        for(var k in field  ) {
            if (/^_/.test(k)) {
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
            if (/^data-/.test(k)) {
                input.attr(k, field[k]);
            } else {
                input.attr("data-"+ k, field[k]);
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
        items   : ".form-group",
        sort    :   function() {
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

        index = index + 1;
        field = $(this).closest(".form-group").clone();
        field.find("[name=-]").attr("name", "-"+index);
        targetz.append(field );
    });

    // 打开设置
    targetz.on("click", ".glyphicon-info-sign", function() {
        field = $(this).closest(".form-group");
        var type  = field.attr ( "data-type" );
        var name  = getTypeName(widgets, type);
        var pane  = getTypePane(context, type);
        modal.find("h4").text(name);
        modal.find( ".simple-set" ) // 基础设置区域
             .empty( ).append(pane);
        modal.find( ".detail-set" ) // 详细设置区域
             .find("tr").not(".hide").remove();

        // 表单设置只显示高级, 其他情况点到基础设置
        var tabs = modal.find(".nav").first( );
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
        $(this).closest(".form-group").next().find("input").each(function() {
            setItemType(this, type);
        });
    });

    // 关联选项
    var datas = {};
    modal.on("pickItem", "[data-ft=_pick]", function() {
        if (arguments[3]) {
            datas[arguments[1]] = arguments[3];
        } else {
            delete datas[arguments[1]];
        }
    });
    modal.on( "change" , "[data-ft=_pick]", function() {
        var  id  = $( this ).find( "input" ).val();
        var  tb  = modal.find(".detail-set tbody");
        var  tp  = tb.find(".hide");
        var data = datas[id];
        if (data) {
            for(var k in data) {
                var v  = data[k];
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
