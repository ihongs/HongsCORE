<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.HashSet" %>
<%@page import="java.util.Iterator"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@include file="_boot_.jsp"%>
<%
    String _action = Synt.declare(request.getAttribute("list.action"), "search");
    String _funcId = "in_"+(_module + "_" + _entity + "_list").replace('/', '_');
    String _pageId = /* */ (_module + "-" + _entity + "-list").replace('/', '-');

    String _conf   = FormSet.hasConfFile(_module + "/" + _entity)
                  || NaviMap.hasConfFile(_module + "/" + _entity)
                   ? _module + "/" + _entity : _module ;

    StringBuilder _ob = new StringBuilder("boost!,mtime!,ctime!");
    StringBuilder _rb = new StringBuilder("id,name");
    Set<String>   _wd = getWordable (_fields);
    Set<String>   _sd = getSrchable (_fields);
%>
<h2 class="hide"><%=_locale.translate("fore."+_action+".title", _title)%></h2>
<div id="<%=_pageId%>" class="<%=_pageId+" "+_action%>-list">
    <form class="findbox toolbox board row">
        <div class="col-xs-7">
            <div class="btn-group">
                <%if ("select".equals(_action)) {%>
                <button type="button" class="commit btn btn-primary"><%=_locale.translate("fore.select", _title)%></button>
                <button type="button" class="checks btn btn-warning"><%=_locale.translate("fore.selall", _title)%></button>
                <%} /*End If*/%>
                <button type="button" class="create btn btn-default"><%=_locale.translate("fore.create", _title)%></button>
                <%if ("search".equals(_action)) {%>
                <button type="button" class="update for-choose btn btn-default"><%=_locale.translate("fore.update", _title)%></button>
                <button type="button" class="recite for-choose btn btn-default"><%=_locale.translate("fore.recite", _title)%></button>
                <button type="button" class="reveal for-choose btn btn-default" title="<%=_locale.translate("fore.reveal", _title)%>"><span class="bi bi-hi-reveal"></span></button>
                <button type="button" class="copies for-checks btn btn-default" title="<%=_locale.translate("fore.copies", _title)%>"><span class="bi bi-hi-export"></span></button>
                <button type="button" class="delete for-checks btn btn-default" title="<%=_locale.translate("fore.delete", _title)%>"><span class="bi bi-hi-remove text-danger"></span></button>
                <%} /*End If*/%>
            </div>
            <%if ("select".equals(_action)) {%>
            <div class="btn btn-text text-muted picksum"><%=_locale.translate("fore.selected", _title)%> <b class="picknum"></b></div>
            <div class="for-checks for-choose invisible"></div>
            <%} /*End If*/%>
        </div>
        <div class="col-xs-5">
            <div class="input-group">
                <%
                    StringBuilder sp = new StringBuilder( );
                    if (! _wd.isEmpty()) {
                    for(String ss : _wd) {
                        ss = Dict.getValue(_fields, "", ss , "__text__" );
                        if (ss.length() != 0) sp.append(ss).append(", " );
                    }   if (sp.length() != 0) sp.setLength(sp.length()-2);
                    } else {
                        sp.append("\" disabled=\"disabled");
                    }
                %>
                <input type="search" class="form-control" name="<%=Cnst.WD_KEY%>" placeholder="<%=sp%>" /><!--<%=_wd%>-->
                <span class="input-group-btn">
                    <button type="submit" class="search btn btn-default" title="<%=_locale.translate("fore.search", _title)%>"><span class="bi bi-hi-search"></span></button>
                    <button type="button" class="filter btn btn-default" title="<%=_locale.translate("fore.filter", _title)%>"><span class="bi bi-hi-filter"></span></button>
                    <button type="button" class="statis btn btn-default" title="<%=_locale.translate("fore.statis", _title)%>"><span class="bi bi-hi-statis"></span></button>
                    <button type="button" class="column btn btn-default" title="<%=_locale.translate("fore.column", _title)%>"><span class="bi bi-hi-column"></span></button>
                </span>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
    <form class="findbox siftbox openbox invisible well">
        <%
        Set dataAdds = new HashSet();
        StringBuilder dataList = new StringBuilder();
        StringBuilder siftList = new StringBuilder();
        Iterator it2 = _fields.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry et = (Map.Entry) it2.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");

            if ("@".equals(name) || "id".equals(name)) {
                continue;
            }
            if (!Synt.declare(info.get("siftable"), false)
            &&  !Synt.declare(info.get("filtable"), false)) {
                continue;
            }

            String  kind = "";
            String  extr = "";

            if ("fork".equals(type) || "pick".equals(type)) {
                String fn = name;
                if (fn.endsWith( "." )) {
                    fn = fn.substring(0, fn.length() - 1);
                }
                String kn = fn +"_fork";
                if (fn.endsWith("_id")) {
                    fn = fn.substring(0, fn.length() - 3);
                    kn = fn;
                }
                String tk = info.containsKey("data-tk") ? (String) info.get("data-tk") : "name";
                String vk = info.containsKey("data-vk") ? (String) info.get("data-vk") : "id";
                String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") :  kn ;
                String al = info.containsKey("data-al") ? (String) info.get("data-al") :  "" ;
                String at = info.containsKey("data-at") ? (String) info.get("data-at") :  "" ;
                al = al.replace("centre", "centra");
                at = at.replace("centre", "centra");
                // 选择时禁用创建
                if ( ! al.isEmpty (   )) {
                if ( ! al.contains("#")) {
                    al = al + "#.deny=.create";
                } else {
                    al = al + "&.deny=.create";
                }}
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
                extr = " data-ln=\""+ln+"\" data-vk=\""+vk+"\" data-tk=\""+tk+"\" data-href=\""+al+"\" data-target=\"@\"";
                kind = "_fork";
            } else
            if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {
                String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") : name;
                // 顺便整理枚举列表
                if (dataAdds.contains(ln) == false) {
                    String sl = "<select data-fn=\""+ln+"\" class=\"form-control\"></select>";
                    dataList. append (sl);
                    dataAdds.   add  (ln);
                }
                extr = " data-ln=\""+ln+"\"";
                kind = "_enum";
            } else
            if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {
                Object fomt = Synt.defoult(info.get("format"),type);
                Object fset = Synt.defoult(info.get("offset"), "" );
                type = Synt.declare(info.get("type"), "time");
                extr = " data-format=\""+fomt+"\" data-offset=\""+fset+"\"";
                kind = "_date";
            } else
            if ("number".equals(type) || "range".equals(type) || "color".equals(type)) {
                Object typa = info.get("type");
                if ("int".equals(typa) || "long".equals(typa) || "color".equals(type)) {
                    extr = " data-for=\"int\"";
                }
                kind = "_number";
            } else
            if ("search".equals(type) || "textarea".equals(type) || "textview".equals(type)) {
                if (_sd.contains(name)) {
                    extr = " data-for=\"search\"";
                }
                kind = "_string";
            } else
            if ("string".equals(type) || "text".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type)) {
                if (_sd.contains(name)) {
                    extr = " data-for=\"serial\"";
                }
                kind = "_string";
            } else
            {
                kind = "_is";
            }

            siftList.append("<option value=\""+name+"\" data-kind=\""+kind+"\" data-type=\""+type+"\""+extr+">"+text+"</option>");
        } /*End While*/
        %>
        <div class="invisible">
            <%=dataList%>
        </div>
        <div class="form-body">
        </div>
        <div class="sift-body">
            <div class="row">
                <div class="col-xs-6">
                    <ul class="list-unstyled clearfix group">
                        <li class="sift-unit template">
                            <div>
                                <legend class="sift-hand">
                                    <a href="javascript:;" class="erase bi bi-x pull-right"></a>
                                    <span class="sift-lr"></span>
                                </legend>
                                <ul class="sift-list repeated" data-name="ar">
                                    <li class="sift-item template label label-info">
                                        <a href="javascript:;" class="erase bi bi-x pull-right"></a>
                                        <span class="sift-hand">
                                            <span class="sift-fn"></span>
                                            <span class="sift-fr"></span>
                                            <span class="sift-fv"></span>
                                        </span>
                                    </li>
                                </ul>
                            </div>
                        </li>
                        <li class="sift-unit sift-root active">
                            <div>
                                <legend class="sift-hand">
                                    <span class="sift-lr">与</span>
                                </legend>
                                <ul class="sift-list repeated" data-name="ar">
                                </ul>
                            </div>
                        </li>
                        <!-- 未免让人觉得复杂, 仅保留一个顶级组
                        <li class="sift-unit sift-root">
                            <div>
                                <legend class="sift-hand">
                                    <span class="sift-lr">或</span>
                                </legend>
                                <ul class="sift-list repeated" data-name="or">
                                </ul>
                            </div>
                        </li>
                        <li class="sift-unit sift-root">
                            <div>
                                <legend class="sift-hand">
                                    <span class="sift-lr">非</span>
                                </legend>
                                <ul class="sift-list repeated" data-name="nr">
                                </ul>
                            </div>
                        </li>
                        //-->
                    </ul>
                    <div class="alert alert-warning alert-dismissible" role="alert">
                        <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <strong>使用说明：</strong>
                        <span>请选择字段、条件、取值，这将添加一条筛查参数；点击“&times;”删除条目或分组。</span>
                        <span>“与”、“或”、“非”表示分组内各条间的关系；点击分组可激活，新增的将放入其下。</span>
                        <span>如需将筛查条目或分组换到其他组，按住条目或分组标题拖拽过去即可。</span>
                        <span><a href="javascript:;" onclick="alert('?'+$.param($(this).closest('form').find('input:hidden')))">检查参数</a></span>
                    </div>
                </div>
                <div class="col-xs-6">
                    <div class="form-group">
                        <select data-sift="fn" class="form-control">
                            <option value="" style="color: gray;">字段</option>
                            <%=siftList%>
                        </select>
                    </div>
                    <div class="form-group">
                        <select data-sift="fr" class="form-control">
                            <option value="" style="color: gray;">条件</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <div class="btn-toolbar">
                            <button type="button" class="btn btn-default" data-sift="fv" data-target="@">取值</button>
                            <div class="btn-group">
                                <button type="button" class="btn btn-default" data-sift="lr" data-name="ar" data-text="与">+ 与</button>
                                <button type="button" class="btn btn-default" data-sift="lr" data-name="or" data-text="或">+ 或</button>
                                <button type="button" class="btn btn-default" data-sift="lr" data-name="nr" data-text="非">+ 非</button>
                            </div>
                        </div>
                    </div>
                    <hr style="border-color: #ccc;"/>
                    <div class="form-group board">
                        <div class="btn-toolbar">
                            <button type="submit" class="btn btn-primary">过滤</button>
                            <button type="reset"  class="btn btn-default">重置</button>
                            <label class="btn-group pull-right form-control-static" style="font-weight: inherit;">
                                <input type="checkbox" name="ob" value="-" /> 按匹配度排序
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </form>
    <!-- 统计 -->
    <form class="findbox statbox openbox invisible well">
        <div class="row">
        <%
        Iterator it3 = _fields.entrySet().iterator();
        while (it3.hasNext()) {
            Map.Entry et = (Map.Entry) it3.next();
            Map     info = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) info.get("__type__");
            String  text = (String) info.get("__text__");

            if ("@".equals(name) || "id".equals(name)
            || !Synt.declare(info.get("statable"), false)) {
                continue;
            }

            // 检查是否有枚举数据
            String enumConf = Synt.defxult((String) info.get("conf"),_conf);
            String enumName = Synt.defxult((String) info.get("enum"), name);
            Map    enumData = null;
            try {
                enumData  = FormSet.getInstance(enumConf).getEnum(enumName);
            } catch (CruxException ex) {
            if (ex.getErrno() != 913 ) {
                throw ex.toExemption();
            }}

            // 统计方法
            String kind = (String)info.get("stat-type");
            if (kind != null && ! kind.isEmpty()) {
                type  = kind;
            } else
            if ("number".equals(type)) {
                if (enumData != null ) {
                    type = "range";
                } else {
                    type = "count";
                }
            } else {
                type  = "count";
            }

            // 图表类型
            String plot = (String)info.get("stat-plot");
            if (plot != null && ! plot.isEmpty()) {
                type += "\" data-plot=\"" + plot;
            }

            // 附加参数
            String prms = (String)info.get("stat-prms");
            if (prms != null && ! prms.isEmpty()) {
                type += "\" data-prms=\"" + prms;
            }
        %>
        <div class="stat-group col-xs-6" data-name="<%=name%>" data-text="<%=text%>" data-type="<%=type%>">
            <div class="panel panel-body panel-default clearfix" style="height: 302px;">
                <div class="checkbox col-xs-3" style="height: 100%; display:none"></div>
                <div class="chartbox col-xs-9" style="height: 100%; display:none"></div>
                <div class="alertbox"><div><%=text%> <%=_locale.translate("fore.loading")%></div></div>
            </div>
        </div>
        <%} /*End For*/%>
        </div>
    </form>
    <!-- 列表 -->
    <div class="listbox rollbox panel panel-default table-responsive">
        <table class="table table-hover table-striped table-compressed">
            <thead>
                <tr>
                    <th data-fn="id." data-ft="_check" class="_check">
                        <input name="id." type="checkbox" class="checkall"/>
                    </th>
                    <%if ("search".equals(_action)) {%>
                    <th data-fn="_" data-ft="_admin" class="_admin _amenu">
                        <div class="dropdown invisible">
                            <a href="javascript:;" data-toggle="dropdown"><span class="bi bi-hi-action"></span></a>
                            <ul class="dropdown-menu">
                                <li><a href="javascript:;" class="update"><%=_locale.translate("fore.update", _title)%></a></li>
                                <li><a href="javascript:;" class="recite"><%=_locale.translate("fore.recite", _title)%></a></li>
                                <li><a href="javascript:;" class="reveal"><%=_locale.translate("fore.reveal", _title)%></a></li>
                                <li><a href="javascript:;" class="delete"><span class="text-danger"><%=_locale.translate("fore.delete", _title)%></span></a></li>
                            </ul>
                        </div>
                    </th>
                    <%} /*End If*/%>
                <%
                Iterator it = _fields.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry et = (Map.Entry) it.next();
                    Map     info = (Map ) et.getValue();
                    String  name = (String) et.getKey();
                    String  type = (String) info.get("__type__");
                    String  text = (String) info.get("__text__");

                    if ("@".equals(name) || "hidden".equals(type)
                    || !Synt.declare(info.get("listable"), false)) {
                        continue;
                    }

                    String ob = "";
                    String oc = "";
                    if (Synt.declare(info.get("sortable"), false)) {
                        ob = (String)info.get("data-ob" );
                        if (ob == null) {
                            ob  = "*,"+ name;
                        }
                        ob = "data-ob=\""+ob+"\"";
                        oc = "sortable";
                    }

                    if ("datetime".equals(type)
                    ||      "date".equals(type)
                    ||      "time".equals(type)) {
                        // Unix 时间戳类需乘 1000 以转换为毫秒
                        Object typa = info.get("type");
                        if ("timestamp".equals( typa )
                        ||  "datestamp".equals( typa )) {
                            ob += " data-fill=\"!v?v:v*1000\"";
                        }
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            ob += " data-format=\"" + frmt + "\"";
                        } else
                        // 默认为短格式
                        if ("datetime" .equals( type )) {
                            type  = "htime";
                        } else
                        if ("date"     .equals( type )) {
                            type  = "hdate";
                        }
                    } else
                    if (  "number".equals(type)
                    ||    "sorted".equals(type)
                    ||     "range".equals(type)
                    ||     "color".equals(type)) {
                        // 自定义格式化
                        String frmt = (String) info.get("format");
                        if (frmt != null && frmt.length( ) != 0 ) {
                            ob += " data-format=\"" + frmt + "\"";
                        }
                    } else
                    if ("textarea".equals(type)
                    ||  "textview".equals(type)) {
                        ob += " data-type=" + Synt.declare(info.get("type"), "text");
                    }

                    _rb.append(',').append(name);
                %>
                <%if ("number".equals(type) || "range".equals(type) || "color".equals(type)) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%> numerial text-right"><%=text%></th>
                <%} else if ("datetime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_datetime" <%=ob%> class="<%=oc%> numerial datetime"><%=text%></th>
                <%} else if ("date".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_date" <%=ob%> class="<%=oc%> numerial date"><%=text%></th>
                <%} else if ("time".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_time" <%=ob%> class="<%=oc%> numerial time"><%=text%></th>
                <%} else if ("hdate".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_hdate" <%=ob%> class="<%=oc%> numerial _hdate"><%=text%></th>
                <%} else if ("htime".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_htime" <%=ob%> class="<%=oc%> numerial _htime"><%=text%></th>
                <%} else if (  "url".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_ulink" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("email".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_email" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("image".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_image" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("video".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_video" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("audio".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_audio" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("file".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_files" <%=ob%> class="<%=oc%> text-center"><%=text%></th>
                <%} else if ("textarea".equals(type) || "textview".equals(type)) {%>
                    <th data-fn="<%=name%>" data-ft="_texts" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("enum".equals(type) || "type".equals(type) || "check".equals(type) || "radio".equals(type) || "select".equals(type)) {%>
                    <%
                        if (name.endsWith( "." )) {
                            name = name.substring(0, name.length() - 1);
                        }
                        if (name.endsWith("_id")) {
                            name = name.substring(0, name.length() - 3);
                        } else {
                            name = name + "_text";
                        }
                    %>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if ("pick".equals(type) || "fork".equals(type)) {%>
                    <%
                        if (name.endsWith( "." )) {
                            name = name.substring(0, name.length() - 1);
                        }
                        if (name.endsWith("_id")) {
                            name = name.substring(0, name.length() - 3);
                        } else {
                            name = name + "_fork";
                        }
                        String subn = "name";
                        if (info.get("data-ln") != null) {
                            name = (String) info.get("data-ln");
                        }
                        if (info.get("data-tk") != null) {
                            subn = (String) info.get("data-tk");
                        }
                        if (Synt.declare(info.get("__repeated__"), false)) {
                            name = name + "." ;
                        }
                        name = name +"."+ subn;
                    %>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} else if (!"primary".equals(info.get("primary")) && !"foreign".equals(info.get("foreign"))) {%>
                    <th data-fn="<%=name%>" <%=ob%> class="<%=oc%>"><%=text%></th>
                <%} /*End If */%>
                <%} /*End For*/%>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <div class="pagebox clearfix">
        <em class="page-text">...</em>
    </div>
</div>
<script type="text/javascript">
(function($) {
    var context = H$("#<%=_pageId%>");
    var loadbox = context.closest(".loadbox");
    var listbox = context.find(".listbox");
    var findbox = context.find(".findbox");
    var siftbox = context.find(".siftbox");
    var statbox = context.find(".statbox");

    var loadres = hsSerialDic(loadbox);
    var denycss = loadres['.deny'];
        delete    loadres['.deny'];

    //** 列表、搜索表单 **/

    var listobj = context.hsList({
        _data : loadres,
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete',
              '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
        ],
        openUrls: [
            [ '<%=_module%>/<%=_entity%>/form_init.html?'+$.param(hsSerialArr(loadres)),
              '.create', '@' ],
            [ '<%=_module%>/<%=_entity%>/form.html?<%=Cnst.ID_KEY%>={ID}',
              '.update', '@' ],
            [ '<%=_module%>/<%=_entity%>/info.html?<%=Cnst.ID_KEY%>={ID}',
              '.recite', '@' ],
            [ '<%=_module%>/<%=_entity%>/snap.html?<%=Cnst.ID_KEY%>={ID}',
              '.reveal', '@' ]
        ],
        load: hsLoadWithWord,
        send: hsSendWithMemo,
        <%if ("select".equals(_action)) {%>
        _fill__check: hsListFillSele,
        <%} /*End If */%>
        // 多行文本, 富文本等
        _fill__texts: hsFillListMore,
        // 链接填充, 支持多值, 占格子窄
        _fill__ulink: hsListWrapOpen("link" ),
        _fill__files: hsListWrapOpen("file" ),
        _fill__image: hsListWrapOpen("image"),
        _fill__video: hsListWrapOpen("video"),
        _fill__audio: hsListWrapOpen("audio")
    });

    var siftobj = context.hsSift({
        _url: "<%=_module%>/<%=_entity%>/recipe.act?<%=Cnst.AB_KEY%>=.enfo"
    });

    var statobj = context.hsStat({
        _url: "<%=_module%>/<%=_entity%>/acount.act?<%=Cnst.RN_KEY%>=<%=Cnst.RN_DEF%>&<%=Cnst.OB_KEY%>=-&<%=Cnst.AB_KEY%>=linked,resort,_text,_fork"
    });

    // 绑定参数
    listobj._url = hsSetPms(listobj._url, loadres);
    statobj._url = hsSetPms(statobj._url, loadres);

    // 延迟加载
    context.on("opened",".siftbox", function() {
        if (siftbox.data("fetched") != true) {
            siftbox.data("fetched"  ,  true);
            siftobj.load();
        }
    });
    context.on("opened",".statbox", function() {
        if (statbox.data("changed") == true) {
            statbox.data("changed"  ,  null);
            statobj.load();
        }
    });

    // 管理动作
    context.on("click", ".toolbox .filter", function() {
        siftbox.toggleClass("invisible");
        if (! siftbox.is("invisible")) {
            siftbox.trigger("opened");
        }
        statbox.addClass("invisible");
    });
    context.on("click", ".toolbox .statis", function() {
        statbox.toggleClass("invisible");
        if (! statbox.is("invisible")) {
            statbox.trigger("opened");
        }
        siftbox.addClass("invisible");
    });
    context.on("click", ".toolbox :submit", function() {
        siftbox.addClass("invisible");
    });
    context.on("click", ".siftbox :submit", function() {
        siftbox.addClass("invisible");
    });
    context.on("click", ".toolbox .copies", function() {
        hsCopyListData(listbox);
    });
    context.on("click", ".toolbox .checks", function() {
        hsPickListMore(listbox);
    });
    context.on("click", ".toolbox .column", function() {
        hsHideListCols(listbox);
    });
    hsSaveListCols(listbox, "<%=_pageId%>");

    hsRequires("<%=_module%>/<%=_entity%>/defines.js", function() {
        // 外部定制
        $.when(window["<%=_funcId%>"] && window["<%=_funcId%>"](context, listobj, siftobj, statobj))
         .then(function() {

        // 权限控制
        $.each({"recite":".recite", "create":".create", "update":".update", "delete":".delete", "reveal":".reveal"}
        , function(k, v) {
            if (! hsChkUri("<%=_module%>/<%=_entity%>/"+k+".act")) {
                context.find(v).remove();
            }
        });
        // 外部限制
        $.each(denycss ? denycss . split (",") : [ ]
        , function(i, n) {
            if (/^find\./.test(n)) {
                n = n.substring(5);
                n = "[data-sift=fn]>[value='"+n+"'],"
                  + ".sift-group[data-name='"+n+"'],"
                  + ".stat-group[data-name='"+n+"']";
                findbox.find(n).remove();
            } else
            if (/^list\./.test(n)) {
                n = n.substring(5);
                n = "th[data-fn='"+n+"']";
                listbox.find(n).remove();
            } else
            {
                context.find(n).remove();
            }
        });
        // 无行内菜单项则隐藏之
        if (listbox.find("thead ._amenu ul>li>a").size() == 0) {
            listbox.find("thead ._amenu").addClass( "hidden" );
        }
        // 无操作按钮则隐藏选择
        if (findbox.find(".for-choose").size() == 0
        &&  findbox.find(".for-checks").size() == 0) {
            listbox.find("thead ._check").addClass( "hidden" );
        }
        // 无过滤或统计则隐藏之
        if (siftbox.find("[data-sift=fn]>*").size() == 1
        &&  siftbox.find(".sift-group").size() == 0) {
            findbox.find(".filter").remove();
        }
        if (statbox.find(".stat-group").size() == 0) {
            findbox.find(".statis").remove();
        }

        <%if ("select".equals(_action)) {%>
        // 单选移除跨页全选
        if (! loadbox.is(".pickmul")) {
            context.find(".toolbox .checks").remove();
        }
        <%} /*End If */%>

        // 自适滚动
        var h = hsFlexRoll(listbox.filter(".rollbox"), $("#main-context"));
        if (h > 0) {
            siftbox.css("max-height", h+"px");
            siftbox.css("overflow-y", "auto");
            statbox.css("max-height", h+"px");
            statbox.css("overflow-y", "auto");
        }

        // 加载数据
        listobj.load(null, findbox);

        }); // End Promise
    });
})(jQuery);
</script>