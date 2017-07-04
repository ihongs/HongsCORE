<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    // 获取路径动作
    int i;
    String _module, _entity;
    _module = ActionDriver.getWorkPath(request);
    i = _module.lastIndexOf('/');
    _module = _module.substring(1, i);
    i = _module.lastIndexOf('/');
    _entity = _module.substring(i+ 1);
    _module = _module.substring(0, i);
    
    String id, nm = "";
    id = (_module +"-"+ _entity +"-logs").replace('/', '-');
    do {
        NaviMap site = NaviMap.hasConfFile(_module+"/"+_entity)
                     ? NaviMap.getInstance(_module+"/"+_entity)
                     : NaviMap.getInstance(_module);
        Map menu  = site.getMenu(_module+"/#"+_entity);
        if (menu != null) {
            nm = (String) menu.get("text");
            if (nm != null) {
                nm  = site.getCurrTranslator( ).translate( nm );
                break;
            }
        }

        FormSet form = FormSet.hasConfFile(_module+"/"+_entity)
                     ? FormSet.getInstance(_module+"/"+_entity)
                     : FormSet.getInstance(_module);
        Map flds  = form.getForm(_entity);
        if (flds != null) {
            nm = (String) Dict.get( flds , null , "@" , "text");
            if (nm != null) {
                nm  = site.getCurrTranslator( ).translate( nm );
                break;
            }
        }
    } while (false);
%>
<h2><%=nm%>历史</h2>
<div id="<%=id%>">
    <div class="listbox table-responsive">
        <table class="table table-hover table-striped">
            <thead>
                <tr>
                    <th data-fn="name">名称</th>
                    <th data-fn="note">备注</th>
                    <th data-fn="ctime" data-ft="_htime" data-fl="v*1000" class="_htime">记录时间</th>
                    <th data-fn="etime" data-ft="_htime" data-fl="v*1000" class="_htime">截止时间</th>
                    <th data-fn="rtime" data-ft="_htime" data-fl="v*1000" class="_htime">恢复起源</th>
                    <th data-fn="state_text">状态</th>
                    <th data-fn="" data-ft="_admin">操作
                        <div class="invisible">
                            <a href="javascript:;" class="review">详情</a>
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
            <input  type="text" name="note" placeholder="备注" class="form-control"/>
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
        var context = $('#<%=id%>').removeAttr("id");
        var loadbox = context.closest(".loadbox");
        var sendbox = context.find(".sendbox");
        
        var listObj = context.hsList({
            loadUrl : "<%=_module%>/<%=_entity%>/revert/search.act?id.eq="+H$("&id", loadbox)+"&ob=-ctime&rb=-data",
            openUrls: [
                [function(ln) {
                    var tr = ln.closest("tr");
                    var id = tr.data("id");
                    var ct = tr.data("ctime");
                    this.open(ln, ln.hsFind("@"),
                      "<%=_module%>/<%=_entity%>/logs_info.html?id="+id+"&ctime="+ct);
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
            var nt = sendbox.find("[name=note]").val();
            var ln = sendbox.data("ln");
            var tr = ln.closest("tr");
            var id = tr.data("id");
            var rt = tr.data("ctime");
            listObj.send(
                ln, "", "<%=_module%>/<%=_entity%>/revert/update.act",
                { id: id, note: nt, rtime: rt }
            );
            sendbox.modal("hide");
        });
    })(jQuery);
</script>
