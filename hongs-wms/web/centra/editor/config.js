(function () {
    var URL = window.UEDITOR_HOME_URL || HsCONF.BASE_HREF + "/static/addons/ueditor/";

    window.UEDITOR_CONFIG = {
        UEDITOR_HOME_URL: URL
        , serverUrl     : HsCONF.BASE_HREF + "/centra/editor/upload.jsp"
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
            'source', 'fullscreen', '|',
            'bold', 'italic', 'underline', 'strikethrough', 'forecolor', 'backcolor', '|',
            'insertorderedlist', 'insertunorderedlist', 'lineheight', 'rowspacingtop', 'rowspacingbottom',
            'indent', 'justifyleft', 'justifycenter', 'justifyright', 'justifyjustify', '|',
            'link', 'blockquote', 'inserttable', 'insertimage', 'insertvideo', 'attachment', '|',
            'removeformat', 'pasteplain', 'preview', 'print', '|',
            'paragraph', 'fontsize', 'fontfamily'//, 'insertcode'
        ]]
        , shortcutMenu: [
            "bold", "italic", "underline", 'strikethrough',
            "forecolor", "backcolor", "insertorderedlist", "insertunorderedlist"
        ]
        ,'fontfamily':[
            { label:'',name:'yahei',val:'微软雅黑,Microsoft YaHei'},
            { label:'',name:'songti',val:'宋体,SimSun'},
            { label:'',name:'kaiti',val:'楷体,SimKai'},
            { label:'',name:'heiti',val:'黑体,SimHei'},
            { label:'',name:'lishu',val:'隶书,SimLi'},
            { label:'',name:'andaleMono',val:'andale mono'},
            { label:'',name:'arial',val:'arial,helvetica,sans-serif'},
            { label:'',name:'arialBlack',val:'arial black,avant garde'},
            { label:'',name:'impact',val:'impact,chicago'},
            { label:'',name:'timesNewRoman',val:'times new roman'}
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
