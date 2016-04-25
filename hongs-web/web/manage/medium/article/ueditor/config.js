(function () {
    var URL = window.UEDITOR_HOME_URL || HsCONF.BASE_HREF + "/static/addons/ueditor/";

    window.UEDITOR_CONFIG = {
        UEDITOR_HOME_URL: URL
        , serverUrl     : HsCONF.BASE_HREF + "/manage/medium/article/ueditor/upload.jsp"
        , iframeCssUrl  : HsCONF.BASE_HREF + "/static/assets/css/bootstrap.min.css"
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
        , topOffset     : 50
        , maximumWords  : 65535
        , saveInterval  : 0
        , enableAutoSave: false
        , initialFrameWidth: '100%'
        , initialStyle  : "body{margin:10px 18px;font-size:15px;font-family:'Open Sans','Helvetica Neue',Helvetica,Arial,STHeiti,'Microsoft Yahei',sans-serif;}"
        , toolbars: [[
            'source', 'fullscreen',
            'bold', 'italic', 'underline', 'strikethrough',
            'forecolor', 'backcolor', 'insertorderedlist', 'insertunorderedlist',
            'lineheight', 'rowspacingtop', 'rowspacingbottom',
            'indent', 'justifyleft', 'justifycenter', 'justifyright', 'justifyjustify',
            'link', 'blockquote', 'inserttable',
            'insertimage', '', 'insertvideo', 'attachment',
            'removeformat', 'pasteplain', 'preview', 'print',
            'paragraph', 'insertcode'

        ]]
        , shortcutMenu: [
            "bold", "italic", "underline", 'strikethrough',
            "forecolor", "backcolor", "insertorderedlist", "insertunorderedlist"
        ]
        , insertcode: {
            'plain':'Plain Text',
            'js':'Javascript',
            'java':'Java',
            'cpp':'C/C++',
            'php':'PHP',
            'python':'Python',
            'ruby':'Ruby',
            'pl':'Perl',
            'c#':'C#',
            'vb':'Vb',
            'scala':'Scala',
            'groovy':'Groovy',
            'delphi':'Delphi',
            'erlang':'Erlang',
            'html':'Html',
            'xml':'Xml',
            'css':'Css',
            'sql':'Sql',
            'bash':'Bash/Shell',
            'ps':'PowerShell'
        }
    };

})();
