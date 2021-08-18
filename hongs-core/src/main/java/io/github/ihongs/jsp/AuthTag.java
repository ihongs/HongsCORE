package io.github.ihongs.jsp;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.NaviMap;
import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * 动作权限判断标签
 *
 * <h3>使用方法:</h3>
 * <pre>
 * &lt;hs:auth conf="navi.name" [menu="menu.href"] [role="role.name"] [act="action"] [not="true|false"]/&gt;
 * &lt;hs:auth conf="navi.name" [menu="menu.href"] [role="role.name"] [act="action"] [not="true|false"]&gt;Some Text&lt;/hs:auth&gt;
 * act,role,menu 任意给一个即可, 给多个则必须满足全部条件才行.
 * </pre>
 *
 * @author Hongs
 */
public class AuthTag extends BodyTagSupport {

  private String  cnf = "default";
  private String  act = null;
  private String  rol = null;
  private String  men = null;
  private Boolean not = false;
  private Boolean ebb = false;

  @Override
  public int doStartTag() throws JspException {
    try {
      NaviMap nav = NaviMap.getInstance(this.cnf);
      this.ebb = (this.act == null || nav.chkAuth(this.act))
              && (this.rol == null || nav.chkRole(this.rol))
              && (this.men == null || nav.chkMenu(this.men));
    } catch ( HongsException ex) {
      throw new JspException(ex);
    }

    if (this.not) {
        this.ebb = !this.ebb;
    }

    if (this.ebb) {
      return BodyTagSupport.EVAL_BODY_BUFFERED;
    } else {
      return BodyTagSupport.SKIP_BODY;
    }
  }

  @Override
  public int doEndTag() throws JspException {
    try {
      BodyContent body = this.getBodyContent();

      if (null != body) {
        JspWriter out = body.getEnclosingWriter();

        if (this.ebb) {
          out.print(body.getString());
        }
      } else {
        JspWriter out = this.pageContext.getOut();

        if (this.ebb) {
          out.print( "true");
        } else {
          out.print("false");
        }
      }
    } catch (IOException ex) {
      throw new JspException("Error in AuthTag", ex);
    }

    return BodyTagSupport.EVAL_PAGE;
  }

  public void setConf(String cn) {
    this.cnf = cn;
  }

  public void setMenu(String men) {
    this.men = men;
  }

  public void setRole(String rol) {
    this.rol = rol;
  }

  public void setAct(String act) {
    this.act = act;
  }

  public void setNot(Boolean not) {
    this.not = not;
  }

}
