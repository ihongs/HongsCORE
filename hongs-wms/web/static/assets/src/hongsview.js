
/**
 * 填充数据、渲染元素
 * 支持功能：
 * 1. 模板插值：{{expr}}
 * 2. 属性绑定：<span title="{{expr}}"></span>
 * 3. 条件渲染：<div data-if="condition expr"></div>
 * 4. 循环渲染：<div data-for="item in items"></div>
 * 5. 另类插值: <!--{{expr}}-->
 * 6. 变量设置：<!--{%_.var_name = 'value'%}-->
 * 特别说明：
 * 1. for 循环是同级复制，并非下级
 * 2. for 内通过 _index 访问下标，_value 访问取值
 * 3. 表达式可以是任意标准的 javascript 代码
 * 4. 不确定变量是否存在时用 _.var_name 引用
 * 5. 模板内 this 指向自身节点对象
 * @param {Element|Array|jQuery} element
 * @param {Object} data
 */
function hsFill(element, data) {
    // 处理数组或类数组对象 (如 NodeList/jQuery 对象等)
    if (hsFill.isArr(element)) {
        for (var i = 0; i < element.length; i ++) {
            hsFill(element[i], data);
        }
        return;
    }

    // 模板内可通过 _.xxx 规避变量不存在的异常
    if (! data._) data._ = data;

    // 内部渲染函数
    function render(element, data, inFor) {
        if (!element || !data) return;

        // 处理注释节点
        if (element.nodeType === 8) {
            renderComm(element, data);
            return;
        }

        // 处理文本节点
        if (element.nodeType === 3) {
            renderText(element, data);
            return;
        }

        // 处理元素节点
        if (element.nodeType === 1) {
            // 排除 for item 已渲染元素
            if (element.hasAttribute('data-for-id')
            && !element.hasAttribute('data-for')
            && !inFor ) {
                return;
            }

            // 处理属性
            renderAttr(element, data);

            // 处理 if 指令
            if (element.hasAttribute('data-if' )) {
                renderIf (element, data);
                return;
            }

            // 处理 for 指令
            if (element.hasAttribute('data-for')) {
                renderFor(element, data);
                return;
            }

            // 递归处理子元素
            var children = element.childNodes;
            for (var i = 0; i < children.length; i++) {
                render(children[i], data);
            }
        }
    }

    // 处理注释
    function renderComm(element, data) {
        var temp = element.nodeValue.trim();
        if (temp.startsWith('{%') && temp.endsWith('%}')) {
            complete(temp.substring(2 , temp.length - 2).trim(), data, element);
        } else
        if (temp.startsWith('{{') && temp.endsWith('}}')) {
            hsFill.fill(element, evaluate(temp.substring(2 , temp.length - 2).trim(), data, element));
        }
    }

    // 处理文本
    function renderText(element, data) {
        var temp ;
        if (element.tempValue !== undefined) {
            temp = element.tempValue;
        } else {
            temp = element.nodeValue;
        }
        var text = inscribe(temp, data, element);
        if (element.nodeValue !== text) {
            element.nodeValue = text;
        if (element.tempValue === undefined) {
            element.tempValue = temp;
        }}
    }

    // 处理属性
    function renderAttr(element, data) {
        var temps = element.tempAttrs || {};
        var attrs = element.attributes;
        var i = 0, j = 0;
        for(; i < attrs.length; i++) {
            var attr = attrs[i];
            var temp ;
            if (temps[attr.name] !== undefined) {
                temp = temps[attr.name];
            } else {
                temp = attr.value;
            }
            var text = inscribe(temp, data, element );
            if (element.getAttribute(attr.name) !== text) {
                element.setAttribute(attr.name, text);
            if (temps[attr.name] === undefined) {
                temps[attr.name] = temp;
                j ++;
            }}
        }
        // 有变更才需要更新记录
        if (j > 0) {
            element.tempAttrs = temps;
        }
    }

    // 处理 if 指令
    function renderIf(element, data) {
        var condition = element.getAttribute('data-if');
        var evaluated = evaluate(condition, data);

        // 记录原始的 display 值
        var oldDisplay = element.getAttribute('data-display');
        if (! oldDisplay) {
            oldDisplay = element.style.display || getComputedStyle(element).display || '-';
            element.setAttribute('data-display', oldDisplay );
        }
        if (oldDisplay === '-') {
            oldDisplay = '';
        }

        if (evaluated) {
            // 条件为真，显示元素
            element.style.display = oldDisplay;
            // 渲染子元素
            var children = element.childNodes;
            for (var i = 0; i < children.length; i++) {
                render(children[i], data);
            }
        } else {
            // 条件为假，隐藏元素
            element.style.display = 'none';
        }
    }

    // 处理 for 指令
    function renderFor(element, data) {
        var forExpr = element.getAttribute('data-for');
        var match = forExpr.match(/(\w+)\s+in\s+(.+)/);
        if (!match) return;

        var itemName = match[1];
        var listExpr = match[2];
        var list = evaluate(listExpr, data);

        if (!list) return;

        // 生成随机的 for-id
        var forId = element.getAttribute('data-for-id');
        if (! forId) {
            forId = 'for-' + Math.random().toString(36).substring(2);
            element.setAttribute('data-for-id' , forId);
        }

        // 记录原始的 display 值
        var oldDisplay = element.getAttribute('data-display');
        if (! oldDisplay) {
            oldDisplay = element.style.display || getComputedStyle(element).display || '-';
            element.setAttribute('data-display', oldDisplay );
        }
        if (oldDisplay === '-') {
            oldDisplay = '';
        }

        // 隐藏 data-for 节点
        element.style.display = 'none';

        var reserve = element.hasAttribute("data-for-reserve");
        var reverse = element.hasAttribute("data-for-reverse");

        // 插入定位
        var endElement = element;
        var parElement = element.parentNode;
        if (! reserve) {
            // 非保留模式, 清理已渲染的节点
            var curr = element.nextSibling;
            var next = curr;
            while (next) {
                curr = next;
                next = curr.nextSibling;
                if (curr.nodeType === 1 && curr.getAttribute('data-for-id') === forId) {
                    parElement.removeChild(curr);
                }
            }
        } else
        if (! reverse) {
            // 保留但非逆序模式, 定位到最后
            var curr = element.nextSibling;
            var next = curr;
            while (next) {
                curr = next;
                next = curr.nextSibling;
                if (curr.nodeType === 1 && curr.getAttribute('data-for-id') === forId) {
                    endElement = curr;
                }
            }
        }
        if (reverse) {
            // 逆序必须从下个节点开始
            // 否则 render 遍历到下个还是它
            // 这样会导致无限循环渲染
            endElement = element.nextSibling;
        }

        // 保存原始元素作为模板
        var template = element.cloneNode(true);

        // 处理数组
        if (Array.isArray(list)) {
            for(var i = 0; i < list.length; i ++) {
                data._index = i; // 隐含索引
                data[itemName] = list[i];

                // 创建新元素
                var newElement = template.cloneNode(true);
                newElement.removeAttribute('data-for');
                newElement.setAttribute('data-for-id' , forId);
                newElement.style.display = oldDisplay ;

                // 渲染新元素
                render(newElement, data, true);

                // 添加新元素
                parElement.insertBefore(newElement, reverse ? endElement : endElement.nextSibling);
                endElement = newElement;
            }
        }
        // 处理对象
        else if (typeof list === 'object') {
            var keys = Object.keys( list );
            for(var i = 0; i < keys.length; i ++) {
                var k = keys[i];
                var v = list[k];
                data._index = i; // 隐含索引
                data._value = v; // 隐含取值
                data[itemName] = k;

                // 创建新元素
                var newElement = template.cloneNode(true);
                newElement.removeAttribute('data-for');
                newElement.setAttribute('data-for-id' , forId);
                newElement.style.display = oldDisplay ;

                // 渲染新元素
                render(newElement, data, true);

                // 添加新元素, 在最后一个元素后面
                element.parentNode.insertBefore(newElement, reverse ? endElement : endElement.nextSibling);
                endElement = newElement;
            }
        }
    }

    // 插值处理
    function inscribe(text, data, node) {
        return text.replace(/\{\{\s*(.*?)\s*\}\}/g, function (mat, expr) {
            var value = evaluate(expr, data, node);
            return value !== undefined ? value : expr;
        });
    }

    // 表达式求值
    function evaluate(expr, data, node) {
        try {
            // data 展开作为函数参数表
            var keys = Reflect.ownKeys(data);
            var vals = keys.map(function(kn) {return data[kn];});
            var func = new Function(keys, 'return ('+ expr +')');
            return func.apply(node, vals);
        } catch (e) {
            console.error('Wrong expression:', e.message, '|', expr, data);
            return undefined;
        }
    }

    // 执行表达式
    function complete(expr, data, node) {
        try {
            // data 展开作为函数参数表
            var keys = Reflect.ownKeys(data);
            var vals = keys.map(function(kn) {return data[kn];});
            var func = new Function(keys, expr);
            func.apply(node, vals);
        } catch (e) {
            console.error('Wrong expression:', e.message, '|', expr, data);
            return undefined;
        }
    }

    // 开始渲染
    render(element, data);
}
hsFill.fill = function(node, cont) {
    if (! node.parentNode) {
        return;
    }
    if (! cont && cont !== 0) {
        return;
    }

    // 转为 NodeList
    if (typeof cont == 'number') {
        cont = document.createTextNode(cont.toString());
    } else
    if (typeof cont == 'string') {
        if (cont.startsWith('<') && cont.endsWith('>')) {
            cont = hsFill.html(cont);
        } else {
            cont = hsFill.Text(cont);
        }
    }
    if (! hsFill.isArr(cont)) {
        cont = [cont];
    }

    // 清理旧节点
    var olds = node.tempNodes;
        node.tempNodes = cont;
    if (olds)
    for(var i = 0; i < olds.length; i ++) {
        var item = olds[i] ;
        if (item.parentNode) {
            item.parentNode.removeChild(item);
        }
    }

    // 写入新节点
    var last = node;
    for(var i = 0; i < cont.length; i ++) {
        var item = cont[i] ;
        node.parentNode.insertBefore(item, last.nextSibling);
        last = item;
    }
};
hsFill.html = function(html) {
    var div = document.createElement( 'div' );
    div.innerHTML = html;
    var cont = [];
    var children = div.childNodes;
    for(var i = 0; i < children.length; i ++) {
        cont.push(children[i]);
    }
    return cont;
};
hsFill.text = function(text) {
    var div = document.createElement( 'div' );
    div.innerText = text;
    var cont = [];
    var children = div.childNodes;
    for(var i = 0; i < children.length; i ++) {
        cont.push(children[i]);
    }
    return cont;
};
hsFill.isArr = function (obj) {
    return Array.isArray(obj) || (obj && typeof obj === 'object' && obj.length !== undefined && obj.nodeType === undefined); // 最后一个规避 CommentNode
};

/**
 * 视图组件
 * 构造：var view = new HsView(element)
 * 首次填充：view.fill({title: 'Hello'})
 * 二次填充：view.fill({topic: 'World'}) // 自动合并数据并更新视图
 * @param {Element|NodeList|jQuery} context
 */
function HsView(context) {
    this.context = context;
}
HsView.prototype = {
    data: {},
    /**
     * 填充数据
     * @param {Object} data
     */
    fill: function(data) {
        // 合并数据, 内联绑定
        data = this.proxy(this.merge(this.data, data));

        // 渲染所有的模板块
        hsFill(this.context, data);
    },
    /**
     * 数据代理
     * @param {Object} data
     * @returns {Proxy}
     */
    proxy: function(data) {
        var dist = {};
        var prox = new Proxy(dist, {
            set: function (dist, key, val) {
                dist[key] = val; // 变量暂存, 用完即弃
            },
            has: function (dist, key) {
                return  (key in dist)
                    ||  (key in data);
            },
            get: function (dist, key) {
                if (key in dist) {
                    return dist [key];
                } else {
                    return data [key];
                }
            },
            ownKeys: function (dist) {
                var dataKeys = Reflect.ownKeys(data);
                var distKeys = Reflect.ownKeys(dist);
                var keys = dataKeys.concat(distKeys);
                return Array.from( new Set( keys ) );
            },
            getOwnPropertyDescriptor: function(dist, key) {
                return {
                  configurable: true,
                    enumerable: true
                };
            }
        });
        dist.__ = this;
        dist._  = prox;
        return prox;
    },
    /**
     * 合并数据
     * @param {Object} target
     * @param {Object} source
     */
    merge: function(target, source) {
        if (source) for (var key in source) {
            if (source.hasOwnProperty(key)) {
                if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
                    if (! target[key]) target[key] = { };
                    this.merge(target[key], source[key]);
                } else {
                    target[key] = source[key];
                }
            }
        }
        return target;
    }
};