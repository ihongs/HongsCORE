#!/usr/bin/python
#coding=utf-8

# 过期文件清理工具
# 用于清理超过一定时间的日志、临时文件
# 作者: kevin.hongs@gmail.com
# 修订: 2016/03/03

import os
import re
import sys
import time
import datetime
from getopt import getopt

def hsClean(dn, tm, ep, op, nm, ne):
    """
    清理工具
    dn: 待清理的目录
    tm: 清除此时间前的文件
    ep: 删除空的目录
    op: 仅输出不删除
    nm: 文件名称正则
    ne: 排除 nm 匹配的文件
    """

    fc  = 0
    fe  = 0
    for fi in os.listdir(dn):
        if  fi == "." or fi == "..":
            continue

        fn  = os.path.join(dn, fi)
        if  os.path.islink(fn):
            continue
        if  os.path.isfile(fn):
            st  =  os.stat(fn)

            if  tm < st.st_mtime:
                continue
            if  nm:
                if  nm.match(fi):
                    if  ne:
                        continue
                else:
                    if  not ne:
                        continue

            print time.strftime("%Y/%m/%d %H:%M:%S", time.localtime(st.st_mtime)), fn
            if  not op:
                os.remove(fn)
            fe += 1
        else:
            ap  = hsClean(fn, tm, ep, op, nm, ne)

            if  not ap:
                continue
            if  not ep:
                continue

            print "0000/00/00 00:00:00" , fn
            if  not op:
                os.remove(fn)
            fe += 1
        fc += 1

    return  fc == fe

def hsPtime(tm):
    """
    时间格式
    1234567890
    1w2d3h5m6s
    2015/10/11
    2015/10/11T10:20:30
    """

    mt = re.compile(r"^\d+$").match(tm)
    if  mt:
        return int (tm)

    mt = re.compile(r"^(\d+w)?(\d+d)?(\d+h+)?(\d+m)?(\d+s)?$").match(tm)
    if  mt:
        tm = datetime.datetime.now()
        tg = mt.group(1)
        if  tg:
            tm -= datetime.timedelta(weeks=int(tg[:-1]))
        tg = mt.group(2)
        if  tg:
            tm -= datetime.timedelta( days=int(tg[:-1]))
        tg = mt.group(3)
        if  tg:
            tm -= datetime.timedelta(hours=int(tg[:-1]))
        tg = mt.group(4)
        if  tg:
            tm -= datetime.timedelta(minutes=int(tg[:-1]))
        tg = mt.group(5)
        if  tg:
            tm -= datetime.timedelta(seconds=int(tg[:-1]))
        return time.mktime(tm.timetuple())

    if  len(tm) <= 10:
        return time.mktime(time.strptime(tm, r"%Y/%m/%d"))
    else:
        return time.mktime(time.strptime(tm, r"%Y/%m/%dT%H:%M:%S"))

if  __name__ == "__main__":
    def cmd_help():
        print "Usage: clean.py DIR_NAME EXP_TIME"
        print "EXP_TIME format:"
        print "  2015/12/17T12:34:56   Before this time"
        print "  1234567890            Before this timestamp"
        print "  1w2d3h5m6s            Before some weeks, days..."
        print "Another options:"
        print "  -p --print            Just print files"
        print "  -e --empty            Remove empty dir"
        print "  -n --name REGEXP      File name regexp"
        print "  -x --deny             Exclude names"
        print "  -h --help             Show this msg"

    if  len(sys.argv) < 3:
        cmd_help( )
        sys.exit(0)

    dn = sys.argv[1]
    tm = sys.argv[2]
    ep = False
    op = False
    nm = None
    ne = False

    if  not dn:
        print "Argument 1 (folder name) required!"
        cmd_help( )
        sys.exit(1)
    if  not tm:
        print "Argument 2 (expire time) required!"
        cmd_help( )
        sys.exit(1)

    opts, args = getopt(sys.argv[3:], "pen:xh", ["print", "empty", "name=", "deny", "help"])
    for n,v in opts:
        if  n in ("-p", "--print"):
            op = True
        if  n in ("-p", "--empty"):
            ep = True
        if  n in ("-n", "--name"):
            nm = v
        if  n in ("-d", "--deny"):
            de = True
        if  n in ("-h", "--help"):
            cmd_help( )
            sys.exit(0)

    tm  = hsPtime( tm )
    dn  = os.path.abspath(dn)
    if  nm:
        nm  =  re.compile(nm)

    print "Delete files before " + time.strftime(r"%Y/%m/%d %H:%M:%S", time.localtime(tm)) + " in " + dn

    hsClean(dn, tm, ep, op, nm, ne)
