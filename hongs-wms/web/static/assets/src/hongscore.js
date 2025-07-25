
/* global Symbol, Element, FormData, File, JSON, eval, encodeURIComponent, decodeURIComponent, jQuery, HsAUTH,HsCONF,HsLANG,HsREQS,HsDEPS */

if (!window.HsAUTH) window.HsAUTH = {};
if (!window.HsCONF) window.HsCONF = {};
if (!window.HsLANG) window.HsLANG = {};
if (!window.HsREQS) window.HsREQS = {};
if (!window.HsDEPS) window.HsDEPS = {};

/**
 * 快捷获取
 * 说明(首参数以下列字符开头的意义):
 * . 获取配置
 * : 获取语言
 * ! 检查权限
 * / 补全路径
 * @ 快捷获取模块对象
 * # 快捷获取容器对象
 * & ? 快捷获取参数值, 第二个参数可指定容器, 未指定默认为 URL
 * % 获取设置本地存储, 第二个参数存在为设置, 第二个参数为 null 则删除
 * $ 获取设置会话存储, 第二个参数存在为设置, 第二个参数为 null 则删除
 * @return {Mixed} 根据开头标识返回不同类型的数据
 */
function H$() {
    var a = arguments[0];
    var b = a.charAt (0);
    arguments[0] = a.substring(1);
    switch (b) {
    case '.': return hsGetConf.apply(this, arguments);
    case ':': return hsGetLang.apply(this, arguments);
    case '!': return hsGetAuth.apply(this, arguments);
    case '/':
            arguments[0] = hsFixUri.apply(this, arguments);
        if (arguments.length > 1) {
            arguments[0] = hsSetPms.apply(this, arguments);
        }
        return arguments[0];
    case '@':
        if (arguments.length === 1) {
          return jQuery("." + arguments[0]).data(arguments[0]);
        } else {
          return jQuery(arguments[1]).closest("." + arguments[0]).data(arguments[0]);
        }
    case '#':
        if (arguments.length === 1) {
          return jQuery("#" + arguments[0]).removeAttr( "id" );
        } else {
          return jQuery(arguments[1]).closest("#" + arguments[0]).removeAttr( "id" );
        }
    case '&':
    case '?':
        // 因 & 为实体标识, 用 ? 规避 &amp;
        if (arguments.length === 1) {
          return hsGetParam(location.href, arguments[0]);
        } else
        if (typeof(arguments[1]) === "string") {
          return hsGetParam(arguments[1] , arguments[0]);
        } else {
          arguments[1] = hsSerialObj (arguments[1]);
          return hsGetSeria(arguments[1] , arguments[0]);
        }
    case '$':
    case '%':
        var c = b === '%' ? window.localStorage : window.sessionStorage;
        if (c === undefined) {
          throw "H$: Does not support '" + (b==='%'?'local':'session') + "Storage'" ;
        }
        if (arguments.length === 1) {
        return c.getItem(arguments[0]);
        } else
        if (arguments[1] === null ) {
            c.removeItem(arguments[0]);
        } else
        {
            c.setItem(arguments[0], arguments[1]);
        }
        return;
    default:
        throw "H$: Unrecognized identified '" + b + "'" ;
    }
}

/**
 * 依赖加载(顺序加载)
 * @param {String|Array} url 依赖JS
 * @param {Function} fun 就绪后执行
 */
function hsRequired(url, fun ) {
    if (! jQuery.isArray(url)) {
        url = [url];
    }

    var uri = [ url[ 0 ] ];
    var urs = url.slice(1);

    hsRequires( uri, function( ) {
        if (urs.length) {
            hsRequired(urs, fun);
        } else if (fun) {
            fun   (   );
        }
    });
}

/**
 * 依赖加载(异步加载)
 * @param {String|Array} url 依赖JS
 * @param {Function} fun 就绪后执行
 */
function hsRequires(url, fun ) {
    if (! jQuery.isArray(url)) {
        url = [url];
    }

    var urs;
    var k = 0;
    var l = url.length;
    var h = document.head
         || jQuery ("head") [0]
         || document.documentElement;

    function toDepUrs(w) {
        var m = HsCONF["deps."+w];
        if (m) {
            if (jQuery.isArray(m)) {
                return m;
            }
            w = m ;
        }
        return [w];
    }

    for(var i = 0 ; i < url.length ; i ++) {
        var w = url[i];
          urs = toDepUrs(w);
            l = l - 1 + urs.length ;
    for(var j = 0 ; j < urs.length ; j ++) {
        var v = urs[j];
        var u = hsFixUri(v);

        if (HsDEPS [u]) {
            HsDEPS [u] += 1;
            if (fun && ++ k == l) {
                fun( );
            }
            continue  ;
        }

        if (jQuery('link[href="'+v+'"],script[src="'+v+'"],'
                  +'link[href="'+u+'"],script[src="'+u+'"]')
                  . size( )) {
            HsDEPS [u] += 1;
            if (fun && ++ k == l) {
                fun( );
            }
            continue  ;
        }

        // 在 head 加 link 或 script 标签
        // 监听其加载事件, 全部完成时回调
        var n = document.createElement(/\.css$/.test(u) ? "link" : "script");
        n.onload = n.onreadystatechange = ( function(n) {
            return function( ) {
                if ( ! n.readyState
                ||  n.readyState == "loaded"
                ||  n.readyState == "complete") {
                    n.onload = n.onreadystatechange = null;
                    if (fun && l == ++ k) {
                        fun( );
                    }
                }
            } ;
        }) (n);
        if (n.tagName == "SCRIPT") {
            n.defer = false;
            n.async = true ;
            n.type  = "text/javascript";
            n.src   = u ;
        } else {
            n.rel   = "stylesheet";
            n.type  = "text/css"  ;
            n.href  = u ;
        }
        h.appendChild(n);
        HsDEPS [u]  = 1 ;
    }}
}

/**
 * 响应数据
 * @param {Object|String|XHR} rst JSON对象/JSON文本/错误消息
 * @param {Number} qut 1不显示消息, 2不执行跳转, 3彻底的静默
 * @return {Object}
 */
function hsResponse(rst, qut) {
    if (typeof (rst.responseText) !== "undefined") {
        rst  =  rst.responseText;
    }
    if (typeof (rst) === "string") {
        rst  =  jQuery.trim( rst );
        if (rst.charAt(0) === '{') {
            if (typeof(JSON) !== "undefined") {
                rst  = JSON.parse( rst );
            }
            else {
                rst  = eval('('+rst+')');
            }
        } else
        if (rst.charAt(0) === '<') {
            /*
             * 某些时候服务器可能出错, 返回错误消息的页面
             * 需清理其中的超文本代码, 以供输出简洁的消息
             */
            var ern, err, msg, mat;
            mat = /<!--ERN:\s*(.+?)\s*-->/.exec(rst);
            if (mat) ern = mat [1];
            mat = /<!--ERR:\s*(.+?)\s*-->/.exec(rst);
            if (mat) err = mat [1];
            mat = /<!--MSG:\s*(.+?)\s*-->/.exec(rst);
            if (mat) msg = mat [1];
            else     msg = rst
                .replace(/<iframe[\s\S]*?>[\s\S]*?<\/iframe>/gi, "") // 清除内嵌框架
                .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, "") // 清除脚本代码
                .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, "")   // 清除样式代码
                .replace(/<!--[\s\S]*?-->/g, "") // 清除注释
                .replace(/<[^>]*?>/g, " ")       // 清除标签
                .replace(/&[^&;]*;/g, " ")       // 清除符号
                .replace(/[\f\t\v]/g, " ")       // 清理空白
                .replace( /[ ]{2,}/g, " ")       // 清理空白
                .replace( /^\s*$/gm , "" );      // 清除空行
            rst = {
                "msg": msg,
                "err": err,
                "ern": ern,
                "ok" : false
            };
        } else {
            rst = {
                "msg": rst,
                "err": "" ,
                "ern": "" ,
                "ok" : false
            };
        }
    }
    if (typeof (rst) === "object") {
        if (rst.ok === 0 || rst.ok === "0") {
            rst.ok = false;
        } else
        if (rst.ok === 1 || rst.ok === "1") {
            rst.ok = true ;
        } else
        if (typeof(rst.ok ) === "undefined") {
            rst.ok = true ;
        }
        if (typeof(rst.ern) === "undefined") {
            rst.ern =  "" ;
        }
        if (typeof(rst.err) === "undefined") {
            rst.err =  "" ;
        }
        if (typeof(rst.msg) === "undefined") {
            rst.msg =  "" ;
        }

        // 服务接口要求跳转(常为未登录或无权限)
        if (! qut || ! (2 == (2 & qut))) {
            if (rst.ern && /^Er[34]\d+/i.test(rst.ern)) {
                var url;
                if (rst.err && /^Goto /i.test(rst.err)) {
                    url = jQuery.trim(rst.err.substring(5));
                } else {
                    url = hsGetConf(rst.ern + ".redirect" );
                }
                if (url !== null && url !== undefined ) {
                    if (! window.HsGone) {
                          window.HsGone = true;
                        if ( rst.msg ) {
                            alert  ( rst.msg );
                        }
                        if (url && url != '#') {
                            location.assign( hsFixUri(url));
                        } else {
                            location.reload( );
                        }
                    }
                    throw new Error( rst.err );
                }
            }
        }

        // 成功失败消息处理(失败则总是发出警告)
        if (! qut || ! (1 == (1 & qut))) {
            if (rst.ok ) {
                if (rst.msg) {
                    jQuery.hsNote(rst.msg, "success");
                }
            } else {
                if (rst.msg) {
                    jQuery.hsWarn(rst.msg, "warning");
                } else {
                    jQuery.hsWarn(hsGetLang('error.undef'), "danger");
                }
            }
        }

        // 针对特定数据结构
        if (typeof(rst['data']) !== "undefined") {
            jQuery.extend(rst , rst['data']);
            delete rst['data'];
        }
    }
    return rst;
}

/**
 * 待序列化对象, 保留 Array,Object,HsSerialDic,HsSerialDat 类型
 * @param {Array|String|Object|Element|FormData} obj
 * @return {Array|HsSerialDic|HsSerialDat}
 */
function hsSerialObj(obj) {
    var typ = jQuery.type(obj);
    if (typ == "array" || typ == "object") {
        if (obj instanceof jQuery
        ||  obj instanceof Element ) {
            typ = "jquery";
        } else
        if (       window .FormData
        &&  obj instanceof FormData) {
            typ = "fordat";
        } else
        if (obj instanceof HsSerialDat) {
            typ = "serdat";
        } else
        if (obj instanceof HsSerialDic) {
            typ = "serdic";
        }
    }
    switch (typ) {
        case "undefined":
        case "null"  :
        case "array" :
        case "object":
        case "serdic":
        case "serdat":
            break;
        case "fordat":
        case "string":
        case "jquery":
            obj = hsSerialArr(obj);
            break;
        default:
            throw new Error("hsSerialObj: Unsupported type "+typ);
    }
    return  obj;
}

/**
 * 序列化为数组, 供发往服务器(类似 jQuery.fn.serializeArray)
 * @param {Array|String|Object|Element|FormData} obj
 * @return {Array}
 */
function hsSerialArr(obj) {
    var arr = [];
    var typ = jQuery.type(obj);
    if (typ == "array" || typ == "object") {
        if (obj instanceof jQuery
        ||  obj instanceof Element ) {
            typ = "jquery";
        } else
        if (       window .FormData
        &&  obj instanceof FormData) {
            typ = "fordat";
        } else
        if (obj instanceof HsSerialDat) {
            typ = "serdat";
        } else
        if (obj instanceof HsSerialDic) {
            typ = "serdic";
        }
    }
    switch (typ) {
        case "undefined":
        case "null"  :
            break;
        case "array" :
            arr = obj;
            break;
        case "fordat":
            obj = obj.entries();
            while  (  true  ) {
                var vxl  = obj.next();
                if (vxl.done)  break ;
                    vxl  = vxl.value ;
                arr.push({name:vxl[0], value:vxl[1]});
            }
            break;
        case "serdic":
            for(var key in obj) {
                var vxl  = obj[ key ];
                if (jQuery.isArray(vxl) ) {
                    if (!/(\[\]|\.\.|\.$)/.test(key)) {
                        key = key+'.';
                    }
                for(var i = 0 ; i < vxl.length; i ++) {
                    var vol = vxl [i];
                    arr.push({name: key, value: vol});
                }} else {
                    arr.push({name: key, value: vxl});
                }
            }
            break;
        case "serdat":
            var add = function (arr, vxl, key, k, r ) {
                if (jQuery.isPlainObject(vxl)) {
                    if (r !== undefined ) {
                        key = key + "." + k;
                    }
                    for(k in vxl) {
                        add (arr, vxl[k], key, k, 1 );
                    }
                }
                else if (jQuery.isArray (vxl)) {
                    if (r !== undefined ) {
                        key = key + "." + k;
                    }
                    for(k = 0; k < vxl.length; k ++ ) {
                        add (arr, vxl[k], key, k, 0 );
                    }
                }
                else {
                    if (r !== undefined ) {
                        k   = r ? k : '';
                        key = key + '.' + k;
                    }
                    arr.push({name: key, value: vxl});
                }
            };
            for(var key in obj) {
                var vxl  = obj[key];
                add(arr  , vxl,key);
            }
            break;
        case "object":
            var add = function (arr, vxl, key, k, r ) {
                if (jQuery.isPlainObject(vxl)) {
                    if (r !== undefined ) {
                        key = key + "[" + k + ']';
                    }
                    for(k in vxl) {
                        add (arr, vxl[k], key, k, 1 );
                    }
                }
                else if (jQuery.isArray (vxl)) {
                    if (r !== undefined ) {
                        key = key + "[" + k + ']';
                    }
                    for(k = 0; k < vxl.length; k ++ ) {
                        add (arr, vxl[k], key, k, 0 );
                    }
                }
                else {
                    if (r !== undefined) {
                        k   = r ? k : '';
                        key = key + '[' + k + ']';
                    }
                    arr.push({name: key, value: vxl});
                }
            };
            for(var key in obj) {
                var vxl  = obj[key];
                add(arr  , vxl,key);
            }
            break;
        case "string":
            var p, i, j, ar0, ar1, ar2, key, vxl;
            ar0 = [];
            p = obj.indexOf('#');
            if (p > -1) {
                ar1 = obj.substring(p + 1);
                obj = obj.substring(0 , p);
                ar0.unshift(ar1);
            }
            p = obj.indexOf('?');
            if (p > -1) {
                ar1 = obj.substring(p + 1);
                obj = obj.substring(0 , p);
                ar0.unshift(ar1);
            }
            for(i = 0; i < ar0.length; i ++) {
                ar1 = ar0[i].split('&'   );
            for(j = 0; j < ar1.length; j ++) {
                ar2 = ar1[j].split('=', 2);
                if (ar2.length > 1) {
                    key = decodeURIComponent (ar2[0]);
                    vxl = decodeURIComponent (ar2[1]);
                    arr.push({name: key, value: vxl});
                } else if (ar2 [0]) {
                    vxl = decodeURIComponent (ar2[0]);
                    arr.push({name: ".", value: vxl});
                    // 匿名参数, 规则同 CombatHelper.getOpts
                }
            }}
            break;
        case "jquery":
            obj = jQuery (obj);
            arr = obj.serializeArray();
            if (! arr.length ) {
                var ref = obj.data("href");
                var dat = obj.data("data");
                if (ref) {
                    var a = hsSerialArr(ref);
                    for(i = 0; i < a.length; i ++) {
                        arr.push(a[i]);
                    }
                }
                if (dat) {
                    var a = hsSerialArr(dat);
                    for(i = 0; i < a.length; i ++) {
                        arr.push(a[i]);
                    }
                }
            }
            break;
        default:
            throw new Error("hsSerialArr: Unsupported type "+typ);
    }
    return  arr;
}

/**
 * 合并多组序列, 类似 jQuery.extend, 但归并层级, 返回为单层 HsSerialDic
 * 支持参数类型: {Array|String|Object|Element|FormData}
 * @returns {Object}
 */
function hsSerialMix() {
    if (arguments.length < 1) {
        throw "hsSerialMix: No less than one arguments";
    }
        var aro = hsSerialDic(arguments[0]);
    for(var x = 1; x < arguments.length; x ++) {
        var arx = hsSerialDic(arguments[x]);
        jQuery.extend( aro, arx );
    }
    return  aro;
}

/**
 * 序列化为字典, 供快速地查找(直接使用Object-Key获取数据)
 * @param {Array|String|Object|Element|FormData} obj
 * @return {Object}
 */
function hsSerialDic(obj) {
    if (obj instanceof HsSerialDic) {
        return obj;
    }
    var arr = hsSerialArr(obj);
    obj = new HsSerialDic(   );
    for(var i = 0; i < arr.length; i ++) {
        var keys = _hsGetDkeys(arr[i].name );
        _hsSetPoint(obj, keys, arr[i].value);
    }
    return  obj;
}

/**
 * 序列化为对象, 供进一步操作(可以使用hsGetValue获取数据)
 * @param {Array|String|Object|Element|FormData} obj
 * @return {Object}
 */
function hsSerialDat(obj) {
    if (obj instanceof HsSerialDat) {
        return obj;
    }
    var arr = hsSerialArr(obj);
    obj = new HsSerialDat(   );
    for(var i = 0; i < arr.length; i ++) {
        var keys = _hsGetPkeys(arr[i].name );
        _hsSetPoint(obj, keys, arr[i].value);
    }
    return obj;
}

/**
 * 序列字典伪类, 可用于识别 hsSerialDic 处理的数据
 * @param {Object} obj
 */
function HsSerialDic(obj) {
    if (obj) jQuery.extend(this, obj);
}

/**
 * 序列对象伪类, 可用于识别 hsSerialDat 处理的数据
 * @param {Object} obj
 */
function HsSerialDat(obj) {
    if (obj) jQuery.extend(this, obj);
}

/**
 * 构建 FormData
 * 尝试将表单转为 FormData 的数据结构
 * 特别对空文件在 Safari 中引发的问题
 * @param {Array|String|Object|Element} data
 * @return {FormData}
 */
function hsToFormData (data) {
    // 自适应输入数据类型, 支持不同来源的数据
    if (! data ) {
        return new FormData(    );
    } else
    if (data instanceof FormData) {
        return data;
    } else
    if (data instanceof Element ) {
        data = data.elements  ||  [  data  ];
    } else
    if (data instanceof jQuery  ) {
        data = data.prop("elements") || data;
    } else
    {
        data = hsSerialArr (data);
    }

    var form = new FormData(    );

    for(var i = 0; i < data.length; i ++) {
        var item = data[i];
        if (item.disabled || ! item.name) {
            continue;
        }
        if (item.type == "file" ) {
            var a = item. filez || item. files; // 由于 files 只读, 自定义文件写入 filez
            if (a.length  ==  0 ) {
                form.append( item.name ,  "" );
            }
            for(var j = 0; j < a.length; j ++) {
                form.append( item.name , a[j]);
            }
        } else
        if (item.tagName == "SELECT") {
            var a = item.options;
            for(var j = 0; j < a.length; j ++) {
            if (! a[j].selected) { continue; }
            form.append(item.name, a[j].value);
            }
        } else
        if (item.type == "radio"
        ||  item.type == "checkbox" ) {
            if (! item.checked ) { continue; }
            form.append(item.name, item.value);
        } else {
            form.append(item.name, item.value);
        }
    }
    return  form;
}

/**
 * 兼容 FormData
 * 将表单转为类似 FormData 的数据结构
 * 使其可执行类似 FormData 的常规操作
 * @param {Array|String|Object|Element} data
 * @return {Array}
 */
function hsAsFormData (data) {
    // 转换为标准列表格式, 不重复绑定兼容函数
    data = hsSerialArr(data);
    if (typeof data.getAll === "function") {
        return data;
    }

    data["append"] = function(name, value) {
        data.push( { name: name, value: value } );
    };
    data["set"   ] = function(name, value) {
        hsSetSeria ( data, name, value );
    };
    data["delete"] = function(name) {
        hsSetSeria ( data, name, [ ] );
    };
    data["get"   ] = function(name) {
        return hsGetSeria (data, name);
    };
    data["getAll"] = function(name) {
        return hsGetSerias(data, name);
    };
    data["has"   ] = function(name) {
        return hsGetSerias(data, name).length > 0;
    };
    data.entriez = data.entries ;
    data.entries = function() {
        var j, i = 0 , x = {
            next : function() {
                if (i < data.length) {
                    j = data[ i ++ ];
                    return {
                        value: [j.name, j.value],
                        done : false
                    };
                } else {
                    return {
                        value: undefined,
                        done : true
                    };
                }
            }
        };
        if (window.Symbol) x[Symbol.iterator] = function() { return x; };
        return x;
    };
    return  data;
}

/**
 * 兼容 FormData
 * 为表单添加一些 FormData 的常规函数
 * 使其可执行类似 FormData 的常规操作
 * @param {Element|Selector} elem
 * @return {Element}
 */
function hsBeFormData (elem) {
    // 清除可能前次添加的, 不重复绑定兼容函数
    elem = jQuery(elem);
    elem.find(".form-data").remove();
    var data = elem.data ("beFormData");
    if (data ) {
        return data;
    } else {
        data = { } ;
        elem.data("beFormData" , data );
    }

    data["append"] = function(name, value) {
        var inp = jQuery('<input type="hidden" class="form-data"/>');
        inp.val ("value", value);
        inp.attr("name" , name );
        elem.append(inp);
    };
    data["delete"] = function(name) {
        elem.find("[name='" + name + "']").remove();
    };
    data["getAll"] = function(name) {
        var arr = [];
        elem.find("[name='" + name + "']").each(function() {
            arr.push(this.value);
        });
        return  arr ;
    };
    data["get"   ] = function(name) {
        return elem.find("[name='" + name + "']").val (/**/);
    };
    data["has"   ] = function(name) {
        return elem.find("[name='" + name + "']").size() > 0;
    };
    data["set"   ] = function(name, value) {
        data["delete"](name       );
        data["append"](name, value);
    };
    data.entriez = data.entries ;
    data.entries = function() {
        var data = elem.find("input,select,textarea");
        var j, i = 0 , x = {
            next : function() {
                if (i < data.length) {
                    j = data[ i ++ ];
                    return {
                        value: [j.name, j.value],
                        done : false
                    };
                } else {
                    return {
                        value: undefined,
                        done : true
                    };
                }
            }
        };
        if (window.Symbol) x[Symbol.iterator] = function() { return x; };
        return x;
    };
    return  data;
}

/**
 * 获取多个参数值
 * @param {String} url
 * @param {String} name
 * @return {String|Array}
 */
function hsGetParam (url, name) {
    name = encodeURIComponent( name );
    var nam = name.replace('.','\\.');
    var reg = new RegExp("[\\?&#]"+ nam +"=([^&#]*)", "g");
    var arr = null;
    var val = [  ];
    while ( ( arr = reg.exec(url) ) ) {
        val.push(decodeURIComponent(arr[1]));
    }
    if (val.length === 0) {
        return undefined;
    }
    if (/(\[\]|\.\.|\.$)/.test(name)) {
        return val;
    }
    return val [0];
}

/**
 * 设置多个参数值
 * @param {String} url
 * @param {String} name
 * @param {String|Array} value
 * @return {String} 同 url
 */
function hsSetParam(url, name, value) {
    name = encodeURIComponent( name );
    var nam = name.replace('.','\\.');
    var reg = new RegExp("[\\?&#]"+ nam +"=([^&#]*)", "g");
    var sig = /^#/.test ( url ) ? '#' : '?'; // hash or search
    url = url.replace(reg, "" );
    if (! jQuery.isArray(value)) {
        value =  value !== undefined
              && value !== null
              ? [value] :  []  ;
    }
    for(var i = 0; i < value.length; i ++) {
        url+= "&"+ name +"="+ encodeURIComponent(value[i]);
    }
    if (url.indexOf(sig) === -1) {
        url = url.replace("&", sig);
    }
    return url;
}

/**
 * 获取多个参数值
 * @param {String} url
 * @param {String} name
 * @return {Array}
 * @deprecated 只用 hsGetParam 即可
 */
function hsGetParams(url, name) {
    var value = hsGetParam(url, name);
    if (value ===  undefined  ) {
        value = [/***/];
    } else
    if (!jQuery.isArray(value)) {
        value = [value];
    }
    return value;
}

/**
 * 设置多个参数值
 * @param {String} url
 * @param {String} name
 * @param {Array} value
 * @return {String} 同 url
 * @deprecated 只用 hsSetParam 即可
 */
function hsSetParams(url, name, value) {
  return hsSetParam (url, name, value);
}

/**
 * 获取序列值
 * @param {Array|Object} arr hsSerialArr 或 HsSerialDic,HsSerialDat
 * @param {String} name
 * @return {String|Array}
 */
function hsGetSeria (arr, name) {
    if (arr instanceof HsSerialDic) {
        return _hsGetPoint (arr, _hsGetDkeys(name));
    }
    if (arr instanceof HsSerialDat) {
        return _hsGetPoint (arr, _hsGetPkeys(name));
    }
    if ( jQuery.isPlainObject(arr)) {
        return _hsGetPoint (arr, _hsGetPkeys(name));
    }
    var val = [  ];
    for(var i = 0 ; i < arr.length; i ++) {
        if (arr[i]["name"] === name ) {
            val.push(arr[i]["value"]);
        }
    }
    if (val.length === 0) {
        return undefined;
    }
    if (/(\[\]|\.\.|\.$)/.test(name)) {
        return val;
    }
    return val [0];
}

/**
 * 设置序列值
 * @param {Array|Object} arr hsSerialArr 或 HsSerialDic,HsSerialDat
 * @param {String} name
 * @param {String|Array} value
 * @return {Array|Object} 同 arr
 */
function hsSetSeria (arr, name, value) {
    if (arr instanceof HsSerialDic) {
        _hsSetPoint (arr, _hsGetDkeyz(name), value);
        return arr;
    }
    if (arr instanceof HsSerialDat) {
        _hsSetPoint (arr, _hsGetPkeyz(name), value);
        return arr;
    }
    if ( jQuery.isPlainObject(arr)) {
        _hsSetPoint (arr, _hsGetPkeyz(name), value);
        return arr;
    }
    if (!jQuery.isArray(value)) {
        value =  value !== undefined
              && value !== null
              ? [value] : [];
    }
    for(var j = arr.length-1; j > -1; j --) {
        if (arr[j]["name"] === name) {
            arr.splice(j, 1);
        }
    }
    for(var i = 0 ; i < value.length; i ++) {
        arr.push({name: name, value: value[i]});
    }
    return arr;
}

/**
 * 获取多个序列值
 * @param {Array|Object} arr hsSerialArr 或 HsSerialDic,HsSerialDat
 * @param {String} name
 * @return {Array}
 * @deprecated 只用 hsGetSeria 亦可
 */
function hsGetSerias(arr, name) {
    var value = hsGetSeria(arr, name);
    if (value ===  undefined  ) {
        value = [/***/];
    } else
    if (!jQuery.isArray(value)) {
        value = [value];
    }
    return value;
}

/**
 * 设置多个序列值
 * @param {Array|Object} arr hsSerialArr 或 HsSerialDic,HsSerialDat
 * @param {String} name
 * @param {Array} value
 * @return {Array|Object} 同 arr
 * @deprecated 只用 hsSetSeria 即可
 */
function hsSetSerias(arr, name, value) {
  return hsSetSeria (arr, name, value);
}

/**
 * 向树对象设置值
 * @param {Object|Array} obj
 * @param {Array|String} path ['a','b'] 或 a.b
 * @param val 将设置的值
 */
function hsSetValue (obj, path, val) {
    /**
     需要注意的键:
     a[1]   数字将作为字符串对待
     a[][k] 空键将作为字符串对待, 但放在末尾可表示push
     */
    if (jQuery.isArray(path)) {
        _hsSetPoint( obj, path, val);
    } else
    if (typeof(path) === "number") {
        var keys = [path];
        _hsSetPoint( obj, keys, val);
    } else
    if (typeof(path) === "string") {
        var keys = _hsGetPkeys(path);
        _hsSetPoint( obj, keys, val);
    } else {
        throw "hsSetValue: 'path' must be an array or string";
    }
}

/**
 * 向树对象设置值(hsSetValue的底层方法)
 * @param {Object|Array} obj
 * @param {Array} keys ['a','b']
 * @param val
 */
function _hsSetPoint(obj, keys, val) {
    if (!obj) {
        return;
    }
    if (!jQuery.isPlainObject(obj)) {
        throw "_hsSetPoint: 'obj' must be an object";
    }
    if (!jQuery.isArray(keys)) {
        throw "_hsSetPoint: 'keys' must be an array";
    }
    if (!keys.length) {
        throw "_hsSetPoint: 'keys' can not be empty";
    }
    _hsSetDepth(obj, keys, val, 0);
}

function _hsSetDepth(obj, keys, val, pos) {
    var key = keys[pos];

    // 按键类型来决定容器类型
    if (key == null) {
        if (obj == null) {
            obj =  [];
        } else
        if (! jQuery.isArray(obj) ) {
            if (jQuery.isPlainObject(obj)) {
                obj = Object.values (obj);
            } else {
                obj = [ obj ];
            }
        }

        if (keys.length == pos + 1) {
            obj.push(val);
        } else {
            obj.push(_hsSetDepth(null, keys, val, pos + 1));
        }

        return obj;
    } else
    if (typeof(key) == "number") {
        if (obj == null) {
            obj =  [];
        } else
        if (! jQuery.isArray(obj) ) {
            if (jQuery.isPlainObject(obj)) {
                obj = Object.values (obj);
            } else {
                obj = [ obj ];
            }
        }

        // 如果列表长度不够, 填充到索引的长度
        if (obj.length <= key) {
            for(var i = 0; i <= key; i++) {
                obj.push(null);
            }
        }

        if (keys.length == pos + 1) {
            obj[key] = val;
        } else {
            obj[key] = _hsSetDepth(obj[key], keys, val, pos + 1);
        }

        return obj;
    } else {
        if (obj == null) {
            obj =  {};
        } else
        if (! jQuery.isPlainObject(obj) ) {
            var dat =  { };
            if (jQuery.isArray(obj)) {
                for(var i = 0; i < obj.length; i ++) {
                    dat[i] = obj[i];
                }
            } else {
                dat [null] = obj;
            }
            obj = dat;
        }

        if (keys.length == pos + 1) {
            obj[key] = val;
        } else {
            obj[key] = _hsSetDepth(obj[key], keys, val, pos + 1);
        }

        return obj;
    }
}

/**
 * 从树对象获取值
 * @param {Object|Array} obj
 * @param {Array|String} path ['a','b'] 或 a.b
 * @param def 默认值
 * @return 获取到的值, 如果没有则取默认值
 */
function hsGetValue (obj, path, def) {
    /**
     需要注意的键:
     a[1]   数字将作为字符串对待
     a[][k] 空键将作为字符串对待, 但放在末尾会直接忽略
     */
    if (jQuery.isArray(path)) {
        return _hsGetPoint(obj, path, def);
    } else
    if (typeof(path) === "number") {
        var keys = [ path ];
        return _hsGetPoint(obj, keys, def);
    } else
    if (typeof(path) === "string") {
        var keys = _hsGetPkeys( path );
        return _hsGetPoint(obj, keys, def);
    } else {
        throw "hsGetValue: 'path' must be an array or string";
    }
}

/**
 * 从树对象获取值(hsGetValue的底层方法)
 * @param {Object|Array} obj
 * @param {Array} keys ['a','b']
 * @param def 默认值
 * @return 获取到的值, 如果没有则取默认值
 */
function _hsGetPoint(obj, keys, def) {
    if (!obj) {
        return def;
    }
    if (!jQuery.isArray(obj ) && !jQuery.isPlainObject(obj)) {
        throw "_hsGetPoint: 'obj' must be an array or object";
    }
    if (!jQuery.isArray(keys)) {
        throw "_hsGetPoint: 'keys' must be an array";
    }
    if (!keys.length) {
        throw "_hsGetPoint: 'keys' can not be empty";
    }
    return _hsGetDepth(obj, keys, def, 0);
}

function _hsGetDepth(obj, keys, def, pos) {
    var key = keys[pos];
    if (obj == null) {
        return def;
    }

    // 按键类型来决定容器类型
    if (key == null) {
        if (keys.length == pos + 1) {
            return obj;
        } else {
            return _hsGetDapth(obj, keys, def, pos + 1);
        }
    } else
    if (typeof(key) == "number") {
        // 如果列表长度不够, 则直接返回默认值
        if (obj.length  <= key ) {
            return def;
        }

        if (keys.length == pos + 1) {
            obj =  obj[key];
            return obj != null ? obj : def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    } else {
        if (keys.length == pos + 1) {
            obj =  obj[key];
            return obj != null ? obj : def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    }
}

function _hsGetDapth(lst, keys, def, pos) {
    /**
     * 获取下面所有的节点的值
     * 下面也要列表则向上合并
     */
    var col = [  ] ;
    var one = true ;
    for(var j = pos; j < keys.length; j ++) {
        if (keys[j] == null) {
            one = false;
            break;
        }
    }
    if (one) {
        for(var i = 0; i < lst.length; i ++) {
            var obj  = _hsGetDepth(lst[i], keys, null, pos);
            if (obj !=  null) {
                col.push(obj);
            }
        }
    } else {
        for(var i = 0; i < lst.length; i ++) {
            var arr  = _hsGetDepth(lst[i], keys, null, pos);
            if (arr !=  null) {
                col.push.apply(col , arr);
            }
        }
    }
    if (! jQuery.isEmptyObject(col)) {
        return  col;
    } else {
        return  def;
    }
}

function _hsGetPkeys(path) {
    // 清理路径符号, 按分隔符进行拆分
    path = path.replace(/\[/g , "." )
               .replace(/\]/g , ""  )
               .split  (/\./ );
    var i , keys = [];
    for(i = 0; i < path.length; i ++) {
        var keyn = path[i];
        if (keyn.length == 0 && i!=0) {
            keys.push(null);
        } else
        {
            keys.push(keyn);
        }
    }
    return  keys;
}

function _hsGetDkeys(path) {
    if (/(\[\]|\.\.|\.$)/.test(path)) {
    // 统一路径符号, 移除数组末尾标识
    path = path.replace(/\[/g , "." )
               .replace(/\]/g , ""  )
               .replace(/\.$/ , ""  );
        return [path , null];
    } else {
        return [path];
    }
}

function _hsGetPkeyz(path) {
    path = path.replace(/(\[\]|\.)$/, '');
    return _hsGetPkeys (path);
}

function _hsGetDkeyz(path) {
    path = path.replace(/(\[\]|\.)$/, '');
    return [path];
}

/**
 * 获取语言
 * @param {String} key
 * @param {Object|Array} rep 替换参数, {a:1,b:2} 或 [1,2]
 * @return {String} 获取到的语言, 其中的 $a或$0 可被 rep 替换
 */
function hsGetLang(key, rep) {
    if (typeof(HsLANG[key]) !== "undefined") {
        key  = HsLANG[key];
    }

    if (rep instanceof Array ) {
        var i, x = {};
        for(i in rep) {
            x[i + ""] = rep[i];
        }
        rep = x;
    }

    if (rep instanceof Object) {
        key = key.replace( /\$(\w+|\{.+?\})/gm, function(w) {
            if (w.substring(0 , 2) == "${") {
                w = w.substring(2, w.length - 1);
            }
            else {
                w = w.substring(1);
            }
            if (typeof(rep[w]) !== "undefined") {
                return rep[w];
            }
            else {
                return "";
            }
        });
    }

    return key;
}

/**
 * 获取配置
 * @param {String} key
 * @param {String} def 默认值
 * @return {String} 获取到的配置, 如果没有则取默认值
 */
function hsGetConf(key, def) {
    if (typeof(HsCONF[key]) !== "undefined") {
        return HsCONF[key];
    }
    else {
        return def;
    }
}

/**
 * 检查权限
 * @param {String} act
 * @param {String} def 缺省值
 * @return {Boolean} 是(true)否(false)有权访问某动作
 */
function hsGetAuth(act, def) {
    if (typeof(HsAUTH[act]) !== "undefined") {
        return HsAUTH[act];
    }
    else {
        return def;
    }
}

/**
 * 检查URI是否有权访问
 * @param {String} uri
 * @return {Boolean} 是否有权
 * @deprecated 为免歧义, 建议使用 hsGetAuth, 不同在于 uri 未做权限控制或不存在时返回 true 而非 undefined
 */
function hsChkUri(uri) {
    if ( ! uri )  uri = "";
    uri  = uri.replace(/[?#].*/, '');
    return false  !== hsGetAuth(uri);
}

/**
 * 补全URI为其增加前缀
 * @param {String} uri
 * @return {String} 完整的URI
 */
function hsFixUri(uri) {
    if (/^(\w+:\/\/|\/|\.\/|\.\.\/)/.test(uri)) {
        return uri;
    }
    var pre  = HsCONF["BASE_HREF"];
    if (pre === undefined) {
        pre  = jQuery("base").attr("href");
    if (pre !== undefined) {
        pre  = pre.replace( /\/$/ ,  ''  );
        HsCONF["BASE_HREF"] = pre ;
    }}
    return pre + "/" + (uri || "");
}

/**
 * 补充URI为其设置参数
 * 当参数有多个值的时候, 需注意参数名加后缀点
 * @param {String} uri
 * @param {Object} pms 可为 hsSerialArr,HsSerialDic,HsSerialDat 或 .loadbox 对象
 * @return {String} 完整的URI
 */
function hsSetPms(uri, pms) {
    if ( ! uri )  uri = "";
    pms = hsSerialDic(pms);
    var ums = hsSerialDic(uri);
    ums = hsSerialMix(ums,pms);
    ums = hsSerialArr(ums);
    ums =jQuery.param(ums);
    uri = uri.replace(/[?#].*/, '');
    return ums ? uri+"?"+ums : uri ;
}

/**
 * 补全URI为其设置参数
 * 每个参数只能有一个值; 亦可作字符串参数注入
 * @param {String} uri
 * @param {Object} pms 可为 hsSerialArr,HsSerialDic,HsSerialDat 或 .loadbox 对象
 * @returns {String} 完整的URI
 */
function hsFixPms(uri, pms) {
    if ( ! uri ) return "";
    pms = hsSerialDic(pms);
    return uri.replace(/\$(\$|\w+|\{.+?\})/gm, function(w) {
        if ("$$" === w) {
            return   w;
        }
        var x = "";
        if ("${" === w.substring(0,2)) {
            w = w.substring(2, w.length - 1);
            // 默认值
            var p  = w. indexOf ("|");
            if (p != -1) {
                x  = w.substring(1+p);
                w  = w.substring(0,p);
            }
        } else {
            w = w.substring(1);
        }   w = pms[ w ];
        return  w !== undefined && w !== null ? w : x;
    });
};

/**
 * 格式文本
 * 类似 printf(FORMAT, ARG1, ARG2...)
 * @returns {String}
 */
function hsFormat() {
    if (!arguments && !arguments.length) {
        throw new Error("hsFormat: params required");
    }

    function _al(s, l, b) {
        var i = s.length;
        for(; i < l; i ++) {
            s = s + b;
        }
        return  s ;
    }
    function _ar(s, l, b) {
        var i = s.length;
        for(; i < l; i ++) {
            s = b + s;
        }
        return  s ;
    }

    var i = 0;
    var a = arguments;
    return (a[i++]+"").replace(
    /%%|%(\-?\+?#?0?)(\d+)?(?:\.(\d+))?([csdfiuoxX])/g,
    function (s, f, l, d, t) {
        if (s === '%%') {
            return '%';
        }

        s = a[i++];
        l = l ? parseInt(l) : -1;
        d = d ? parseInt(d) : -1;

        switch (t) {
            case 'c':
            case 's':
                if (f.indexOf('-') !== -1) {
                    s = _al(s, l, ' ');
                } else {
                    s = _ar(s, l, ' ');
                }
                break;
            default:
                var n, r, x, z;

                // 进制
                switch (t) {
                    case 'X': r = 16; x = '0X'; n = Math.abs(parseInt(s)); break;
                    case 'x': r = 16; x = '0x'; n = Math.abs(parseInt(s)); break;
                    case 'o': r =  8; x = '0' ; n = Math.abs(parseInt(s)); break;
                    case 'u': r = 10; x = ''  ; n = Math.abs(parseInt(s)); break;
                    case 'i': r = 10; x = ''  ; n = parseInt( s ); break;
                    default : r = 10; x = ''  ; n = parseFloat(s); break;
                }

                // 四舍五入
                if (d >= 0) {
                    var p;
                    p = Math.pow  (r, d);
                    n = Math.round(n* p) / p;
                }

                // 转字符串
                s = n.toString(r);
                if (t === 'X') {
                    s = s.toUpperCase( ); /*
                } else {
                    s = s.toLowerCase( ); */
                }
                if (n <  0) {
                    s = s. substring (1);
                    z = "-";
                } else {
                    z = "+";
                }

                // 小数补位
                if (d >= 1) {
                    var p, y;
                    p = s.indexOf('.');
                    if (p === -1) {
                        p  = s.length ;
                        s  = s  + '.' ;
                    }
                    y = f.indexOf('0') !== -1 ? '0' : ' ';
                    for(var j = s.length - p; j < d; j ++) {
                        s += y;
                    }
                }

                // 补位拼接
                if (f.indexOf('+') === -1) {
                    z = '' ;
                }
                if (f.indexOf('#') === -1) {
                    x = '' ;
                }
                if (f.indexOf('0') !== -1) {
                    s = _ar(s, l - z.length - x.length, '0');
                    s = z + x + s;
                } else {
                    s = z + x + s;
                    s = _ar(s, l - z.length - x.length, ' ');
                }

                break;
        }
        return  s;
    });
}

/**
 * 格式数字
 * @param {Number} num
 * @param {Number} len 整数位
 * @param {Number} dec 小数位
 * @param {Number} seg 分位长
 * @param {String} sep 分位符
 * @param {String} dot 小数点
 * @return {String}
 */
function hsFmtNum(num, len, dec, seg, sep, dot) {
  if (typeof(len) === "undefined") {
    len = -1 ;
  }
  if (typeof(dec) === "undefined") {
    dec = -1 ;
  }
  if (typeof(seg) === "undefined") {
    seg =  0 ;
  }
  if (typeof(sep) === "undefined") {
    sep = ",";
  }
  if (typeof(dot) === "undefined") {
    dot = ".";
  }

  // 正负符号
  var sym = parseFloat(num);
  if (isNaN( sym )) {
      return num ;
  }
  num = ( Math.abs( sym ) );
  sym = sym < 0 ? "-" : "" ;

  // 四舍五入
  if ( dec !== -1 ) {
    var p = Math.pow(10, dec);
    num = Math.round(num*p)/p;
  }
  num = num.toString( );

  var a = num.split(".", 2);
  if (a.length < 2) {
    if (  dec  > 0) {
      a[1] = "0";
    } else {
      a[1] = "" ;
      dot  = "" ;
    }
  }
  var n = a[0];
  var d = a[1];

  // 整数位补零
  var nl = n.length;
  for (var i = nl; i < len; i ++) {
    n = "0" + n;
  }

  // 小数位补零
  var dl = d.length;
  for (var j = dl; j < dec; j ++) {
    d = d + "0";
  }

  num = "";
  dec = "";

  // 添加分隔符
  if (seg && sep) {
    var k, s  = "";
    // 整数部分从右往左每3位分割
    while (n != "") {
      k = (n.length - seg);
      s = n.substring(  k);
      n = n.substring(0,k);
      num = s + sep + num ;
    }
    // 整数部分扔掉最右边一位
    if (num) {
      k = num.length - 1;
      num = num.substring(0,k);
    }
    // 小数部分从左往右每3位分割
    while (d != "") {
      k = seg;
      s = d.substring(0,k);
      d = d.substring(  k);
      dec = dec + sep + s ;
    }
    // 小数部分扔掉最左边一位
    if (dec) {
      k = seg;
      dec = dec.substring(  k);
    }
  }
  else {
    num = n;
    dec = d;
  }

  // 组合整数位和小数位
  return sym + num + dot + dec;
}

/**
 * 格式日期
 * 此方法实现较简单
 * 仅支持基础的日期时间格式
 * @param {Date} date
 * @param {String} format 类似 Java SimpleDateFormat
 * @return {String}
 */
function hsFmtDate(date, format) {
  if (date === undefined
  ||  date === null
  ||  date === '') {
      return   '';
  }

  if (typeof(date) === "number") {
      date = new Date(date);
  } else
  if (typeof(date) === "string") {
  do {
    var time = Number(date);
    if ( ! isNaN(time) ) {
      date = new Date(time);
      break;
    }
    time = Date.parse(date);
    if ( ! isNaN(time) ) {
      date = new Date(time);
      break;
    }
    throw new Error("hsFmtDate: invalid date: " + date);
  } while (false);
  }

  var y = date.getFullYear( );
  var M = date.getMonth();
  var d = date.getDate( );
  var H = date.getHours();
  var k = H + 1;
  var K = H > 11 ? H - 12 : H;
  var h = H > 12 ? H - 12 : H;
  var m = date.getMinutes();
  var s = date.getSeconds();
  var S = date.getMilliseconds();
  var U = date.getDay( );
  var a = H < 12 ? 0 : 1;

  if (K == 12) K = 0;
  if (h == 0) h = 12;

  function _addzero(num, len) {
    num = num ? num : "";
    num = num.toString();
    var gth = num.length;
    for (var i = gth; i < len; i ++) {
      num = "0" + num;
    }
    return num;
  }

  function _replace(mat) {
    var len = mat.length;
    var flg = mat.substring(0, 1);
    switch (flg) {
      case 'M':
        if (len >= 4) {
          return hsGetLang("date.LM")[M];
        }
        if (len == 3) {
          return hsGetLang("date.SM")[M];
        }
        else {
          return _addzero(M + 1, len);
        }
      case 'y':
        if (len <= 2) {
          return _addzero(y % 100, len);
        }
        else {
          return _addzero(y, len);
        }
      case 'd':
        return _addzero(d, len);
      case 'H':
        return _addzero(H, len);
      case 'k':
        return _addzero(k, len);
      case 'K':
        return _addzero(K, len);
      case 'h':
        return _addzero(h, len);
      case 'm':
        return _addzero(m, len);
      case 's':
        return _addzero(s, len);
      case 'S':
        return _addzero(S, len);
      case 'a':
        if (len >= 4) {
          return hsGetLang("date.La")[a];
        }
        else {
          return hsGetLang("date.Sa")[a];
        }
      case 'E':
        if (len >= 4) {
          return hsGetLang("date.LE")[U];
        }
        else {
          return hsGetLang("date.SE")[U];
        }
      case 'u':
        return U != 0 ? U : 7;
      case 'U':
        return U;
      default:
        return mat.substring(1, len - 1);
    }
  }

  return format.replace(/M+|d+|y+|H+|k+|K+|h+|m+|s+|S+|a+|E+|u+|U+|'.*?'/g, _replace);
}

/**
 * 解析日期
 * 此方法实现较简单
 * 无法处理除格式外其他字母
 * @param {String} text
 * @param {String} format 类似 Java SimpleDateFormat
 * @return {Date}
 */
function hsPrsDate(text, format) {
  if (! text) {
      return new Date(1970, 0, 1, 0, 0, 0, 0); // 本地时间零点
  }
  if (typeof(text) === "number") {
      return new Date(text);
  }
  if (typeof(text) === "string") {
    var time = Number(text);
    if ( ! isNaN(time) ) {
      return new Date(time);
    }
    /* // 强制 format 格式
    time = Date.parse(text);
    if ( ! isNaN(time) ) {
      return new Date(time);
    }
    */
  }

  function _getcode(arr, wrd) {
    for (var j = 0; j < arr.length; j ++) {
      if (wrd == arr[j]) {
        return j;
      }
    }
  }

  var y, M, d, H = 0, m = 0, s = 0, S = 0, a = 0;
  var fs = format.split(/[\WTZ]+/);
  var ws =   text.split(/[\WTZ]+/);

  for (var i = 0; i < fs.length; i ++) {
    if (ws[i] == null) continue;

    var wrd = ws[i];
    var len = fs[i].length;
    var flg = fs[i].substring(0, 1);
    switch (flg) {
      case 'M':
        if (len >= 4) {
          M = _getcode(hsGetLang("date.LM"), wrd);
        } else
        if (len == 3) {
          M = _getcode(hsGetLang("date.SM"), wrd);
        }
        else {
          M = parseInt(wrd, 10);
        }
      break;
      case 'y':
        if (len <= 2) {
          y = parseInt(wrd, 10) + 2000;
        }
        else {
          y = parseInt(wrd, 10);
        }
      break;
      case 'd':
        d = parseInt(wrd, 10);
      break;
      case 'H':
      case 'K':
        H = parseInt(wrd, 10);
      break;
      case 'k':
      case 'h':
        H = parseInt(wrd, 10) - 1;
      break;
      case 'm':
        m = parseInt(wrd, 10);
      break;
      case 's':
        s = parseInt(wrd, 10);
      break;
      case 'S':
        S = parseInt(wrd, 10);
      break;
      case 'a':
        if (len >= 4) {
          a = _getcode(hsGetLang("date.La"), wrd);
        }
        else {
          a = _getcode(hsGetLang("date.Sa"), wrd);
        }
      break;
    }
  }

  if (a) {
    H += 12;
  }
  if (M) {
    M -= 1 ;
  }

  var date ;
  if (typeof(M) !== "undefined"
  &&  typeof(d) !== "undefined"
  &&  typeof(y) !== "undefined") {
    date =  new Date( );
    date.setFullYear(y);
    date.setMonth   (M);
    date.setDate    (d);
    date.setHours   (H);
    date.setMinutes (m);
    date.setSeconds (s);
    date.setMilliseconds(S);
  }
  else {
    date =  new Date(0);
    date.setHours   (H);
    date.setMinutes (m);
    date.setSeconds (s);
    date.setMilliseconds(S);
  }
  return date;
}

/**
 * 偏移值转换为UTC时区
 * @param {Number} off
 * @return {String} 例如 -480 可转为 UTC+08:00
 */
function hsGmtZone(off) {
    var hur, min;
    min = Math.abs(off);
    hur = Math.floor(min / 60);
    min = Math.floor(min % 60);
    if (hur < 10) hur = "0" + hur;
    if (min < 10) min = "0" + min;
    return "UTC"+ (off > 0 ? "-" : "+") + hur + ":" + min;
}

(function($) {

/**
 * 为了特别区分才搞出来的 HsSerialDic 和 HsSerialDat
 * 仍需要被认为是基础对象
 */
if ($.jqPlainObject === undefined ) {
    $.jqPlainObject = $.isPlainObject;
    $.isPlainObject = function(obj) {
        if (obj instanceof HsSerialDic
        ||  obj instanceof HsSerialDat) {
            return true;
        }
        return $.jqPlainObject(obj);
    };
}

$.hsAjax = function(url, settings) {
    if (typeof(url) ===  "object") {
        settings = url;
        if (typeof(url["url"]) !== "undefined") {
            url  = url["url"];
        }
    }
    url = hsFixUri(url);

    if (settings) {
    // 明确发送数据的类型, 便于服务端正确解析
    if (settings.dataKind) {
        var FormData = window.FormData || Array ;
        switch(settings.dataKind.toLowerCase()) {
            case "part":
                settings.contentType  = false ;
                settings.processData  = false ;
                settings.data = hsToFormData(settings.data);
                break;
            case "json":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element
                ||  settings.data instanceof FormData) {
                    var hsSerialStr = JSON.stringify ;
                    settings.data = hsSerialDat(settings.data);
                    settings.data = hsSerialStr(settings.data);
                }
                settings.contentType = "application/json; charset=UTF-8";
                break;
            case "form":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element
                ||  settings.data instanceof FormData) {
                    var hsSerialStr = jQuery . param ;
                    settings.data = hsSerialArr(settings.data);
                    settings.data = hsSerialStr(settings.data);
                }
                settings.contentType = "application/x-www-form-urlencoded; charset=UTF-8";
                break;
            default:
                throw new Error ( "hsAjax: Unrecognized dataKind " + settings.dataKind ) ;
        }
    } else
    // 统一自定义数据结构, 避免转换后出现偏差
    if (settings.data
    && (settings.data instanceof HsSerialDic
    ||  settings.data instanceof HsSerialDat) ) {
        settings.data = hsSerialArr(settings.data);
    }}

    return  $.ajax (url, settings);
};

$.hsPost = function(url, data, complete) {
    if (complete === undefined) {
        if ($.isFunction(data)) {
            complete  =  data  ;
            data  =  undefined ;
        }
    }
    return  $.hsAjax({
        url : url ,
        data: data,
        type: data ? "POST" : "GET",
        dataKind: "form",
        dataType: "json",
        complete: function(rst) {
            complete && complete(hsResponse(rst, 3));
        }
    });
};

$.hsOpen = function(url, data, complete) {
    var div = $('<div class="modal fade in"><div class="modal-dialog">'
              + '<div class="modal-content"><div class="modal-header">'
              + '<button type="button" class="close" data-dismiss="modal">&times;</button>'
              + '<h4  class="modal-title"  >' + hsGetLang( "opening" ) + '</h4>'
              + '</div><div class="modal-body openbox"></div></div></div></div>');
    var box = div.find('.openbox');
    div.appendTo ( document.body );
    box.hsLoad.apply(box, arguments);
    div.on("hidden.bs.modal", function() {
        div.remove();
    } );
    div.modal();
    return  box;
};

$.hsMask = function(opt) {
    var mod , div , btt, btx, btn ;
    var ini = { show: true };
    var end = function() { };
    var foc = 0;
    var dow = 0;

    // 构建窗体
    if (opt["mode"]) {
        mod = $('<div class="modal fade in"><div class="alert dialog">'
              + '<div class="alert-content"><div class="alert-header">'
              + '<button type="button" class="close" data-dismiss="modal">&times;</button>'
              + '<h4  class="alert-title" ></h4></div>'
              + '<div class="alert-body"  ></div>'
              + '<div class="alert-footer"></div>'
              + '</div></div></div>'  );
        div = mod.find(".alert.dialog");
        btt = div.find(".alert-title" );
        btx = div.find(".alert-body"  );
        btn = div.find(".alert-footer");
        mod . addClass( "alert-modal" ); // 规避警告框右侧的滚动条间隙, CSS 中实现
    } else {
        mod = $('<div class="modal fade in"><div class="modal-dialog">'
              + '<div class="modal-content"><div class="modal-header">'
              + '<button type="button" class="close" data-dismiss="modal">&times;</button>'
              + '<h4  class="modal-title" ></h4></div>'
              + '<div class="modal-body"  ></div>'
              + '<div class="modal-footer"></div>'
              + '</div></div></div>'  );
        div = mod.find(".modal-dialog");
        btt = div.find(".modal-title" );
        btx = div.find(".modal-body"  );
        btn = div.find(".modal-footer");
    }

    // 预置组合
    if (opt["mode"])switch(opt["mode"]) {
    case "note":
        div.addClass("notebox");
        if (opt.position === undefined) {
            opt.position  = "middle";
        }
        if (arguments.length < 2) {
        if (opt.backdrop === undefined) {
            opt.backdrop  = "hidden";
        }} else {
        if (opt.backdrop === undefined) {
            opt.backdrop  =  false;
        }
        if (opt.keyboard === undefined) {
            opt.keyboard  =  false;
        }
        if (opt.closable === undefined) {
            opt.closable  =  false;
        }}
        break;
    case "warn":
        div.addClass("warnbox");
        if (opt.position === undefined) {
            opt.position  = "middle";
        }
        if (arguments.length > 1) {
        if (opt.backdrop === undefined) {
            opt.backdrop  = "static";
        }
        if (opt.keyboard === undefined) {
            opt.keyboard  =  false;
        }
        if (opt.closable === undefined) {
            opt.closable  =  false;
        }}
        break;
    case "wait":
        div.addClass("waitbox");
        if (opt.position === undefined) {
            opt.position  = "middle";
        }
        if (opt.backdrop === undefined) {
            opt.backdrop  = "static";
        }
        if (opt.keyboard === undefined) {
            opt.keyboard  =  false;
        }
        if (opt.closable === undefined) {
            opt.closable  =  false;
        }
        break;
    default:
        throw new Error("hsMask: Unsupported mode " + opt.mode);
    }

    // 设置参数
    ini.backdrop = opt.backdrop;
    ini.keyboard = opt.keyboard;
    if (opt["close"]) {
        end = opt["close"];
    }
    if (opt["count"] !== undefined) {
        dow = opt["count"];
    }
    if (opt["focus"] !== undefined) {
        foc = opt["focus"];
    }
    if (opt["title"] !== undefined) {
        btt.text (opt["title"]);
    }
    if (opt["text" ] !== undefined) {
        btx.text (opt["text" ]);
    } else
    if (opt["html" ] !== undefined) {
        btx.html (opt["html" ]);
    } else
    if (opt["node" ] !== undefined) {
        btx. append  (opt["node" ]);
    } else
    if (opt["load" ] !== undefined) {
        btx. hsLoad  (opt["load" ]);
    }
    if (opt["class"] !== undefined) {
        div.addClass (opt["class"]);
    } else
    if (opt["glass"] !== undefined) {
        div.addClass (opt["glass"]);
    }
    if (opt["space"] !== undefined) {
        btx.css( "white-space", opt["space"] );
    }

    // 设置按钮
    for(var i = 1; i < arguments.length; i ++) {
        var cnf = arguments[i];
        var btm = $('<button type="button" class="btn"></button>');
        btn.append (btm);
        if (cnf["focus"]) {
            foc = i;
        }
        if (cnf["label"]) {
            btm.text (cnf["label"]);
        }
        if (cnf["click"]) {
            btm.click(cnf["click"]);
        }
        if (cnf["class"]) {
            btm.addClass( cnf["class"]);
        } else
        if (cnf["glass"]) {
            btm.addClass( cnf["glass"]);
        } else
        {
            btm.addClass("btn-default");
        }
    }

    // 按钮聚焦
    if (foc !== 0 )
    setTimeout( function( ) {
        $(":focus").blur( );
        var fox = foc  < 0
          ? btt.siblings(".close").eq(0)
          : btn.find("button").eq(foc-1);
        if (fox.size() > 0) {
            fox[0].focus( );
        } else {
            div[0].focus( );
        }
    } , 100);

    // 延时关闭
    if (dow !== 0 )
    setTimeout( function( ) {
        mod.modal( "hide" );
    } , dow * 1000);

    // 附加开关
    if (opt.closable ===  false  ) {
        btt.siblings(".close").remove();
    }

    // 附加类型
    if (ini.backdrop === "hidden") {
        ini.backdrop   =  false  ;

        // 无遮罩时点对话框外也关闭
        mod.on("click", function( evt ) {
            if ($(this).is(evt.target)) {
                mod.modal ( "hide" );
            }
        } );
    }

    // 显示位置
    if (opt.position === "middle") {
        mod.addClass("modal-middle");
    } else
    if (opt.position === "bottom") {
        mod.addClass("modal-bottom");
    }

    // 规避再打开不触发显示事件
    delete( $.support.transition );

    btn.on("click", "button", function(evt) {
        if (evt.isPropagationStopped()
        ||  evt.isDefaultPrevented( )) {
            return;
        }
        mod.modal ( "hide" );
    } );
    mod.on("hidden.bs.modal", function(evt) {
        end. call (div, evt);
        mod.remove(        );
    } );

    btx = $( document.body );
    btx.append(mod);
    mod.modal (ini);

    return  div;
};
$.hsNote = function(msg, typ, end, sec) {
    // 参数
    if (typeof typ !== "string") {
        sec  = end;
        end  = typ;
        typ  = "" ;
    }
    var txt  ;
    var pos  = msg.indexOf( "\r\n" );
    if (pos !== -1) {
        txt  = msg.substring(1+ pos);
        msg  = msg.substring(0, pos);
    }
    var opt  = {
        mode : "note" ,
        text : txt ,
        title: msg ,
        close: end ,
        count: sec ,
        focus: -1
    };

    // 默认计时
    if (sec === undefined) {
        opt.count = 2 ;
    }

    // 样式
    if (typ) switch (typ) {
        case 'info':
            opt.glass = "alert-info" ;
            break;
        case 'danger' :
            opt.glass = "alert-danger" ;
            break;
        case 'warning':
            opt.glass = "alert-warning";
            break;
        case 'success':
            opt.glass = "alert-success";
            break;
        case 'default':
            break;
        default:
            console.warn("hsNote: Wrong type " + typ);
    }

    return $.hsMask.call ( this , opt );
};
$.hsWarn = function(msg, typ, yes, not) {
    // 参数
    if (typeof typ !== "string") {
        not  = yes;
        yes  = typ;
        typ  = "" ;
    }
    var txt  ;
    var pos  = msg.indexOf( "\r\n" );
    if (pos !== -1) {
        txt  = msg.substring(1+ pos);
        msg  = msg.substring(0, pos);
    }
    var opt  = {
        mode : "warn" ,
        text : txt ,
        title: msg ,
        focus: -1
    };
    var arr  = [ opt ];

    // 聚焦
    if (yes !== undefined) {
        opt.focus = 1 ;
    } else
    if (not !== undefined) {
        opt.focus = 2 ;
    }

    // 样式
    if (typ) switch (typ) {
        case 'info':
            opt.glass = "alert-info" ;
            break;
        case 'danger' :
            opt.glass = "alert-danger" ;
            break;
        case 'warning':
            opt.glass = "alert-warning";
            break;
        case 'success':
            opt.glass = "alert-success";
            break;
        case 'default':
            break;
        default:
            console.warn("hsWarn: Wrong type " + typ);
    } else {
        typ = "primary" ;
    }

    // 按钮
    if (null===yes) yes = function() {};
    if (typeof yes  ===  "function") {
        yes = {
            click: yes,
            glass: "btn-" + typ ,
            label: hsGetLang ("ensure")
        };
    }
    if (null===not) not = function() {};
    if (typeof not  ===  "function") {
        not = {
            click: not,
            glass: "btn-default",
            label: hsGetLang ("cancel")
        };
    }
    if (yes) arr.push( yes );
    if (not) arr.push( not );

    return $.hsMask.apply( this , arr );
};

/**
 * 为异步通讯对象包装上进度条
 * @param {String} msg 提示语
 * @param {XMLHttpRequest} xhr
 * @param {XMLHttpRequestUpload} xhu
 */
$.hsWait = function(msg, xhr, xhu) {
    var box = $.hsMask({
        title:  msg  ,
        mode : "wait",
        html : '<div class="progress">'
             + '<div class="progress-bar progress-bar-striped active" style="width: 100%;" '
             + 'role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">'
             + '</div></div>'
    });
    box.find(".alert-footer").append('<span class="progress-per code">...</span>');
    var mod = box.closest(".modal");
    var bar = box.find(".progress-bar");
    var per = box.find(".progress-per");

    // 百分保留两位小数, 小数位补 0
    function progcent(pct) {
        var pzt = ""+(Math.ceil(pct * 100) / 100);
        if (/^\d+$/.test(pzt)) {
            pzt = pzt +".00%";
        } else
        if (/\.\d$/.test(pzt)) {
            pzt = pzt +  "0%";
        } else
        {
            pzt = pzt +   "%";
        }
        return pzt ;
    }

    // 剩余时间文本表示, h:mm:ss
    function progtime(rtt) {
        var ctt, snt;
        snt  =  ""  ;
        rtt  = Math.round(rtt /1000); // 精确到秒
        ctt  = Math.floor(rtt /3600);
        if ( 0<ctt) rtt  =rtt %3600 ;
    //  if (10>ctt) snt += "0";
        snt += ctt + ":";
        ctt  = Math.floor(rtt / 60 );
        if ( 0<ctt) rtt  =rtt % 60  ;
        if (10>ctt) snt += "0";
        snt += ctt + ":";
        if (10>rtt) snt += "0";
        snt += rtt ;
        return snt ;
    }

    // 旧版方案, 不建议用, 改用下面 progress
    var stt = 0; // 开始时间, 正数: 执行时间, 负数: 剩余时间
    var pct = 0; // 执行进度, [0.00,1.00]
    box.getStarting = function() {
        return stt ;
    };
    box.getProgress = function() {
        return pct ;
    };
    box.setStarting = function(ctt) {
        stt  = ctt ;
    };
    box.setProgress = function(pzt) {
        pct  = pzt ;

        if (isNaN   (pct)
        || !isFinite(pct)) {
            box.progress(1.0, "...");
            return;
        }
        if (stt < 0 && pct <= 0) {
            box.progress(0.0, "...");
            return;
        }

        if (stt < 0) {
            pzt = new Date().getTime( ) + stt;
            pzt = 0 - (pzt / pct - pzt); // 剩余时间
        } else
        if (stt > 0) {
            pzt = new Date().getTime( ) - stt;
        } else {
            pzt = "...";
        }

        box.progress(pct, pzt);
    };

    /**
     * @param {type} pct 进度, 取值范围: [0.0,1.0]
     * @param {type} tip 提示; 或者计时, 已用时间, 单位毫秒, 负值估算倒计时, 为零显示百分数
     */
    box.progress = function(pct , tip) {
        pct *= 100 ;
        bar.css ( "width" , pct + "%");
        bar.attr("aria-valuenow", pct);

        // 时间提示
        if (typeof tip == "number") {
            var tim = tip ;
            if (tim < 0.0) {
                tim = Math.abs(tim);
                pct = progcent(pct);
                tim = progtime(tim);
                tip = pct +" "+box.remaining+" "+ tim;
            } else
            if (tim > 0.0) {
                pct = progcent(pct);
                tim = progtime(tim);
                tip = pct +" "+box.consuming+" "+ tim;
            } else
            {
                tip = progcent(pct);
            }
        }

        per.text(tip || "");
    };
    box.hide = function() {
        mod.modal("hide");
    };
    box.over = function() {}; // 发送完成
    box.done = function() {}; // 全部完成
    box.remaining = hsGetLang("time.remaining"); // 剩余时间
    box.consuming = hsGetLang("time.consuming"); // 已用时间

    if (xhu)
    xhu.addEventListener("progress", function(evt) {
        if (evt.lengthComputable) {
            var pct = evt.loaded / evt.total;
            if (pct < 1.0) {
                box.progress(pct,  0.0 );
            } else {
                box.progress(pct, "...");
                box.over();
            }
        }
    } , false );
    if (xhr)
    xhr.addEventListener(  "load"  , function(   ) {
        box.done();
        box.hide();
    } , false );

    return  box;
};
/**
 * 带上传进度条的异步通讯方法
 * @param  {String} msg 提示语
 * @param  {Function} evt 事件
 * @return {Function} xhr 回调
 */
$.hsXhup = function(msg, evt) {
    return function() {
        var xhr = $.ajaxSettings.xhr();
        var xhu = xhr.upload ||  xhr  ;
        var box = $.hsWait( msg, xhr, xhu );
        if (evt) {
            evt.call(box, "init");
            box.over = function() { evt.call(box, "over"); };
            box.done = function() { evt.call(box, "done"); };
        }
        return xhr;
    };
};
/**
 * 带加载进度条的异步通讯方法
 * @param  {String} msg 提示语
 * @param  {Function} evt 事件
 * @return {Function} xhr 回调
 */
$.hsXhrp = function(msg, evt) {
    return function() {
        var xhr = $.ajaxSettings.xhr();
        var box = $.hsWait( msg, xhr, xhr );
        if (evt) {
            evt.call(box, "init");
            box.over = function() { evt.call(box, "over"); };
            box.done = function() { evt.call(box, "done"); };
        }
        return xhr;
    };
};

/**
 * 加载内容块
 * 注意: .html 默认不会发送 data, 增加参数 ?_=任意取值 才会发送
 * @param {String} url
 * @param {Object} data
 * @param {Function} complete
 * @returns {jQuery} 内容区域
 */
$.fn.hsLoad = function(url, data, complete) {
    if (complete === undefined) {
        if ($.isFunction(data)) {
            complete  =  data  ;
            data  =  undefined ;
        }
    }
    if (url === undefined
    && data === undefined) {
        url = this.data("href");
       data = this.data("data");
    }

    var dat ;
    url = hsFixUri    (url ) || "";
    dat = hsSerialObj (data) || {};
    this.data( "href", url )
        .data( "data", dat );

    /**
     * 为了给加载区域内传递参数
     * 通常将参数附加在请求之上
     * 但这样会导致静态页不缓存
     * 故, 如果是 html 且无参数"_=RANDOM"
     * 则不传递任何参数到服务端
     */
    var pos = url. indexOf ("#");
    if (pos > -1) {
        url = url.substring(0 , pos);
    }
    if (/\.html$|\.html\?/.test(url)
    && !hsGetParam(url, "_")
    && !hsGetSeria(dat, "_")) {
        pos = url. indexOf ("?");
    if (pos > -1) {
        url = url.substring(0 , pos);
    }
        dat = undefined;
    } else
    if ($.isEmptyObject(dat)) {
        dat = undefined;
    }

    this.addClass("loadbox")
        .addClass("loading");

    return $.fn.load.call (this, url, dat, function() {
        if ( $.isFunction (complete) ) {
            complete.apply(this, arguments);
        }
        $(this).removeClass("loading");
        $(this).hsReady();
    });
};
/**
 * 打开内容块(可关闭恢复)
 * 注意: .html 默认不会发送 data, 增加参数 ?_=任意取值 才会发送
 * @param {String} url
 * @param {Object} data
 * @param {Function} complete
 * @returns {jQuery} 内容区域
 */
$.fn.hsOpen = function(url, data, complete) {
    var prt = $(this);
    var box;
    var bak;
    var tab;

    /**
     * 获取标签页或面包屑的导航条和对应的区块;
     * 可后退的导航条可打开多个相同链接的页面,
     * 即 hsTadd(undefined), 这将总是新加页签.
     */
    if (prt.parent().is(".tabs")
    ||  prt.parent().is(".labs")) {
        prt = prt.parent();
    }
    if (prt.is(".tabs")) {
        prt = prt.hsTadd(prt.is(".laps") ? undefined : url);
        tab = prt[0];
        prt = prt[1];
    } else
    if (prt.is(".labs")) {
        prt = prt.data("tabs");
        prt = prt.hsTadd(prt.is(".laps") ? undefined : url);
        tab = prt[0];
        prt = prt[1];
    }

    if (tab) {
        bak = tab.parent().children(".active");
        tab.find("a").click(); // 这将触发 hsHide/hsShow
        // 关闭关联的页签
        if (prt.children().size( ) ) {
            prt.children().hsCloze();
            prt.empty();
        }
    } else {
        prt.trigger("hsHide"); // 触发事件
        tab = prt.contents( ); // 原始内容
        bak = $('<div class="openbak"></div>');
        bak.hide().append(tab).appendTo( prt );
    }

    box = $('<div class="openbox"></div>');
    box.appendTo(prt).data("rev", bak);
    box.hsLoad.apply( box , arguments);
    return box;
};
/**
 * 准备内容块
 * 执行初始化, 如其内部的国际化、标题、加载、执行、组件等
 * @returns {jQuery} 当前区域
 */
$.fn.hsReady = function() {
    var box = $(this);

    // 国际化, 取标题
    box.find("[data-i18n]").each(function() {
        $(this).hsI18n();
    }); $( box).hsL10n();

    // 加载
    box.find("[data-load]").each(function() {
        $(this).hsLoad($(this).attr("data-load"), $(this).attr("data-data"));
    });

    // 执行
    box.find("[data-eval]").each(function() {
        //eval 效率低且不安全
        //eval('( false||function(){'+$(this).attr("data-eval")+'} )').call(this);
        Function('return function(){'+$(this).attr("data-eval")+'}')().call(this);
    });

    // 组件
    box.find("[data-topple]").each( function() {
        var func = $(this).attr("data-topple");
        if ($.fn[func]) {
            $.fn[func].call($(this));
        } else if ( window.console ) {
            console.warn("hsReady: Can not bind "+func);
        }
    });

    // 加载前触发事件
    box.trigger("hsReady");

    return box;
};
/**
 * 关闭内容块
 * @returns {jQuery} 当前区域
 */
$.fn.hsClose = function() {
    var prt = $(this).parent();
    var box = $(this);
    var tab;

    if (prt.parent().is(".tabs")
    ||  prt.parent().is(".labs")) {
        box = prt;
        prt = prt.parent();
    }
    if (prt.is(".tabs")) {
        prt = prt.data("labs").children().eq(box.index());
        tab = box;
        box = prt.children(".openbox");
        if (! box.size( ) ) box = prt ;
    } else
    if (prt.is(".labs")) {
        tab = prt.data("tabs").children().eq(box.index());
        prt = box;
        box = prt.children(".openbox");
        if (! box.size( ) ) box = prt ;
    }

    // 触发事件
    box.trigger("hsClose");

    // 联动关闭
    box.hsCloze(/* rel */);

    // 恢复标签
    if (tab) {
        var idx = box.data("rev") ? box.data("rev").index() : 0 ;
        var tbs = tab.parent().children();
        var pns = prt.parent().children();
        var tb1 = tbs.filter(".active");
        var pn1 = pns.eq(tb1.index( ) );
        var tb2 = tbs.eq(idx);
        var pn2 = pns.eq(idx);
        // 移除可关闭的页签
        if (tab.has(".close").size( ) ) {
            tab.remove();
            prt.remove();
        }
        tb1.removeClass("active");
        tb2.   addClass("active")
           .   css("display", "");
        tb2.trigger("hsStab"); // 切换事件
        pn1.trigger("hsHide").hide();
        pn2.show().trigger("hsShow");
    } else
    // 恢复内容
    if (box.data("rev")) {
        var bak =  box.data("rev");
        prt.append(bak.contents());
        box.remove();
        bak.remove();
        prt.trigger("hsShow"); // 重现事件
    } else
    // 关闭浮窗
    if (box.closest(".modal").size()) {
        box.closest(".modal").modal ("hide");
    } else
    // 关闭通知
    if (box.closest(".alert").size()) {
        box.closest(".alert").remove( /**/ );
    }

    return box;
};
/**
 * 关闭关联块
 * @returns {jQuery} 当前区域
 */
$.fn.hsCloze = function() {
    var box = $(this);
    $( document.body).find( ".openbox" ).each(function( ) {
        if (!box.is(this) && box.is($(this).data("rel"))) {
            $(this).hsClose();
        }
    });
    return box;
};

// 选项卡
$.fn.hsTabs = function(tar) {
    var box = $(this);
    if (! tar || !(tar instanceof jQuery)) {
        tar = box.attr("data-target");
        if (tar) {
            tar = box.hsFind(tar);
        } else {
            tar = box.next( );
        }
    }
    box.addClass( "tabs");
    box.data("labs", tar);
    tar.addClass( "labs");
    tar.data("tabs", box);
    box.attr("data-toggle", "hsTabs" );

    var act = box.children(".active" );
    if (act.size() === 0) {
        act = box.children("li:first");
    }
    var idx = act.index();
    act.trigger("hsStab");
    tar.children().hide()
       .eq ( idx ).show();

    return [box, tar];
};
$.fn.hsTadd = function(ref) {
    var box = $(this);
    var tab;
    var lab;
    if (! ref || ! box.find("[data-hrel='"+ref+"']").size() ) {
        tab = $('<li><a href="javascript:;"><span class="title"></span><span class="close">&times;</span></a></li>')
                              .appendTo( box  );
        lab = $('<div></div>').appendTo( box.data( "labs" ) );
        tab.find("a").attr("data-hrel" , ref  );
    } else {
        tab = box.find("[data-hrel='"+ref+"']").closest("li");
        lab = $(box.data("labs")).children().eq(tab.index( ));
    }
    return [tab, lab];
};
$.fn.hsTdel = function(ref) {
    var box = $(this);
    var tab = box.find("[data-hrel='"+ref+"']").closest("li");
    var lab = $(box.data("tabs")).children().eq(tab.index( ));
    tab.children("a").hsClose( ); // 先关闭再删除
    tab.remove();
    lab.remove();
    return [tab, lab];
};

// 标题化
$.fn.hsL10n = function(tit) {
    var box = $(this);
    var til = tit;

    // 从其下标题提取
    if (! tit) {
        var hea = box.children("[data-l10n]");
        if ( 1 <= hea.size()) {
            tit = hea.data("l10n");
        } else {
            hea = box.children("h1,h2,h3,h4");
        if ( 1 <= hea.size()) {
            tit = hea.text();
        } else {
           return box;
        }}
    }

    // 针对共用的表单, 有 ID 即为更新
    if (/^\{DO\}.*/.test(tit)) {
        tit = $.trim(tit.substring(4));
        tit = H$("?id", box)
            ? hsGetLang("update.title", [ tit ])
            : hsGetLang("create.title", [ tit ]);
    }

    if (box.is(".labs>*,.labs>*>*")) {
        var idx = box.closest(".labs>*").index();
        var tbs = box.closest(".labs"  ).data( "tabs") || $();
        var tab = tbs.children().eq(idx).find(".title:first");
        if (til || tab.is(":empty")) {
            tab.text( tit );
        }
    } else
    if (box.is(".modal-body")) {
        box.closest(".modal")
           .find(".modal-title:first")
           .text( tit );
    } else
    if (box.is(".alert-body")) {
        box.closest(".alert")
           .find(".alert-title:first")
           .text( tit );
    }

    return box;
};

// 国际化
$.fn.hsI18n = function(rep) {
    var box = $(this);
    var lng;

    if (rep === undefined ) {
        rep = box.hsData( ).reps ;
    }

    if (box.data("l10n")) {
        lng = box.data("l10n");
        lng = hsGetLang(lng, rep);
        box.data("l10n", lng );
    } else
    if (box.data("i18n")) {
        lng = box.data("i18n");
        lng = hsGetLang(lng, rep);
        box.text( lng );
    } else
    if (box.text( /**/ )) {
        lng = box.text( /**/ );
        lng = hsGetLang(lng, rep);
        box.text( lng );
    }

    if (box.attr("alt")) {
        lng = box.attr("alt");
        lng = hsGetLang(lng, rep);
        box.attr("alt", lng );
    }
    if (box.attr("title")) {
        lng = box.attr("title");
        lng = hsGetLang(lng, rep);
        box.attr("title", lng );
    }
    if (box.attr("tooltip")) {
        lng = box.attr("tooltip");
        lng = hsGetLang(lng, rep);
        box.attr("tooltip", lng );
    }
    if (box.attr("placeholder")) {
        lng = box.attr("placeholder");
        lng = hsGetLang(lng, rep);
        box.attr("placeholder", lng );
    }

    return box;
};

/**
 * 清理空白
 * 规避出现空白间隔, 清除独立空白节点
 * @returns {jQuery}
 */
$.fn.hsTidy = function() {
    this.find("*").contents().filter(function() {
        return this.nodeType === 3 && /^[ \r\n\v\t\f]+$/.test(this.nodeValue);
    }).remove();
    return this;
};

/**
 * 配置数据
 * @param {Object} vals
 * @returns {Object}
 */
$.fn.hsData = function(vals) {
    if (vals) {
        return this.data(vals);
    }
    var conf = this.data(/**/);
    if (conf._hs_data_ === undefined) {
        conf._hs_data_ = true ;
    } else {
        return conf;
    }
    var nreg = /^-\d+$/;
    var areg =  /-\d+$/;
    var freg =  /-\w/g ;
    var that = this.get(0);
    var frep = function(n) {
        return n.substring(1).toUpperCase();
    };
    var vrep = function(v) {
        if (/^(\{.*\}|\[.*\]|\(.*\))$/.test(v)) {
            return Function('return function(){return '+v+'}')().call(this);
        }
        return v ;
    };
    this.each( function( ) {
        var a = this.attributes;
        if (! a ) {
            return;
        }
        for(var i = 0; i < a.length; i ++) {
            var n = a[i].name ;
            if (n.substring(0 , 5) == 'data-') {
                n = n.substring(5);
            } else {
                continue;
            }
            var v = a[i].value;
            if (nreg.test(n)) {
                var o = v.indexOf(':');
                n = v.substring(0 , o);
                v = v.substring(1 + o);
                n = jQuery.trim(n);
                v = jQuery.trim(v);
                v = vrep.call(that, v);
                hsSetValue(conf, n, v);
            } else
            if (areg.test(n)) {
                var o, j;
                o = n.lastIndexOf('-');
                j = n.substring(1 + o);
                n = n.substring(0 , o);
                j = parseInt (   j   );
                v = vrep.call(that, v);
                n = n.replace(freg, frep );
                if (conf[n] === undefined) {
                    conf[n] = [];
                }
                conf [n][j] = v ;
            }
        }
    });
    return  conf;
};
$.fn._hsConfig = $.fn.hsData; // 兼容旧版命名

/**
 * 快捷查找
 * @param {String} selr
 * @returns {jQuery}
 */
$.fn.hsFind = function(selr) {
    if (typeof selr != "string") {
        return $ (selr);
    }
    selr = $.trim(selr);
    var elem = this;
    var flag = selr.charAt   (0);
    var salr = selr.substring(1);
    salr = $.trim(salr);
    switch (flag) {
        case '@':
            do {
                var x;
                x = elem.closest(".laps>*" );
                if (x.size()) { elem = x; break; }
                x = elem.closest(".labs>*" );
                if (x.size()) { elem = x; break; }
                x = elem.closest(".openbox");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".loadbox");
                if (x.size()) { elem = x; break; }
                elem = $(document.body);
            } while (false);
            return salr ? $(salr, elem) : elem ;
        case '%':
            do {
                var x;
                // 与上面的不同, 找最近的那个
                x = elem.closest(".loadbox,.openbox,.labs>*,.laps>*");
                if (x.size()) { elem = x; break; }
                elem = $(document.body);
            } while (false);
            return salr ? $(salr, elem) : elem ;
        case '/':
            {
                elem = elem.parent();
                var a = salr.split('/', 2);
                if (a[0]) elem = elem.closest(a[0]);
                if (a[1]) elem = elem. find  (a[1]);
            }
            return elem;
        case '>':
            return elem.children(salr);
        case '~':
            return elem.siblings(salr);
        case '-':
            return elem.prev(salr);
        case '+':
            return elem.next(salr);
        case '*':
            return elem.find(salr);
        case '$':
            return $(salr);
        case '#':
            return $(selr);
        case '' :
            return $();
        default : // .:[
            /**
             * 往下找不到节点时,
             * 则尝试在全局搜索.
             */
            elem = elem.find(selr);
            return elem.size(    )
                 ? elem : $ (selr);
    }
};
$.fn._hsTarget = $.fn.hsFind; // 兼容旧版命名

/**
 * 模块助手
 * @param {Module} func
 * @param {Object} opts
 * @returns {Object}
 */
$.fn.hsBind = function(func, opts) {
    var name = func.name || /^function\s+(\w+)/.exec(func.toString())[1];
    var inst = this.data(name);
    if (! inst) {
        if (! opts) {
            opts = this.hsData ( );
        } else
        if (typeof opts === "function") {
            opts = opts.call(this);
        }
        inst = new func(this,opts);
        this.data(name , inst);
        this. addClass ( name);
    } else
    if (opts) {
        if (typeof opts === "function") {
            opts = opts.call(this);
        }
        if (typeof inst.reset === "function") {
            inst.reset ( opts);
        } else
        if (typeof inst.setup === "function") {
            inst.setup ( opts);
        }
    }
    return  inst;
};
$.fn._hsModule = $.fn.hsBind; // 兼容旧版命名

/**
 * 滚屏助手
 * @param {Selector} body 外部容器
 * @param  {boolean} setHeight false: max-height, true: height
 * @param   {number} minHeight 最小高, 0~1 为比例, 默认为 0.5, 低于此不处理
 * @returns {number} 适配高度, 无则为 0
 */
$.fn.hsRoll = function(body, setHeight, minHeight) {
    var part = this;
    if (body) {
        body = this.hsFind(body);
    } else {
        body = this.parent().closest(".rollbox");
    }
    if (! part.size()) return 0 ;
    if (! body.size()) return 0 ;
    if (!$.contains(body[0], part[0])) return 0 ;

    part.css("overflow-y", "auto");
    part.css("max-height", "none");
    var viewHeight = parseFloat(body.prop("clientHeight") || 0);
    part.height ( viewHeight ); // 规避内容不够而计算不对
    var bodyHeight = parseFloat(body.prop("scrollHeight") || 0);
    var partHeight = parseFloat(part.prop("offsetHeight") || 0);
    var rollHeight = viewHeight - bodyHeight + partHeight;

    // 需区分是否包含 border
    if (part.css("box-sizing") !== "border-box") {
        rollHeight = rollHeight
            -  parseFloat(part.css("border-top-width"   ) || 0)
            -  parseFloat(part.css("border-bottom-width") || 0);
    }

    // 可以是容器比例
    if (minHeight === undefined) {
        minHeight  = 0.5;
    }
    if (minHeight >= 0 && minHeight <= 1) {
        minHeight  =  minHeight * viewHeight ;
    }

    part.css ("height" , "auto");
    if (minHeight <= rollHeight) {
        if (setHeight) {
            part.css(/**/"height", Math.floor(rollHeight)+"px");
        } else {
            part.css("max-height", Math.floor(rollHeight)+"px");
        }
        return rollHeight;
    }
    return 0;
};

/**
 * 拷贝助手
 */
$.fn.hsCopy = function() {
    if (window.clipboardData) {
        clipboardData.setData("Text", $(this).prop("outerHTML"));
    } else
    if (window.getSelection
    &&  document.execCommand
    &&  document.createRange) {
        var rng = document.createRange();
        var sel = window.getSelection( );
        sel.removeAllRanges();
        for ( var i = 0; i < this.length; i ++) {
            try {
                rng.selectNodeContents(this[i]);
                sel.addRange(rng);
            } catch (e) {
                rng.selectNode/*Text*/(this[i]);
                sel.addRange(rng);
            }
        }
        document.execCommand("Copy");
        sel.removeAllRanges();
    } else
    {
        throw new Error("hsCopy: Copy is not supported");
    }
    return  this;
};
$.hsCanCopy = function() {
    if (window.clipboardData) {
        return true ;
    } else
    if (window.getSelection
    &&  document.execCommand
    &&  document.createRange) {
        return true ;
    } else
    {
        return false;
    }
};

// 三态选择
// indeterminate 有三个值: true 选中, null 半选, false 未选
$.propHooks.choosed = {
    get : function(elem) {
        return elem.checked ? true : (elem.indeterminate ? null : false);
    },
    set : function(elem, stat) {
        if (stat === null) {
            elem.checked = false ;
            elem.indeterminate = true ;
        } else {
            elem.checked = !!stat;
            elem.indeterminate = false;
        }
    }
};

// 字串长度
String.prototype.baseSize = function() {
    var len = 0;
    for(var i = 0; i < this.length; i ++) {
        var c = this.charCodeAt(i);
        if (c > 128) {
            len += 2;
        } else {
            len += 1;
        }
    }
    return  len;
};
String.prototype.byteSize = function() {
    var len = 0;
    for(var i = 0; i < this.length; i ++) {
        var c = this.charCodeAt(i);
        if (c > 128) {
            len += 3;
        } else {
            len += 1;
        }
    }
    return  len;
};

// 对象合并
if (Object.assign === undefined)
Object.assign = function() {
    var a = arguments[0]||{};
    for(var i = 1; i < arguments.length; i ++) {
        var b = arguments[i];
        for(var k in b) {
            a[k] = b[k];
        }
    }
    return a;
};

//** Global Events **/

// Ajax 全局错误处理
$(document).ajaxError(
  function(evt, xhr) {
    hsResponse (xhr);
});

/**
 * Bootstrap 模态框会在禁用滚动条时给右侧加一个间隙, 以防止闪烁.
 * 首次开启时, 可将样式暂存起来,
 * 完全关闭时, 再将样式填充回去;
 * 如存在多个, 须将模态类加回去.
 */
$(document)
.on("show.bs.modal",
function() {
    var bod = $ (document . body );
    if (bod.hasClass("modal-open") === false) {
        bod.data( "style" , bod.attr("style") || "");
    }
})
.on("hidden.bs.modal",
function() {
    var bod = $ (document . body );
    if (bod.hasClass("modal-open") === false) {
    if (bod.find(".modal:visible").size()<=0) {
        bod.attr( "style" , bod.data("style") /**/ );
    } else {
        bod.addClass("modal-open");
    }}
});

// 多选中单击复选
$(document)
.on("change", "select[multiple]",
function(evt) {
    if (evt.shiftKey || evt.metaKey || evt.ctrlKey || evt.altKey) {
        return;
    }
    var vals = $(this).data("vals") || [];
    var valz = $(this).val ( /**/ ) || [];
    $.each(valz, function(x, v    ) {
        var i = $.inArray(v, vals );
        if (i < 0) {
            vals.push  ( v );
        } else {
            vals.splice(i,1);
        }
    });
    $(this).data("vals", vals);
    $(this).val ( vals );
});

// 快捷开启和关闭
$(document)
.on("mouseover", "[data-toggle=hsMenu],[data-toggle=hsMenu]~.dropdown-menu",
function(evt) {
    var cd = $(this).parent(".dropdown,.dropup");
        cd.   addClass("open").children("[aria-expanded]").attr("aria-expanded", "true" );
    var od = $(".dropdown.open").not(cd);
        od.removeClass("open").children("[aria-expanded]").attr("aria-expanded", "false");
    if (window._hsMenuOut) clearTimeout(window._hsMenuOut);
})
.on("mouseout" , "[data-toggle=hsMenu],[data-toggle=hsMenu]~.dropdown-menu",
function(evt) {
    var cd = $(this).parent(".dropdown,.dropup");
    window._hsMenuOut = setTimeout(function() {
        cd.removeClass("open").children("[aria-expanded]").attr("aria-expanded", "false");
    }, 500);
})
.on("click", "[data-toggle=hsDrop]",
function(evt) {
    if ($(this).siblings(".dropdown-body,.dropdown-list").size()
    && !$(evt.target).is(".dropdown-deny,input")) {
        $(this).parent( ).toggleClass( "dropup" );
    }
})
.on("click", "[data-toggle=hsDisp]",
function(evt) {
    var btn = $(this);
    var tit = btn.data("title");
    var txt = btn.data("text");
    var htm = btn.data("html");
    $.hsMask({
        title: tit,
        text : txt,
        html : htm
    });
})
.on("click", "[data-toggle=hsLoad]",
function(evt) {
    var btn = $(this);
    var box = btn.data("target");
    var url = btn.data("href");
    var dat = btn.data("data");
    if (box) {
        box = btn.hsFind(box);
        box.hsLoad(url , dat);
    } else {
        box = btn.hsFind("~");
        box.hsLoad(url , dat);
    }
})
.on("click", "[data-toggle=hsOpen]",
function(evt) {
    var btn = $(this);
    var box = btn.data("target");
    var url = btn.data("href");
    var dat = btn.data("data");
    var siz = btn.data("size");
    if (box) {
        box = btn.hsFind(box);
        box.hsOpen(url , dat);
    } else {
    var b=$.hsOpen(url , dat);
    if (siz) {
        b.closest ("modal-dialog")
         .addClass("modal-"+ siz );
    }}
})
.on("cilck", "[data-toggle=hsExit]",
function(evt) {
    var btn = $(this);
    var box = btn.hsData("target");
        box = btn.hsFind(box||"@");
        box.hsClose();
});

// 选项卡和导航条
$(document)
.on("click", "[data-toggle=hsTabs]>li>a .close",
function() {
    $(this).closest("a").hsClose();
})
.on("click", "[data-toggle=hsTabs]>li>a",
function() {
    var lnk = $(this);
    var tab = lnk.parent();
    var nav = tab.parent();
    var tao = tab.siblings(".active");
    var pns = nav.hsFind(nav.attr("data-target") || nav.next());
    var pno = pns.children().eq(tao.index());
    var pne = pns.children().eq(tab.index());
    if (tab.is(".active,.inactive") ) {
        return;
    }
    // 联动关闭
    if (nav.is(".tabs.laps") && ! tab.is(".host-crumb,.hold-crumb")) {
        var lis = tab.nextAll().find("a");
        if (lis.size()) {
            for(var i = lis.size() -1; i > -1; i --) {
              $(lis[i]).hsClose( );
            }
            return;
        }
    }
    // 延迟加载
    var ref = lnk.data("href");
    if (ref) {
        if (pns.is(".labs" )) {
        if (pne.is(":empty")) {
            var box = $('<div></div>');
            pne.append(box);
            box.hsLoad(ref);
        }} else {
            pno = pne = $();
            pns.hsLoad(ref);
        }
    }
    // 切换页签
    tao.removeClass("active");
    tab.   addClass("active")
       .   css("display", "");
    tab.trigger("hsStab");
    pno.trigger("hsHide").hide();
    pne.show().trigger("hsShow");
})
.on("hsStab", "[data-toggle=hsTabs]",
function() {
    var nav = $(this);
    nav.toggleClass("less-bread", nav.children().size() <= 1); // 单一页签
    nav.toggleClass("home-bread", nav.children('.home-crumb').is(".active")); // 主页
    nav.toggleClass("host-bread", nav.children('.host-crumb').is(".active")); // 主页显示
    nav.toggleClass("hold-bread", nav.children('.hold-crumb').is(".active")); // 总是显示
});

$(function() {$(document).hsReady();});

})(jQuery);
