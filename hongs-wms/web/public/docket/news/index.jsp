<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.action.ActionRunner"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
String ln = "article";
String id = Core.ACTION_NAME.get(  ).substring( 19 );
String ms = request.getHeader( "If-Modified-Since" );
if ("".equals(id) || "index.jsp".equals(id)) {
    throw new HongsException (0x1104, "ID required");
}

Data news = Data.getInstance("centre/data/docket/news", "news");
SimpleDateFormat hfmt = null;
SimpleDateFormat dfmt = null;
Map  info ;

// 更新判断
if (ms != null) {
    info = news.getOne(Synt.mapOf(
            "rb", Synt.setOf("mtime"),
            "id", id
    ));
    if (info == null || info.isEmpty()) {
        throw new HongsException(0x1104, "Not found !");
    }
    hfmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    hfmt.setTimeZone(TimeZone.getTimeZone( "GMT" ));
    long mtime = Synt.asLong( info.get ( "mtime" ));
    long xtime = hfmt.parse ( ms ).getTime() / 1000;
    if ( mtime == xtime ) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return;
    }
}

info = news.get(id);
if (info == null || info.isEmpty()) {
    throw new HongsException(0x1104, "Not found !");
}

if (hfmt == null) {
    hfmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    hfmt.setTimeZone(TimeZone.getTimeZone( "GMT" ));
}
if (dfmt == null) {
    dfmt = new SimpleDateFormat( "yyyy/MM/dd HH:mm" , Locale.US  );
    dfmt.setTimeZone(TimeZone.getTimeZone(Core.ACTION_ZONE.get()));
}

long xtime = Synt.asLong(info.get("mtime"));
Date mdate = new Date(xtime*1000);
String mtime = dfmt.format(mdate);
String htime = hfmt.format(mdate);
response.setHeader("Last-Modified", htime );
response.setHeader("ETag", id +":"+ xtime );

String author = (String) info.get("author");
String source = (String) info.get("source");

// 把标签串起来作为关键词
Set<String>   ts = Synt.declare(info.get("tags"), Set.class);
StringBuilder wd = new StringBuilder();
if (null  !=  wd)  for(String tx : ts) {
    wd.append(tx).append(",");
}   info.put ("word", wd.toString( ) );
%>
<!doctype html>
<html>
<head>
    <title><%=escape((String)info.get("name"))%></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="keywords" content="<%=escape((String)info.get("word"))%>">
    <meta name="description" content="<%=escape((String)info.get("note"))%>">
    <base href="<%=request.getContextPath()%>/" target="_blank"/>
    <link rel="icon" type="image/x-icon" href="favicon.ico"/>
    <link rel="stylesheet" type="text/css" href="static/assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="static/assets/css/hongscore.min.css"/>
    <!--[if glt IE8.0]>
    <script type="text/javascript" src="static/addons/respond/respond.min.js"></script>
    <![endif]-->
    <script type="text/javascript" src="static/assets/jquery.min.js"></script>
    <script type="text/javascript" src="static/assets/bootstrap.min.js"></script>
    <script type="text/javascript" src="static/assets/hongscore.min.js"></script>
    <script type="text/javascript" src="common/conf/default.js"></script>
    <script type="text/javascript" src="common/lang/default.js"></script>
    <style type="text/css">
        h1 {text-align: center;}
        .container>p>span+span {margin-left: 1em;}
        .comment-list .listbox p {white-space: pre-line;}
    </style>
</head>
<body>
    <div class="invisible">
        <img src="<%=info.get("logo")%>"/>
    </div>
    <div class="container">
        <h1><%=escape((String) info.get("name"))%></h1>
        <p class="text-center">
            <span><%=mtime%></span>
        <%if (author != null && !author.equals("")) {%>
            <span>作者: <%=escape(author)%></span>
        <%} /*End if*/%>
        <%if (source != null && !source.equals("")) {%>
            <span>来源: <%=escape(source)%></span>
        <%} /*End if*/%>
        </p>
        <div class="row">
            <div class="col-md-8">
                <%=info.get("body")%>
            </div>
            <div class="col-md-4">
                <!-- 评分打星 -->
                <div style="margin-bottom: 2em;">
                    <span id="endorse-score" style="padding-right: 0.5em;">
                    <%for (int i = 0; i < 5; i ++) {%>
                        <a href="javascript:;" class="glyphicon glyphicon-star-empty" data-score="<%=(i+1)%>"></a>
                    <%} /*End for*/%>
                    </span>
                    <span id="impress-count">1</span>人浏览,
                    <span id="endorse-count">0</span>人评分.
                    <a href="javascript:;" ><span data-toggle="modal" data-target="#dissent-modal">举报</span></a>
                </div>
                <!-- 评论列表 -->
                <div id="comment-list"
                     data-module="hsList"
                     data-load-url="centre/medium/comment/search.act?link=<%=ln%>&link_id=<%=id%>"
                     data-fill-list="(hsListFillItem)"
                     data-fill-page="(hsListFillNext)"
                     style=" padding: 0.5em; background: #eee;">
                    <div class="itembox" style="display: none;">
                        <input type="hidden" name="id"      data-fn="id"      data-fl="$(this).val(v) && undefined"/>
                        <input type="hidden" name="user_id" data-fn="user_id" data-fl="$(this).val(v) && undefined"/>
                        <h6>
                            <span data-fn="user.name" data-fl="list._info.mate.name ? v+' 回复 '+list._info.mate.name : v"></span>
                            <span data-fn="ctime" data-ft="_htime" data-fl="v*1000" style="color:#666;padding:0 1em;"></span>
                            <a href="javascript:;"><span data-toggle="modal" data-target="#comment-modal">回复</span></a>
                        </h6>
                        <p data-fn="note"></p>
                    </div>
                    <div class="listbox">
                    </div>
                    <div class="pagebox">
                        <button type="button" class="btn btn-sm btn-primary send-note" data-toggle="modal" data-target="#comment-modal">添加评论</button>
                        <button type="button" class="btn btn-sm btn-default page-prev" data-pn="" style="margin-left: 0.25em;" >
                            <span class="glyphicon glyphicon-backward"></span>
                        </button>
                        <button type="button" class="btn btn-sm btn-default page-next" data-pn="" style="margin-left: 0.20em;" >
                            <span class="glyphicon glyphicon-forward" ></span>
                        </button>
                        <span class="btn btn-sm btn-link" style="text-decoration: none;">页码: <span class="page-curr" data-pn=""></span></span>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!-- 评论对话框 -->
    <div id="comment-modal" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <form id="comment-form" method="post" action="centre/medium/comment/create.act?link=<%=ln%>&link_id=<%=id%>&ab=.errs" class="loadbox">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">评论</h4>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" name="prev_id" value=""/>
                        <input type="hidden" name="mate_id" value=""/>
                        <div class="form-group row">
                            <div class="col-sm-12">
                                <textarea name="note" class="form-control" required="required"></textarea>
                            </div>
                            <div class="col-sm-12 help-block"></div>
                        </div>
                        <div class="form-group row" style="margin-bottom: 0;">
                            <div class="col-sm-6">
                                <input type="text" name="capt" value="" placeholder="验证码" class="form-control" required="required" autocomplete="off"/>
                            </div>
                            <div class="col-sm-6">
                                <img src="" onclick="this.src='centre/capt/create.act?b=ffffff&f=222222&h=43&_='+Math.random()" class="capt-img" style="width: 86px; height: 43px;" title="点我, 点我, 点我..."/>
                            </div>
                            <div class="col-sm-12 help-block"></div>
                        </div>
                    </div>
                    <div class="modal-footer" style="text-align: left;">
                        <button type="submit" class="btn btn-primary">发送评论</button>
                        <button type="reset"  class="btn btn-default" data-dismiss="modal">取消</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <!-- 举报对话框 -->
    <div id="dissent-modal" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <form id="dissent-form" method="post" action="centre/medium/dissent/create.act?link=<%=ln%>&link_id=<%=id%>&ab=.errs" class="loadbox">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title">举报</h4>
                    </div>
                    <div class="modal-body">
                        <div class="form-group row">
                            <div class="col-sm-12">
                                <select name="cause" class="form-control">
                                    <option value="1">虚假信息</option>
                                    <option value="2">敏感信息</option>
                                    <option value="3">淫秽色情</option>
                                    <option value="4">人身攻击</option>
                                    <option value="*">其他</option>
                                </select>
                            </div>
                            <div class="col-sm-12 help-block"></div>
                        </div>
                        <div class="form-group row">
                            <div class="col-sm-12">
                                <textarea name="note" class="form-control"></textarea>
                            </div>
                            <div class="col-sm-12 help-block"></div>
                        </div>
                        <div class="form-group row" style="margin-bottom: 0;">
                            <div class="col-sm-6">
                                <input type="text" name="capt" value="" placeholder="验证码" class="form-control" required="required" autocomplete="off"/>
                            </div>
                            <div class="col-sm-6">
                                <img src="" onclick="this.src='centre/capt/create.act?b=ffffff&f=222222&h=43&_='+Math.random()" class="capt-img" style="width: 86px; height: 43px;" title="点我, 点我, 点我..."/>
                            </div>
                            <div class="col-sm-12 help-block"></div>
                        </div>
                    </div>
                    <div class="modal-footer" style="text-align: left;">
                        <button type="submit" class="btn btn-primary">立即举报</button>
                        <button type="reset"  class="btn btn-default" data-dismiss="modal">取消</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script type="text/javascript">
        (function($) {
            // 记录浏览和获取统计信息
            $.ajax({
                url     : hsFixUri("common/more.act"),
                data    : {
                    "link"   : "<%=ln%>",
                    "link_id": "<%=id%>",
                    "a.impress": "centre/medium/impress/create",
                    "a.statist": "centre/medium/statist/search"
                },
                type    : "POST",
                dataType: "JSON",
                success : function(rst) {
                    var stat = rst.statist.info;
                    if (stat) {
                        var star = Math.round(parseFloat(stat.endorse_score)
                                            / parseFloat(stat.endorse_count));
                        $("#impress-count").text(stat.impress_count);
                        $("#endorse-count").text(stat.endorse_count);
                        $("#comment-count").text(stat.comment_count);
                        for (var i = 0; i < star; i ++) {
                            $("#endorse-score")
                                .find ("[data-score='"+(i + 1)+"']")
                                .removeClass("glyphicon-star-empty")
                                   .addClass("glyphicon-star"      );
                        }
                    }
                }
            });

            // 评分
            $("#endorse-score a").click(function() {
                var score = $(this).data("score");
                $.ajax({
                    url     : hsFixUri("centre/medium/endorse/update.act"),
                    data    : "link=<%=ln%>&link_id=<%=id%>&score="+score ,
                    type    : "POST",
                    dataType: "JSON",
                    success : function(rst) {
                        rst = hsResponse(rst);
                        if (rst.ok) {
                            location.reload();
                        }
                    }
                });
            });

            // 没有评论时不要弹出错误
            $("#comment-list" ).on("loadBack", function(evt, rst) {
                if (rst.page.ern == 1 && rst.page.page == 1) {
                    rst.page.ern =  0;
                }
            });

            // 评论成功后切到最后一页
            $("#comment-modal").on("saveBack", function(evt, rst) {
                var listobj = $("#comment-list").data("HsList");
                hsSetSeria(listobj._data, "pn", "-1");
                listobj.load( );
            });

            // 模态框打开时重取验证码
            $("#comment-modal,#dissent-modal").on("show.bs.modal", function() {
                $(this).find( "form" ).hsForm();
                $(this).find(".capt-img").click();
                $(this).find("[name=note]").val("");
                $(this).find("[name=capt]").val("");
                $(this).find("[name=cause]").val("1");
            });

            // 回复时带上留言相关的ID
            $(document).on("click", '[data-target="#comment-modal"]', function() {
                var prevIdInp = $("#comment-modal").find("[name=prev_id]");
                var mateIdInp = $("#comment-modal").find("[name=mate_id]");
                if ($(this).parent( ).is("a")) {
                    var box = $(this).closest(".itembox");
                    var pid = box.find("[name=id]").val();
                    var mid = box.find("[name=user_id]" ).val();
                    prevIdInp.val(pid);
                    mateIdInp.val(mid);
                } else {
                    prevIdInp.val("" );
                    mateIdInp.val("" );
                }
            });
        })(jQuery);
    </script>
</body>
</html>