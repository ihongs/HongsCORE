<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.CoreConfig"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="org.apache.http.HttpResponse"%>
<%@page import="org.apache.http.util.EntityUtils"%>
<%@page import="org.apache.http.impl.client.HttpClients"%>
<%@page import="org.apache.http.client.methods.HttpRequestBase"%>
<%@page import="org.apache.http.client.methods.HttpPost"%>
<%@page import="org.apache.http.entity.StringEntity"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.TreeMap"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Properties"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="java.net.URI"%>
<%@page import="java.net.URISyntaxException"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.UnsupportedEncodingException"%>
<%@page import="java.security.MessageDigest"%>
<%@page import="java.security.InvalidKeyException"%>
<%@page import="java.security.NoSuchAlgorithmException"%>
<%@page import="javax.crypto.Mac"%>
<%@page import="javax.crypto.spec.SecretKeySpec"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
private static final Pattern RESP_PATT = Pattern.compile("<([^>]+)><!\\[CDATA\\[(.*?)\\]\\]></[^>]+>", Pattern.DOTALL | Pattern.MULTILINE);

private float getPrice(String billId) throws HongsException {
    if (billId == null || billId.length() == 0) {
        throw new HongsException(0x1100, "bill_id required");
    }
    Data mod = Data.getInstance("centre/data/dining/bill", "bill");
    Map  row = mod.get(billId);
    if ( row == null || row.isEmpty()) {
        throw new HongsException(0x1100, "The bill does not exist.");
    }
    return Synt.asFloat(row.get("total"));
}

private String getText(Map<String, String> xml, String key) throws HongsException {
    StringBuilder sb = new StringBuilder();
    sb.append("<xml>" );
    for (Map.Entry<String, String> one : xml.entrySet()) {
        sb.append("<" ).append(one.getKey()).append(">")
          .append(one.getValue())
          .append("</").append(one.getKey()).append(">");
    }
    sb.append("<sign>").append(getSign(xml, key)).append("</sign>");
    sb.append("</xml>");
    return sb.toString();
}

private String getSign(Map<String, String> xml, String key) throws HongsException {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> one : xml.entrySet()) {
        String nam = one.getKey(  );
        String val = one.getValue();
        if (val == null || val.length() == 0) {
            continue;
        }
        sb.append("&").append(nam)
          .append("=").append(val);
    }
    sb.append("key").append("=").append(key);
    return md5(sb.toString());
}

private String post(String url, String req) throws HongsException {
    try {
        HttpPost http = new HttpPost();
        http.setEntity(new StringEntity(req));
        http.setHeader("Content-Type", "text/xml;charset=UTF-8");
        http.setURI(new URI(url));
        HttpResponse rsp = HttpClients
                 .createDefault()
                 .execute( http );
        return EntityUtils.toString(rsp.getEntity(), "UTF-8").trim();
    } catch (UnsupportedEncodingException ex) {
        throw new HongsException.Common(ex);
    } catch (URISyntaxException ex) {
        throw new HongsException.Common(ex);
    } catch (IOException ex) {
        throw new HongsException.Common(ex);
    }
}

private Map toMap(String str) {
    Map     map = new HashMap();
    Matcher mat = RESP_PATT.matcher ( str );
    while ( mat.find() ) {
        map.put(mat.group(1), mat.group(2));
    }
    return  map;
}

private String mac(String str, String key) throws HongsException {
    try {
        SecretKeySpec k = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        Mac m = Mac.getInstance( "HmacSHA1" ); m.init( k );
        byte[] s = m.doFinal(str.getBytes());
        return byte2hex16str(s);
    } catch (NoSuchAlgorithmException ex) {
        throw new HongsException.Common(ex);
    } catch (InvalidKeyException ex) {
        throw new HongsException.Common(ex);
    }
}

private String md5(String str) throws HongsException {
    try {
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] s = m.digest (str.getBytes());
        return byte2hex16str(s);
    } catch (NoSuchAlgorithmException ex) {
        throw new HongsException.Common(ex);
    }
}

private String byte2hex16str(byte[] pzwd) {
    byte[] pxwd = new byte[pzwd.length * 2];
    for (int i = 0, j = 0; i < pzwd.length; i ++) {
        byte pzbt = pzwd[i];
        pxwd[j++] = HEX16[pzbt >>> 4 & 15];
        pxwd[j++] = HEX16[pzbt       & 15];
    }
    return new String(pxwd);
}

private static final byte[] HEX16 = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
%>
<%
Properties conf = CoreConfig.getInstance("centre/data/dining/pay");

String apiUrl = conf.getProperty("bining.pay.weixin.api.url");
String openId = request.getParameter("open_id");
String billId = request.getParameter("bill_id");
String mchid  = conf.getProperty("bining.pay.weixin.mchid");
String appid  = conf.getProperty("bining.pay.weixin.appid");
String appsk  = conf.getProperty("bining.pay.weixin.appsk");
float  price  = getPrice( billId );
String nstr   = Core.newIdentity();
String nurl   = "http://" + request.getServerName() + Core.BASE_HREF
              + "/centre/data/dining/pay/weixin/update.act";
String addr   = ActionDriver.getClientAddr(request);

Map<String, String> inf = Synt.mapOf(
    "h5_info", Synt.mapOf(
        "type", "Wap",
        "wap_url" , "http://" + request.getServerName(),
        "wap_name", ""
    )
);

Map<String, String> xml = new TreeMap();
xml.put("appid"             , appid);
xml.put("mch_id"            , mchid);
xml.put("openid"            , openId);
xml.put("out_trade_no"      , billId);
xml.put("nonce_str"         , nstr);
xml.put("notify_url"        , nurl);
xml.put("spbill_create_ip"  , addr);
xml.put("total_fee"         , Float.toString(price));
xml.put("scene_info"        , app.hongs.util.Data.toString(inf));
xml.put("trade_type"        , "MWEB");
xml.put("attach"            , "");
xml.put("body"              , "");

String text   = getText(xml, appsk);
String resp   = post(apiUrl, text );
Map    rsp    = toMap(resp);

ActionHelper helper = Core.getInstance(ActionHelper.class);
if ("SUCCESS".equals(rsp.get("result_code") )
&&  "SUCCESS".equals(rsp.get("return_code"))) {
    helper.reply( Synt.mapOf("href", rsp.get("mweb_url")));
} else {
    helper.fault((String) rsp.get("return_msg"));
}

%>