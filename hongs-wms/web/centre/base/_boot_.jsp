<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.FormSet"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.dh.ModelCase"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    /**
     * 获取全局搜索字段
     */
    Set<String> getWordable() throws HongsException {
        Map fc = (Map) _fields.get("word");
        if (fc != null && ! Synt.declare(fc.get("readonly"), false)) {
            return /**/ Synt.setOf("word");
        }

        if (_sample == null ) _sample = new Sample(_fields);
        Set fs = _sample.getCaseNames("wordable");
        if (fs == null || fs.isEmpty()) {
            fs = _sample.getCaseNames("srchable");
        }
        return fs;
    }

    /**
     * 获取可搜索的字段
     */
    Set<String> getSrchable() throws HongsException {
        if (_sample == null ) _sample = new Sample(_fields);
        Set fs = _sample.getCaseNames("srchable");
        return fs;
    }

    private static class Sample extends ModelCase {
        public Sample(Map fields) {
            setFields(/**/fields);
        }
    }

    Map        _fields = null;
    ModelCase  _sample = null;
%>
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

            /**
             * 从字段配置里提取表单标题
             */
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

        if (_fields == null) {
            throw new HongsException(0x1104, _locale.translate("core.error.no.thing"));
        }

        //out.println("<!-- " + io.github.ihongs.util.Dawn.toString(_fields) + " -->");
    }
%>