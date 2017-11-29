
/* global qq */

// 腾讯地图通过 document.write 加载 apifiles
// 这在非文档写入流中并不能正常工作
// 而且其下面的 apifiles 版本总是变
// 为免维护麻烦只好用相同方式预加载
document.write('<script type="text/javascript" src="//map.qq.com/api/js?v=2.exp"></script>');

function withQQMap(func) {
    hsRequires([
//      "//map.qq.com/api/js?v=2.exp",
//      "//open.map.qq.com/apifiles/2/4/73/main.js",
        "static/addons/bootstrap-suggest/suggest.min.js" // 用于地址搜索
    ], func);
}

function fillBillName(form, text) {
    $('<div></div>').appendTo(this).text(text).attr("title", text)
        .css({
            "width"         : "20em",
            "overflow"      : "hidden",
            "text-overflow" : "ellipsis",
            "white-space"   : "nowrap"
        });
}

function in_gerent_data_dining_bill_list(context) {
    // 坐标暂无查询
    context.find("[name='ar.0.lat_lng']")
           .closest(".form-group" )
           .remove ();

    // 避免名字太宽
    context.find("[data-fn=name]" )
           .removeAttr( "data-ft" )
           .css ( "width", "20em" )
           .data( "fl", fillBillName );

    // 缺省时的显示
    context.find("[data-fn=ctime]")
           .removeAttr( "data-ft" )
           .attr( "data-fl", "v ? HsList.prototype._fill__htime(this, v) : '-'");
}

function in_gerent_data_dining_bill_info(context) {
    // 添加套餐选择列表
    var id      = H$("#id", context);
    var loadUrl = 'gerent/data/dining/item_view_list.html?unit=bill&bill_id='+id;
    var typeInp = context.find("[data-fn=total]");
    var typeBox = typeInp.closest(".form-group" );
    var itemBox = $(
          '<div class="row form-group">'
        + '<label class="col-sm-3 control-label form-control-static text-right">菜单</label>'
        + '<div class="col-sm-6" data-load="'+loadUrl+'">'
        + '</div>'
        + '</div>'
    );
    itemBox.insertBefore(typeBox).hsReady();

    //** 地图展示 **/

    withQQMap(function() {
        var provInp = context.find("[data-fn=prov]");
        var poisInp = context.find("[name=lat_lng]");
        var areaBox = $(
              '<div class="row form-group">'
            + '<label class="col-sm-3 control-label form-control-static text-right">地图</label>'
            + '<div class="col-sm-6">'
            + '<div class="map" style="height: 300px;"></div>'
            + '</div>'
            + '</div>'
        );
        areaBox.insertBefore(provInp.closest(".form-group"));
        context.find("[data-fn=prov],[data-fn=city],[data-fn=dist]")
               .closest (".form-group")
               .addClass( "invisible" );

        var map = new qq.maps.Map(
        areaBox.find(".map")[0], {
            zoom: 14
        });
        var geo = new qq.maps.Geocoder();
        var mak ;

        function setPos(pos) {
            if (mak) {
                mak.setMap ( null );
            }
            if (pos) {
                map.setCenter (pos);
                geo.getAddress(pos);
                mak = new qq.maps.Marker({
                    position : pos ,
                    map      : map
                });
            }
        }

        poisInp.on("change", function() {
            var pos = $(this).val();
            if (pos) {
                pos = pos.split(",", 2);
                pos[0] = parseFloat(pos[0]);
                pos[1] = parseFloat(pos[1]);
                pos = new qq.maps.LatLng(pos[0], pos[1]);
                setPos(pos);
            }
        }).change();
    });
}

function in_gerent_data_dining_bill_form(context, formobj) {
    // 添加套餐选择列表
    var id      = H$("#id", context);
    var loadUrl = 'gerent/data/dining/item_edit_list.html?unit=bill&bill_id='+id;
    var typeInp = context.find   ("[name=total]");
    var typeBox = typeInp.closest(".form-group" );
    var itemBox = $(
          '<div class="row form-group">'
        + '<label class="col-sm-3 control-label form-control-static text-right">菜单</label>'
        + '<div class="col-sm-6" data-load="'+loadUrl+'">'
        + '</div>'
        + '</div>'
    );
    context.find("[name=name]").addClass("bill-name"); // 标识以便自动填充
    itemBox.insertBefore(typeBox).hsReady();

    //** 地图选取 **/

    withQQMap(function() {
        var addrInp = context.find("[name=addr]");
        var provInp = context.find("[name=prov]");
        var cityInp = context.find("[name=city]");
        var distInp = context.find("[name=dist]");
        var poisInp = context.find("[name=lat_lng]");
        var areaBox = $(
              '<div class="row form-group">'
            + '<label class="col-sm-3 control-label form-control-static text-right">地图</label>'
            + '<div class="col-sm-6">'
            + '<div class="map" style="height:300px;" data-fn="map"></div>'
            + '</div>'
            + '<div class="col-sm-3 help-block form-control-static"></div>'
            + '</div>'
        );
        areaBox.insertBefore(provInp.closest(".form-group"));
        context.find("[name=prov],[name=city],[name=dist],[name=name]")
               .closest (".form-group")
               .addClass( "invisible" );

        var map = new qq.maps.Map(
        areaBox.find(".map")[0], {
            zoom: 14
        });
        var geo = new qq.maps.Geocoder();
        var sea = new qq.maps.SearchService();
        var mak ;

        function setPos(pos) {
            if (mak) {
                mak.setMap ( null );
            }
            if (pos) {
                map.setCenter (pos);
                geo.getAddress(pos);
                mak = new qq.maps.Marker({
                    position : pos ,
                    map      : map
                });
            } else {
                // 没有地址则清空
                provInp.val(""); cityInp.val("");
                distInp.val(""); poisInp.val("");
            }
        }

        // 点选地址
        qq.maps.event.addListener(map, 'click', function(evt) {
            addrInp.val ( "" ); // 点选总是重写地址输入框
            setPos(evt.latLng);
        });

        // 地图回调
        geo.setComplete(function(rst) {
            rst = rst.detail;
            if (! addrInp.val()) {
                addrInp.val(rst.address);
            }
            provInp.val(rst.addressComponents.province);
            cityInp.val(rst.addressComponents.city    );
            distInp.val(rst.addressComponents.district);
            poisInp.val(rst.location.lat+","+rst.location.lng);
            formobj.seterror( "map" );
        });
        sea.setComplete(function(rst) {
            rst = rst.detail;
            if (rst.pois && rst.pois.length) {
                // 将结果转交给建议插件
                suggestData = {value : rst.pois};
                suggestOpts.data  =  suggestData;
                suggestFunc(addrInp, suggestData, suggestOpts);
            }
        });

        // 表单联动
        poisInp.on("change", function() {
            var pos = $(this).val();
            if (pos) {
                pos = pos.split(",", 2);
                pos[0] = parseFloat(pos[0]);
                pos[1] = parseFloat(pos[1]);
                pos = new qq.maps.LatLng(pos[0], pos[1]);
                setPos(pos);
            }
        }).change();

        var suggestFunc;
        var suggestOpts;
        var suggestData;

        // 地址搜索
        addrInp.wrap('<div class="input-group"></div>').parent().append(
                $('<div>'
                + '<a  data-toggle="dropdown"></a>'
                + '<ul class="dropdown-menu"></ul>'
                + '</div>')
            );
        addrInp.bsSuggest({
            showBtn         : false,
            idField         :  "id",
            keyField        :  "name" ,
            searchFields    : ["name"],
            effectiveFields : ["name"],
            fnGetData       : function(keyword, $input$, display, options) {
                if (keyword) {
                    suggestFunc = display;
                    suggestOpts = options;
                    sea.search( keyword );
                } else {
                    setPos(null);
                }
            }
        }).on("onSetSelectValue", function(evt, addr, data) {
            setPos (data.latLng);
        });
    });

    // 如果有填写地址, 则位置是必选项
    formobj.rules["[data-fn=map]"] = function(val, inp) {
        if (!context.find("[name=lat_lng]").val()) {
            if (context.find("[name=addr]").val()) {
                return formobj.geterror(inp, "请在地图点选一个位置");
            }
        }
        return  true;
    };
}
