/* global self, echarts */

(function($) {
    function setPieOption(a, legendData, seriesData) {
        var b  = a[1].replace(/([A-Z])/g, "$1 ")
                     .replace(/ \d*$/, "") +"B";
        legendData.push(a[2]+"("+b+")");
        seriesData.push({
                name  : a[2]+"("+b+")" ,
                value : a[0]
            });
    }

    function getPieOption(data, title) {
        var legendData = [];
        var seriesDat0 = [];
        var seriesDat1 = [];

        for(var k in data) {
            if (k != '#' && k != '$') {
                continue;
            }
            setPieOption(data[ k ], legendData, seriesDat0);
        }

        for(var k in data) {
            if (k == '#' || k == '$' || k == '@' || k == '.' || k == '!') {
                continue;
            }
            setPieOption(data[ k ], legendData, seriesDat1);
        }

        if (data["!"][0] != 0) {
            setPieOption(data["!"], legendData, seriesDat1);
        }

        var option = {
            tooltip: {
                trigger: 'item',
                formatter: "{a} <br/>{b} : {c} ({d}%)"
            },
            toolbox: {
                show : false,
                feature : {
                    mark : {show: true},
                    magicType : {
                        show: true,
                        type: ['pie', 'bar']
                    },
                    restore : {show: true},
                    saveAsImage : {show: true}
                }
            },
            calculable : false,
            legend: {
                orient : 'vertical',
                x : 'left',
                data:legendData
            },
            series: [
                {
                    itemStyle : {
                        normal : {
                            label : {
                                show : false
                            },
                            labelLine : {
                                show : false
                            }
                        }
                    },
                    center : ['82%', '50%'],

                    name:title+'所在磁盘',
                    type:'pie',
                    selectedMode: 'single',
                    radius : [ 0, 50],

                    data:seriesDat0
                },
                {
                    itemStyle : {
                        normal : {
                            label : {
                                show : false
                            },
                            labelLine : {
                                show : false
                            }
                        }
                    },
                    center : ['82%', '50%'],

                    name:title+'对应目录',
                    type:'pie',
                    radius : [60, 80],

                    data:seriesDat1
                }
            ]
        };

        return option;
    }

    function getSysOption(data, ctime) {
        var option = {
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    params = params[0];
                    var date = new Date(params.name);
                    return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + ' : ' + params.value[1];
                },
                axisPointer: {
                    animation: false
                }
            },
            grid: {
                top: 15,
                left: 15,
                right: 15,
                bottom: 15
            },
            xAxis: {
                show: false,
                type: 'time',
                splitLine: {
                    show: false
                }
            },
            yAxis: {
                show: false,
                type: 'value',
                splitLine: {
                    show: false
                }
            },
            series: [{
                type: 'line',
                showSymbol: false,
                hoverAnimation: false,
                data: [],
                itemStyle: {
                    normal: {
                        color: '#d5d5d5'
                    }
                },
                areaStyle: {
                    normal: {
                        color: '#f5f5f5'
                    }
                },
                markLine: {
                    silent: true,
                    symbol: "",
                    label: {
                        "normal": {
                            "position": "start"
                        }
                    },
                    data: [{
                        yAxis: 0
                    }, {
                        yAxis: 1
                    }, {
                        yAxis: 2
                    }, {
                        yAxis: 3
                    }, {
                        yAxis: 4
                    }, {
                        yAxis: 5
                    }]
                }
            }],
            visualMap: {
                show: false,
                pieces: [{
                    gt: 0,
                    lte: 1,
                    color: '#096'
                }, {
                    gt: 1,
                    lte: 2,
                    color: '#ffde33'
                }, {
                    gt: 2,
                    lte: 3,
                    color: '#ff9933'
                }, {
                    gt: 3,
                    lte: 4,
                    color: '#cc0033'
                }, {
                    gt: 4,
                    lte: 5,
                    color: '#660099'
                }, {
                    gt: 5,
                    color: '#7e0023'
                }],
                outOfRange: {
                    color: '#999'
                }
            }
        };
        
        addSysOption(option, data, ctime);
        
        return option;
    }
    
    function addSysOption(opts, data, ctime) {
        ctime = hsFmtDate(ctime, "yyyy/MM/dd HH:mm:ss");
        var list = opts.series[0].data;
        list.push({
            value: [ctime, data.load[0]],
            name :  ctime
        });
        if (list.length > 60) {
            list.shift();
        }
    }

    function getRunOption(data, ctime) {
        var option = {
            tooltip : {
                formatter: "{a} <br/>{c}"
            },
            toolbox: {
                show : false,
                feature : {
                    mark : {show: true},
                    restore : {show: true},
                    saveAsImage : {show: true}
                }
            },
            series : [
                {
                    name:'负载',
                    type:'gauge',
                    radius : '100%',
                    min:0,
                    max:10,
                    splitNumber:10,
                    axisLine: {            // 坐标轴线
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: [[0.1, '#228822'],[0.5, '#ff8800'],[1.0, '#ff4400']], 
                            width: 10
                        }
                    },
                    axisTick: {            // 坐标轴小标记
                        show: false,
                        length :15,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: 'auto'
                        }
                    },
                    splitLine: {           // 分隔线
                        length :20,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
                            color: 'auto'
                        }
                    },
                    title : {
                        show : false,
                        textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
                            fontWeight: 'bolder',
                            fontSize: 20,
                            fontStyle: 'italic'
                        }
                    },
                    detail : {
                        show : false,
                        textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
                            fontWeight: 'bolder'
                        }
                    },
                    data : [{}]
                },
                {
                    name:data.free[2],
                    type:'gauge',
                    center : ['25%', '50%'],    // 默认全局居中
                    radius : '80%',
                    min:0,
                    max:data.size[0],
                    endAngle:45,
                    splitNumber:4,
                    axisLine: {            // 坐标轴线
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: [[0.1, '#ff4500'],[1, '#48b']],
                            width: 8
                        }
                    },
                    axisTick: {            // 坐标轴小标记
                        length :12,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: 'auto'
                        }
                    },
                    axisLabel: {
                        formatter:function(v){
                            return '';
                        }
                    },
                    splitLine: {           // 分隔线
                        length :20,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
                            color: 'auto'
                        }
                    },
                    pointer: {
                        width:5,
                    },
                    title : {
                        show: false,
                        offsetCenter: [0, '-30%'],       // x, y，单位px
                    },
                    detail : {
                        show: false,
                        textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
                            fontWeight: 'bolder'
                        }
                    },
                    data : [{}]
                },
                {
                    name:data.used[2],
                    type:'gauge',
                    center : ['75%', '50%'],    // 默认全局居中
                    radius : '75%',
                    min:0,
                    max:data.dist[0],
                    startAngle:135,
                    endAngle:45,
                    splitNumber:4,
                    axisLine: {            // 坐标轴线
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: [[0.25, '#228b22'],[0.75, '#48b'],[1, '#ff4500']], 
                            width: 8
                        }
                    },
                    axisTick: {            // 坐标轴小标记
                        show : false,
                        splitNumber:5,
                        length :10,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: 'auto'
                        }
                    },
                    axisLabel: {
                        formatter:function(v){
                            return '';
                        }
                    },
                    splitLine: {           // 分隔线
                        length :15,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
                            color: 'auto'
                        }
                    },
                    pointer: {
                        width:2
                    },
                    title : {
                        show: false
                    },
                    detail : {
                        show: false
                    },
                    data : [{}]
                },
                {
                    name:data.uses[2],
                    type:'gauge',
                    center : ['75%', '50%'],    // 默认全局居中
                    radius : '75%',
                    min:0,
                    max:data.dist[0],
                    startAngle:315,
                    endAngle:225,
                    splitNumber:4,
                    axisLine: {            // 坐标轴线
                        lineStyle: {       // 属性lineStyle控制线条样式
                            color: [[0.25, '#228b22'],[0.75, '#48b'],[1, '#ff4500']], 
                            width: 8
                        }
                    },
                    axisTick: {            // 坐标轴小标记
                        show: false
                    },
                    axisLabel: {
                        formatter:function(v){
                            return '';
                        }
                    },
                    splitLine: {           // 分隔线
                        length :15,        // 属性length控制线长
                        lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
                            color: 'auto'
                        }
                    },
                    pointer: {
                        width:2
                    },
                    title : {
                        show: false
                    },
                    detail : {
                        show: false
                    },
                    data : [{}]
                }
            ]
        };
        
        addRunOption(option, data, ctime);

        return option;
    }

    function addRunOption(opts, data, ctime) {
        opts.series[0].data[0].value = data.load[0];
//      opts.series[0].data[0].name  = data.load[1];
        opts.series[1].data[0].value = data.free[0];
//      opts.series[1].data[0].name  = hsFmtNum(data.free[0]/data.size[0]*100)+"%, "+data.free[1].replace(/([A-Z])/g, "$1 ")+"B";
        opts.series[2].data[0].value = data.used[0];
//      opts.series[2].data[0].name  = hsFmtNum(data.free[0]/data.size[0]*100)+"%, "+data.used[1].replace(/([A-Z])/g, "$1 ")+"B";
        opts.series[3].data[0].value = data.uses[0];
//      opts.series[3].data[0].name  = hsFmtNum(data.free[0]/data.size[0]*100)+"%, "+data.uses[1].replace(/([A-Z])/g, "$1 ")+"B";
    }

    function setAllCharts(ec, et) {
        $.hsAjax({
            url: "manage/info/search.act",
            dataType: "json",
            success: function(rst) {
                var box;
                var opts;
                var chart;
                var runChart;
                var sysChart;
                var runOpts = {};
                var sysOpts = {};
                var context = $("#manage-info");

                /*
                box = context.find(".sys-info-box");
                opts = rst.info.sys_info;
                box.find("[data-fn=java]").text(    "Java "  +opts.java);
                box.find("[data-fn=name]").text(opts.name+" "+opts.vers);
                box.find("[data-fn=user]").text(opts.user);
                opts = rst.info.app_info;
                opts.open_time = hsFmtDate(opts.open_time, hsGetLang("datetime.format"));
                opts.base_href = location.protocol+"//"+location.host+opts.base_href+"/";
                box.find("[data-fn=server_id]").text(opts.server_id);
                box.find("[data-fn=open_time]").text(opts.open_time);
                box.find("[data-fn=base_href]").text(opts.base_href);
                box.find("[data-fn=base_path]").text(opts.base_path);
                */

                // 历史负载
                box = context.find(".sys-info-box")[0];
                opts = getSysOption(rst.info.run_info, rst.info.now_msec);
                chart = ec.init(box, et);
                chart.setOption(opts);
                sysChart = chart;
                sysOpts = opts;

                // 当前负载
                box = context.find(".run-info-box")[0];
                opts = getRunOption(rst.info.run_info, rst.info.now_msec);
                chart = ec.init(box, et);
                chart.setOption(opts);
                runChart = chart;
                runOpts = opts;

                // 网站目录
                box = context.find(".base-dir-pie")[0];
                opts = getPieOption(rst.info.base_dir, "网站");
                chart = ec.init(box, et);
                chart.setOption(opts);

                // 数据目录
                box = context.find(".data-dir-pie")[0];
                opts = getPieOption(rst.info.data_dir, "数据");
                chart = ec.init(box, et);
                chart.setOption(opts);

                // 配置目录
                box = context.find(".conf-dir-pie")[0];
                opts = getPieOption(rst.info.conf_dir, "配置");
                chart = ec.init(box, et);
                chart.setOption(opts);

                // 核心目录
                box = context.find(".core-dir-pie")[0];
                opts = getPieOption(rst.info.core_dir, "系统");
                chart = ec.init(box, et);
                chart.setOption(opts);

                var itr = setInterval(function() {
                    if (!context.is(":visible")) {
                        clearInterval(itr);
                        return;
                    }
                    $.hsAjax({
                        url: "manage/info/search.act?rb=run_info",
                        dataType: "json",
                        success: function(rst) {
                            addSysOption(sysOpts, rst.info.run_info, rst.info.now_msec);
                            sysChart.setOption(sysOpts, true);
                            
                            addRunOption(runOpts, rst.info.run_info, rst.info.now_msec);
                            runChart.setOption(runOpts, true);
                        }
                    });
                }, 2000);
            }
        });
    }

    self.infoInit = function() {
        setAllCharts(echarts);
        /*
        require.config({
            paths: {
                echarts: hsFixUri('static/addons/echarts')
            }
        });
        require(
            [
                'echarts',
                'echarts/theme/sakura',
                'echarts/chart/gauge',
                'echarts/chart/pie'
            ],
            setAllCharts
        );
        */
    };
})(jQuery);