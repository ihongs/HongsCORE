<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.FormSet"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.dh.JFigure"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String     _title = "";
    String     _module;
    String     _entity;
    CoreLocale _locale;
    Map        _fields = null;
    Map        _params = null;

    {
        // 拆解路径
        int i;
        _module = ActionDriver.getOriginPath(request);
        // 一层文件名
        i = _module.lastIndexOf('/');
        _module = _module.substring( 0, i );
        // 二层子目录
        i = _module.lastIndexOf('/');
        _module = _module.substring( 1, i );
        // 实体和模块
        i = _module.lastIndexOf('/');
        _entity = _module.substring( 1+ i );
        _module = _module.substring( 0, i );

        // 获取语言
        _locale = CoreLocale.getInstance( ).clone(  );
        _locale.fill(_module);
        _locale.fill(_module +"/"+ _entity);

        // 获取字段
        String[] a= {_module +"/"+ _entity , _module};
        for(String name : a) {
            Map _fieldx = null;
                _fields = null;

            /**
             * 字段以 centra/data 的字段为基础
             * 但可在 centre/data 重设部分字段
             */
            do {
                try {
                    _fields = FormSet.getInstance(name).getFormTranslated(_entity);
                } catch (HongsException ex) {
                    if (ex.getErrno() != 0x10e8
                    &&  ex.getErrno() != 0x10ea) {
                        throw ex;
                    }
                    break;
                }

                if (_fields != null && name.startsWith("centre/") ) {
                    name = "centra/" + name.substring (7);
                } else {
                    break;
                }

                try{
                    _fieldx = FormSet.getInstance(name).getFormTranslated(_entity);
                } catch (HongsException ex) {
                    if (ex.getErrno() != 0x10e8
                    &&  ex.getErrno() != 0x10ea) {
                        throw ex ;
                    }
                    break;
                }

                _fieldx = new LinkedHashMap(_fieldx);
                _fieldx.putAll(_fields);
                _fields = _fieldx;
            }
            while (false);

            /**
             * 从字段配置里提取表单标题
             */
            if (_fields != null) {
                _params  = (Map) _fields.get("@");
                if (_params == null) {
                    _params = new LinkedHashMap();
                }
                _title   = (String) _params.get("__text__");
                if (_title  == null) {
                    _title  = "";
                }
                break;
            }
        }

        if (_fields == null) {
            throw new HongsException(404, _locale.translate("core.error.no.thing"));
        }
    }
%>