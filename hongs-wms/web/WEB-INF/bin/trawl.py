#!python3
#coding=utf-8

# 数据导出到文件
# Excel 时间戳转时间串公式: =TEXT((A1+8*3600)/86400+70*365+19,"yyyy-mm-dd hh:mm:ss")

import re
import os
import sys
import json
from getopt import getopt
from urllib import parse, request
from openpyxl import Workbook

def hsFills(fs):
    def fil(sh, row):
        ra = []
        for fn in fs:
            fv = hsFetch (row, fn.split('.'))
            if isinstance(fv,dict):
                fv = fv.values(  )
            if isinstance(fv,list):
                fv = ', '.join(fv)
            if  fv is None:
                fv = ''
            fv = str(fv).strip(  )
            ra . append (fv)
        sh.append(ra)
    return fil

def hsFetch(fv, ks, i=0):
    fv = fv.get(ks[ i ])
    if  fv is None:
        return fv
    if  len(ks) == i + 1:
        return fv
    if  isinstance(fv, dict):
        return   hsFetch(fv, ks, i + 1)
    if  isinstance(fv, list):
        fa = [ ]
        for fx in fv:
            fx = hsFetch(fx, ks, i + 1)
            fa . append (fx)
        return fa
    return  fv

def hsTrawl(sh, url, cok, fil, prt=False):
    pn  = 0
    tn  = 1

    while True:
        pn  = pn + 1
        dat = parse.urlencode({'pn': pn}).encode('utf-8')
        req = request.Request(url, data=dat, headers={'Cookie': cok, 'X-Requested-With': 'XMLHttpRequest'})
        rsp = request.urlopen(req)
        rst = rsp.read().decode('utf-8')
        rst = json.loads(rst)
        lst = rst.get("list")
        pag = rst.get("page")

        if  lst is None or not len (lst):
            break
        
        for row in lst:
            fil(sh,row)
        
        if  pag is None or not prt :
            continue

        tn  = int(pag.get( "total" , tn))
        print('\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r', end='')
        print('%d%% Page %d/%d' % (int(pn / tn * 100), pn, tn), end='')

    if  prt :
        pn  = tn
        print('\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r', end='')
        print('%d%% Page %d/%d' % (int(pn / tn * 100), pn, tn), end='')
        print('')

if __name__ == '__main__':
    def cmd_help():
        print("Usage: trawl.py --output FILE.xlsx --search http://xx --cookie COOKIE --fields Fn1,Fn2 --labels Fn1,Fn2")
        print("Options:")
        print("  -o --output file      Output Xlsx")
        print("  -s --search href      Search Href")
        print("  -c --cookie key=val   HTTP Cookie")
        print("  -f --fields fn1,fn2   List fields")
        print("  -l --labels fn1,fn2   List labels")
        print("  -h --help             Show this msg")

    xls = ''
    url = ''
    cok = ''
    fns = []
    lns = []

    opts, args = getopt(sys.argv[1:], "s:c:f:l:h", ["search=", "cookie=", "fields=", "labels=", "help"])
    for n,v in opts:
        if  n in ("-o", "--output"):
            xls = v
        if  n in ("-s", "--search"):
            url = v
        if  n in ("-c", "--cookie"):
            cok = v
        if  n in ("-f", "--fields"):
            fns = v.split(',')
        if  n in ("-l", "--labels"):
            lns = v.split(',')
        if  n in ("-h", "--help"):
            cmd_help( )
            sys.exit(0)
    
    if  len(opts) == 0:
        cmd_help( )
        sys.exit(0)
    if  xls == '' or url == '':
        print("Options `--output` and `--search` required")
        cmd_help( )
        sys.exit(1)

    xls = os.path.abspath(xls)
    fil = hsFills(fns)

    wb  = Workbook()
    sh  = wb.active

    sh.append(lns)

    hsTrawl(sh, url, cok, fil, True)

    wb.save(xls)
