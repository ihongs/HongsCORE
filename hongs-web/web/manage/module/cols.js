
function getTypeName(widgets, type) {
    if (type == "select") {
        return "选择";
    }
    return widgets.find( "[data-type='"+type+"']"   ).find("span").first().text();
}
function getTypeItem(widgets, type) {
    return widgets.find( "[data-type='"+type+"']"   ).clone();
}
function getTypePane(context, type) {
    return context.find(".widget-pane-"+type).first().clone();
}

/**
 * 载入字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function loadConf(modal, field) {
    modal.find("input,textarea").each(function() {
        var name = $(this).attr("name");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
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
            if (attr === "text") {
                $(this).val(field.find(name).text());
            } else {
                $(this).val(field.find(name).attr(attr));
            }
        } else {
            $(this).val(field.find(name).text());
        }
    });
}

/**
 * 设置字段配置
 * @param {jQuery} modal
 * @param {jQuery} field
 */
function saveConf(modal, field) {
    modal.find("input,textarea").each(function() {
        var name = $(this).attr("name");
        var attr ;
        var pos  = name.indexOf("|");
        if (pos !== -1) {
            attr = name.substring(pos + 1);
            name = name.substring(0 , pos);
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
            if (attr === "text") {
                field.find(name).text($(this).val());
            } else {
                field.find(name).attr(attr, $(this).val());
            }
        } else {
            field.find(name).text($(this).val());
        }
    });
}

/**
 * 从表单获取字段
 * @param {Array} fields
 * @param {jQuery} area
 */
function gainFlds(fields, area) {
    area.find(".form-group").each(function() {
        var label = $(this).find("label span:first");
        var input = $(this).find("input,select,textarea");
        var disp  = label.text();
        var name  = input.attr("name");
        var type  = input.attr("type") || input.prop("tagName").toLowerCase();
        var required = input.prop("required") ? "true" : "";
        var repeated = input.prop("multiple") ? "true" : "";
        var params   = {};
        var a = input.get(0).attributes;
        for(var i = 0; i < a.length; i ++) {
            var k = a[i].nodeName ;
            var v = a[i].nodeValue;
            if (v !== ""
            &&  k.substr(0,5) === "data-") {
                params[k.substring(5)] = v;
            }
        }
        if (name.substr(0, 1) === /**/"-") {
            name = "";
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
            params["datalist"] = JSON.stringify(datalist);
            params["selected"] = JSON.stringify(selected);
        }
        fields.push($.extend({
            "__disp__": disp,
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
        var disp  = field["__disp__"];
        var name  = field['__name__'];
        var type  = field["__type__"];
        var required = field["__required__"];
        var repeated = field["__repeated__"];
        if (pre) {
            name  = pre + name;
        }
        if (suf) {
            name  = name + suf;
        }
        var group = getTypeItem(wdgt, type);
        var label = group.find("label span:first");
        var input = group.find("input,select,textarea");
        label.text(disp);
        input.attr("name", name);
        input.prop("required", !! required);
        input.prop("multiple", !! repeated);
        for(var k in field  ) {
            if (/^_/.test(k)) {
                continue;
            }
            if (k === "selected") {
                continue;
            }
            if (k === "datalist") {
                input.empty();
                var datalist = JSON.parse(field["datalist"]) || [];
                var selected = JSON.parse(field["selected"]) || [];
                for(var i = 0; i < datalist.length; i ++ ) {
                    var a = datalist[i];
                    var o = $("<option></option>");
                    o.val(a[0]).text(a[1]).appendTo(input);
                }
                input.val(selected);
                continue;
            }
            input.attr("data-"+k, field[k]);
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

    // 添加字段
    widgets.on("click", ".glyphicon-plus-sign", function() {
        index = index + 1;
        field = $(this).closest(".form-group").clone();
        field.find("[name=-]").attr("name", "-"+index);
        targetz.append(field );
    });
    // 删除字段
    targetz.on("click", ".glyphicon-remove-sign", function() {
        $(this).closest(".form-group").remove();
    });

    // 打开设置
    targetz.on("click", ".glyphicon-info-sign", function() {
        field = $(this).closest(".form-group");
        var type  = field.attr ( "data-type" );
        var name  = getTypeName(widgets, type);
        var pane  = getTypePane(context, type);
        modal.find("h4").text(name);
        modal.find( ".modal-body" )
             .empty( ).append(pane);

        loadConf(modal, field);
        modal.modal( "show"  );
    });
    // 完成设置
    modal.find("form").submit(function() {
        modal.modal( "hide"  );
        saveConf(modal, field);
    });
};

// 关联数据
$.ajax({
    "url": ""
});
