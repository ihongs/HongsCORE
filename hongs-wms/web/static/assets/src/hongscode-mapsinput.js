/* global qq */

// 位置可能需要用到几个字段, 这不利于封装, 此代码为范例
// 腾讯地图通过 document.write 加载 apifiles
// 这在非文档写入流中并不能正常工作
// 而且其下面的 apifiles 版本经常变
// 为免维护麻烦只好用相同方式预加载
document.write('<script type="text/javascript" src="//map.qq.com/api/js?v=2.exp"></script>');

function forMapsInput(func) {
    hsRequires([
//      "//map.qq.com/api/js?v=2.exp",
//      "//open.map.qq.com/apifiles/2/4/73/main.js",
        "static/addons/bootstrap-suggest/suggest.min.js" // 用于地址搜索
    ], func);
}

function in_maps_info(context) {
    withQQMap(function() {
        var addrInp = context.find("[data-fn=addr]");
        var provInp = context.find("[data-fn=prov]");
        var cityInp = context.find("[data-fn=city]");
        var distInp = context.find("[data-fn=dist]");
        var poisInp = context.find("[data-fn=lat_lng]");
        var areaBox = $(
              '<div class="row form-group">'
            + '<label class="col-sm-3 control-label form-control-static text-right">地图</label>'
            + '<div class="col-sm-6">'
            + '<div class="map" style="height: 300px;"></div>'
            + '</div>'
            + '</div>'
        );
        areaBox.insertAfter(addrInp.closest(".form-group"));
        provInp.closest(".form-group").addClass("invisble");
        cityInp.closest(".form-group").addClass("invisble");
        distInp.closest(".form-group").addClass("invisble");
        poisInp.closest(".form-group").addClass("invisble");

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

function in_maps_form(context, formobj) {
    forMapsInput(function() {
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
        areaBox.insertAfter(addrInp.closest(".form-group"));
        provInp.closest(".form-group").addClass("invisble");
        cityInp.closest(".form-group").addClass("invisble");
        distInp.closest(".form-group").addClass("invisble");
        poisInp.closest(".form-group").addClass("invisble");

        var map = new qq.maps.Map(
        areaBox.find(".map")[0], {
            zoom: 14
        });
        var geo = new qq.maps.Geocoder();
        var cit = new qq.maps.CityService();
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
                    map      : map ,
                    position : pos ,
                    draggable: true
                });
            } else {
                // 没有地址则清空
                provInp.val(""); cityInp.val("");
                distInp.val(""); poisInp.val("");
            }
        }

        function getPos(fun) {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
            function (rst) {
                fun  (rst.coords.latitude, rst.coords.longitude);
            },
            function (err) {
                throw new Error("Can not get current position!");
            }
            );
            } else {
                throw new Error("Can not get current position.");
            }
        }

        // 点选地址
        qq.maps.event.addListener(map, 'click', function(evt) {
            if(!addrInp.data("changed")) {
                addrInp.val ("");
            }
            setPos( evt.latLng );
        });

        // 当前位置
        cit.setComplete(function(rst) {
            map.setCenter(rst.detail.latLng); // 转入本市
            getPos(function(lat, lng) {
                if(! addrInp.val( ) ) {
                    setPos(new qq.maps.LatLng(lat, lng));
                }
            });
        });
        cit.searchLocalCity();

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
                     sea.search ( keyword );
                    addrInp.data("changed", true);
                } else {
                    addrInp.removeData("changed");
                    setPos(null);
                }
            }
        }).on("onSetSelectValue", function(evt, addr, data) {
            addrInp.removeData("changed");
            setPos (data.latLng);
        });
    });
}
