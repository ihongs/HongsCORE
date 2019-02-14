/* global qq */

// 位置可能需要用到几个字段, 这不利于封装, 此代码为范例
// 腾讯地图通过 document.write 加载 apifiles
// 这在非文档写入流中并不能正常工作
// 而且其下面的 apifiles 版本经常变
// 为免维护麻烦只好用相同方式预加载
document.write('<script type="text/javascript" src="//map.qq.com/api/js?v=2.exp&key='+(HsCONF['qq.map.key']||'')+'"></script>');

function forMapsInput(func) {
    hsRequires([
//      "//map.qq.com/api/js?v=2.exp",
//      "//open.map.qq.com/apifiles/2/4/73/main.js",
        "static/addons/bootstrap-suggest/suggest.min.js" // 搜索建议
    ], func);
}

function setMapsInput(opts, func) {
    var readonly = !! opts.readonly;
    var context = opts.context || $( );
    var formobj = opts.formobj || null;

    var addrInp = opts.addrinp || context.find("[name=addr],[data-fn=addr]");
    var provInp = opts.provinp || context.find("[name=prov],[data-fn=prov]");
    var cityInp = opts.cityinp || context.find("[name=city],[data-fn=city]");
    var distInp = opts.distinp || context.find("[name=dist],[data-fn=dist]");
    var poisInp = opts.lat_lng || context.find("[name=lat_lng]");
    var areaBox = $(
          '<div class="row form-group">'
        + '<label class="col-sm-3 control-label form-control-static text-right">地图</label>'
        + '<div class="col-sm-6">'
        + '<div class="map" style="width: 100%; height: 300px;"></div>'
        + '</div>'
        + '<div class="col-sm-3 help-block form-control-static"></div>'
        + '</div>'
    );
    var mapsBox = areaBox.find(".map");
    var changed = false;

    // 规避异异步情况下方注册时值已设置
    context.on("loadOver", function( ) {
        changed = true ;
    });

    areaBox.insertAfter( addrInp.closest ( ".form-group" ) );
    if (!readonly) {
        provInp.closest(".form-group").addClass("invisible");
        cityInp.closest(".form-group").addClass("invisible");
        distInp.closest(".form-group").addClass("invisible");
        poisInp.closest(".form-group").addClass("invisible");
    }

    forMapsInput(function( ) {
        var map = new qq.maps.Map(
            mapsBox[0], {zoom: 14}
        );
        var geo = new qq.maps.Geocoder( );
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
                    draggable: ! readonly
                });
            } else {
                // 没有地址则清空
                provInp.val(""); cityInp.val("");
                distInp.val(""); poisInp.val("");
            }
        }

        function savPos(pos) {
            if (pos) {
                pos = pos.split(",",2);
                pos[0] = parseFloat(pos[0]);
                pos[1] = parseFloat(pos[1]);
                setPos(new qq.maps.LatLng(pos[0], pos[1]));
            } else
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition (
            function (rst) {
                pos =[rst.coords. latitude ,
                      rst.coords.longitude];
                setPos(new qq.maps.LatLng(pos[0], pos[1]));
            },
            function (err) {
                cit.searchLocalCity( );
            }
                );
            } else {
                cit.searchLocalCity( );
            }
        }

        if (! readonly) {
            var suggFunc;
            var suggOpts;
            var suggData;

            // 搜索地址
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
                        suggFunc = display;
                        suggOpts = options;
                        sea.search(keyword);
                        addrInp.data("changed", true);
                    } else {
                        addrInp.removeData("changed");
                        setPos(null);
                    }
                }
            }).on("onSetSelectValue", function ( evt , add , dat) {
                addrInp.removeData("changed");
                setPos( dat.latLng );
            });

            // 点选地址
            qq.maps.event.addListener(map, 'click', function(evt) {
                if (! addrInp.data("changed")) {
                    addrInp.val ("");
                }
                setPos( evt.latLng );
            });

            // 推荐地址
            sea.setComplete(function(rst) {
                rst = rst.detail;
                if (rst.pois && rst.pois.length) {
                    // 将结果转交给建议插件
                    suggData = {value: rst.pois};
                    suggOpts["data"] = suggData ;
                    suggFunc(addrInp , suggData , suggOpts);
                }
            });

            // 地图回调
            geo.setComplete(function(rst) {
                rst = rst.detail;
                provInp.val(rst.addressComponents.province);
                cityInp.val(rst.addressComponents.city    );
                distInp.val(rst.addressComponents.district);
                poisInp.val(rst.location.lat+","+rst.location.lng);
                if (addrInp.val( ) == '') {
                    addrInp.val( rst.address);
                }
                if (formobj && !readonly) {
                    formobj.seterror(mapsBox);
                }
            });
        }

        // 当前位置
        cit.setComplete(function(rst) {
            map.setCenter (rst.detail.latLng);
        });

        context.on("loadOver", function() {
            savPos(poisInp.val());
        });
        if (changed) {
            savPos(poisInp.val());
        }

        if (func) {
            func( );
        }
    });
}

function in_maps_info(context, formobj) {
    setMapsInput({
        context : context,
        formobj : formobj,
        readonly: true
    });
}

function in_maps_form(context, formobj) {
    setMapsInput({
        context : context,
        formobj : formobj,
        readonly: false
    });
}
