<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CoreLocale"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionDriver"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="java.util.Map"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    /**
     * 静默或非调试模式下开启缓存策略
     * 但须在系统启动之后和八小时以内
     */
    if (0 == Core.DEBUG || 8 == (8 & Core.DEBUG)) {
        long s , a , m;
        s = Core.STARTS_TIME;
        a = Core.ACTION_TIME.get();
        m = request .getDateHeader("If-Modified-Since");
        if ( m < Math.max(s , a - 28800000) ) {
            response.setDateHeader("Last-Modified", a );
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
    }

    boolean    $hide = false;
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
                    $hide   = "HIDE".equals(menu.get("hrel"));
                    $title  = (String) menu.get("text");
                if ($title != null) {
                    $title  = $locale.translate($title);
                } else {
                    $title  = "";
                }
                break;
            }
            menu  = site.getMenu($module +"/#"+$entity);
            if (menu != null) {
                    $hide   = "HIDE".equals(menu.get("hrel"));
                    $title  = (String) menu.get("text");
                if ($title != null) {
                    $title  = $locale.translate($title);
                } else {
                    $title  = "";
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
        if ($title == null) {
            throw new HongsException(404, $locale.translate("core.error.no.thing"));
        }
    }
%>