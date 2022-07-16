#!python3
#coding=utf-8

# 数据导出到文件
# Excel 时间戳转时间串公式: =TEXT((A1+8*3600)/86400+70*365+19,"yyyy-mm-dd hh:mm:ss")

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
            fv = row.get(fn, '')
            if fv is None:
                fv = ''
            if isinstance(fv, list):
                fv = ', '.join (fv)
            fv = str(fv).strip()
            ra . append (fv)
    #   print('\t'.join (ra) )
        sh.append(ra)
    return fil

def hsTable(sh, url, cok, fil, prt=False):
    pn  = 0
    if  prt :
        dat = parse.urlencode({'pn': pn}).encode('utf-8')
        req = request.Request(url, data=dat, headers={'Cookie': cok, 'X-Requested-With': 'XMLHttpRequest'})
        rsp = request.urlopen(req)
        rst = rsp.read().decode('utf-8')
        rst = json.loads(rst)
        tn  = int (rst["page"]["pages"])

        print('%d%% Page %d/%d' % (0, 0, tn), end='')

    while True:
        pn  = pn + 1
        dat = parse.urlencode({'pn': pn}).encode('utf-8')
        req = request.Request(url, data=dat, headers={'Cookie': cok, 'X-Requested-With': 'XMLHttpRequest'})
        rsp = request.urlopen(req)
        rst = rsp.read().decode('utf-8')
        rst = json.loads(rst)
        lst = rst["list"]

        if  lst is None or not len (lst):
            break
        
        for row in lst:
            fil(sh,row)
        
        if  not prt:
            continue
        print('\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r', end='')
        print('%d%% Page %d/%d' % (int(pn / tn * 100), pn, tn), end='')

    if  prt :
        pn  = tn
        print('\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r', end='')
        print('%d%% Page %d/%d' % (int(pn / tn * 100), pn, tn), end='')
        print('')

if __name__ == '__main__':
    def cmd_help():
        print("Usage: table.py FILE.xlsx http://xx --cookie COOKIE --fields Fn1,Fn2 --labels Fn1,Fn2")
        print("Options:")
        print("  -c --cookie key=val   HTTP Cookie")
        print("  -f --fields fn1,fn2   List fields")
        print("  -l --labels fn1,fn2   List labels")
        print("  -h --help             Show this msg")

    if  len(sys.argv) < 3:
        cmd_help( )
        sys.exit(0)

    xls = sys.argv[1]
    url = sys.argv[2]
    cok = ''
    fns = []
    lns = []

    if  not xls:
        print("Argument 1 (file name) required!")
        cmd_help( )
        sys.exit(1)
    if  not url:
        print("Argument 2 (href link) required!")
        cmd_help( )
        sys.exit(1)

    opts, args = getopt(sys.argv[3:], "c:f:l:h", ["cookie=", "fields=", "labels=", "help"])
    for n,v in opts:
        if  n in ("-c", "--cookie"):
            cok = v
        if  n in ("-f", "--fields"):
            fns = v.split(',')
        if  n in ("-l", "--labels"):
            lns = v.split(',')
        if  n in ("-h", "--help"):
            cmd_help( )
            sys.exit(0)
    
    xls = os.path.abspath(xls)
    fil = hsFills(fns)

    wb  = Workbook()
    sh  = wb.active

    sh.append(lns)

    hsTable(sh, url, cok, fil, True)

    wb.save(xls)
