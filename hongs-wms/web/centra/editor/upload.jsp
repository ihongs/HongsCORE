<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="com.baidu.ueditor.ActionEnter" %>
<%@page language="java"
        contentType="text/html; charset=UTF-8"
        pageEncoding="UTF-8"
        trimDirectiveWhitespaces="true" %>
<%!
    private static Set<String> allowActions = new HashSet();
    static {
        allowActions.add("config");
        allowActions.add("uploadimage");
        allowActions.add("uploadvideo");
        allowActions.add("uploadfile" );
        allowActions.add( "listimage" );
        allowActions.add( "listvideo" );
        allowActions.add( "listfile"  );
    }
%>
<%
    String action = request.getParameter("action");
    if (! allowActions.contains(action) ) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("Unsupported action: "+ action );
        return;
    }

    request.setCharacterEncoding("utf-8");
    response.setHeader("Content-Type","text/html");

    String rootPath = application.getRealPath("/");

    out.write( new ActionEnter(request , rootPath).exec( ) );
%>