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
                    <button type="button" class="sifter btn btn-default" title="<%=_locale.translate("fore.sifter", _title)%>"><span class="bi bi-hi-sifter"></span></button>
                    <button type="button" class="statis btn btn-default" title="<%=_locale.translate("fore.statis", _title)%>"><span class="bi bi-hi-statis"></span></button>
                    <button type="button" class="column btn btn-default" title="<%=_locale.translate("fore.column", _title)%>"><span class="bi bi-hi-column"></span></button>
                </span>
            </div>
        </div>
    </form>
    <!-- 筛选 -->
    <form class="findbox siftbox openbox invisible well">
        <div class="row" style="margin-top: -15px;">
            <div class="col-xs-6 filt-body">
                <%
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
                    // 明确不过滤或两种都不支持则跳过
                    if (!Synt.declare(info.get("filtable"), true )) {
                        continue;
                    }
                    if (!Synt.declare(info.get("filtable"), false)
                    &&  !Synt.declare(info.get("siftable"), false)) {
                        continue;
                    }
                %>
                <div class="filt-group form-group" data-name="<%=name%>">
                    <label class="control-label form-control-static">
                        <%=text != null ? text : ""%>
                    </label>
                    <div>
                    <%if ("fork".equals(type) || "pick".equals(type)) {%>
                        <%
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
                            al = al.replace("centre", "centra");
                            // 选择时禁用创建
                            if ( ! al.isEmpty (   )) {
                            if ( ! al.contains("#")) {
                                al = al + "#.deny=.create";
                            } else {
                                al = al + "&.deny=.create";
                            }}
                        %>
                        <div class="form-control multiple">
                            <ul class="repeated labelbox labelist forkbox" data-ft="_fork" data-fn="<%=name%>.<%=Cnst.EQ_REL%>" data-ln="<%=ln%>" data-tk="<%=tk%>" data-vk="<%=vk%>"></ul>
                            <a href="javascript:;" data-toggle="hsFork" data-target="@" data-href="<%=al%>"><%=_locale.translate("fore.fork.select", text)%></a>
                        </div>
                    <%} else if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {%>
                        <%
                            String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") : name;
                        %>
                        <select class="form-control" name="<%=name%>.<%=Cnst.EQ_REL%>" data-ln="<%=ln%>" data-ft="_enum">
                            <option value="" style="color: gray;"><%=_locale.translate("fore.fork.select", text)%></option>
                        </select>
                    <%} else if ("date".equals(type) || "time" .equals(type) || "datetime" .equals(type)) {%>
                        <%
                            Object typa = Synt.declare(info.get("type"),"time");
                            Object fomt = Synt.defoult(info.get("format"),type);
                            Object fset = Synt.defoult(info.get("offset"), "" );
                        %>
                        <div class="input-group">
                            <span class="input-group-addon">从</span>
                            <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.GE_REL%>" data-toggle="hsDate" data-type="<%=typa%>" data-format="<%=fomt%>" data-offset="<%=fset%>" />
                            <span class="input-group-addon"></span>
                        </div>
                        <div class="input-group">
                            <span class="input-group-addon">到</span>
                            <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.LE_REL%>" data-toggle="hsDate" data-type="<%=typa%>" data-format="<%=fomt%>" data-offset="<%=fset%>" />
                            <span class="input-group-addon"></span>
                        </div>
                    <%} else if ("number".equals(type) || "range".equals(type) || "color".equals(type) || "sorted".equals(type)) {%>
                        <div class="input-group">
                            <span class="input-group-addon">从</span>
                            <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.GE_REL%>" />
                            <span class="input-group-addon">到</span>
                            <input type="<%=type%>" class="form-control" name="<%=name%>.<%=Cnst.LE_REL%>" />
                        </div>
                    <%} else if ("string".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type) || "text".equals(type)) {%>
                        <%if (_sd.contains(name)) {%>
                        <div class="input-group input-group">
                            <input class="form-control" type="text" name="<%=name%>.<%=Cnst.EQ_REL%>" placeholder="精确查找" />
                            <div class="input-group-btn input-group-rel">
                                <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                    <i class="caret"></i>
                                </button>
                                <ul class="dropdown-menu dropdown-menu-right">
                                    <li class="active">
                                        <a href="javascript:;" data-name="<%=name%>.<%=Cnst.EQ_REL%>" data-placeholder="精确查找">精确查找</a>
                                    </li>
                                    <li>
                                        <a href="javascript:;" data-name="<%=name%>.<%=Cnst.SP_REL%>" data-placeholder="模糊匹配">模糊匹配</a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                        <%} else {%>
                        <input class="form-control" type="text" name="<%=name%>.<%=Cnst.EQ_REL%>" placeholder="精确查找" />
                        <%}%>
                    <%} else if ("search".equals(type) || "textarea".equals(type) || "textview".equals(type)) {%>
                        <%if (_sd.contains(name)) {%>
                        <input class="form-control" type="text" name="<%=name%>.<%=Cnst.SP_REL%>" placeholder="模糊匹配" />
                        <%} else {%>
                        <input class="form-control" type="text" name="<%=name%>.<%=Cnst.EQ_REL%>" placeholder="精确查找" />
                        <%}%>
                    <%} else {%>
                        <select class="form-control" name="<%=name%>.<%=Cnst.IS_REL%>">
                            <option value="" style="color: gray;">选择...</option>
                            <option value="not-none">不为空</option>
                            <option value="none">为空</option>
                        </select>
                    <%} /*End If */%>
                    </div>
                </div>
                <%} /*End For*/%>
            </div>
            <div class="col-xs-6 sift-body">
                <ul class="list-unstyled clearfix">
                    <li class="sift-unit template">
                        <div>
                            <legend class="sift-hand">
                                <a href="javascript:;" class="erase bi bi-x pull-right"></a>
                                <span class="sift-lr"></span>
                            </legend>
                            <ul class="sift-list repeated labelbox" data-name="ar">
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
                    <li class="sift-unit sift-root">
                        <div class="form-group">
                            <label  class="sift-hand control-label form-control-static">
                                高级查询
                            </label>
                            <div class="form-control multiple">
                                <ul class="sift-list repeated labelbox" data-name="ar" data-fn>
                                </ul>
                            </div>
                        </div>
                    </li>
                </ul>
                <div class="form-group">
                    <select data-sift="fn" class="form-control">
                        <option style="color: gray;" value="-" data-rels="-">字段...</option>
                        <%
                        Set<String> siftEnum = new HashSet( );
                        it2 = _fields.entrySet( ).iterator( );
                        while (it2.hasNext()) {
                            Map.Entry et = (Map.Entry) it2.next();
                            Map     info = (Map ) et.getValue();
                            String  name = (String) et.getKey();
                            String  type = (String) info.get("__type__");
                            String  text = (String) info.get("__text__");

                            if ("@".equals(name) || "id".equals(name)) {
                                continue;
                            }
                            // 明确不过滤或两种都不支持则跳过
                            if (!Synt.declare(info.get("siftable"), true )) {
                                continue;
                            }
                            if (!Synt.declare(info.get("siftable"), false)
                            &&  !Synt.declare(info.get("filtable"), false)) {
                                continue;
                            }

                            String  kind = "";
                            String  rels = "";
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
                                rels =  "is,eq,ne";
                                kind =  "fork";
                            } else
                            if ("enum".equals(type) || "type".equals(type) || "select".equals(type) || "check".equals(type) || "radio".equals(type)) {
                                String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") : name;
                                siftEnum.add(ln); // 需从后端获取数据
                                extr = " data-ln=\""+ln+"\"";
                                rels =  "is,eq,ne";
                                kind =  "enum";
                            } else
                            if ("date".equals(type) || "time".equals(type) || "datetime".equals(type)) {
                                Object fomt = Synt.defoult(info.get("format"),type);
                                Object fset = Synt.defoult(info.get("offset"), "" );
                                type = Synt.declare(info.get("type"), "time");
                                extr = " data-format=\""+fomt+"\" data-offset=\""+fset+"\"";
                                rels =  "is,eq,ne,gt,ge,lt,le";
                                kind =  "date";
                            } else
                            if ("number".equals(type) || "range".equals(type) || "color".equals(type)) {
                                Object typa = info.get("type");
                                if ("int".equals(typa) || "long".equals(typa) || "color".equals(type)) {
                                    extr = " data-for=\"int\"";
                                }
                                rels =  "is,eq,ne,gt,ge,lt,le";
                                kind =  "number";
                            } else
                            if ("string".equals(type) || "email".equals(type) || "url".equals(type) || "tel".equals(type) || "sms".equals(type) || "text".equals(type)) {
                                String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") : name;
                                extr = " data-ln=\""+ln+"\"";
                                if (_sd.contains(name)) {
                                    rels =  "is,eq,ne,sp,ns";
                                } else {
                                    rels =  "is,eq,ne";
                                }
                                kind =  "string";
                            } else
                            if ("search".equals(type) || "textarea".equals(type) || "textview".equals(type)) {
                                String ln = info.containsKey("data-ln") ? (String) info.get("data-ln") : name;
                                extr = " data-ln=\""+ln+"\"";
                                if (_sd.contains(name)) {
                                    rels =  "is,sp,ns";
                                } else {
                                    rels =  "is,eq,ne";
                                }
                                kind =  "string";
                            } else
                            {
                                rels =  "is";
                                kind =  "no";
                            }
                        %>
                        <option value="<%=name%>" data-type="<%=type%>" data-kind="<%=kind%>" data-rels="<%=rels%>"<%=extr%>><%=text%></option>
                        <%} /*End For*/%>
                    </select>
                </div>
                <div class="form-group">
                    <select data-sift="fr" class="form-control">
                        <option value="-" style="color: gray;">关系...</option>
                        <option value="is">为</option>
                        <option value="eq">等于</option>
                        <option value="ne">不等于</option>
                        <option value="sp">匹配</option>
                        <option value="ns">不匹配</option>
                        <option value="gt">大于</option>
                        <option value="ge">大于或等于</option>
                        <option value="lt">小于</option>
                        <option value="le">小于或等于</option>
                    </select>
                </div>
                <div class="form-group">
                    <div data-sift="fv">
                        <div data-kind="-">
                            <div class="btn btn-block btn-default text-left disabled">请先选择<b>筛查字段</b>和<b>条件关系</b></div>
                        </div>
                        <div data-kind="is" class="sift-select">
                            <select class="value form-control">
                                <option value=""></option>
                                <option value="none">空</option>
                                <option value="not-none">非空</option>
                            </select>
                        </div>
                        <div data-kind="string" class="sift-input">
                            <div class="input-group">
                                <input class="value form-control" type="text"/>
                                <div class="input-group-btn">
                                    <button type="button" class="ensue btn btn-info"><span class="bi bi-plus-lg"></span></button>
                                </div>
                            </div>
                        </div>
                        <div data-kind="number" class="sift-input">
                            <div class="input-group">
                                <input class="value form-control" type="number"/>
                                <div class="input-group-btn">
                                    <button type="button" class="ensue btn btn-info"><span class="bi bi-plus-lg"></span></button>
                                </div>
                            </div>
                        </div>
                        <div data-kind="date">
                            <button type="button" class="btn btn-default btn-block text-left">选择...</button>
                        </div>
                        <div data-kind="fork">
                            <button type="button" class="btn btn-default btn-block text-left">选择...</button>
                        </div>
                        <div data-kind="sift">
                            <button type="button" class="btn btn-default btn-block text-left">取值...</button>
                        </div>
                        <%for (String ln : siftEnum) {%>
                        <div data-kind="enum" data-name="<%=ln%>" class="sift-select">
                            <select data-fn="<%=ln%>" data-ft="_enum" class="value form-control"></select>
                        </div>
                        <%} /*End for*/%>
                    </div>
                </div>
                <div class="form-group">
                    <div class="btn-toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-default" data-sift="lr" data-name="or" data-text="或">+ 或</button>
                            <button type="button" class="btn btn-default" data-sift="lr" data-name="nr" data-text="非">+ 非</button>
                            <button type="button" class="btn btn-default" data-sift="lr" data-name="ar" data-text="与">+ 与</button>
                        </div>
                        <div class="btn-group pull-right form-control-static">
                            <a href="javascript:;" onclick="alert('?'+$.param($(this).closest('form').find('.sift-root input:hidden')))">检查参数</a>
                        </div>
                    </div>
                </div>
                <div class="alert alert-warning alert-dismissible" role="alert">
                    <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <p>
                        <i class="bi bi-arrow-up-circle-fill"></i>
                        <strong>高级查询：</strong>
                        <span>请选择字段、关系、取值，这将添加一条筛查参数；</span>
                        <span>“与”、“或”、“非”，表示分组内各条间关系；</span>
                        <span>点击分组标题可激活分组，新设置的的将放入其下；</span>
                        <span>如需变更筛查条件的分组，按住条目拖拽过去即可；</span>
                        <span>点击“&times;”删除条目或分组。</span>
                    </p>
                    <p>
                        <i class="bi bi-arrow-left-circle-fill"></i>
                        <strong>快捷查询：</strong>
                        <span>可对文本、标签、选项、关联进行简单查询，各项仅支持单个取值；</span>
                        <span>可对数值、日期、时间等进行区间范围查询，包含最大值和最小值。</span>
                        <span>当快捷查询无法满足需求时请使用高级查询。</span>
                    </p>
                </div>
            </div>
        </div>
        <hr style="margin-top: 0; border-color: #ccc;"/>
        <div class="form-foot">
            <div class="form-group board">
                <div  class="btn-toolbar">
                    <button type="submit" class="btn btn-primary">查找</button>
                    <button type="reset"  class="btn btn-default">重置</button>
                    <div class="btn-group pull-right form-control-static checkbox" style="margin: 0;">
                        <label><input type="checkbox" name="ob" value="-"> 按匹配度排序</label>
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
                <div class="chartbox col-xs-9" style="height: 100%; display:none"></div>
                <div class="checkbox col-xs-3" style="height: 100%; display:none"></div>
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
        _url: "<%=_module%>/<%=_entity%>/search.act?<%=Cnst.AB_KEY%>=_text,_fork&<%=Cnst.OB_KEY%>=<%=_ob%>&<%=Cnst.RB_KEY%>=<%=_rb%>",
        loadData: loadres,
        sendUrls: [
            [ '<%=_module%>/<%=_entity%>/delete.act',
              '.delete', '<%=_locale.translate("fore.delete.confirm", _title)%>' ]
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

    var siftobj = siftbox.hsSift({
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
    context.on("click", ".toolbox .sifter", function() {
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
                  + ".filt-group[data-name='"+n+"'],"
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
        if (siftbox.find("[data-sift=fn]>[value!='-']").size() == 0) {
            siftbox.find(".sift-body" ).hide();
        }
        if (siftbox.find(".filt-group").size() == 0) {
            siftbox.find(".filt-body" ).hide();
        }
        if (siftbox.find("[data-sift=fn]>[value!='-']").size() == 0
        &&  siftbox.find(".filt-group").size() == 0) {
            findbox.find(".sifter").remove();
        }
        if (statbox.find(".stat-group").size() == 0) {
            findbox.find(".statis").remove();
        }
        <%if ("select".equals(_action)) {%>
        // 单选移除跨页全选
        if (! loadbox.is(".pickmul")) {
            findbox.find(".checks").remove();
        }
        <%} /*End If */%>

        // 自适滚动
        var h = hsFlexRoll(listbox.filter(".rollbox"), $("#main-context"));
        if (h > 300) {
            statbox.css ("max-height", h+"px")
                   .css ("overflow-y", "auto");
            h = h - 80; // 去掉边框和底部高度
        if (h > 300) {
            siftbox.find(".sift-body")
                   .css ("max-height", h+"px")
                   .css ("overflow-y", "auto");
            siftbox.find(".filt-body")
                   .css ("max-height", h+"px")
                   .css ("overflow-y", "auto");
        }}

        // 加载数据
        listobj.load(null, findbox);

        }); // End Promise
    });
})(jQuery);
</script>