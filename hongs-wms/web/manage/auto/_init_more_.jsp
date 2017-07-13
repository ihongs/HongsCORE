<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.HongsError"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.db.Mview"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    String     _module;
    String     _entity;
    String     _title ;
    CoreLocale _locale;
    Map        _fields;

    do {
        // 拆解路径
        int i;
        _module = ActionDriver.getWorkPath(request);
        i = _module.lastIndexOf('/');
        _module = _module.substring( 1, i );
        i = _module.lastIndexOf('/');
        _entity = _module.substring( 1+ i );
        _module = _module.substring( 0, i );

        // 通过模型视图获取字段、标题
        try {
            Mview  view = new Mview(DB.getInstance(_module).getTable(_entity));
            _locale = view.getLocale();
            _fields = view.getFields();
            _title  = view.getTitle( );
            break;
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x1039) {
                throw ex;
            }
        } catch (HongsError ex) {
            if (ex.getErrno() != 0x2a  ) {
                throw ex;
            }
        }

        // 获取语言
        _locale = CoreLocale.getInstance( ).clone();
        _locale.fill(_module);
        _locale.fill(_module +"/"+ _entity);

        // 通过表单配置获取字段、标题
        try {
            FormSet fset = FormSet.hasConfFile(_module+"/"+_entity)
                         ? FormSet.getInstance(_module+"/"+_entity)
                         : FormSet.getInstance(_module);
            _fields = fset.getFormTranslated  (_entity);
            _title  = Dict.getValue( _fields, "", "@", "__text__" );
            break;
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e8
            &&  ex.getErrno() != 0x10ea) {
                throw ex;
            }
        }

        throw new HongsException(0x1104, "资源不存在");
    }
    while (false);
%>