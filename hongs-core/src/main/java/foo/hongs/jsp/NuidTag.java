package foo.hongs.jsp;

import foo.hongs.Core;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * 唯一ID生成器
 *
 * <h3>使用方法:</h3>
 * <pre>
 * &lt;hs:nuid [sid="SID"]/&gt;
 * </pre>
 *
 * @author Hongs
 */
public class NuidTag extends SimpleTagSupport {

  private String sid = null;

  @Override
  public void doTag() throws JspException {
    JspWriter out = getJspContext().getOut();

    String uid;
    if (this.sid != null) {
      uid = Core.newIdentity(this.sid);
    } else {
      uid = Core.newIdentity();
    }

    try {
      out.print(uid);

      JspFragment f = getJspBody();
      if (f != null) f.invoke(out);
    } catch (java.io.IOException ex) {
      throw new JspException("Error in NuidTag", ex);
    }
  }

  public void setSid(String sid) {
    this.sid = sid;
  }
}
