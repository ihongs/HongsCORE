<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_base_.jsp"%>
<%
    String _pageId = (_module + "_" + _entity + "_logs").replace('/', '_');
%>
<h2><%=_locale.translate("fore.revert.title", _title)%></h2>
<div id="<%=_pageId%>">
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="name">名称</th>
                    <th data-fn="memo">备注</th>
                    <th data-fn="user.name">操作人员</th>
                    <th data-fn="ctime" data-ft="_htime" data-fl="v*1000" class="_htime">记录时间</th>
                    <th data-fn="etime" data-ft="_htime" data-fl="v*1000" class="_htime">截止时间</th>
                    <th data-fn="rtime" data-ft="_htime" data-fl="v*1000" class="_htime">恢复起源</th>
                    <th data-fn="state_text" style="width:3.5em;">状态</th>
                    <th data-fn="" data-ft="_admin" style="width:6em;">
                        操作
                        <div class="invisible">
                            <a href="javascript:;" class="review">详情</a>
                            <span style="margin-left:0.5em;"></span>
                            <a href="javascript:;" class="revert">恢复</a>
                        </div>
                    </th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
    <div class="pagebox"></div>
    
    <div class="sendbox modal fade" tabindex="-1" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
            <h4 class="modal-title">确定恢复到此版本吗?</h4>
          </div>
          <div class="modal-body">
            <input  type="text" name="memo" placeholder="备注" class="form-control"/>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-primary">确定</button>
            <button type="button" class="btn btn-link" data-dismiss="modal">取消</button>
          </div>
        </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
    
</div>

<script type="text/javascript">
    (function($) {
        var context = $('#<%=_pageId%>').removeAttr("id");
        var sendbox = context.find(".sendbox");
        
        var listObj = context.hsList({
            loadUrl : "<%=_module%>/<%=_entity%>/revert/search.act?id!eq=\${id}&ob=-ctime&rb=-data,user.*",
            openUrls: [
                [function(ln) {
                    var tr = ln.closest("tr");
                    var id = tr.data("id");
                    var ct = tr.data("ctime");
                    this.open(ln, ln.hsFind("@"),
                      "<%=_module%>/<%=_entity%>/info_logs.html?ab=_enum,_fork&id="+id+"&ctime="+ct);
                }, ".review"],
                [function(ln) {
                    sendbox.data ("ln", ln);
                    sendbox.modal( "show" );
                }, ".revert"]
            ],
            _fill_name: function( td, v ) {
                td.parent().data("id"   , this._info.id   );
                td.parent().data("ctime", this._info.ctime);
                return v;
            }
        });
        
        sendbox.modal({show: false});
        sendbox.on("click", ".btn-primary", function() {
            var nt = sendbox.find("[name=memo]").val();
            var ln = sendbox.data("ln");
            var tr = ln.closest("tr");
            var id = tr.data("id");
            var rt = tr.data("ctime");
            listObj.send(
                ln, "", "<%=_module%>/<%=_entity%>/revert/update.act",
                { id: id, memo: nt, rtime: rt }
            );
            sendbox.modal("hide");
        });
    })(jQuery);
</script>
