# HongsCORE framework for Javascript

* 文档版本: 15.09.20
* 软件版本: 0.3.8-20151030
* 设计作者: 黄弘(Kevin Hongs)
* 技术支持: kevin.hongs@gmail.com

本工具集与 HongsCORE(Java) 配套使用, 使用 jQuery 作为核心辅助库, 使用 Bootstrap 作为 UI 库. 源码为 src 下以 hongs- 开头的文件, 以 hs 开头的为普通函数, 以 Hs 开头的为伪类函数, this 指向调用的容器对象.

## 属性缩写:

    List:
        data-fn     Field name  字段名称
        data-ft     Field type  字段类型
        data-fl     Fill lambda 填充取值
        data-pn     Page num    页码序号
        data-ob     Order by    排序字段
    Form:
        data-fn     Field name  字段名称
        data-ft     Field type  字段类型
        data-fl     Fill lambda 填充取值
        data-dl     Doll lambda 预置选项
    Pick:
        data-vk     Value key   关联数据取值键
        data-tk     Title key   关联数据标题键
        data-ak     Assoc key   关联数据键
        data-al     Assoc lnk   关联选取页
        data-rl     Refer lnk   关联查阅页
    Part:
        data-al     Assoc lnk   关联表单页
        data-rl     Refer lnk   关联详情页
    Checkbox:
        data-vk     Value key   选项数据取值键
        data-tk     Title key   选项数据标签键
    Checkset:
        data-vl     Value leg   子项数据取值键
        data-tl     Title leg   子项数据标题键

data-fl,data-dl 的取值为表达式, 参数可使用 this,form|list,v,n; data-toggle,data-target 等的意义同 bootstrap 中相关功能.

## 环境加载

```html
    <base href="../">
    <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
    <script type="text/javascript" src="static/assets/jquery.min.js"></script>
    <script type="text/javascript" src="static/assets/jquery-ui.min.js"></script>
    <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
    <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
    <script type="text/javascript" src="common/conf/default.js"></script>
    <script type="text/javascript" src="common/lang/default.js"></script>
    <script type="text/javascript" src="common/auth/default.js"></script>
```

注: 将以上代码加入 head 中, 注意 link 的 href 和 script 的 src 路径; 这里用 base 定义了基础路径, 以下的相对路径均基于此.

## HsList 列表组件的用法

```html
    <div id="master-user-list"
         data-topple="hsList"
         data-load-url="centra/master/user/list.act?unit_id=${unit_id}"
         data-send-urls-0="['centra/master/user/delete.act','.delete','您确定要删除此用户?']"
         data-open-urls-0="['centra/master/user/form.html?unit_id=${unit_id}','.create','@']"
         data-open-urls-1="['centra/master/user/form.html?id={ID}'           ,'.modify','@']">
        <div class="row board">
            <div class="toolbox col-md-8 btn-group">
                <button type="button" class="create btn btn-default">创建用户</button>
                <button type="button" class="modify for-choose btn btn-default">修改</button>
                <button type="button" class="delete for-checks btn btn-warning">删除</button>
            </div>
            <form class="findbox col-md-4 input-group" action="" method="POST">
                <input type="search" name="wd" class="form-control input-search"/>
                <span class="input-group-btn">
                    <button type="submit" class="btn btn-default">查找</button>
                </span>
            </form>
        </div>
        <div class="listbox table-responsive">
            <table class="table table-hover table-striped">
                <thead>
                    <tr>
                        <th data-fn="id[]" data-ft="_check" class="_check">
                            <input type="checkbox" class="checkall" name="id[]"/>
                        </th>
                        <th data-fn="name" class="sortable">名称</th>
                        <th data-fn="username" class="sortable">账号</th>
                        <th data-fn="mtime" data-ft="_htime" class="_htime sortable">修改时间</th>
                        <th data-fn="ctime" data-ft="_htime" class="_htime sortable">创建时间</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
        <div class="pagebox"></div>
    </div>
```

## HsTree 树型组件的用法

```html
    <div id="master-unit-tree"
         data-topple="hsTree"
         data-load-url="centra/master/unit/list.act"
         data-send-urls-0="['centra/master/unit/delete.act','.delete','您确定要删除此部门?']"
         data-link-urls-0="['centra/master/user/list.html?unit_id={ID}','.main-context']"
         data-open-urls-0="['centra/master/unit/form.html?pid={ID}','.create','@']"
         data-open-urls-1="['centra/master/unit/form.html?id={ID}' ,'.modify','@']"
         data-root-name="组织架构">
        <div class="toolbox btn-group board">
            <button type="button" class="create btn btn-default">添加</button>
            <button type="button" class="modify for-select btn btn-default">修改</button>
            <button type="button" class="delete for-select btn btn-warning">删除</button>
        </div>
        <div class="treebox"></div>
   </div>
```

## HsForm 表单组件的用法

```html
    <h2>{DO}部门</h2>
    <div id="master-unit-form"
         data-topple="hsForm"
         data-load-url="centra/master/unit/info.act?id=${id}"
         data-save-url="centra/master/unit/save.act?id=${id}"
         data--0="initInfo:($(this).hsFind('%'))">
        <form action="" method="POST">
            <input type="hidden" name="pid"/>
            <div class="row">
                <div class="col-md-6 center-block">
                    <div class="form-group">
                        <label class="control-label">名称</label>
                        <input type="text" name="name" class="form-control" required="required"/>
                    </div>
                    <div class="form-group">
                        <label class="control-label">备注</label>
                        <textarea name="note" class="form-control"></textarea>
                    </div>
                    <div>
                        <button type="submit" class="commit btn btn-primary">提交</button>
                        <button type="button" class="cancel btn btn-default">取消</button>
                    </div>
                </div>
            </div>
        </form>
    </div>
```