
/* global self, eval, Symbol, Element, FormData, File, encodeURIComponent, decodeURIComponent, HsAUTH,HsCONF,HsLANG,HsREQS,HsDEPS */

if (!self.HsAUTH) self.HsAUTH = {};
if (!self.HsCONF) self.HsCONF = {};
if (!self.HsLANG) self.HsLANG = {};
if (!self.HsREQS) self.HsREQS = {};
if (!self.HsDEPS) self.HsDEPS = {};

/**
 * 快捷获取
 * 说明(首参数以下列字符开头的意义):
 * . 获取配置
 * : 获取语言
 * / 补全路径
 * ? 检查权限
 * ! 快捷获取模块对象
 * # 快捷获取容器对象
 * @ 获取参数, 第二个参数指定容器, 当键含有[]或..或以.结尾则返回数组
 * % 获取/设置本地存储, 第二个参数存在为设置, 第二个参数为null则删除
 * $ 获取/设置会话存储, 第二个参数存在为设置, 第二个参数为null则删除
 * @return {Mixed} 根据开头标识返回不同类型的数据
 */
function H$() {
    var a = arguments[0];
    var b = a.charAt (0);
    arguments[0] = a.substring(1);
    switch (b) {
    case '.': return hsGetConf.apply(this, arguments);
    case ':': return hsGetLang.apply(this, arguments);
    case '/': return hsFixUri .apply(this, arguments);
    case '?': return hsChkUri .apply(this, arguments);
    case '!':
        if (arguments.length == 1) {
          return jQuery("." + arguments[0]).data(arguments[0]);
        } else {
          return jQuery(arguments[1]).closest("."+ arguments[0]).data(arguments[0]);
        }
    case '#':
        if (arguments.length == 1) {
          return jQuery("#" + arguments[0]).removeAttr( "id" );
        } else {
          return jQuery(arguments[1]).closest("#"+ arguments[0]).removeAttr( "id" );
        }
    case '@':
    case '&': // 因在 html 中为特殊符号, 为避麻烦弃用
        if (arguments.length == 1) {
            arguments.length =  2;
            arguments[1] = location.href;
        }
        if (typeof(arguments[1]) !== "string") {
            if (!jQuery.isArray(arguments[1])) {
                arguments[1]= hsSerialArr(jQuery(arguments[1]).closest(".loadbox"));
            }
            if (/(\[\]|\.\.|\.$)/.test(arguments[0]))
                return hsGetSerias(arguments[1], arguments[0]);
            else
                return hsGetSeria (arguments[1], arguments[0]);
        } else {
            if (/(\[\]|\.\.|\.$)/.test(arguments[0]))
                return hsGetParams(arguments[1], arguments[0]);
            else
                return hsGetParam (arguments[1], arguments[0]);
        }
    case '%':
    case '$':
        var c = b === '$' ? window.sessionStorage : window.localStorage;
        if (typeof c === "undefined") {
            throw "H$: Does not support '"
                + (b === '$' ? 'session' : 'local')
                + "Storage'" ;
        }
        if (arguments.length == 1) {
            return c.getItem(arguments[0]);
        } else
        if (arguments[1] === null) {
            c.removeItem/**/(arguments[0]);
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

    var i = 0;
    var j = 0;
    var l = url.length;
    var h = document.head
         || jQuery ("head") [0]
         || document.documentElement;

    while ( l >= ++ i ) {
        var u = url[i - 1 ];
        if (HsREQS [u]) {
            u = HsREQS  [u];
        }   u = hsFixUri(u);
        if (HsDEPS [u]) {
            HsDEPS [u] += 1;
            if (fun && l == ++ j) {
                fun( );
            }
            continue  ;
        }

        // 在 head 加 link 或 script 标签
        // 监听其加载事件, 全部完成时回调
        var n = document.createElement(/\.css$/.test (u) ? "link" : "script");
        n.onload = n.onreadystatechange = ( function (n, u) {
            return function( ) {
                if ( ! n.readyState
                ||  n.readyState == "loaded"
                ||  n.readyState == "complete") {
                    n.onload = n.onreadystatechange = null;
                    HsDEPS [u] = 1;
                    if (fun && l == ++ j) {
                        fun( );
                    }
                }
            };
        }) (n, u);
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
    }
}

/**
 * 响应数据
 * @param {Object|String|XHR} rst JSON对象/JSON文本或错误消息
 * @param {Boolean} qut 1不显示消息, 2不执行跳转, 3彻底的静默
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
                "msg": msg ,
                "err": err ,
                "ern": ern ,
                "ok" : false
            };
        } else {
            rst = {
                "msg": rst ,
                "err":  "" ,
                "ern":  "" ,
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
                    if (! self.HsGone) {
                          self.HsGone = true;
                        if ( rst.msg ) {
                            alert( rst.msg );
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
                    jQuery.hsWarn(hsGetLang('error.unkwn'), "danger");
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
        if (obj instanceof HsSerialDic) {
            typ = "serdic";
        } else
        if (obj instanceof HsSerialDat) {
            typ = "serdat";
        }
    }
    switch (typ) {
        case "array" :
            arr = obj;
            break;
        case "object":
            for(var key in obj) {
                var vxl  = obj[ key ];
                arr.push({name: key, value: vxl});
            }
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
            hsForEach(obj, function(vxl, key) {
                if (key.length) {
                    key  = key/***/[ 0 ]; // 一键多值但无层级
                    arr.push({name: key, value: vxl});
                }
            });
            break;
        case "serdat":
            hsForEach(obj, function(vxl, key) {
                if (key.length) {
                    key  = key.join('.'); // 需将多层键串起来
                    arr.push({name: key, value: vxl});
                }
            });
            break;
        case "string":
            var ar1, ar2, key, vxl, i = 0;
            ar1 = obj.split('#' , 2);
            if (ar1.length > 1) obj = ar1[0];
            ar1 = obj.split('?' , 2);
            if (ar1.length > 1) obj = ar1[1];
            ar1 = obj.split('&');
            for ( ; i < ar1.length ; i ++ ) {
                ar2 = ar1[i].split('=' , 2);
                if (ar2.length > 1) {
                    key = decodeURIComponent (ar2[0]);
                    vxl = decodeURIComponent (ar2[1]);
                    arr.push({name: key, value: vxl});
                }
            }
            break;
        case "jquery":
            obj = jQuery( obj  );
            if (obj.data("href")) {
                var url = obj.data("href");
                var dat = obj.data("data");
                var p, a, i;
                p = url.indexOf("?");
                if (p != -1) {
                    a  = hsSerialArr(url.substring(p + 1));
                    for(i = 0; i < a.length; i ++) {
                        arr.push(a[i]);
                    }
                }
                p = url.indexOf("#");
                if (p != -1) {
                    a  = hsSerialArr(url.substring(p + 1));
                    for(i = 0; i < a.length; i ++) {
                        arr.push(a[i]);
                    }
                }
                if (dat) {
                    a  = hsSerialArr(dat);
                    for(i = 0; i < a.length; i ++) {
                        arr.push(a[i]);
                    }
                }
            } else {
                arr = jQuery(obj).serializeArray();
            }
            break;
        case "undefined":
        case "null":
            break;
        default:
            throw new Error("hsSerialArr: Unsupported type "+typ);
    }
    return  arr;
}

/**
 * 合并多组序列, 类似 jQuery.merge, 但归并层级, 返回为单层 HsSerialDic
 * @param {Array|String|Object|Element|FormData} obj0, obj1, obj2...
 * @returns {Object}
 */
function hsSerialMix() {
    if (arguments.length < 2) {
        throw "hsSerialMix: No less than two arguments";
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
 */
function HsSerialDic(obj) {
    if (obj) jQuery.extend(this, obj);
}

/**
 * 序列对象伪类, 可用于识别 hsSerialDat 处理的数据
 */
function HsSerialDat(obj) {
    if (obj) jQuery.extend(this, obj);
}

/**
 * 兼容 FormData
 * 将表单转为类似 FormData 的数据结构
 * 使其可执行类似 FormData 的常规操作
 * @param {Array|String|Object|Element} data
 * @return {Array}
 */
function hsAsFormData (data) {
    data = hsSerialArr(data);
    data["append"] = function(name, value) {
        data.push( { name: name, value: value } );
    };
    data["set"   ] = function(name, value) {
        hsSetSeria ( data, name, value );
    };
    data["delete"] = function(name) {
        hsSetSerias( data, name, [] );
    };
    data["get"   ] = function(name) {
        return hsGetSeria (name);
    };
    data["getAll"] = function(name) {
        return hsGetSerias(name);
    };
    data["has"   ] = function(name) {
        return hsGetSerias(name).length ;
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
        if (self.Symbol) x[Symbol.iterator] = function() { return x; };
        return x;
    };
    return  data;
}

/**
 * 构建 FormData
 * 尝试将表单转为 FormData 的数据结构
 * 特别对空文件在 Safari 中引发的问题
 * @param {Array|String|Object|Element} data
 * @return {FormData}
 */
function hsToFormData (data) {
    var form = new FormData( );

    // 自适应输入数据类型, 支持不同来源的数据
    if (! data ) {
        return form;
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
 * 获取多个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @return {Array}
 */
function hsGetSerias(arr, name) {
    var val = [];
    for(var i = 0; i < arr.length; i ++) {
        if (arr[i]["name"] === name ) {
            val.push(arr[i]["value"]);
        }
    }
    return val;
}

/**
 * 设置多个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @param {Array} value
 */
function hsSetSerias(arr, name, value) {
    for(var j = arr.length-1; j > -1; j --) {
        if (arr[j]["name"] === name) {
            arr.splice(j, 1);
        }
    }
    for(var i = 0 ; i < value.length; i ++) {
        arr.push({name: name, value: value[i]});
    }
}

/**
 * 获取单个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @return {String}
 */
function hsGetSeria (arr, name) {
    var val = hsGetSerias(arr, name);
    if (val.length) return val.pop();
    else            return "";
}

/**
 * 设置单个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @param {Array} value
 */
function hsSetSeria (arr, name, value) {
    hsSetSerias(arr, name, value != undefined && value != null ? [value] : []);
}

/**
 * 获取多个参数值
 * @param {String} url
 * @param {String} name
 * @return {Array}
 */
function hsGetParams(url, name) {
    name = encodeURIComponent ( name );
    var nam = name.replace('.', '\\.');
    var reg = new RegExp("[\\?&#]"+ nam +"=([^&#]*)", "g");
    var arr = null, val = [  ];
    while ((arr = reg.exec(url))) {
        val.push(decodeURIComponent(arr[1]));
    }
    return val;
}

/**
 * 设置多个参数值
 * @param {String} url
 * @param {String} name
 * @param {Array} value
 */
function hsSetParams(url, name, value) {
    name = encodeURIComponent ( name );
    var nam = name.replace('.', '\\.');
    var reg = new RegExp("[\\?&#]"+ nam +"=([^&#]*)", "g");
    url = url.replace(reg, "");
    for(var i = 0; i < value.length; i ++) {
        url+= "&"+ name +"="+ encodeURIComponent(value[i]);
    }
    if (url.indexOf("?") < 0 ) {
        url = url.replace("&", "?");
    }
    return url;
}

/**
 * 获取单个参数值
 * @param {String} url
 * @param {String} name
 * @return {String}
 */
function hsGetParam (url, name) {
    var val = hsGetParams(url, name);
    if (val.length) return val.pop();
    else            return "";
}

/**
 * 设置单个参数值
 * @param {String} url
 * @param {String} name
 * @param {String} value
 */
function hsSetParam (url, name, value) {
    return hsSetParams(url, name, value != undefined && value != null ? [value] : []);
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
            return obj[key] || def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    } else {
        if (keys.length == pos + 1) {
            return obj[key] || def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    }
}

function _hsGetDapth(lst, keys, def, pos) {
    var col = [];
    for(var i = 0; i < lst.length; i ++) {
        var obj  = _hsGetDepth(lst[i], keys, def, pos);
        if (obj !=  null) {
            col.push(obj);
        }
    }
    if (!jQuery.isEmptyObject(col)) {
        return col;
    } else {
        return def;
    }
}

function _hsGetDkeys(path) {
    if (/(\[\]|\.\.|\.$)/.test(path)) {
        return [path , null];
    } else {
        return [path];
    }
}

function _hsGetPkeys(path) {
    path = path.replace(/\]\[/g, ".")
               .replace(/\[/   , ".")
               .replace(/\]/   , "" )
               .split  (/\./ );
    var i , keys = [];
    for(i = 0; i < path.length; i ++) {
        var keyn = path[i];
        /*
        if (keyn.substr(0, 1) == '#') {
            keys.push(parseInt(keyn.substr(1)));
        } else
        */
        if (keyn.length == 0 && i!=0) {
            keys.push(null);
        } else
        {
            keys.push(keyn);
        }
    }
    return  keys;
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
 * 遍历对象或数组的全部叶子节点
 * @param {Object,Array} data
 * @param {Function} func
 */
function hsForEach(data, func) {
    function _each(data, func, path) {
        if (jQuery.isPlainObject(data)) {
            for (var k in data) {
                _each(data[k], func, path.concat([k]));
            }
        }
        else if (jQuery.isArray (data)) {
            for (var i = 0 ; i < data.length ; i ++ ) {
                _each(data[i], func, path.concat([i]));
            }
        }
        else if (path.length > 0) {
            func(data, path);
        }
    }
    _each( data, func, [ ] );
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
 * @return {Boolean} 是(true)否(false)有权访问某动作
 */
function hsGetAuth(act) {
    if (typeof(HsAUTH[act]) !== "undefined") {
        return HsAUTH[act];
    }
    else {
        return true;
    }
}

/**
 * 检查URI是否有权访问
 * @param {String} uri
 * @return {Boolean} 是否有权
 */
function hsChkUri(uri) {
    uri  = uri.replace(/[?#].*/, '');
    return hsGetAuth(uri) !== false ;
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
 * @param {type} uri
 * @param {type} pms 可以是 hsSerialArr 或 .loadbox 节点
 * @return {String} 完整的URI
 */
function hsSetPms(uri, pms) {
    if (pms instanceof Element || pms instanceof jQuery) {
        pms = jQuery(pms).closest(".loadbox");
        pms = hsSerialArr(pms);
    }   pms = hsSerialDic(pms);
    for(var n in pms) {
        var v  = pms[n];
        if (jQuery.isArray(v)) {
            uri= hsSetParams(uri, n, v);
        }
        else {
            uri= hsSetParam (uri, n, v);
        }
    }
    return  uri;
}

/**
 * 补全URI为其设置参数
 * 注意: 参数必须是单个的, 对多个参数如 &a[]=$a&a[]=$a 只会设置两个一样的值
 * @param {String} uri
 * @param {Object} pms 可以是 hsSerialArr 或 .loadbox 节点
 * @returns {String} 完整的URI
 */
function hsFixPms(uri, pms) {
    if (pms instanceof Element || pms instanceof jQuery) {
        pms = jQuery(pms).closest(".loadbox");
        pms = hsSerialArr(pms);
    }   pms = hsSerialDat(pms);
    return uri.replace(/\$(\w+|\{.+?\})/gm , function(w) {
        if (w.substring(0 , 2) === "${") {
            w = w.substring(2, w.length -1);
        }
        else {
            w = w.substring(1);
        }
        w = hsGetValue(pms, w);
        return  w || "";
    });
};

/**
 * 格式数字
 * @param {Number} num
 * @param {Number} len 总长度(不含小数点)
 * @param {Number} dec 小数位
 * @param {String} sep 千分符
 * @param {String} dot 小数点
 * @return {String}
 */
function hsFmtNum(num, len, dec, sep, dot) {
  if (typeof(len) === "undefined") {
    len = 0;
  }
  if (typeof(dec) === "undefined") {
    dec = 0;
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
  var p = Math.pow(10, dec);
  num = ( Math.round(num * p) / p).toString();

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
  if (sep) {
    var k, s  = "";
    // 整数部分从右往左每3位分割
    while (n != "") {
      k = (n.length - 3);
      s = n.substring( k );
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
      s = d.substring(0,3);
      d = d.substring( 3 );
      dec = dec + sep + s ;
    }
    // 小数部分扔掉最左边一位
    if (dec) {
      dec = dec.substring( 1 );
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

  if (typeof(date) === "string") {
    if (  isNaN(Number (date) )) {
      // 整理成 yyyy/MM/dd HH:mm:ss 的格式, 并尝试解析它
      var text = date ;
      date = date.replace(/-/g, "/").replace(/t/i, " ");
      date = date.replace(/(\.\d+\s*$|\s+$|^\s+)/g, "");
      date = Date.parse(date);
      if (isNaN( date )) {
          return text ;
      }
    } else {
      date = parseInt(date);
    }
  }

  if (typeof(date) === "number") {
      date = new Date(date);
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
        return mat;
    }
  }

  return format.replace(/M+|d+|y+|H+|k+|K+|h+|m+|s+|S+|a+|E+|u+|U+|'.*'/g, _replace);
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
  if (!text) {
    return  new Date(  0 );
  }
  if (typeof(text) === "number") {
    return  new Date(text);
  }
  if (typeof(text) === "string") {
    var  x  = Number(text);
    if (!isNaN(x)) {
        return new Date(x);
    }
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

  if (a == 1) {
    H += 12;
  }

  var text2;
  if (typeof(M) !== "undefined"
  &&  typeof(d) !== "undefined"
  &&  typeof(y) !== "undefined") {
    text2 = M+"/"+d+"/"+y+" "+H+":"+m+":"+s+"."+S;
  }
  else {
    text2 = H+":"+m+":"+s+"."+S;
  }

  return new Date(Date.parse(text2));
}

/**
 * 偏移值转换为GMT时区
 * @param {Number} off
 * @return {String} 例如 -480 可转为 GMT+08:00
 */
function hsGmtZone(off) {
    var hur, min;
    min = Math.abs(off);
    hur = Math.floor(min / 60);
    min = Math.floor(min % 60);
    if (hur < 10) hur = "0" + hur;
    if (min < 10) min = "0" + min;
    return "GMT"+ (off > 0 ? "-" : "+") + hur + ":" + min;
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

    // 统一自定义数据结构, 避免转换后出现偏差
    if (settings.data  instanceof  HsSerialDic
    ||  settings.data  instanceof  HsSerialDat) {
        settings.data = hsSerialArr(settings.data);
    }

    // 为明确所送数据类型, 便于服务端正确解析
    // 增加 dataKind, 取值 form,json,xml
    if (settings.dataKind) {
        switch(settings.dataKind.toLowerCase()) {
            case "form":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    var hsStringprm = jQuery.param  ;
                    settings.data = hsSerialArr(settings.data);
                    settings.data = hsStringprm(settings.data);
                }
                settings.contentType = "application/x-www-form-urlencoded; charset=UTF-8";
                break;
            case "json":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    var hsStringify = JSON.stringify;
                    settings.data = hsSerialDat(settings.data);
                    settings.data = hsStringify(settings.data);
                }
                settings.contentType = "application/json; charset=UTF-8";
                break;
            case "xml" :
                if (settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    settings.data = '<?xml version="1.0" encoding="UTF-8"?>'
                                  + $( settings.data).prop( 'outerHTML');
                }
                settings.contentType = "application/xml; charset=UTF-8" ;
                break;
            default:
                throw new Error("hsAjax: Unrecognized dataKind " + settings.dataKind);
        }
    }

    return  $.ajax (url, settings);
};

$.hsOpen = function(url, data, complete) {
    var div = $('<div class="modal fade in"><div class="modal-dialog">'
              + '<div class="modal-content"><div class="modal-header">'
              + '<button type="button" class="close" data-dismiss="modal">&times;</button>'
              + '<h4  class="modal-title"  >' + hsGetLang( "opening" ) + '</h4>'
              + '</div><div class="modal-body openbox"></div></div></div></div>');
    var box = div.find ( '.openbox' );
    box.hsLoad( url, data, complete );
    div.on("hide.bs.modal",function() {
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
    switch (opt["mode"]) {
    case "warn":
        div.addClass(opt.mode + "box" );
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
    case "note":
        div.addClass(opt.mode + "box" );
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
    }

    // 设置参数
    ini.backdrop = opt.backdrop;
    ini.keyboard = opt.keyboard;
    if (opt["count"] !== undefined) {
        dow = opt["count"]*1000;
    }
    if (opt["focus"] !== undefined) {
        foc = opt["focus"];
    }
    if (opt["close"] !== undefined) {
        end = opt["close"];
    }
    if (opt["title"] !== undefined) {
        btt.text (opt["title"]);
    }
    if (opt["text" ] !== undefined) {
        btx.text (opt["text" ]);
    } else
    if (opt["html" ] !== undefined) {
        btx.html (opt["html" ]);
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
        var btm = $('<button type="button" class="btn btn-md"></button>');
        btn.append( btm );
        if (opt["focus"]) {
            foc = (i - 1);
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
    $(":focus").blur();
    setTimeout( function( ) {
        var fox = btn.find("button").eq(foc);
        if (fox.size() > 0) {
            fox[0].focus( );
        } else {
            div[0].focus( );
        }
    } , 500);

    // 延时关闭
    if (dow !== 0)
    setTimeout( function( ) {
        mod.modal( "hide" );
    } , dow);

    // 附加开关
    if (opt.closable ===  false  ) {
        btt.siblings(".close").remove();
    }

    // 附加类型
    if (ini.backdrop === "hidden") {
        ini.backdrop  =   false  ;

        // 无遮罩时点对话框外也关闭
        mod.on("click", function( evt ) {
            if ($(this).is(evt.target)) {
                mod.modal (  "hide"  );
            }
        } );
    }

    // 显示位置
    if (opt.position === "middle") {
        // 规避再打开不触发显示事件
        delete( $.support.transition );

        mod.on("shown.bs.modal", function(evt) {
            var wh =$(window).height();
            var mh = div.outerHeight();
            if (wh > mh) {
                mh = Math.floor((wh - mh) / 2);
                div.css("margin-top", mh+"px");
                mod.css("padding"   ,   "0px");
            }
        } );
    } else
    if (opt.position === "bottom") {
        // 规避再打开不触发显示事件
        delete( $.support.transition );

        mod.on("shown.bs.modal", function(evt) {
            var wh =$(window).height();
            var mh = div.outerHeight();
            if (wh > mh) {
                mh = wh - mh;
                div.css("margin-top", mh+"px");
                mod.css("padding"   ,   "0px");
            }
        } );
    }

    btn.on("click", "button", function(evt) {
        mod.modal ( "hide" );
    } );
    mod.on("hidden.bs.modal", function(evt) {
        end. call (div, evt);
        mod.remove();
    } );
    mod.modal( ini );

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
        count: sec || 1.5
    };

    // 样式
    switch (typ) {
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
        default:
            console.warn("hsWarn: Wrong type " + typ);
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
        title: msg
    };
    var arr  = [ opt ];

    // 样式
    switch (typ) {
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
        default:
            console.warn("hsWarn: Wrong type " + typ);
    }

    // 按钮
    if (null===yes) yes = function() {};
    if (typeof yes  ===  "function") {
        yes = {
            click: yes,
            glass: "btn-primary",
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
$.hsXhwp = function(msg, xhr, xhu) {
    var box = $.hsMask({
        title: msg,
        mode : "prog",
        glass: "progbox",
        html : '<div class="progress"><div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div></div>',
        position: "middle",
        backdrop: "static",
        keyboard:  false,
        closable:  false
    });
    var mod = box.closest(".modal");
    var foo = box.find(".alert-footer");
    var bar = box.find(".progress-bar");
    var stt = new Date().getTime()/1000;
    var pct = 0;
    var rtt = 0;

    foo.empty().text("...");
    box.find ( ".close").remove();
    box.trigger("shown.bs.modal");

    if (xhr)
    xhr.addEventListener(  "load"  , function(   ) {
        mod.modal( "hide" );
    } , false);
    if (xhu)
    xhu.addEventListener("progress", function(evt) {
        if (pct >= 100 || ! evt.lengthComputable ) {
            return;
        }

        var tal  = evt.total ;
        var snt  = evt.loaded;
        var ctt  = new Date().getTime( )/1000 - stt;
        pct  = Math.ceil((100 * snt) / (0.0 + tal));
        rtt  = Math.ceil((100 - pct) * (ctt / pct));

        // 剩余时间文本表示, h:mm:ss
        snt  =  "" ;
        ctt  = Math.floor(rtt /3600);
        if ( 0<ctt) rtt  =rtt %3600 ;
//      if (10>ctt) snt += "0";
        snt += ctt + ":";
        ctt  = Math.floor(rtt / 60 );
        if ( 0<ctt) rtt  =rtt % 60  ;
        if (10>ctt) snt += "0";
        snt += ctt + ":";
        if (10>rtt) snt += "0";
        snt += rtt ;

        bar.attr("aria-valuenow", pct);
        bar.css ( "width" , pct + "%");
        foo.text(  pct  + "% -" + snt);
    } , false);

    return box;
};
/**
 * 带加载进度条的异步通讯方法
 * @param {String} msg 提示语
 * @return {Function} xhr回调
 */
$.hsXhrp = function(msg) {
    return function(   ) {
        var xhr = $.ajaxSettings.xhr();
        $.hsXhwp( /**/ msg, xhr, xhr );
        return xhr;
    };
};
/**
 * 带上传进度条的异步通讯方法
 * @param {String} msg 提示语
 * @return {Function} xhr回调
 */
$.hsXhup = function(msg) {
    return function(   ) {
        var xhr = $.ajaxSettings.xhr();
        var xhu = xhr.upload ||  xhr  ;
        $.hsXhwp( /**/ msg, xhr, xhu );
        return xhr;
    };
};

$.fn.hsLoad = function(url, data, complete) {
    if ( $.isFunction(  data  )) {
        complete = data ;
        data = undefined;
    }
    if (!$.isFunction(complete)) {
        complete = function() {};
    }

    var dat = data ? hsSerialArr(data): [];
    this.data( "href", url )
        .data( "data", dat )
        .addClass("loadbox")
        .addClass("loading");

    /**
     * 为了给加载区域内传递参数
     * 通常将参数附加在请求之上
     * 但这样会导致静态页不缓存
     * 故, 如果是 html 且无参数"_=RANDOM"
     * 则不传递任何参数到服务端
     */
    var pos;
    pos = url.indexOf( "#" );
    if (pos != -1) {
        url = url.substring(0 , pos);
    }
    if (/\.html$|\.html\?/.test(url)
    && !hsGetParam(url,"_" )
    && !hsGetSeria(dat,"_")) {
    pos = url.indexOf( "?" );
    if (pos != -1) {
        url = url.substring(0 , pos);
    }
        dat = undefined;
    } else
    if (dat.length == 0) {
        dat = undefined;
    }
    url = hsFixUri(url);

    return  $.fn.load.call (this, url, dat, function() {
        $(this).removeClass("loading").hsReady();
        complete.apply(this,arguments);
    });
};
$.fn.hsOpen = function(url, data, complete) {
    var prt = $(this);
    var box;
    var tab;
    var bak;

    /**
     * 获取标签页或面包屑的导航条和对应的区块;
     * 可后退的导航条可打开多个相同链接的页面,
     * 即 hsTadd(undefined), 这将总是新加页签.
     */
    if (prt.is(".labs")) {
        prt = prt.data("tabs");
        prt = prt.hsTadd(prt.is(".laps") ? undefined : url);
        tab = prt[0];
        prt = prt[1];
    } else
    if (prt.is(".tabs")) {
        prt = prt.hsTadd(prt.is(".laps") ? undefined : url);
        tab = prt[0];
        prt = prt[1];
    } else
    if (prt.parent().is(".tabs")) {
        tab = prt;
        prt = prt.parent().data( "labs").children().eq(tab.index());
    } else
    if (prt.parent().is(".labs")) {
        tab = prt.parent().data( "tabs").children().eq(prt.index());
    }

    if (tab) {
        bak = tab.parent().children( ).filter(".active");
        tab.show( ).find("a").click( );
        if (tab.find("a b,a span").size()) {
            tab.find("a b,a span").not(".close")
               .text("...");
        } else {
            tab.find( "a" )
               .text("...");
        }
        // 关闭关联的 tab
        if (prt.children().size( ) ) {
            prt.children().hsCloze();
            prt.empty();
        }
    } else {
        bak = $('<div class="openbak"></div>').hide()
            .append(prt.contents( )).appendTo ( prt );
    }

    box = $('<div class="openbox"></div>')
          .appendTo(prt).data("hrev", bak);
    box.hsLoad(url , data, complete);
    return box;
};
$.fn.hsReady = function() {
    var box = $(this);

    // 为避免 chrome 等浏览器中显示空白间隔, 清除全部独立的空白文本节点
    box.find("*").contents().filter(function() {
        return this.nodeType === 3 && /^\s+$/.test(this.nodeValue);
    }).remove();

    // 输入类
    box.find("input").each(function() {
        $(this).addClass("input-"+$(this).attr("type"));
    });

    // 国际化
    box.find(".i18n,[data-i18n]").each(function() {
        $(this).hsI18n();
    });

    // 选项卡
    box.find("[data-toggle=hsTabs]").each(function() {
        $(this).hsTabs();
    });

    // 初始化
    box.find("[data-toggle=hsInit]").each(function() {
        $(this).hsInit();
    });

    // 初始化
    if (! box.children("[data-toggle=hsInit],[data-module=hsInit]").size()) {
        $(this).hsInit();
    }

    // 组件化
    box.find("[data-module]").each( function() {
        var prnt = $(this);
        var opts = $(this).hsData();
        var func = $(this).attr("data-module");
        if (typeof(prnt[func]) === "function") {
            prnt[func]( opts );
        }
    });

    // 在加载前触发事件
    box.trigger("hsReady");

    // 加载、打开、执行
    box.find("[data-load]").each(function() {
        $(this).hsLoad($(this).attr("data-load"), $(this).attr("data-data"));
    });
    box.find("[data-open]").each(function() {
        $(this).hsOpen($(this).attr("data-open"), $(this).attr("data-data"));
    });
    box.find("[data-eval]").each(function() {
        eval('(false||function(){'+ $(this).attr("data-eval") +'})').call(this);
    });

    return box;
};
$.fn.hsClose = function() {
    var prt = $(this).parent();
    var box = $(this);
    var tab;

    if (prt.parent().is(".tabs")) {
        tab = prt;
        prt = prt.parent().data("labs").children().eq(tab.index());
        box = prt.children(".openbox" ); // Get the following boxes
    } else
    if (prt.parent().is(".labs")) {
        tab = prt.parent().data("tabs").children().eq(prt.index());
    }

    // 触发事件
    box.trigger("hsClose");

    // 联动关闭
    box.hsCloze(/* rel */);

    // 恢复标签
    if (tab) {
        var idx = box.data("hrev") ? box.data("hrev").index() : 0;
        var tbs = tab.parent().children();
        var pns = prt.parent().children();
        var tb2 = tbs.eq(idx);
        var pn2 = pns.eq(idx);
        tbs./**/removeClass("active");
        tb2.show().addClass("active");
        pns.hide();
        pn2.show();
        if (tab.has(".close").size()) {
            tab.remove();
            prt.remove();
        }
        pn2.trigger("hsRecur"); // 触发重现事件
    } else
    // 恢复内容
    if (box.data("hrev")) {
        var bak =  box.data( "hrev" );
        prt.append(bak.contents ( ) );
        box.remove();
        bak.remove();
        prt.trigger("hsRecur"); // 触发重现事件
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
$.fn.hsTabs = function(rel) {
    var box = $(this);
    if (! rel) {
        rel = box.attr("data-target");
        if (rel) {
            rel = box.hsFind(rel);
        } else {
            rel = box.next( );
        }
    }
    box.addClass( "tabs");
    box.data("labs", rel);
    rel.addClass( "labs");
    rel.data("tabs", box);

    var act = box.children(".active" );
    if (act.size() === 0) {
        act = box.children("li:first");
    }
    act.children("a").click();

    return box;
};
$.fn.hsTadd = function(ref) {
    var box = $(this);
    var tab;
    var pne;
    if (! ref || ! box.find("[data-hrel='"+ref+"']").size() ) {
        tab = $('<li><a href="javascript:;"><span class="title">&minus;</span><span class="close">&times;</span></a></li>')
                              .appendTo( box  );
        pne = $('<div></div>').appendTo( box.data( "labs" ) );
    } else {
        tab = box.find("[data-hrel='"+ref+"']").closest("li");
        pne = $(box.data("tabs")).children().eq(tab.index( ));
    }
    return [tab, pne];
};
$.fn.hsTdel = function(ref) {
    var box = $(this);
    var tab = box.find("[data-hrel='"+ref+"']").closest("li");
    var pne = $(box.data("tabs")).children().eq(tab.index( ));
    tab.children("a").hsClose( ); // 先关闭
    tab.remove( ); pne.remove( ); // 再删除
    return [tab, pne];
};

// 初始化
$.fn.hsInit = function(cnf) {
    if (cnf ===  undefined) {
        cnf = $(this).hsData();
    }
    var box = $(this);

    // 标题上的设置作用在其容器上
    if (box.is("h1,h2,h3")) {
        box = box.parent();
    }

    // 自动提取标题, 替换编辑文字
    // 如主键不叫id, 打开编辑页面, 则需加上id=0
    var h = box.children("h1,h2,h3").first();
    if (h.length
    && !cnf.title) {
        cnf.title = h.text();
    }
    if (cnf.title) {
        cnf.title = hsGetSeria(hsSerialArr(box.closest( ".loadbox" )), "id") ?
            cnf.title.replace('{DO}', cnf.update || hsGetLang("form.update")):
            cnf.title.replace('{DO}', cnf.create || hsGetLang("form.create"));
    }
    if (h.length ) {
        h.text ( cnf.title );
    }

    if (box.is(".modal-body")) {
        var a = box.closest(".modal");
        for(var k in cnf) {
            var v =  cnf[k];
            switch (k) {
                case "title":
                    a.find(".modal-title" ).text( v );
                    break;
                case "modal":
                    a.find(".modal-dialog").addClass("modal-" + v);
                    break;
            }
        }
        a.modal();
    } else
    if (box.parent(".labs").size()
    ||  box.parent().parent(".labs").size()) {
        var a = box.closest(".labs>*" );
            a = box.closest(".labs")
                   .data("tabs").children( ).eq(a.index());
        for(var k in cnf) {
            var v =  cnf[k];
            switch (k) {
                case "title":
                    var x = a.find("a");
                    var y = x.find("b,span").not(".close");
                    if (y.size()) {
                        y.text(v);
                    } else {
                        x.text(v);
                    }
                    break;
            }
        }
    }

    return box;
};

// 国际化
$.fn.hsI18n = function(rep) {
    var box = $(this);
    var lng;

    if (box.attr("data-i18n")) {
        lng = box.attr("data-i18n");
        lng = hsGetLang(lng, rep);
        box.text( lng );
    } else
    if ($(this).text()) {
        lng = box.text( );
        lng = hsGetLang(lng, rep);
        box.text( lng );
    }

    if (box.attr("alt")) {
        lng = box.attr("alt");
        lng = hsGetLang(lng, rep);
        box.attr("alt" , lng);
    }
    if (box.attr("title")) {
        lng = box.attr("title");
        lng = hsGetLang(lng, rep);
        box.attr("title" , lng);
    }
    if (box.attr("placeholder")) {
        lng = box.attr("placeholder");
        lng = hsGetLang(lng, rep);
        box.attr("placeholder" , lng);
    }

    return box;
};

$.fn.hsData = function() {
    var that = this.get(0);
    var conf = this.data();
    var nreg = /^data-\d+$/;
    var treg = /-\d+$/;
    var freg = /-\w/g ;
    var frep = function(n) {
        return n.substring(1).toUpperCase();
    };
    var vrep = function(v) {
        if ( /^(\{.*\}|\[.*\])$/.test( v )) {
            v = eval('('+v+')');
        } else if ( /^(\(.*\))$/.test( v )) {
            v = eval(    v    );
        }
        return  v;
    };
    var mrep = function(v) {
        if (!/^(\{.*\})$/.test( v )) {
                v  = '{'+v+'}' ;
        }
        return  eval('('+v+')');
    };
    this.each( function( ) {
        var a = this.attributes;
        if (! a ) {
            return;
        }
        var j = a.length;
        var i = 0;
        for ( ; i < j; i ++ ) {
            var n = a[i].name ;
            if (n.substring(0 , 5) == 'data-') {
                n = n.substring(5);
            } else {
                continue;
            }
            var v = a[i].value;
            if ('data' == n ) {
                v = mrep.call(that, v);
                jQuery.extend(conf, v);
            } else
            if (nreg.test(n)) {
                var o = v.indexOf(':');
                n = v.substring(0 , o);
                v = v.substring(1 + o);
                n = jQuery.trim(n);
                v = jQuery.trim(v);
                v = vrep.call(that, v);
                hsSetValue(conf, n, v);
            } else
            if (treg.test(n)) {
                n = n.replace(treg,  ''  );
                n = n.replace(freg, frep );
                if (conf[n] === undefined) {
                    conf[n] = [];
                }
                v = vrep.call(that, v);
                 conf[n].push(v);
            } else {
                n = n.replace(freg, frep );
                v = vrep.call(that, v);
                 conf[n]  =   v ;
            }
        }
    });
    return  conf;
};
$.fn._hsConfig = $.fn.hsData; // 兼容旧版命名

$.fn.hsFind = function(selr) {
    if (typeof selr != "string") {
        return $ (selr);
    }
    var elem = this;
    selr = $.trim(selr);
    var flag = selr.charAt   (0);
    var salr = selr.substring(1);
    salr = $.trim(salr);
    switch (flag) {
        case '@':
            do {
                var x;
                x = elem.closest(".labs");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".openbox");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".loadbox");
                if (x.size()) { elem = x; break; }
                elem = $(document);
            } while (false);
            return salr ? $(salr, elem) : elem;
        case '&':
            do {
                var x;
                x = elem.closest(".loadbox");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".openbox");
                if (x.size()) { elem = x; break; }
                elem = $(document);
            } while (false);
            return salr ? $(salr, elem) : elem ;
        case '^':
            elem = elem.parent();
            var a = salr.split(';' , 2);
            if (a[0]) elem = elem.closest(a[0]);
            if (a[1]) elem = elem.hsFind (a[1]);
            return elem;
        case '>':
            return elem.children(salr);
        case '~':
            return elem.siblings(salr);
        case '+':
            return elem.next(salr);
        case '-':
            return elem.prev(salr);
        case '?':
            return elem.find(salr);
        case '*':
            return $(salr);
        case '#':
            return $(selr);
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

$.fn._hsModule = function(func, opts) {
    var name = func.name || /^function\s+(\w+)/.exec(func.toString())[1];
    var inst = this.data(name);
    if (! inst) {
        inst =  new func(this , opts ? opts : {});
        this.data(name , inst);
        this. addClass ( name);
    } else
    if (opts) {
        if (typeof inst.reset === "function") {
            inst.reset ( opts);
        } else
        if (typeof inst.setup === "function") {
            inst.setup ( opts);
        }
    }
    return  inst;
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

$(document).ajaxError(function(evt, xhr, cnf) {
    var rst = hsResponse(xhr);
    if (typeof(cnf.funcName) === "undefined") {
        return;
    }
    if (typeof(cnf.trigger ) !== "undefined") {
        var btn = $(cnf.trigger);
        btn.trigger(cnf.funcName+"Error", evt, rst);
    }
    if (typeof(cnf.context ) !== "undefined") {
        var box;
        if ( typeof(cnf.context.context) !== "undefined") {
            box = $(cnf.context.context);
        } else {
            box = $(cnf.context);
        }
        box.trigger(cnf.funcName+"Error", evt, rst);
    }
});

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
})
.on("click", ".dropdown-toggle",
function(evt) {
    var body = $(this).siblings(".dropdown-body,.dropdown-list");
    if (! body.size() || $(evt.trget).is("input,.dropdown-deny")) {
        return; // 需跳过全选等
    }
    var cont = $(this).parent();
    cont.toggleClass ("dropup");
})
.on("click", "[data-toggle=hsOpen]",
function() {
    var btn = $(this);
    var box = btn.data("target");
    var url = btn.data("href");
    var dat = btn.data("data");
    var das = btn.data();
    var evs = { };

    /**
     * 从数据属性中提取出事件句柄
     * 绑定在打开的区域上
     * 因打开的区域会在关闭后销毁
     * 故可在打开重复绑定
     */
    for(var n in das) {
        var f  = das[n];
        if (! /^on[A-Z]\w*/.test( n )) {
            continue;
        }
        if (typeof(f) !== "function" ) {
            f = eval( '(null||function(event){' + f + '})' );
        }
        n = n.substring(2, 3).toLowerCase() + n.substring(3);
        evs[n] = f;
    }

    url = hsFixPms(url, this);
    if (box) {
        box = btn.hsFind(box)
                 .hsOpen(url, dat, evs.hsReady);
    } else {
        box =   $.hsOpen(url, dat, evs.hsReady);
    }

    for(var n in evs) {
        var f  = evs[n];
        if (n === "hsReady") {
            continue;
        }
        box.on(n, function() {
            f.apply(btn, arguments);
        });
    }
})
.on("cilck", "[data-toggle=hsExit]",
function() {
    var sel = $(this).attr("data-target");
    var box = $(this).hsFind (sel || "@");
    box.hsClose( );
})
.on("click", ".close,.cancel",
function() {
    var box;
    var ths = $(this);
    do {
        // 云标签
        if (ths.is("li>.close")) {
            return;
        }
        // 选项卡
        if (ths.is("li .close")) {
            box = ths.closest("a");
            break ;
        }
        // 标题栏
        box = ths.closest(".modal-header").next();
        if (box.is( ".openbox")) {
            box = box.closest(".modal");
            break ;
        }
        // 模态框和警示框
        box = ths.closest( ".openbox" );
        if (box.is( ".modal-body")) {
            box = box.closest(".modal");
            break ;
        }
        if (box.is( ".alert-body")) {
            box = box.closest(".alert");
            break ;
        }
        if (box.size()
        &&  box.closest(".modal,.alert").size()) {
            break ;
        }
        if (ths.closest(".modal,.alert").size()) {
            return;
        }
    } while(false);
    // 禁止关闭
    if(ths.closest(".dont-close",box[0]).size()) {
            return;
    }
    box.hsClose( );
})
.on("click", ".tabs > li > a",
function() {
    var ths = $(this);
    var tab = ths.parent();
    var nav = tab.parent();
    var pns = nav.data("labs");
    var pne = pns ? pns.children().eq(tab.index()) : $();
    if (tab.is(".dont-close")
    ||  tab.is(".back-crumb")) {
        return;
    }
    // 联动关闭
    if (nav.is(".breadcrumb")
    && !tab.is(".hold-crumb")) {
        tab.nextAll( ).find("a").each( function() {
            $(this).hsClose();
        });
    }
    // 延迟加载
    if (ths.is("[data-href]")) {
        var ref;
        ref = ths.attr("data-href");
        ths.removeAttr("data-href");
        pne.hsOpen( ref );
    }
    pne.siblings().hide();
    tab.siblings()
           .removeClass("active");
    tab.show().addClass("active");
    pne.show().trigger("hsRecur");
})
.on("click", ".home-crumb a",
function() {
    // Nothing to do...
})
.on("click", ".back-crumb a",
function() {
    var nav = $(this).closest('.breadcrumb');
    nav.find('li:last a').hsClose();
    nav.find('li:last a').  click();
})
.on("hsReady hsRecur", ".labs.laps",
function() {
    var nav = $(this).siblings('.breadcrumb') || $(this).data("tabs");
    if (nav.children().not('.back-crumb,.home-crumb').size( )) {
        nav.show().removeClass( "invisible" )
                  .removeClass(  "hide"  );
    } else {
        nav.hide();
    }
    if (nav.children(".active").is('.home-crumb,.hold-crumb')) {
        nav.children('.back-crumb').hide();
    } else {
        nav.children('.back-crumb').show();
    }
})
.on("hidden.bs.modal",
function() {
    // 关闭模态框时, 为了保障滚动条有效,
    // 如果存在多个, 必须将模态类加回去.
    if ($("body .modal:visible").size()) {
        $("body").addClass("modal-open");
    }
});

$(function() {
    $(document).hsReady();
});

})(jQuery);
