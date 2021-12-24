<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = "survey";
    String _pageId = (_module + "-" + _entity + "-" + _action).replace('/', '-');
    String _funcId = "in_"+(_module + "_" + _entity + "_form").replace('/', '_');
%>
<div id="<%=_pageId%>" class="<%=_action%>-form">
    <form class="form-horizontal <%=Synt.declare(_params.get("page-form-class"), "")%>"
          method="POST" enctype="multipart/form-data">
        <%
        int ii = 0;
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

            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");
            String  hint = (String) info.get("__hint__");
            boolean rqrd = Synt.declare(info.get("__required__"), false);
            boolean rptd = Synt.declare(info.get("__repeated__"), false);
            boolean roly = Synt.declare(info.get(  "readonly"  ), false);

            /**
             * 新增时只读字段无默认值的,
             * 转为非只读字段供首次输入.
             */
            if (roly) {
                Object defoult = info.get("default");
                Object deforce = info.get("deforce");
                if ((null == defoult || "".equals(defoult) )
                && ( null == deforce || "".equals(deforce))) {
                    roly = false;
                } else {
                    continue;
                }
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
            <input type="hidden" name="<%=name%>" />
        <%} else if ("legend".equals(type)) {%>
            <legend class="text-center"><%=text%></legend>
        <%} else {%>
            <%
                String gfc = "";
                if (rqrd) gfc += " is-required";
                if (rptd) gfc += " is-repeated";
                if (roly) gfc += " is-readonly";
                if (gfc.length() > 0) gfc = gfc.substring(1);
                String pfc = Synt.declare(info.get("page-form-class"), "");
            %>
            <div class="form-grade <%=pfc%>">
            <div class="form-group <%=gfc%>" data-name="<%=name%>">
                <label class="control-label">
                    <span class="control-order-txt"><%=++ii%></span><span class="control-order-end"></span>
                    <span class="control-label-txt"><%=text%></span><span class="control-label-end"></span>
                </label>
                <div class="control-input">
                <%if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <%
                        String extr = "";
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
                            extr += " style=\"width:100%; height:15em; border:0;\"";
                        } else {
                            extr += " class=\"form-control\" style=\"height:5em;\"";
                        }
                    %>
                    <textarea id="<%=_pageId%>-<%=name%>" name="<%=name%>" placeholder="<%=hint%>"<%=extr%>></textarea>
                    <%hint = null;%>
                <%} else if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (rptd) {
                            extr += " data-toggle=\"tagsinput\"";
                            type  =  "text";
                        } else {
                            if ("string".equals(type)) type = "text";
                            if (info.containsKey("minlength")) extr += " minlength=\""+info.get("minlength").toString()+"\"";
                            if (info.containsKey("maxlength")) extr += " maxlength=\""+info.get("maxlength").toString()+"\"";
                            if (info.containsKey("pattern"  )) extr += " pattern=\""  +info.get("pattern"  ).toString()+"\"";
                        }
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hint%>"<%=extr%>/>
                    <%hint = null;%>
                <%} else if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                        if (rptd) {
                            extr += " data-toggle=\"tagsinput\"";
                            type  =  "text";
                        } else {
                            if ("sorted".equals(type)) type = "number";
                            if (info.containsKey("min"      )) extr += " min=\""      +info.get("min"      ).toString()+"\"";
                            if (info.containsKey("max"      )) extr += " max=\""      +info.get("max"      ).toString()+"\"";
                            if (info.containsKey("step"     )) extr += " step=\""     +info.get("step"     ).toString()+"\"";
                        }
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hint%>"<%=extr%>/>
                    <%hint = null;%>
                <%} else if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {%>
                    <%
                        String typa = Synt.declare(info.get( "type" ), "date");
                        String fomt = Synt.declare(info.get("format"),  type );
                        String fset = Synt.declare(info.get("offset"),   ""  );
                        String extr = " data-type=\""+typa +"\" data-format=\""+fomt+"\" data-offset=\""+fset+"\" data-toggle=\"hsDate\"";
                        if (rqrd) {
                        if ("time".equals(typa)
                        ||  "date".equals(typa)) {
                            extr += " data-fl=\"v ? v : new Date().getTime()\""     ;
                        } else {
                            extr += " data-fl=\"v ? v : new Date().getTime()/1000\"";
                        }
                            extr += " required=\"required\"";
                        }
                        if (info.containsKey("min")) extr += " min=\""+info.get("min").toString()+"\"";
                        if (info.containsKey("max")) extr += " max=\""+info.get("max").toString()+"\"";
                    %>
                    <input class="form-control" type="text" name="<%=name%>"<%=extr%>/>
                <%} else if ("check".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rptd) {
                            name += "." ;
                        }
                        if (rqrd) {
                            extr  = "data-required=\"required\"";
                        } else {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */ %>
                    <div class="checkbox" data-fn="<%=name%>" data-ft="_check" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>"<%=extr%>></div>
                <%} else if ("radio".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rptd) {
                            name += "." ;
                        }
                        if (rqrd) {
                            extr += " data-required=\"required\"";
                        } else {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */ %>
                    <div class="radio"    data-fn="<%=name%>" data-ft="_radio" data-vk="<%=Synt.defoult(info.get("data-vk"), "0")%>" data-tk="<%=Synt.defoult(info.get("data-tk"), "1")%>"<%=extr%>></div>
                <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type)) {%>
                    <%
                        String extr = "";
                        if (rptd) {
                            name += "." ;
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        } else
                        if (rptd) {
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <%} /* End if */ %>
                    <select class="form-control" name="<%=name%>"<%=extr%>></select>
                <%} else if ("fork".equals(type) || "pick".equals(type)) {%>
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
                        String ak = info.containsKey("data-ak") ? (String) info.get("data-ak") :  kn ;
                        String rl = info.containsKey("data-rl") ? (String) info.get("data-rl") :  "" ;
                        String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                        String at = info.containsKey("data-at") ? (String) info.get("data-at") :  "" ;
                        rl = rl.replace("centra", "centre");
                        al = al.replace("centra", "centre");
                        at = at.replace("centra", "centre");
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
                        kind += "\" data-ak=\""+ak+"\" data-tk=\""+tk+"\" data-vk=\""+vk+"\" data-at=\""+at
                             +  "\" data-href=\""+rl+"\" data-target=\"";
                        mode += "\" data-href=\""+al+"\" data-target=\"";
                    %>
                    <input type="hidden" name="<%=name%>" class="form-ignored"/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.fork.select", text)%></button>
                <%} else if ("file".equals(type) || "image".equals(type) || "video".equals(type) || "audio".equals(type)) {%>
                    <%
                        String extr = "";
                        String mode = "hsFile";
                        String kind =  "_file";
                        String typa = (String) info.get("type");
                        if (typa == null || typa.length() == 0) {
                            typa  = type +"/*";
                        }
                        if (rptd) {
                            name  = name + "."; // 多选末尾加点
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
                    <input type="file" name="<%=name%>" accept="<%=typa%>" class="form-ignored invisible"/>
                    <ul class="pickbox" data-fn="<%=name%>" data-ft="<%=kind%>"<%=extr%>></ul>
                    <button type="button" class="btn btn-default form-control" data-toggle="<%=mode%>"><%=_locale.translate("fore.file.browse", text)%></button>
                <%} else {%>
                    <%
                        String extr = "";
                        if (rptd) {
                            name += "." ;
                            extr += " multiple=\"multiple\"";
                        }
                        if (rqrd) {
                            extr += " required=\"required\"";
                        }
                    %>
                    <input class="form-control" type="<%=type%>" name="<%=name%>" placeholder="<%=hint%>"<%=extr%>/>
                    <%hint = null;%>
                <%} /*End If */%>
                    <%
                        String hist = "";
                        if (hint == null)
                               hint = "";
                        hist = Synt.declare(info.get("hist"), hist);
                        hint = Synt.declare(info.get("hint"), hint);
                    %>
                    <div class="help-block text-error form-control-static"><%=hist%></div>
                    <div class="help-block text-muted form-control-static"><%=hint%></div>
                </div>
            </div>
            </div>
        <%} /*End If */%>
        <%} /*End For*/%>
        <br/>
        <div class="btns-group">
            <div>
                <button type="submit" class="commit btn btn-success form-control text-center">
                    <%=Synt.defxult((String) _params.get("page-form-commit"), _locale.translate("fore.commit"))%>
                </button>
            </div>
        </div>
        <br/>
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
        formobj._url = "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text";
    } else {
        formobj.formBox.attr("action", "<%=_module%>/<%=_entity%>/create.act");
        formobj._url = "<%=_module%>/<%=_entity%>/select.act?<%=Cnst.AB_KEY%>=.enfo,.info,.fall,_fork,_text";
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