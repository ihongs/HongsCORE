package io.github.ihongs.jsp;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.util.Syno;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * 配置信息读取标签
 *
 * <h3>使用方法:</h3>
 * <pre>
 * &lt;hs:conf load"config.name|CoreConfig"/&gt;
 * &lt;hs:conf key="config.key" [esc="yes|no|EscapeSymbol"] [def="default.value"]/&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ConfTag extends TagSupport {

  private CoreConfig conf = null;
  private String     name = null;
  private String     key  = null;
  private String     esc  = null;
  private String     def  = null;

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = this.pageContext.getOut();

    if (this.conf == null) {
    if (this.name == null) {
      conf = CoreConfig.getInstance(/**/);
    } else {
      conf = CoreConfig.getInstance(name);
    }}

    if (this.key  != null) {
      String str = conf.getProperty(this.key , this.def != null ? this.def : "");

      if (this.esc  != null
      &&  !    "".equals(this.esc)
      &&  !  "no".equals(this.esc)) {
        if ("yes".equals(this.esc)) {
          str = Syno.escape(str);
        }
        else if ("xml".equals(this.esc)) {
          str = Pagelet.escapeXML (str);
        }
        else if ("url".equals(this.esc)) {
          str = Pagelet.encodeURL (str);
        }
        else if ("jss".equals(this.esc)) {
          str = Pagelet.escapeJSS (str);
        }
        else {
          str = Syno.escape(str, this.esc);
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

  public void setConf(Object conf) {
    if (conf instanceof CoreConfig) {
      this.conf = (CoreConfig) conf;
    } else {
      this.name = conf.toString ( );
    }
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
