#!/usr/bin/python
#coding=utf-8

# 调试日志过滤工具
# 用于对调试日志进行过滤.
# 一个日志段落可能多行, 非起始行用制表符开头, 此方法用于替代 grep 来对日志进行筛选.
# 作者: kevin.hongs@gmail.com
# 修订: 2016/06/06

import re
import sys

if  __name__ == "__main__":
    def cmd_help():
        print "Usage: | clogs.py REGEXP"
        print "Another options:"
        print "  -n --name             Action or Cmdlet name"
        print "  -d --addr             Client IP address"
        print "  -l --leve             Logs save level"
        print "  -t --time             Logs time range"
        print "  -h --help             Show this msg"

    if  len(sys.argv) < 2:
        cmd_help( )
        sys.exit(0)

    tx = None
    tn = None
    ip = None
    lv = None
    tr = None

    opts, args = getopt(sys.argv[1:], "n:a:l:t:h", ["name", "addr", "leve", "time", "help"])
    for n,v in opts:
        if  n in ("-n", "--name"):
            if  v.startswith('*')
                tn = re.compile(re.escape(v[:-1]+".*"))
            elif  v.endswith('*'):
                tn = re.compile(re.escape(".*"+v[ 1:]))
            else:
                tn = re.compile(re.escape(v))
        if  n in ("-d", "--addr"):
            if  v.startswith('*')
                ip = re.compile(re.escape(v[:-1]+".*"))
            elif  v.endswith('*'):
                ip = re.compile(re.escape(".*"+v[ 1:]))
            else:
                ip = re.compile(re.escape(v))
        if  n in ("-l", "--leve"):
            lv = v
        if  n in ("-t", "--time"):
            tr = v
        if  n in ("-h", "--help"):
            cmd_help( )
            sys.exit(0)

    if  len(args) > 1:
        tx = args[0]
        tx = re.compile(tx)

    rh = re.compile(r'^(\d+/\d+/\d+) ') # 起始行
    rl = re.compile(r'^(\s)')           # 连续行
    rf = None # 是否找到
    rb = None # 日志缓冲

    for ln in sys.stdin:
        if  rb:
            if  rl.search(ln):
                rb = rb + ln
                continue

            if  rf or (tx and tx.search(rb)):
                print  rb.strip()

        rb = None
        rf = None

        if  not rh.search(ln):
            continue

        rb = ln
        mt = ln.strip().split(' ', 6) # 日志拆解, 此处格式: Date Time ClientAddr ActionPath

        if  tn and tn.match(mt[4]):
            rf = True
            continue

        if  ip and ip.match(mt[3]):
            rf = True
            continue

        if  lv and lv == mt[2]:
            rf = True
            continue

        if  tm:
            pass

    # 退出时检查缓冲区是否有内容
    # 不做此操作可能遗失最后一个
    if  rb:
        if  rf or (tx and tx.search(rb)):
            print  rb.strip()
