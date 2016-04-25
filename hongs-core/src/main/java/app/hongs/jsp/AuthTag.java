package app.hongs.jsp;

import app.hongs.HongsException;
import app.hongs.action.NaviMap;
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
 * &lt;hs:auth act="action" [not="true|false"] [els="true|false"]/&gt;
 * &lt;hs:auth act="action" [not="true|false"] [els="true|false"]&gt;Some Text&lt;/hs:auth&gt;
 * </pre>
 *
 * @author Hongs
 */
public class AuthTag extends BodyTagSupport {

  private String act;
  private Boolean not = false;
  private Boolean els = false;
  private Boolean ebb = false;
  private String cnf = "default";

  @Override
  public int doStartTag() throws JspException {
    try {
      this.ebb = NaviMap.getInstance(this.cnf).chkAuth(this.act);
    } catch ( HongsException ex) {
      throw new JspException(ex);
    }

    if (this.not) {
        this.ebb =! this.ebb;
    }

    if (this.els || this.ebb) {
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
        String[] arr = body.getString().trim()
                      .split("<!--ELSE-->", 2);

        JspWriter out = body.getEnclosingWriter();

        if (this.ebb) {
          out.print(arr[0]);
        } else if (arr.length > 1) {
          out.print(arr[1]);
        }
      } else {
        JspWriter out = this.pageContext.getOut();

        if (this.ebb) {
          out.print("true");
        } else {
          out.print("false");
        }
      }
    } catch (IOException ex) {
      throw new JspException("Error in ActTag", ex);
    }

    return BodyTagSupport.EVAL_PAGE;
  }

  public void setConf(String cn) {
    this.cnf = cn;
  }

  public void setAct(String act) {
    this.act = act;
  }

  public void setNot(Boolean not) {
    this.not = not;
  }

  public void setEls(Boolean els) {
    this.els = els;
  }

}
