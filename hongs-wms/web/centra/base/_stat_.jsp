<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<%@page import="io.github.ihongs.CruxException"%>
<%@page import="io.github.ihongs.action.FormSet"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<form class="findbox statbox openbox invisible well">
    <div class="row">
    <%
    Iterator its = _fields.entrySet().iterator();
    while (its.hasNext()) {
        Map.Entry et = (Map.Entry) its.next();
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
