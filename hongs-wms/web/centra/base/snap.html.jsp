<%@page import="io.github.ihongs.Cnst"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _pageId = (_module + "-" + _entity + "-snap").replace('/', '-');
%>
<h2><%=_locale.translate("fore.record.title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId%> snap-list board board-end">
    <form class="findbox">
        <ul class="nav nav-tabs board clearfix">
            <li class="active">
                <a href="javascript:;"><b>全部记录</b></a>
            </li>
            <li data-state="0" data-etime="0">
                <a href="javascript:;"><b> 回收站 </b></a>
            </li>
            <li class="dropdown">
                <a href="javascript:;" data-toggle="dropdown"><b>行为</b><i class="caret"></i></a>
                <ul class="dropdown-menu sta-menu">
                    <li data-state="1"><a href="javascript:;"><b>新增</b></a></li>
                    <li data-state="2"><a href="javascript:;"><b>更新</b></a></li>
                    <li data-state="0"><a href="javascript:;"><b>删除</b></a></li>
                    <li data-state="3"><a href="javascript:;"><b>恢复</b></a></li>
                </ul>
            </li>
            <div class="form-inline pull-right" style="line-height: 0px;">
                <input type="hidden" name="state"/>
                <input type="hidden" name="etime"/>
                <div class=" form-group" style="display:inline-block;margin:0px;">
                <div class="input-group" style="display:inline-table;width:30em;">
                    <input type="datetime-local" data-type="timestamp" data-toggle="hsTime" class="form-control" style="padding-right:0;">
                    <input type="hidden" class="form-ignored" name="ctime.<%=Cnst.GE_REL%>">
                    <span class="input-group-addon" style="padding-left:0.2em;padding-right:0.2em;">~</span>
                    <input type="datetime-local" data-type="timestamp" data-toggle="hsTime" class="form-control" style="padding-right:0;">
                    <input type="hidden" class="form-ignored" name="ctime.<%=Cnst.LE_REL%>">
                    <span class="input-group-btn">
                        <button type="submit" class="btn btn-default"><span class="bi bi-hi-reload"></span></button>
                    </span>
                </div>
                </div>
            </div>
        </ul>
    </form>
    <div class="table-responsive-revised">
    <div class="table-responsive listbox">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="_" data-ft="_admin" class="_admin _amenu">
                        <div class="dropdown invisible">
                            <a href="javascript:;" data-toggle="dropdown"><span class="bi bi-hi-action"></span></a>
                            <ul class="dropdown-menu adm-menu">
                                <li><a href="javascript:;" class="review">查看</a></li>
                                <li><a href="javascript:;" class="revert">恢复</a></li>
                                <li class="divider"></li>
                                <li><a href="javascript:;" class="same-r">同资源的</a></li>
                                <li><a href="javascript:;" class="same-u">同用户的</a></li>
                                <li><a href="javascript:;" class="same-m">同终端的</a></li>
                            </ul>
                        </div>
                    </th>
                    <th data-fn="ctime" data-ft="time" class="sortable" style="width: 7em;">时间</th>
                    <th data-fn="user">用户</th>
                    <th data-fn="state" data-ft="stat" class="sortable" style="width: 4em;">行为</th>
                    <th data-fn="name">资源题要</th>
                    <th data-fn="memo">操作备注</th>
                    <th data-fn="meno">终端标识</th>
                    <th data-fn="etime" data-ft="time" class="sortable" style="width: 7em;">截止时间</th>
                    <th data-fn="rtime" data-ft="time" class="sortable" style="width: 7em;">恢复起源</th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
    </div>
    <div class="pagebox clearfix"></div>
</div>

<script type="text/javascript">
(function($) {
    var statmap = {
        '0': [ "text-danger" , "删除" ],
        '1': [ "text-default", "新增" ],
        '2': [ "text-default", "更新" ],
        '3': [ "text-success", "恢复" ]
    };

    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var loadpms = hsSerialArr(loadbox);

    context.find("[name='ctime.<%=Cnst.GE_REL%>']").val(hsGetSeria(loadpms, "ctime_ge"));
    context.find("[name='ctime.<%=Cnst.LE_REL%>']").val(hsGetSeria(loadpms, "ctime_le"));

    var listobj = context.hsList({
        loadUrl : "<%=_module%>/<%=_entity%>/reveal.act?<%=Cnst.ID_KEY%>.=$<%=Cnst.ID_KEY%>&<%=Cnst.OB_KEY%>=ctime!,etime&<%=Cnst.RB_KEY%>=data!,user.*&user=$user&meno=$meno",
        send    : hsSendWithMemo,
        _fill_stat: function(td , stat) {
            var st = statmap['' + stat];
            td.parent( ).data(this._info).addClass(st[0]);
            return st[1];
        },
        _fill_time: function(td , time) {
            if (time && time != 0) {
                return this._fill__htime(td, time * 1000);
            }
            return '-';
        },
        _fill_user: function(td , user) {
            if (user && user.name) {
                return  user.name;
            }
            return '-';
        }
    });

    // 独立记录
    if (hsGetSeria(loadpms,  "id"  )
    ||  hsGetSeria(loadpms, "user")) {
        context.find("ul.nav-tabs>li:eq(1),ul.adm-menu>li:gt(1)")
               .hide();
    }

    // 权限检查
    if (! hsChkUri("<%=_module%>/<%=_entity%>/revert.act")) {
        var btn = context.find(".revert");
        var spn = btn.siblings(  "span" );
        btn.remove();
        spn.remove();
    }

    context.on("click", ".revert", function() {
        var ms = "确定恢复到此版本吗?";
        var tr = $(this).closest("tr");
        var id =      tr.data(   "id");
        var ct =      tr.data("ctime");
        if (0  ==     tr.data("etime")) ct = 0;
        listobj.send (tr, ms, "<%=_module%>/<%=_entity%>/revert.act"    , {id: id, rtime: ct});
    });

    context.on("click", ".review", function() {
        var lo = context.hsFind ("@" );
        var tr = $(this).closest("tr");
        var id =      tr.data(   "id");
        var ct =      tr.data("ctime");
        listobj.open (tr, lo, "<%=_module%>/<%=_entity%>/info_snap.html", {id: id, ctime: ct});
    });

    context.on("click", ".same-r", function() {
        var lo = context.hsFind ("@" );
        var tr = $(this).closest("tr");
        var tt = $(this).text   (    );
        var id =    tr.data(     "id");
        var ge = context.find("[name='ctime.<%=Cnst.GE_REL%>']").val();
        var le = context.find("[name='ctime.<%=Cnst.LE_REL%>']").val();
        lo.hsOpen ("<%=_module%>/<%=_entity%>/snap.html", { id : id, ctime_ge: ge, ctime_le: le}, function() { $(this).hsL10n(tt); });
    });

    context.on("click", ".same-u", function() {
        var lo = context.hsFind ("@" );
        var tr = $(this).closest("tr");
        var tt = $(this).text   (    );
        var id =    tr.data("user_id");
        var ge = context.find("[name='ctime.<%=Cnst.GE_REL%>']").val();
        var le = context.find("[name='ctime.<%=Cnst.LE_REL%>']").val();
        lo.hsOpen ("<%=_module%>/<%=_entity%>/snap.html", {user: id, ctime_ge: ge, ctime_le: le}, function() { $(this).hsL10n(tt); });
    });

    context.on("click", ".same-m", function() {
        var lo = context.hsFind ("@" );
        var tr = $(this).closest("tr");
        var tt = $(this).text   (    );
        var id =    tr.data(   "meno");
        var ge = context.find("[name='ctime.<%=Cnst.GE_REL%>']").val();
        var le = context.find("[name='ctime.<%=Cnst.LE_REL%>']").val();
        lo.hsOpen ("<%=_module%>/<%=_entity%>/snap.html", {meno: id, ctime_ge: ge, ctime_le: le}, function() { $(this).hsL10n(tt); });
    });

    context.on("click", ".nav li", function() {
        if ( $(this).is(".active,.dropdown")) {
            return;
        }

        var fv = $(this).attr("data-state") || "";
        var fe = $(this).attr("data-etime") || "";
        var fd = $(this).closest("form");
        fd.find("[name=state]").val(fv );
        fd.find("[name=etime]").val(fe );
        fd.find(":submit").click();

        $(this).closest(".nav").find(".active")
            .removeClass("active");
        $(this).closest(".dropdown")
               .addClass("active");
        $(this).addClass("active");
    });
})(jQuery);
</script>
