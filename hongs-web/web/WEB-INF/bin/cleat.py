#!/usr/bin/python
#coding=utf-8

# 目录文件备份工具
# 用于备份特定的数据目录或文件
# 作者: kevin.hongs@gmail.com
# 修订: 2016/03/03

import os
import re
import sys
import time
import shutil
import tarfile
from getopt import getopt

if  __name__ == "__main__":
    def cmd_help():
        print "Usage: cleat.py SRC_PATH DST_PATH"
        print "Another options:"
        print "  -n --name             Dest name with time format"
        print "  -p --pack             Pack the back"
        print "  -z --gzip             Gzip the back"
        print "  -h --help             Show this msg"
        print "--name(-n) syntax:"
        print "  $n_%Y%m%d$x for default"
        print "  $n is file name, like 'dump'"
        print "  $x is extension, like '.sql'"
        print "  %Y,%m,%d,%H,%M,%S is current year,month,date,hour,minute,second"

    if  len(sys.argv) < 3:
        cmd_help( )
        sys.exit(0)

    sp = sys.argv[1]
    dn = sys.argv[2]
    tn = None
    pc = False
    gz = False

    if  not sp:
        print "Argument 1 (source path) required!"
        cmd_help( )
        sys.exit(1)
    if  not dn:
        print "Argument 2 (target path) required!"
        cmd_help( )
        sys.exit(1)

    rp = re.compile("/$")
    sp = rp.sub( "", sp )
    dn = rp.sub( "", dn )

    opts, args = getopt(sys.argv[3:], "n:zxh", ["name", "gzip", "pack", "help"])
    for n,v in opts:
        if  n in ("-n", "--name"):
            tn = v
        if  n in ("-p", "--pack"):
            pc = True
        if  n in ("-z", "--gzip"):
            gz = True
        if  n in ("-h", "--help"):
            cmd_help( )
            sys.exit(0)

    # 构建目标名称

    if  tn is None:
        tn = "$n_%Y%m%d$x"
    tn = time.strftime(tn)

    (fe, fn) = os.path.split   (sp)
    (fn, fe) = os.path.splitext(fn)
    tn = tn.replace("$n", fn)
    tn = tn.replace("$x", fe)

    tn = dn + "/" + tn

    if  pc or gz:
        if  gz:
            tn = tn+".tar.gz"
            md = "w:gz"
        else:
            tn = tn+".tar"
            md = "w"

    # 执行备份操作

    print "Backup files from '" + sp + "' to '" + tn + "'"

    if  pc or gz:
        zp = tarfile.open(tn, md)
        zp.add(sp)
        zp.close()
    else:
        shutil.copytree  (sp, tn)
