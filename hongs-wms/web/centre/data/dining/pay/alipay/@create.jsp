<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.CoreConfig"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="com.alipay.api.AlipayApiException"%>
<%@page import="com.alipay.api.AlipayClient"%>
<%@page import="com.alipay.api.DefaultAlipayClient"%>
<%@page import="com.alipay.api.request.AlipayTradeWapPayRequest"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Properties"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!

private String[] getPrice(String billId) throws HongsException {
    if (billId == null || billId.length() == 0) {
        throw new HongsException(0x1100, "bill_id required");
    }
    Data mod = Data.getInstance("centre/data/dining/bill", "bill");
    Map  row = mod.get(billId);
    if ( row == null || row.isEmpty()) {
        throw new HongsException(0x1100, "The bill does not exist.");
    }
    return new String[] {String.valueOf(Synt.asFloat(row.get("total"))), (String) row.get("name")};
}

%>
<%
Properties conf = CoreConfig.getInstance("centre/data/dining/pay");
String billId = request.getParameter("bill_id");
String appid  = conf.getProperty("bining.pay.weixin.appid");
String appsk  = conf.getProperty("bining.pay.weixin.appsk");
String alipk  = conf.getProperty("bining.pay.weixin.alipk");
String apiUrl = conf.getProperty("bining.pay.weixin.api.url");
String rurl   = "http://" + request.getServerName() + Core.BASE_HREF
              + "/public/dining/bill/" + billId;
String nurl   = "http://" + request.getServerName() + Core.BASE_HREF
              + "/centre/data/dining/pay/alipay/update.act";

String[] a = getPrice(billId);
Map content   = Synt.mapOf(
    "product_code", "QUICK_WAP_PAY",
    "out_trade_no", billId,
    "total_amout" , a[0],
    "subject"     , a[1]
);

AlipayClient alipayClient = new DefaultAlipayClient(apiUrl, appid, appsk, "json", "utf-8", alipk, "RSA2");
AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
alipayRequest.setReturnUrl(rurl);
alipayRequest.setNotifyUrl(nurl);
alipayRequest.setBizContent(app.hongs.util.Data.toString(content));
String form="";
try {
    form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
} catch (AlipayApiException e) {
    e.printStackTrace();
}

ActionHelper helper = Core.getInstance(ActionHelper.class);
helper.reply(Synt.mapOf("html", form));

%>