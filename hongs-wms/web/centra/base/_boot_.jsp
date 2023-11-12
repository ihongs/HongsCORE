<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.CruxException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.FormSet"%>
<%@page import="io.github.ihongs.dh.JFigure"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    /**
     * 获取全局搜索字段
     */
    Set<String> getWordable(Map _fields) throws CruxException {
        Map fc = (Map) _fields.get("word");
        if (fc != null
        &&!Synt.declare(fc.get("readonly"), false)
        &&!Synt.declare(fc.get("disabled"), false)) {
            return /**/ Synt.setOf("word");
        }

        Sample   _sample = new Sample( _fields  );
        Set fs = _sample.getCaseNames("wordable");
        if (fs == null || fs.isEmpty()) {
            fs = _sample.getCaseNames("srchable");
        }
        return fs;
    }

    /**
     * 获取可搜索的字段
     */
    Set<String> getSrchable(Map _fields) throws CruxException {
        Sample   _sample = new Sample( _fields  );
        Set fs = _sample.getCaseNames("srchable");
        return fs;
    }

    private static class Sample extends JFigure {
        public Sample(Map fields) {
            setFields(/**/fields);
        }
    }
%>
<%
    String     _title = "";
    String     _config;
    String     _module;
    String     _entity;
    CoreLocale _locale;
    Map        _fields = null;
    Map        _params = null;

    {
        // 拆解路径
        int i;
        _config = "";
        _module = ActionDriver.getOriginPath(request);
        i = _module.lastIndexOf('/');
        _module = _module.substring( 1, i );
        i = _module.lastIndexOf('/');
        _entity = _module.substring( 1+ i );
        _module = _module.substring( 0, i );

        // 获取语言
        _locale = CoreLocale.getMultiple(_module +"/"+ _entity, _module, "default");

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
                    _config = name;
                    _fields = FormSet.getInstance(name).getForm(_entity);
                } catch (CruxException ex) {
                    if (ex.getErrno() != 910
                    &&  ex.getErrno() != 912) { // 非表单缺失
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
                    _config = name;
                    _fieldx = FormSet.getInstance(name).getForm(_entity);
                } catch (CruxException ex) {
                    if (ex.getErrno() != 910
                    &&  ex.getErrno() != 912) { // 非表单缺失
                        throw ex ;
                    }
                    break;
                }

                // 非破坏式合并
                _fieldx = new LinkedHashMap(_fieldx);
                for(Object ot : _fields.entrySet()) {
                    Map.Entry et = (Map.Entry) ot ;
                    Object    fn = et.getKey  (  );
                    Map fc = (Map) et.getValue(  );
                    Map fx = (Map) _fieldx.get(fn);
                    if (null != fx) {
                        fx = new LinkedHashMap(fx);
                        fx.putAll(fc);
                        _fieldx.put(fn, fx);
                    } else {
                        _fieldx.put(fn, fc);
                    }
                }
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
            throw new CruxException(404, _locale.translate("core.error.no.thing"));
        }
    }
%>