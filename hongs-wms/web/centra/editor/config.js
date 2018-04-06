(function () {
    var URL = window.UEDITOR_HOME_URL || HsCONF.BASE_HREF + "/static/addons/ueditor/";

    window.UEDITOR_CONFIG = {
        UEDITOR_HOME_URL: URL
        , serverUrl     : HsCONF.BASE_HREF + "/centra/editor/upload.jsp"
        , imageUrlPrefix: HsCONF.BASE_HREF
        , videoUrlPrefix: HsCONF.BASE_HREF
        , fileUrlPrefix : HsCONF.BASE_HREF
        , imageManagerUrlPrefix: HsCONF.BASE_HREF
        , videoManagerUrlPrefix: HsCONF.BASE_HREF
        , fileManagerUrlPrefix : HsCONF.BASE_HREF
        , lang          : HsLANG.lang
        , langPath      : URL + "lang/"
        , theme         : 'default'
        , themePath     : URL + "themes/"
        , iframeCssUrl  : URL + "themes/iframe.css"
        , topOffset     : 50
        , maximumWords  : 65535
        , saveInterval  :  0
        , enableAutoSave: false
        , initialFrameWidth: '100%'
        , initialStyle  : "body{margin:10px 18px;font-size:15px;font-family:'Microsoft Yahei','Helvetica Neue','Open Sans',Helvetica,Tahoma,Arial,sans-serif;}"
        , toolbars: [[
            'fullscreen', 'source', '|',
            'bold', 'italic', 'underline', 'strikethrough', 'forecolor', 'backcolor', '|',
            'insertorderedlist', 'insertunorderedlist', 'lineheight', 'rowspacingtop', 'rowspacingbottom', '|',
            'indent', 'justifyleft', 'justifycenter', 'justifyright', 'justifyjustify', '|',
            'paragraph', 'fontsize', 'fontfamily', '|',
            'link', 'blockquote', 'inserttable', 'map', 'insertimage', 'insertvideo', 'attachment', '|',
            'removeformat', 'formatmatch', 'searchreplace', 'pasteplain', 'preview', 'print'
        ]]
        , shortcutMenu: [
            "bold", "italic", "underline", 'strikethrough',
            "forecolor", "backcolor", "insertorderedlist", "insertunorderedlist"
        ]
        , fontfamily: [
            { label:'',name:'yahei',val:'微软雅黑,Microsoft YaHei'},
            { label:'',name:'songti',val:'宋体,SimSun,STSong'},
            { label:'',name:'kaiti',val:'楷体,SimKai,STHeiti'},
            { label:'',name:'heiti',val:'黑体,SimHei,STKaiti'},
            { label:'',name:'lishu',val:'隶书,SimLi'},
            { label:'Sans Serif',name:'sans-serif',val:'sans-serif'},
            { label:     'Serif',name:     'serif',val:     'serif'},
            { label:'Monospace' ,name:'monospace' ,val:'monospace' }
        ]
        , fontsize: [10, 11, 12, 14, 16, 18, 20, 24, 36, 48, 60]
    };

})();
