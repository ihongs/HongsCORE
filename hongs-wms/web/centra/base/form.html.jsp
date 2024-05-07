<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("form.action"), "update");
    String _funcId = "in_"+(_module + "_" + _entity + "_form").replace('/', '_');
    String _pageId = /* */ (_module + "-" + _entity + "-form").replace('/', '-');
%>
<h2 class="hide"><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId+" "+_action%>-form">
    <form action="<%=_module%>/<%=_entity%>/<%=_action%>.act"
          method="POST" enctype="multipart/form-data" class="form-horizontal">
        <div class="rollbox panel panel-default">
        <div class="form-body">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get( "disabled" ), false)
            ||  Synt.declare(info.get("unwritable"), false)) {
                continue ;
            }

            String  type = Synt.asString(info.get("__type__"));
            String  text = Synt.asString(info.get("__text__"));
            String  hint = Synt.asString(info.get("__hint__"));
            boolean rqrd = Synt.declare(info.get("__required__"), false);
            boolean rptd = Synt.declare(info.get("__repeated__"), false);
            boolean roly = Synt.declare(info.get(  "readonly"  ), false);

            /**
             * 新增时只读字段未强制默认,
             * 转为非只读字段供首次输入.
             */
            if (roly && _action.equals("create")) {
                Object defo = info.get("deforce");
                if (defo != null && ! defo.equals("")) {
                    continue;
                }
                roly = false;
            }
        %>
        <%if ("hidden".equals(type)) {%>
            <input type="hidden" name="<%=name%>" data-fn/>
        <%} else if ("legend".equals(type)) {%>
            <%
                text = Synt.defxult(Synt.asString(info.get("form-text")), text, "");
            %>
            <legend data-name="<%=name%>"><%=text%></legend>
        <%} else if ("figure".equals(type)) {%>
            <%
                text = Synt.defxult(Synt.asString(info.get("form-text")), text, "");
            %>
            <figure data-name="<%=name%>"><%=text%></figure>
        <%} else if ( roly ) { //** 此部分来自 info.jsp **/ %>
            <%
                text = Synt.defxult(Synt.asString(info.get("info-text")), text, "");
                hint = Synt.defxult(Synt.asString(info.get("info-hint")), hint, "");
            %>
            <%if ("form".equals(type) || "part".equals(type)) {%>
            <div class="form-group row" data-name="<%=name%>">
                <%
                    String extr = "";
                    String kind =  "_form";
                    String href = Synt.defxult(Synt.asString(info.get("data-rl")), "");
                    href = href.replace( "centre", "centra" );
                    if (rptd) {
                        name  = name + "."; // 多选末尾加点
                        extr += " data-repeated=\"repeated\"";
                    }
                %>
                <div class="col-xs-12">
                    <legend class="group"><%=text%></legend>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                    <div class="form-subs" data-ft="<%=kind%>" data-fn="<%=name%>" data-href="<%=href%>"<%=extr%> data-sub-class="group panel panel-body panel-default"></div>
                </div>
            </div>
            <%continue; } /*End sub form*/%>
            <div class="form-group row" data-name="<%=name%>">
                <label class="col-xs-3 col-md-2 text-right form-label control-label form-control-static"><%=text%></label>
                <div class="col-xs-9 col-md-8">
                <%if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFork";
                        String kind =  "_fork";
                        String kn   =    name ;
                        if (kn.endsWith("_id")) {
                            kn = kn.substring(0, kn.length() - 3);
                        } else {
                            kn = kn +  "_fork"; // 增加特定后缀
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " data-repeated=\"repeated\"";
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") :  kn ;
                        String rl = info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" ;
                        rl = rl.replace("centre", "centra");
                        /**
                         * 默认禁止扩展功能
                         */
                        if (!rl.isEmpty() && !rl.contains(".deny=")) {
                            if (!rl.contains("?") && !rl.contains("#")) {
                                rl = rl + "?.deny=.expand";
                            } else {
                                rl = rl + "&.deny=.expand";
                            }
                        }
                        kind += "\" data-ln=\""+ln+"\" data-tk=\""+tk+"\" data-vk=\""+vk
                             +  "\" data-href=\""+rl+"\" data-target=\"@";
                    %>
                    <div class="form-control-static">
                        <ul class="repeated forkbox" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
                    </div>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFile";
                        String kind =  "_file";
                        String klas =   "file";
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " data-repeated=\"repeated\"";
                        }
                        if ("image".equals(type)) {
                            mode = "hsPict";
                            kind =  "_pict";
                            klas =   "pict";
                            String size = Synt.declare(info.get("thumb-size"), "");
                            String moda = Synt.declare(info.get("thumb-mode"), "");
                            if (rptd
                            &&  moda.length() == 0) {
                                moda = "keep"; // 多选但未指定模式，采用保留以便对齐
                            }
                            if (size.length() != 0) {
                                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)").matcher(size);
                                if ( m.find() ) {
                                    // 限制最大尺寸, 避免撑开容器
                                    int w  = 450 ;
                                    int h  = 150 ;
                                    int sw = Synt.declare(m.group(1), w);
                                    int sh = Synt.declare(m.group(2), h);
                                    if (sw > w) {
                                        sh = w * sh / sw;
                                        sw = w;
                                    }
                                    if (sh > h) {
                                        sw = h * sw / sh;
                                        sh = h;
                                    }
                                    size = sw+"*"+sh;
                                } else {
                                    size = "150*150";
                                }
                            } else {
                                size = "150*150";
                            }
                            kind += "\" data-size=\""+size+"\" data-mode=\""+moda;
                        }
                    %>
                    <div class="form-control-static">
                        <ul class="repeated <%=klas%>box" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> data-readonly="readonly"></ul>
                    </div>
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <%
                        String typa = (String) info.get("type");
                        String mode = (String) info.get("mode");
                    %>
                    <%  /**/ if ("code".equals(typa)) {%>
                    <pre class="form-control-static _code" data-fn="<%=name%>" data-ft="_text" data-type="<%=typa%>" data-mode="<%=mode%>"></pre>
                    <%} else if ("html".equals(typa)) {%>
                    <div class="form-control-static _html" data-fn="<%=name%>" data-ft="_html"></div>
                    <%} else {%>
                    <div class="form-control-static _text" data-fn="<%=name%>" data-ft="_text"></div>
                    <%}%>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String kind = "_"+ type;
                        // 日期类需注意 Unix 时间戳需要乘 1000
                        Object typa = info.get("type");
                        if ("timestamp".equals( typa )
                        ||  "datestamp".equals( typa )) {
                            kind += "\" data-fill=\"!v?v:v*1000" ;
                        }
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            kind += "\" data-format=\"" + frmt;
                        }
                    %>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} else if ("number".equals(type) || "sorted".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <%
                        String kind = "_review";
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            kind += "\" data-format=\"" + frmt;
                        }
                    %>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="<%=kind%>"></div>
                <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                    <%
                        name += "_text";
                        if (rptd) {
                            name += ".";
                        }
                    %>
                    <div class="form-control-static">
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated" data-item-class="label label-default"></ul>
                    </div>
                <%} else if ( rptd ) {%>
                    <%
                            name += ".";
                    %>
                    <div class="form-control-static">
                        <ul data-fn="<%=name%>" data-ft="_review" class="repeated" data-item-class="label label-default"></ul>
                    </div>
                <%} else {%>
                    <div class="form-control-static" data-fn="<%=name%>" data-ft="_review"></div>
                <%} /*End If */%>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
        <%} else {%>
            <%
                String hold;
                text = Synt.defxult(Synt.asString(info.get("form-text")), text, "");
                hint = Synt.defxult(Synt.asString(info.get("form-hint")), hint, "");
                hold = Synt.defxult(Synt.asString(info.get("form-hold")), "");
            %>
            <%if ("form".equals(type) || "part".equals(type)) {%>
            <div class="form-group" data-name="<%=name%>" data-form-error="&gt;*&gt;.text-error">
                <%
                    String extr = "";
                    String kind =  "_form";
                    String href = Synt.defxult(Synt.asString(info.get("data-al")), "");
                    href = href.replace("centre", "centra");
                    if (rptd) {
                        name  = name + ".";
                        extr += " data-repeated=\"repeated\"";
                    }
                    if (rqrd) {
                        extr += " data-required=\"required\"";
                    }
                %>
                <div class="col-xs-12">
                    <legend class="group"><%=text%></legend>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                    <div class="form-subs" data-ft="<%=kind%>" data-fn="<%=name%>" data-href="<%=href%>"<%=extr%> data-sub-class="group panel panel-body panel-default"></div>
                    <div class="help-block text-error form-control-static"></div>
                    <div class="row form-sub-add hide">
                        <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                            <button type="button" class="btn btn-default" data-toggle="hsFormSubAdd"><%=Synt.defxult(hold, _locale.translate("fore.form.sub.add", text))%></button>
                        </div>
                    </div>
                    <div class="row form-sub-del hide">
                        <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                            <button type="button" class="btn btn-warning" data-toggle="hsFormSubDel"><%=Synt.defxult(hold, _locale.translate("fore.form.sub.del", text))%></button>
                        </div>
                    </div>
                </div>
            </div>
            <%continue; } /*End sub form*/%>
            <div class="form-group row" data-name="<%=name%>">
                <label class="col-xs-3 col-md-2 text-right form-label control-label form-control-static"><%=text%></label>
                <div class="col-xs-9 col-md-8">
                <%if ("fork".equals(type) || "pick".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsForkInit";
                        String kn   =    name ;
                        if (kn.endsWith("_id")) {
                            kn = kn.substring(0, kn.length() - 3);
                        } else {
                            kn = kn +  "_fork"; // 增加特定后缀
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") :  kn ;
                        String rl = info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" ;
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                        String at = info.containsKey("data-at") ? (String) info.get("data-at") :  "" ;
                        rl = rl.replace("centre", "centra");
                        al = al.replace("centre", "centra");
                        at = at.replace("centre", "centra");
                        /**
                         * 默认禁止扩展功能
                         */
                        if (!rl.isEmpty() && !rl.contains(".deny=")) {
                            if (!rl.contains("?") && !rl.contains("#")) {
                                rl = rl + "?.deny=.expand";
                            } else {
                                rl = rl + "&.deny=.expand";
                            }
                        }
                        if (!al.isEmpty() && !al.contains(".deny=")) {
                            if (!al.contains("?") && !al.contains("#")) {
                                al = al + "?.deny=.expand";
                            } else {
                                al = al + "&.deny=.expand";
                            }
                        }
                        /**
                         * 关联路径: base/search|data/xxxx/search?rb=a,b,c
                         * 需转换为: data/xxxx/search.act?rb=a,b,c
                         */
                        if (!at.isEmpty()) {
                            int p  = at.indexOf  ('|');
                            if (p != -1) {
                                at = at.substring(1+p);
                            }   p  = at.indexOf  ('?');
                            if (p != -1) {
                                at = at.substring(0,p)
                                   +      Cnst.ACT_EXT
                                   + at.substring(0+p);
                            } else {
                                at = at + Cnst.ACT_EXT;
                            }
                        }
                        extr += " data-ln=\""+ln+"\" data-tk=\""+tk+"\" data-vk=\""+vk+"\" data-at=\""+at+"\""
                             +  " data-link-href=\""+rl+"\" data-link-target=\"@\""
                             +  " data-pick-href=\""+al+"\" data-pick-target=\"@\"";
                    %>
                    <input type="hidden" name="<%=name%>" data-toggle="<%=mode%>"<%=extr%> class="form-final"/>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFileInit";
                        String typa = (String) info.get("accept");
                        if (typa == null || typa.length() == 0) {
                            typa  = ! "file".equals(type) ? type+"/*" : "*/*";
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if ("image".equals(type)) {
                            mode  = "hsPictInit";
                            String moda = Synt.declare(info.get("thumb-mode"), "");
                            String size = Synt.declare(info.get("thumb-size"), "");
                            if (rptd
                            &&  moda.length() == 0) {
                                moda = "keep"; // 多选但未指定模式，采用保留以便对齐
                            }
                            if (size.length() != 0) {
                                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)").matcher(size);
                                if ( m.find() ) {
                                    // 限制最大尺寸, 避免撑开容器
                                    int w  = 450 ;
                                    int h  = 150 ;
                                    int sw = Synt.declare(m.group(1), w);
                                    int sh = Synt.declare(m.group(2), h);
                                    if (sw > w) {
                                        sh = w * sh / sw;
                                        sw = w;
                                    }
                                    if (sh > h) {
                                        sw = h * sw / sh;
                                        sh = h;
                                    }
                                    size = sw+"*"+sh;
                                } else {
                                    size = "150*150";
                                }
                            } else {
                                size = "150*150";
                            }
                            extr += " data-size=\""+size+"\" data-mode=\""+moda+"\"";
                        }
                    %>
                    <input type="file" name="<%=name%>" accept="<%=typa%>" data-toggle="<%=mode%>"<%=extr%> class="invisible"/>
                <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("data-ln")) {
                            extr += " data-ln=\""+info.get("data-ln")+"\"";
                        }
                        if (rptd) {
                            name += "." ;
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        } else
                        if (rptd) {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-final"/>
                    <%} /* End if */ %>
                    <select class="form-field form-control" name="<%=name%>"<%=extr%>></select>
                <%} else if ("check".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("data-ln")) {
                            extr += " data-ln=\""+info.get("data-ln")+"\"";
                        }
                        if (rptd) {
                            name += "." ;
                        }
                        if (rqrd) {
                            extr  = "data-required=\"required\"";
                        } else {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-final"/>
                    <%} /* End if */ %>
                    <div class="checkbox" data-fn="<%=name%>" data-ft="_check" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>"<%=extr%>></div>
                <%} else if ("radio".equals(type)) {%>
                    <%
                        String extr = "";
                        if (info.containsKey("data-ln")) {
                            extr += " data-ln=\""+info.get("data-ln")+"\"";
                        }
                        if (rptd) {
                            name += "." ;
                        }
                        if (rqrd) {
                            extr += " data-required=\"required\"";
                        } else {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-final"/>
                    <%} /* End if */ %>
                    <div class="radio"    data-fn="<%=name%>" data-ft="_radio" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>"<%=extr%>></div>
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <%
                        String extr = "";
                        String clas = "";
                        String typa = (String) info.get("type");
                        String mode = (String) info.get("mode");
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (null != typa &&!"".equals(typa)) {
                            extr += " data-type=\"" + typa + "\"";
                        if (null != mode &&!"".equals(mode)) {
                            extr += " data-mode=\"" + mode + "\"";
                        }
                            clas  = "\" style=\"width:100%; height:15em; border:0;\"";
                        } else {
                            clas  = " form-control\" style=\"height:5em;\"";
                        }
                    %>
                    <textarea class="form-field<%=clas%>" id="<%=_pageId%>-<%=name%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>></textarea>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String typa = Synt.declare(info.get( "type" ), "date");
                        String fomt = Synt.declare(info.get("format"),  type );
                        String fset = Synt.declare(info.get("offset"),   ""  );
                        String extr = " data-type=\""+typa +"\" data-format=\""+fomt+"\" data-offset=\""+fset+"\" data-toggle=\"hsDate\"";
                        if (rqrd) {
                        if ("timestamp".equals(typa)
                        ||  "datestamp".equals(typa)) {
                            extr += " data-fill=\"v ? v : new Date().getTime()/1000\"";
                        } else {
                            extr += " data-fill=\"v ? v : new Date().getTime()\"";
                        }
                            extr += " required=\"required\"";
                        }
                        if (info.containsKey("min")) extr += " min=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("max")) extr += " max=\""+info.get("max").toString()+"\"";
                    %>
                    <input class="form-field form-control" type="text" name="<%=name%>"<%=extr%>/>
                <%} else if ("number".equals(type) || "sorted".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (info.containsKey("min" )) extr += " min=\"" +info.get("min" ).toString()+"\"";
                        if (info.containsKey("max" )) extr += " max=\"" +info.get("max" ).toString()+"\"";
                        if (info.containsKey("step")) extr += " step=\""+info.get("step").toString()+"\"";
                        if (! type.matches("range|color")) type = "number";
                    %>
                    <input class="form-field form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>/>
                <%} else {%>
                    <%
                        String extr = "";
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (rptd) {
                            extr += " multiple=\"multiple\"";
                            extr += " data-toggle=\"hsDits\"";
                            name += "." ;
                        }
                        if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                        if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                        if (info.containsKey("pattern"  )) extr += " pattern=\""  +info.get("pattern"  ).toString()+"\"";
                        if (! type.matches("email|url|tel|sms")) type = "text";
                    %>
                    <input class="form-field form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>/>
                <%} /*End If */%>
                    <div class="help-block text-error form-control-static"></div>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
                <div class="col-md-2 hidden-sm hidden-xs">
                    <div class="form-control-static">
                        <%if (rqrd) {%><span class="form-icon-required" title="必填/必选字段"></span><%}%>
                    </div>
                </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
            <hr/><!-- 兼容占位 -->
        </div><!-- end form-body -->
        </div><!-- end panel -->
        <div class="form-foot">
            <div class="form-group row">
                <div class="col-xs-9 col-md-8 col-xs-offset-3 col-md-offset-2">
                    <div class="btn-toolbar">
                        <button type="submit" class="commit btn btn-primary"><%=_locale.translate("fore.commit")%></button>
                        <button type="button" class="cancel btn btn-default"><%=_locale.translate("fore.cancel")%></button>
                    </div>
                </div>
            </div>
            <br/><!-- 兼容占位 -->
        </div><!-- end form-foot -->
    </form>
    <div class="pagebox"></div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest( ".loadbox" );
    var formbox = context.find("form").first( );

    var loadres = hsSerialDic(loadbox);
    var initres = hsSerialArr(loadres);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    // 清理参数
    for(var j = initres.length-1; j > -1; j --) {
        var n = initres[j];
        if (! n.name || ! n.value) {
            initres.splice( j, 1 );
        }
    }

    var formobj = context.hsForm({
        <%if ("create".equals(_action)) {%>
        _url : "<%=_module%>/<%=_entity%>/recipe.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text",
        <%} else {%>
        _url : "<%=_module%>/<%=_entity%>/recite.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text",
        <%if (! _fields.containsKey("memo")) {%>
         save: hsSaveWithMemo('<%=_locale.translate("fore.update.confirm", _title)%>'),
        <%}} /* End if */%>
        _data: loadres,
         initInfo: initres,
        _fill__file: hsFormFillFile,
        _fill__pict: hsFormFillPict,
        _fill__fork: hsFormFillFork,
        _fill__form: hsFormFillPart,
        _feed__form: hsFormFeedPart,
        _test__form: hsFormTestPart
    });

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, formobj))
         .then(function() {

        // 外部限制
        $.each(denycss ? denycss.split(",") : []
        , function(i, n) {
            if (/^form\./.test(n)) {
                n = ".form-group[data-name='"+n.substring(5)+"']";
                formbox.find(n).remove();
            } else {
                context.find(n).remove();
            }
        });

        // 自适滚动
        hsFlexRoll(formbox.children(".rollbox"), $("#main-context"));

        // 特殊控件
        setFormItems(formbox, loadbox);

        // 加载数据
        formobj.load();

        }); // End Promise
    });
})(jQuery);
</script>