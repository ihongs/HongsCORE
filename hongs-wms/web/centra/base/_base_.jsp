<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="java.util.Map"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    Map        $menu  = null;
    String     $hrel  = null;
    String     $title = null;
    String     $module;
    String     $entity;
    CoreLocale $locale;

    {
        // 拆解路径
        int i;
        $module = ActionDriver.getOriginPath(request);
        i = $module.lastIndexOf('/');
        $module = $module.substring( 1, i );
        i = $module.lastIndexOf('/');
        $entity = $module.substring( 1+ i );
        $module = $module.substring( 0, i );

        // 获取语言
        $locale = CoreLocale.getInstance( ).clone(  );
        $locale.fill($module);
        $locale.fill($module +"/"+ $entity);

        // 查找标题
        String[] a= {$module +"/"+ $entity , $module , "centra"};
        for (String name : a) try {
            NaviMap site = NaviMap.getInstance ( name );
            Map menu;
            menu  = site.getMenu($module +"/"+ $entity +"/");
            if (menu != null) {
                    $menu   = menu;
                    $hrel   = (String) menu.get("hrel");
                    $title  = Synt.declare (menu.get("text"),"");
                break;
            }
            menu  = site.getMenu($module +"/#"+$entity);
            if (menu != null) {
                    $menu   = menu;
                    $hrel   = (String) menu.get("hrel");
                    $title  = Synt.declare (menu.get("text"),"");
                break;
            }
        } catch (HongsException ex) {
            // 忽略配置文件缺失的异常情况
            if (ex.getErrno() != 920) {
                throw ex ;
            }
        }

        // 没菜单配置则抛出资源缺失异常
        if ($title == null) {
            throw new HongsException(404, $locale.translate("core.error.no.thing"));
        }
    }
%>