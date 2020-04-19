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
    var context = opts.context || $( );
    var formobj = opts.formobj || null;

    var provInp = opts.provinp || context.find("[name=prov],[data-fn=prov]");
    var cityInp = opts.cityinp || context.find("[name=city],[data-fn=city]");
    var distInp = opts.distinp || context.find("[name=dist],[data-fn=dist]");
    var addrInp = opts.addrinp || context.find("[name=addr],[data-fn=addr]");
    var poisInp = opts.lat_lng || context.find("[name=lat_lng]");
    var mapsInp = opts.map_inp || context.find(".map-find");
    var mapsBox = opts.map_box || context.find(".map-area");

    // 附加的隐藏地址以外字段的快捷操作
    var readonly  = opts.readonly;
    if (readonly == 1 || readonly == -1) {
        readonly  = 0 <  readonly;
        poisInp.attr("readonly", true);
        provInp.attr("readonly", true);
        cityInp.attr("readonly", true);
        distInp.attr("readonly", true);
        addrInp.attr("readonly", true);
    } else
    if (readonly == 2 || readonly == -2) {
        readonly  = 0 <  readonly;
        poisInp.closest(".form-group").addClass("invisible");
        provInp.closest(".form-group").addClass("invisible");
        cityInp.closest(".form-group").addClass("invisible");
        distInp.closest(".form-group").addClass("invisible");
        addrInp.closest(".form-group").addClass("invisible");
    }

    // 地图组件区域如缺失则自动添加一个
    if (mapsBox.size() == 0) {
        mapsBox = $ (
            '<div class="row form-group">'
          + '<label class="col-xs-3 col-md-2 control-label form-control-static text-right">地图</label>'
          + '<div class="col-xs-9 col-md-8"><div class="map-area" style=" height: 250px; "></div></div>'
          + '</div>'
        ).insertBefore(poisInp.closest(".form-group"));
        mapsBox = mapsBox.find(".map-area");
    }
    if (mapsInp.size() == 0 && ! readonly ) {
        mapsInp = $ (
            '<input type="search" class="map-find form-control" placeholder="请输入城市或地点进行搜索"/>'
        ).insertBefore(mapsBox);
    }

    // 规避异异步情况下方注册时值已设置
    var changed = false;
    context.on("loadOver", function( ) {
        changed = true ;
    });

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
                distInp.val(""); addrInp.val("");
                poisInp.val("");
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
            mapsInp.wrap('<div class="input-group"></div>').parent().append(
                       $('<div>'
                       + '<a  data-toggle="dropdown"></a>'
                       + '<ul class="dropdown-menu"></ul>'
                       + '</div>')
            );
            mapsInp.bsSuggest({
                showBtn         : false,
                idField         :  "id",
                keyField        :  "name" ,
                searchFields    : ["name"],
                effectiveFields : ["name"],
                fnGetData       : function(keyword, inp, display, options) {
                    if (keyword) {
                        suggFunc = display;
                        suggOpts = options;
                        sea.search(keyword);
                    } else {
                        setPos(null);
                    }
                }
            }).on("keydown", function(evt) {
                if (evt.which == 13) {
                    mapsInp.parent()
                           .find( "tr" ).eq( 0 )
                           .trigger("mousedown");
                }
            }).on("onSetSelectValue", function ( evt , add , dat) {
                setPos(dat ? dat.latLng : null );
            });

            // 点选地址
            qq.maps.event.addListener(map, 'click', function(evt) {
                setPos(evt ? evt.latLng : null );
            });

            // 推荐回调
            sea.setComplete(function(rst) {
                rst = rst.detail;
                if (rst.pois && rst.pois.length) {
                    // 将结果转交给建议插件
                    suggData = {value: rst.pois};
                    suggOpts["data"] = suggData ;
                    suggFunc(mapsInp , suggData , suggOpts);
                }
            });

            // 地图回调
            geo.setComplete(function(rst) {
                rst = rst.detail;
                poisInp.val(rst.location.lat+","+rst.location.lng);
                provInp.val(rst.addressComponents.province);
                cityInp.val(rst.addressComponents.city    );
                distInp.val(rst.addressComponents.district);
                addrInp.val(rst.address ) ;
                sea.setLocation(rst.addressComponents.city);
                mapsBox.trigger("mapsBack", [rst]);
                if (formobj && !readonly) {
                    formobj . seterror ( mapsBox );
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
