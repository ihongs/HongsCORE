<%@page import="io.github.ihongs.combat.CombatHelper"%>
<%@page extends="io.github.ihongs.jsp.Evalet"%>
<%@page contentType="text/plain" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String[] args = args();

    if (args.length == 0) {
        CombatHelper.println(
              "Usage: access.exec access/test.jsp TEXT SECS"
        );
        return;
    }

    CombatHelper.println (args[0]) ;

    int n = args.length > 1 ? Integer.parseInt(args[1]) : 0;
    if (n > 0) {
        CombatHelper.progres(0, n) ;
    for(int i = 1 ; i <= n ; i ++) {
        Thread.sleep(1000) ;
        CombatHelper.progres(i, n) ;
    }
        CombatHelper.progres(/**/) ;
    }
%>