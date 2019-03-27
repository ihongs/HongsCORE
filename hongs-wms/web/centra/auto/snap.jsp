<%@page import="io.github.ihongs.Cnst"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    String _pageId = (_module + "_" + _entity + "_logs").replace('/', '_');
%>
<h2><%=_locale.translate("fore.record.title", _title)%></h2>
<div id="<%=_pageId%>" class="logs-list">
    <form class="findbox">
        <ul class="nav nav-tabs board clearfix">
            <li class="active"><a href="javascript:;"><b>全部记录</b></a></li>
            <li data-state="0"><a href="javascript:;"><b>回收站</b></a></li>
            <li data-state="1"><a href="javascript:;"><b>新增</b></a></li>
            <li data-state="2"><a href="javascript:;"><b>更新</b></a></li>
            <li data-state="3"><a href="javascript:;"><b>恢复</b></a></li>
            <div class="form-inline pull-right">
                <input type="hidden" name="state"/>
                <input type="hidden" name="etime"/>
                <!--
                <div class="form-group" style="margin-right:0.5em;">
                    <input type="search" name="wd" class="form-control" placeholder="名称、备注">
                </div>
                //-->
                <div class="form-group" style="margin-right:0.5em;">
                    <div class="input-group">
                        <input type="date" name="ctime:ge" data-type="timestamp" data-toggle="hsTime" class="form-control" style="padding-right:0;width:11em;">
                        <span class="input-group-addon" style="padding-left:0.2em;padding-right:0.2em;">~</span>
                        <input type="date" name="ctime:le" data-type="timestamp" data-toggle="hsTime" class="form-control" style="padding-right:0;width:11em;">
                    </div>
                </div>
                <button type="submit" class="btn btn-default"><span class="glyphicon glyphicon-search"></span></button>
            </div>
        </ul>
    </form>
    <div class="table-responsive-revised">
    <div class="table-responsive listbox">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="ctime" data-ft="time" class="_htime">记录时间</th>
                    <th data-fn="state" data-ft="stat" style="width:4em;">行为</th>
                    <th data-fn="user">用户</th>
                    <th data-fn="name">资源标题</th>
                    <th data-fn="memo">操作备注</th>
                    <th data-fn="etime" data-ft="time" class="_htime">截止时间</th>
                    <th data-fn="rtime" data-ft="time" class="_htime">恢复起源</th>
                    <th data-fn="_" data-ft="_admin" style="width:4.5em;">操作
                        <div class="invisible">
                            <a href="javascript:;" class="review"><span class="glyphicon glyphicon-eye-open" title="查看快照"></span></a>
                            <span style="margin-left:1em;"></span>
                            <a href="javascript:;" class="revert"><span class="glyphicon glyphicon-refresh " title="恢复记录"></span></a>
                        </div>
                    </th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div></div>
    <div class="pagebox clearfix"></div>
</div>

<script type="text/javascript">
    (function($) {
        var statcol = {'0':"text-danger", '1':"text-default", '2':"text-success", '3':"text-warning"};
        var statmap = {'0':"删除", '1':"更新", '2':"新增", '3':"恢复"};
        var context = $('#<%=_pageId%>').removeAttr("id");
        var sendbox = context.find(".sendbox");
        var listobj = context.hsList({
            loadUrl : "<%=_module%>/<%=_entity%>/revert/search.act?<%=Cnst.ID_KEY%>.=$<%=Cnst.ID_KEY%>&<%=Cnst.OB_KEY%>=-ctime&<%=Cnst.RB_KEY%>=-data,user.*",
            send    : hsSendWithMemo ,
            _fill_time: function( td , time) {
                td.parent().data(this._info).addClass(statcol[ this._info.state + '' ]);
                return ! time || time == '0' ? '-' : this._fill__htime(td, time * 1000);
            },
            _fill_user: function( td , user) {
                return   user  . name || '-';
            },
            _fill_stat: function( td , stat) {
                return  statmap [ '' + stat];
            }
        });

        // 权限检查
        if (! hsChkUri("<%=_module%>/<%=_entity%>/revert/update.act")) {
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
            listobj.send (tr, ms, "<%=_module%>/<%=_entity%>/revert/update.act", {id: id, rtime: ct});
        });

        context.on("click", ".review", function() {
            var lo = context.hsFind ("@" );
            var tr = $(this).closest("tr");
            var id =      tr.data(   "id");
            var ct =      tr.data("ctime");
            listobj.open (tr, lo, "<%=_module%>/<%=_entity%>/info_snap.html"   , {id: id, ctime: ct});
        });

        context.on("click", ".nav li", function() {
            if ($(this).is(".active")) return;
            $(this).   addClass("active")
                   .siblings(  )
                   .removeClass("active");
            var fd = $(this).siblings("div");
            var fv = $(this).data  ("state");
            if (fv) {
                fd.find("[name=state]").val(fv );
                fd.find("[name=etime]").val("0");
            } else {
                fd.find("[name=state]").val("" );
                fd.find("[name=etime]").val("" );
            }
        });

        sendbox.modal({show : false});
    })(jQuery);
</script>
