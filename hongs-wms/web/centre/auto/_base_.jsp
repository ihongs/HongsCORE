<%@page import="foo.hongs.HongsException"%>
<%@page import="foo.hongs.CoreLocale"%>
<%@page import="foo.hongs.action.ActionDriver"%>
<%@page import="foo.hongs.action.FormSet"%>
<%@page import="foo.hongs.action.NaviMap"%>
<%@page import="foo.hongs.util.Dict"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String     _module;
    String     _entity;
    String     _title ;
    CoreLocale _locale;

    do {
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

        // 从菜单中提取标题
        try {
            NaviMap site = NaviMap.hasConfFile(_module+"/"+_entity)
                         ? NaviMap.getInstance(_module+"/"+_entity)
                         : NaviMap.getInstance(_module);
            Map menu;
            menu  = site.getMenu(_module +"/"+ _entity+"/");
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
            if (ex.getErrno() != 0x10e0) {
                throw ex ;
            }
        }

        // 没有则从表单提取
        try {
            FormSet fset = FormSet.hasConfFile(_module+"/"+_entity)
                         ? FormSet.getInstance(_module+"/"+_entity)
                         : FormSet.getInstance(_module);
            Map form;
            form  = fset.getForm(_entity);
            if (form != null) {
                    _title  = (String) Dict.get( form , null, "@", "__text__" );
                if (_title != null) {
                    _title  = _locale.translate(_title);
                    break;
                }
            }
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e8
            &&  ex.getErrno() != 0x10ea) {
                throw ex ;
            }
        }

        _title  = "" ;
    }
    while (false);
%>