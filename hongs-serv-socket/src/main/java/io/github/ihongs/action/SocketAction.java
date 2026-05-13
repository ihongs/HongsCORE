package io.github.ihongs.action;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.anno.Action;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

/**
 * WebSocket 动作
 * 必须有一个无参构造器
 * 必须设置注解 @Action
 * 必须要实现 onMessage
 * @author Hongs
 */
abstract public class SocketAction extends Endpoint {

    public SocketAction() {
        super();
    }

    @Action("__main__")
    public void main(ActionHelper helper) throws CruxException {
        HttpServletRequest  req = helper.getRequest ();
        HttpServletResponse rsp = helper.getResponse();

        // 协议升级
        SocketHelper.upgrade(req, rsp, this.getClass());
    }

    @Override
    public void onOpen (Session sn, EndpointConfig ec) {
        // 消息接收
        sn.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String msg) {
                SocketAction.this.onMessage(sn, msg);
            }
        });

        try (
            SocketHelper sh = SocketHelper.getInstance(sn, "open" );
        ) {
            this.onOpen (sh);
        }
        catch (Exception ex) {
            CoreLogger.error(ex);
        }
    }

    public void onOpen (SocketHelper sh) {
        // Pass
    }

    @Override
    public void onClose(Session sn, CloseReason rs) {
        super.onClose(sn,rs);
        try (
            SocketHelper sh = SocketHelper.getInstance(sn, "close");
        ) {
            this.onClose(sh, rs);
        }
        catch (Exception ex) {
            CoreLogger.error(ex);
        }
    }

    public void onClose(SocketHelper sh, CloseReason rs) {
        // Pass
    }

    @Override
    public void onError(Session sn, Throwable er) {
        super.onError(sn,er);
        try (
            SocketHelper sh = SocketHelper.getInstance(sn, "error");
        ) {
            this.onError(sh, er);
        }
        catch (Exception ex) {
            CoreLogger.error(ex);
        }
    }

    public void onError(SocketHelper sh, Throwable ex) {
        // Pass
    }

    public void onMessage(Session sn, String msg) {
        try (
            SocketHelper sh = SocketHelper.getInstance(sn, "message");
        ) {
            this.onMessage(sh, msg);
        }
        catch (Exception ex) {
            CoreLogger.error(ex);
        }
    }

    public void onMessage(SocketHelper sh, String msg) {
        // Pass
    }

}
