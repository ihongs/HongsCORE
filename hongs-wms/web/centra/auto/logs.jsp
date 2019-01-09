<%@page import="io.github.ihongs.Cnst"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    String _pageId = (_module + "_" + _entity + "_logs").replace('/', '_');
%>
<h2><%=_locale.translate("fore.record.title", _title)%></h2>
<div id="<%=_pageId%>" class="logs-list">
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="name">名称</th>
                    <th data-fn="memo">备注</th>
                    <th data-fn="user.name" style="width: 8.5em;">操作人员</th>
                    <th data-fn="ctime" data-ft="time" class="_htime">记录时间</th>
                    <th data-fn="etime" data-ft="time" class="_htime">截止时间</th>
                    <th data-fn="rtime" data-ft="time" class="_htime">恢复起源</th>
                    <th data-fn="state" data-ft="stat" style="width:4em;">状态</th>
                    <th data-fn="" data-ft="_admin" style="width: 6.5em;">
                        操作
                        <div class="invisible">
                            <a href="javascript:;" class="review">详情</a>
                            <span style="margin-left:1em;"></span>
                            <a href="javascript:;" class="revert">恢复</a>
                        </div>
                    </th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
    <div class="pagebox"></div>
    <!-- 恢复确认及备注弹框 -->
    <div class="sendbox modal fade" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">确定恢复到此版本吗?</h4>
                </div>
                <div class="modal-body"  >
                    <input name="memo" type="text" class="form-control" placeholder="备注"/>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-info commit" data-dismiss="modal">确定</button>
                    <button type="button" class="btn btn-link closes" data-dismiss="modal">取消</button>
                </div>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    (function($) {
        var context = $('#<%=_pageId%>').removeAttr("id");
        var sendbox = context.find(".sendbox");
        var listobj = context.hsList({
            loadUrl : "<%=_module%>/<%=_entity%>/revert/search.act?<%=Cnst.ID_KEY%>.=$<%=Cnst.ID_KEY%>&<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.RB_KEY%>=-data,user.*&<%=Cnst.OB_KEY%>=-ctime",
            _fill_stat: function( td , stat) {
                td.parent().data(this._info);
                return ! stat || stat == '0' ? "删除" : "正常";
            },
            _fill_time: function( td , time) {
                return ! time || time == '0' ? '-' : this._fill__htime(td, time * 1000);
            }
        });

        // 权限检查
        if (! hsChkUri("<%=_module%>/<%=_entity%>/revert/update.act")) {
            var btn = context.find(".revert");
            var spn = btn.siblings(  "span" );
            btn.remove();
            spn.remove();
        }

        context.on("click", ".review", function() {
            var lo = context.hsFind ("@" );
            var tr = $(this).closest("tr");
            var id =      tr.data(   "id");
            var ct =      tr.data("ctime");
            listobj.open (tr, lo, "<%=_module%>/<%=_entity%>/info_logs.html?<%=Cnst.AB_KEY%>=_text,_fork", {id: id, ctime: ct});
        });

        sendbox.on("click", ".commit", function() {
            var mt = sendbox.find("input").val ();
            var tr = sendbox.data(   "tr");
            var id =      tr.data(   "id");
            var ct =      tr.data("ctime");
            listobj.send (tr, "", "<%=_module%>/<%=_entity%>/revert/update.act", {id: id, rtime: ct, memo: mt});
        });

        context.on("click", ".revert", function() {
            var tr = $(this).closest("tr");
            sendbox.data (   "tr" ,   tr );
            sendbox.find ("input").val("");
            sendbox.modal( "show");
        });
        
        sendbox.modal({show : false});
    })(jQuery);
</script>
