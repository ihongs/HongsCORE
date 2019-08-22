<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    /**
     * 静默或非调试模式下开启缓存策略
     * 但须在系统启动之后和八小时以内
     */
    if (0 == Core.DEBUG || 8 == (8 & Core.DEBUG)) {
        long n , m;
        n = Math.max(Core.STARTS_TIME, Core.ACTION_TIME.get() - 28800000) / 1000 * 1000; // 精确到秒
        m = request .getDateHeader("If-Modified-Since");
        if ( n > m) {
            response.setDateHeader("Last-Modified", n );
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
    }

    String     _title = null;
    String     _module;
    String     _entity;
    CoreLocale _locale;

    {
        // 拆解路径
        int i;
        _module = ActionDriver.getOriginPath(request);
        i = _module.lastIndexOf('/');
        _module = _module.substring( 1, i );
        i = _module.lastIndexOf('/');
        _entity = _module.substring( 1+ i );
        _module = _module.substring( 0, i );

        // 获取语言
        _locale = CoreLocale.getInstance( ).clone(  );
        _locale.fill(_module);
        _locale.fill(_module +"/"+ _entity);

        // 查找标题
        String[] a= {_module +"/"+ _entity , _module , "centra"};
        for (String name : a) try {
            NaviMap site = NaviMap.getInstance ( name );
            Map menu;
            menu  = site.getMenu(_module +"/"+ _entity +"/");
            if (menu != null) {
                    _title  = (String) menu.get("text");
                if (_title != null) {
                    _title  = _locale.translate(_title);
                } else {
                    _title  = "";
                }
                break;
            }
            menu  = site.getMenu(_module +"/#"+_entity);
            if (menu != null) {
                    _title  = (String) menu.get("text");
                if (_title != null) {
                    _title  = _locale.translate(_title);
                } else {
                    _title  = "";
                }
                break;
            }
        } catch (HongsException ex) {
            // 忽略配置文件缺失的异常情况
            if (ex.getErrno() != 0x10e0) {
                throw ex ;
            }
        }

        // 没菜单配置则抛出资源缺失异常
        if (_title == null) {
            throw new HongsException(0x1104, _locale.translate("core.error.no.thing"));
        }
    }
%>