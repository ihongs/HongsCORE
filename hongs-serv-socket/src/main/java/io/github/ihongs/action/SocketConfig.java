package io.github.ihongs.action;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocket 配置器
 *
 * 用于初始化请求环境和记录 HttpSession 等
 * <code>
 * \@ServerEndpoint(value="/xxx" configurator=SocketConfig)
 * </code>
 *
 * @author Hongs
 */
public class SocketConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        Map head = request.getHeaders();
        Map data = request.getParameterMap ();
        Map prop = config.getUserProperties();
        prop.put(SocketHelper.class.getName() + ".httpHeaders", head);
        prop.put(SocketHelper.class.getName() + ".httpRequest", data);
        prop.put( HttpSession.class.getName(), request.getHttpSession());
    }

}
