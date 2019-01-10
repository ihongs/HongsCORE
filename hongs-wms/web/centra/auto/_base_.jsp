<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="java.util.Map"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String     _title = "";
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
                    break;
                }
            }
            menu  = site.getMenu(_module +"/#"+_entity);
            if (menu != null) {
                    _title  = (String) menu.get("text");
                if (_title != null) {
                    _title  = _locale.translate(_title);
                    break;
                }
            }
        } catch (HongsException ex) {
            // 忽略配置文件缺失的异常情况
            if (ex.getErrno() != 0x10e0) {
                throw ex ;
            }
        }
    }
%>