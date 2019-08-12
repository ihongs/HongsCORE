package io.github.ihongs.jsp;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.util.Syno;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;

/**
 * 语言信息读取标签
 *
 * <h3>使用方法:</h3>
 * <pre>
 * &lt;hs:lang load="locale.config.name"/&gt;
 * &lt;hs:lang key="locale.config.key" [esc="yes|no|EscapeSymbol"] [_0="replacement0" _1="replacement1"]/&gt;
 * &lt;hs:lang key="locale.config.key" [esc="yes|no|EscapeSymbol"] [xx="replacementX" yy="replacementY"]/&gt;
 * &lt;hs:lang key="locale.config.key" [esc="yes|no|EscapeSymbol"] [rep=String[]|List&lt;String&gt;|Map&lt;String, String&gt;]/&gt;
 * </pre>
 *
 * @author Hongs
 */
public class LangTag extends TagSupport implements DynamicAttributes {

  private CoreLocale lang = null;
  private String     load = null;
  private String     key  = null;
  private String     esc  = null;
  private String[]             repArr = null;
  private List<String>         repLst = null;
  private Map <String, String> repMap = null;

  @Override
  public int doStartTag() throws JspException {
    JspWriter out = this.pageContext.getOut();

    if (this.lang == null) {
      lang = CoreLocale.getInstance().clone();
    }

    if (this.load != null) {
      lang.load(this.load);
    }

    if (this.key  != null) {
      String str;
      if (this.repMap != null) {
        str = lang.translate(this.key, this.repMap);
      }
      else if (this.repLst != null) {
        str = lang.translate(this.key, this.repLst);
      }
      else if (this.repArr != null) {
        str = lang.translate(this.key, this.repArr);
      }
      else {
        str = lang.translate(this.key);
      }

      if (this.esc != null
      && ! "".equals(this.esc)
      && ! "no".equals(this.esc)) {
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
        throw new JspException("Error in LangTag", ex);
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

  @Override
  public void setDynamicAttribute(String uri, String name, Object value) throws JspException {
    if (value instanceof Map) {
      this.repMap = (Map<String, String>)value;
    }
    else if (value instanceof List) {
      this.repLst = (List<String>)value;
    }
    else if (value instanceof Object[]) {
      this.repArr = (String[])value;
    }
    else {
      if (name.matches("^_\\d+$")) {
        name = name.substring(1);
      }
      if (this.repMap == null) {
        this.repMap = new HashMap( );
      }
      this.repMap.put(name, (String)value);
    }
  }

}
