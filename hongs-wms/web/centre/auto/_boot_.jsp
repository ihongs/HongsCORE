<%@page import="app.hongs.CoreLocale"%>
<%@page import="app.hongs.HongsError"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.HongsExpedient"%>
<%@page import="app.hongs.action.ActionDriver"%>
<%@page import="app.hongs.action.FormSet"%>
<%@page import="app.hongs.action.NaviMap"%>
<%@page import="app.hongs.db.DB"%>
<%@page import="app.hongs.db.Mview"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
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
        _module = ActionDriver.getOriginPath(request);
        i = _module.lastIndexOf('/');
        _module = _module.substring( 1, i );
        i = _module.lastIndexOf('/');
        _entity = _module.substring( 1+ i );
        _module = _module.substring( 0, i );

        // 通过模型视图获取字段、标题
        try {
            Mview view = new Mview(DB.getInstance(_module).getTable(_entity));
            _fields = view.getFields();
            _locale = view.getLocale();
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

        // 通过表单配置获取字段、标题
        try {
            String _modulx = FormSet.hasConfFile(_module + "/" + _entity)
                      ?  _module + "/" + _entity : _entity ;
            Data data = Data.getInstance(_modulx , _entity);
            _fields = data.getFields();
            _title  = Synt.declare(data.getParams().get("__text__"), "" );
            _locale = CoreLocale.getInstance().clone();
            _locale.fill (  _module  );
            _locale.fill (  _modulx  );
            break;
        } catch (HongsExpedient ex) {
            if (ex.getErrno() != 0x1104) {
                throw ex;
            }
        }

        throw new HongsException(0x1104, "资源不存在");
    }
    while (false);
%>