package io.github.ihongs.serv.caesar;

import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

/**
 * 维护脚本
 *
 * @author Hongs
 */
abstract public class CaesarEvalet extends ActionDriver implements HttpJspPage
{

  @Override
  public void jspInit()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void jspDestroy()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void doAction(Core core, ActionHelper ah)
    throws ServletException, IOException
  {
    HttpServletRequest  req = ah.getRequest ( );
    HttpServletResponse rsp = ah.getResponse( );

    // 检查来源
    DispatcherType dt = req.getDispatcherType();
    if (dt != DispatcherType.INCLUDE && dt != DispatcherType.FORWARD) {
        rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "What's your problem?");
        return;
    }
    if (! getOriginPath(req).startsWith("/caesar/eval.")) {
        rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "What's your problem?");
        return;
    }

    // 绑定输出, 通常外部已设
    PrintStream out = new PrintStream(rsp.getOutputStream(), true);
    if (core.get(CombatHelper.OUT.key()) == null) {
        CombatHelper.OUT.set(out);
    }
    if (core.get(CombatHelper.ERR.key()) == null) {
        CombatHelper.ERR.set(out);
    }

    try
    {
      this._jspService (req, rsp);
    }
    catch ( ServletException ex )
    {
        Throwable ax = ex.getCause();
        if (ax == null) { ax = ex; }

        ax.printStackTrace(CombatHelper.ERR.get());
    }
    catch ( RuntimeException ax )
    {
        ax.printStackTrace(CombatHelper.ERR.get());
    }
  }

  /**
   * 命令参数
   * @return
   */
  public String[] args() {
      ActionHelper ah = ActionHelper.getInstance();
      List<String> ar = Synt.asList( ah.getRequestData().get("args") );
      return ar.toArray( new String[ ar.size() ] );
  }

}
