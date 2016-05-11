<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Model"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page extends="app.hongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String root = request.getContextPath();
    String href = "handle/x160510/company";
    Model model = DB.getInstance("manage/x160510").getModel("company");
    ActionHelper helper = Core.getInstance(ActionHelper.class);

    Map       data = model.retrieve( helper.getRequestData() );
    List<Map> list = Synt.declare(data.get("list"),List.class);

%>
<!DOCTYPE html>
<html>
    <head>
        <%@include file="../../../handle/meta.jsp" %>
        <title>全部公司</title>
    </head>
    <body>
        <div class="container">
            <div class="row">
                <%for (Map info : list) {%>
                <%
                    String   id = (String) info.get( "id" );
                    String logo = (String) info.get("logo");
                    String name = (String) info.get("name");
                    String note = Synt.asserts(info.get("note"), "");
                    if (logo == null || logo.equals("")) {
                        logo  = root + "/static/addons/meeting/img/empty_snap_bg_sm.jpg";
                    } else
                    if (!logo.startsWith("/") && !logo.matches("^(\\w+:)?//")) {
                        logo  = root + "/" + logo.replaceFirst("(\\.\\w+)$", "_sm$1");
                    }
                %>
                <div class="col-md-4">
                    <div class="row">
                        <div class="col-sm-4">
                            <a class="section_snap" href="<%=root%>/<%=href%>/?id=<%=id%>">
                                <img src="<%=logo%>" alt="<%=name%>" style="width:100%;overflow:hidden;"/>
                            </a>
                        </div>
                        <div class="col-sm-8">
                            <h3>
                                <a href="<%=root%>/<%=href%>/?id=<%=id%>"><%=name%></a>
                            </h3>
                            <p>
                                <a href="<%=root%>/<%=href%>/?id=<%=id%>"><%=note%></a>
                            </p>
                        </div>
                    </div>
                </div>
                <%} /*End For*/%>
            </div>
        </div>
    </body>
</html>