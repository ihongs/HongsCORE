<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.FormSet"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.dh.ModelCase"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    private static class MyCase extends ModelCase {
        public MyCase(Map fields) {
            setFields(fields);
        }
    }

    /**
     * 获取搜索提示语
     * @param fields 表单字段
     * @param limit  数量限制
     */
    String getSearchHolder(Map fields, int limit) throws HongsException {
        StringBuilder sb = new StringBuilder();
        ModelCase mc = new MyCase(fields);
        Set fs  = mc.getCaseNames("wordable" );
        if (fs == null || fs.isEmpty( ) ) {
            fs  = mc.getCaseNames("srchable" );
        }

        int total = fs.size();
        if (limit == 0) {
            limit = ( total );
        }

        int i = 0;
        for (Object fn : fs ) {
            i ++ ;

            Map  fc;
            fc = (Map) fields.get(fn);
            fn = fc.get( "__text__" );
            if (null  == fn
            || "".equals(fn)) {
                continue ;
            }
            sb.append(fn);

            if (i < total) {
            if (i < limit) {
                sb.append( ", ");
            } else {
                sb.append("...");
                break;
            }}
        }

        return  sb.toString().replaceFirst("[, ]+$", "");
    }
%>
<%
    CoreLocale _locale;
    String     _module;
    String     _entity;
    String     _title = null;
    Map        _fields= null;

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

        // 获取字段
        String[] a= {_module +"/"+ _entity , _module};
        for(String name : a) {
            Map _params = null;
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

            // 从字段配置里提取表单标题
            if (_fields != null) {
                _params  = (Map) _fields.get ( "@" );
                if (_params != null) {
                    _title   = (String) _params.get("__text__");
                if (_title  == null) {
                    _title   =  "" ;
                }}
                break;
            }
        }

        // 没菜单配置则抛出资源缺失异常
        if (_fields == null) {
            throw new HongsException(0x1104, _locale.translate("core.error.no.thing"));
        }
    }
%>