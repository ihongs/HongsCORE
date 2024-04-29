<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = "survey";
    String _funcId = "in_"+(_module + "_" + _entity + "_form").replace('/', '_');
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
%>
<div id="<%=_pageId%>" class="<%=_action%>-form">
    <form class="form-horizontal <%=Synt.declare(_params.get("page-form-class"), "")%>"
          method="POST" enctype="multipart/form-data">
        <div class="form-body">
        <%
        Iterator it = _fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if ("@".equals(name)
            ||  Synt.declare(info.get( "disabled" ), false)
            ||  Synt.declare(info.get("unwritable"), false)
            ||  Synt.declare(info.get("unopenable"), false)) {
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
            if (roly) {
                Object defo = info.get("deforce");
                if (defo != null && ! defo.equals("")) {
                    continue;
                }
                roly = false;
            }

            // 可以自定义字段显示文本
            if (info.containsKey("text")) {
                text  = (String) info.get("text");
            }
            if (info.containsKey("hint")) {
                hint  = (String) info.get("hint");
            }
        %>
        <%/****/ if ("hidden".equals(type)) {%>
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
        <%} else {%>
            <%
                String hold, hist, pfc, gfc;
                text = Synt.defxult(Synt.asString(info.get("form-text")), text, "");
                hint = Synt.defxult(Synt.asString(info.get("form-hint")), hint, "");
                hold = Synt.defxult(Synt.asString(info.get("form-hold")), "" );
                hist = Synt.defxult(Synt.asString(info.get("form-hist")), "" );
                pfc  = Synt.defxult(Synt.asString(info.get("page-form-class")), "");
                gfc  =  "" ;
                if (rqrd) {
                    if ("enum".equals(type) ||  "type".equals(type) || "check".equals(type) || "radio".equals(type) || "select".equals(type)
                    ||  "file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)
                    ||  "fork".equals(type) ||  "pick".equals(type)) {
                        hist = "(必选)";
                    } else {
                        hist = "(必填)";
                    }
                } else
                if (rptd) {
                    if ("enum".equals(type) ||  "type".equals(type) || "check".equals(type) || "radio".equals(type) || "select".equals(type)
                    ||  "file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)
                    ||  "fork".equals(type) ||  "pick".equals(type)) {
                        hist = "(多选)";
                    }
                }
                if (rqrd) gfc += " is-required";
                if (rptd) gfc += " is-repeated";
                if (roly) gfc += " is-readonly";
                if (gfc.length() > 0) gfc = gfc.substring(1);
            %>
            <%if ("form".equals(type) || "part".equals(type)) {%>
            <div class="form-grade <%=pfc%>">
            <div class="form-group" data-name="<%=name%>" style="margin-left:0;margin-right:0;">
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
                <legend class="group"><%=text%></legend>
                <div class="help-block text-muted form-control-static"><%=hint%></div>
                <div class="form-subs" data-ft="<%=kind%>" data-fn="<%=name%>" data-href="<%=href%>"<%=extr%> data-sub-class="group panel panel-body panel-default" data-sub-style="padding-left:0;padding-right:0;"></div>
                <div class="row form-sub-add hide">
                    <div class="col-sm-9 col-md-8 col-sm-offset-3 col-md-offset-2">
                        <button type="button" class="btn btn-default" data-toggle="hsFormSubAdd"><%=Synt.defxult(hold, _locale.translate("fore.form.sub.add", text))%></button>
                    </div>
                </div>
                <div class="row form-sub-del hide">
                    <div class="col-sm-9 col-md-8 col-sm-offset-3 col-md-offset-2">
                        <button type="button" class="btn btn-warning" data-toggle="hsFormSubDel"><%=Synt.defxult(hold, _locale.translate("fore.form.sub.del", text))%></button>
                    </div>
                </div>
            </div>
            </div>
            <%continue; } /*End sub form*/%>
            <div class="form-grade <%=pfc%>">
            <div class="form-group <%=gfc%>" data-name="<%=name%>">
                <label class="control-label">
                    <span class="control-label-text"><%=(text != null ? text : "")%></span>
                    <span class="control-label-hist"><%=(hist != null ? hist : "")%></span>
                </label>
                <div class="control-input">
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
                        if (rqrd) {
                            extr += " data-required=\"required\"";
                        }
                        String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                        String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                        String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") :  kn ;
                        String rl = info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" ;
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                        String at = info.containsKey("data-at") ? (String) info.get("data-at") :  "" ;
                        rl = rl.replace("centra", "centre");
                        al = al.replace("centra", "centre");
                        at = at.replace("centra", "centre");
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
                        kind += "\" data-ln=\""+ln+"\" data-tk=\""+tk+"\" data-vk=\""+vk+"\" data-at=\""+at
                             +  "\" data-href=\""+rl+"\" data-target=\"";
                        mode += "\" data-href=\""+al+"\" data-target=\"";
                    %>
                    <input name="<%=name%>" type="hidden" class="form-final"/>
                    <ul data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> class="pickbox"></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=Synt.defxult(hold, _locale.translate("fore.fork.select", text))%></button>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFile";
                        String kind =  "_file";
                        String typa = (String) info.get("accept");
                        if (typa == null || typa.length() == 0) {
                            typa  = ! "file".equals(type) ? type+"/*" : "*/*";
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
                            typa += "\" multiple=\"multiple";
                            extr += " data-repeated=\"repeated\"";
                        }
                        if (rqrd) {
                            extr += " data-required=\"required\"";
                        }
                        if ("image".equals(type)) {
                            mode  = "hsView";
                            kind  =  "_view";
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
                            kind += "\" data-size=\""+size+"\" data-mode=\""+moda;
                        }
                    %>
                    <input type="file" name="<%=name%>" accept="<%=typa%>" class="invisible"/>
                    <ul data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%> class="pickbox"></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=Synt.defxult(hold, _locale.translate("fore.file.browse", text))%></button>
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
                        if ("time".equals(typa)
                        ||  "date".equals(typa)) {
                            extr += " data-fill=\"v ? v : new Date().getTime()\"";
                        } else {
                            extr += " data-fill=\"v ? v : new Date().getTime()/1000\"";
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
                        if (rptd) {
                            extr += " multiple=\"multiple\"";
                            extr += " data-toggle=\"hsBags\"";
                            name += "." ;
                        }
                        if (info.containsKey("min" )) extr += " min=\"" +info.get("min" ).toString()+"\"";
                        if (info.containsKey("max" )) extr += " max=\"" +info.get("max" ).toString()+"\"";
                        if (info.containsKey("step")) extr += " step=\""+info.get("step").toString()+"\"";
                        if ("sorted".equals(type)) type = "number";
                    %>
                    <input class="form-field form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>/>
                <%} else if ("string".equals(type) || "stored".equals(type) || "search".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (rptd) {
                            extr += " multiple=\"multiple\"";
                            extr += " data-toggle=\"hsBags\"";
                            name += "." ;
                        }
                        if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                        if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                        if (info.containsKey("pattern"  )) extr += " pattern=\""  +info.get("pattern"  ).toString()+"\"";
                        if ("string".equals(type) || "stored".equals(type) || "search".equals(type)) type = "text";
                    %>
                    <input class="form-field form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>/>
                <%} else {%>
                    <%
                        String extr = "";
                        if (rptd) {
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                    %>
                    <input class="form-field form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hold%>"<%=extr%>/>
                <%} /*End If */%>
                    <div class="help-block text-error form-control-static"></div>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        </div><!-- end form-body -->
        <div class="form-foot">
            <div class="form-group">
                <button type="submit" class="commit btn btn-success form-control text-center">
                    <%=Synt.defxult((String) _params.get("page-form-commit"), _locale.translate("fore.commit"))%>
                </button>
            </div>
        </div><!-- end form-foot -->
    </form>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var formbox = context.find("form");

    var formobj = context.hsForm({
        _fill__fork: hsFormFillFork,
        _fill__file: hsFormFillFile,
        _fill__view: hsFormFillView
    });
    if (H$("?id")) {
        formobj.formBox.attr("action", "<%=_module%>/<%=_entity%>/update.act");
        formobj._url = "<%=_module%>/<%=_entity%>/recite.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text";
    } else {
        formobj.formBox.attr("action", "<%=_module%>/<%=_entity%>/create.act");
        formobj._url = "<%=_module%>/<%=_entity%>/recipe.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text";
    }

    // 外部定制
    if (window["<%=_funcId%>"]) {
        window["<%=_funcId%>"](context, formobj);
    }

    // 特殊控件
    setFormItems( formbox , loadbox);

    // 加载数据
    formobj.load(undefined, loadbox);
})( jQuery );
</script>