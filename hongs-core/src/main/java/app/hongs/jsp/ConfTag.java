package app.hongs.jsp;

import app.hongs.CoreConfig;
import app.hongs.util.Tool;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * 配置信息读取标签
 *
 * <h3>使用方法:</h3>
 * <pre>
 * &lt;hs:conf load"config.name"/&gt;
 * &lt;hs:conf key="config.key" [esc="yes|no|EscapeSymbol"] [def="default.value"]/&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ConfTag extends TagSupport {

  private CoreConfig conf = null;
  private String     load = null;
  private String     key  = null;
  private String     esc  = null;
  private String     def  = null;

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = this.pageContext.getOut();

    if (this.conf == null) {
      conf = CoreConfig.getInstance().clone();
    }

    if (this.load != null) {
      conf.load(this.load);
    }

    if (this.key  != null) {
      String str = conf.getProperty(this.key , this.def != null ? this.def : "");

      if (this.esc  != null
      &&  !    "".equals(this.esc)
      &&  !  "no".equals(this.esc)) {
        if ("yes".equals(this.esc)) {
          str = Tool.escape(str);
        }
        else if ("xml".equals(this.esc)) {
          str = Pagelet.escapeXML (str);
        }
        else if ("url".equals(this.esc)) {
          str = Pagelet.escapeURL (str);
        }
        else if ("jss".equals(this.esc)) {
          str = Pagelet.escapeJSS (str);
        }
        else {
          str = Tool.escape(str, this.esc);
        }
      }

      try {
        out.print(str);
      } catch (java.io.IOException ex) {
        throw new JspException("Error in ConfTag", ex);
      }
    }

    return TagSupport.SKIP_BODY;
  }

  public void setLoad(String load) {
    this.load = load;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setEsc(String esc) {
    this.esc = esc;
  }

  public void setDef(String def) {
    this.def = def;
  }

}
