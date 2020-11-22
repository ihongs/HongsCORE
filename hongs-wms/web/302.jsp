<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="java.io.PrintStream"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%
    Integer code;
    String  text;
    String  href;
    String  link;
    code = (Integer ) request.getAttribute("javax.servlet.error.status_code");
    text = (String  ) request.getAttribute("javax.servlet.error.message");
    href = (String  ) request.getAttribute("javax.servlet.location");
    link = CoreLocale.getInstance().translate("core.error.redirect");
    if (href == null) {
        href  = request.getContextPath() + "/";
    }
    if (code != null
    &&  code != 301
    &&  code != 302 ) {
        response.setStatus(code);
    }
%>
<!doctype html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="Refresh" content="3; url=<%=escapeXML(href)%>">
    <title><%=escapeXML(link)%></title>
    <style type="text/css">
        html ,
        body {
            background: #066;
            color  : #eee;
            width  : 100%;
            height : 100%;
            margin : 0;
            padding: 0;
        }
        a {
            color  : #eee;
            text-decoration: none;
        }
        .context {
            width  : 100%;
            height : 100%;
            display: flex;
            align-items: center;     /* 垂直居中 */
            justify-content: center; /* 水平居中 */
        }
        .content {
            max-width: 600px;
            font-size:  28px;
        }
    </style>
</head>
<body>
    <!--MSG: <%=escapeXML(text)%>-->
    <div class="context">
        <div class="content">
            <a href="<%=escapeXML(href)%>"
              title="<%=escapeXML(link)%>">
                     <%=escapeXML(text)%>
            </a>
        </div>
    </div>
</body>
</html>