# Modelo de programação linear inteira para solucionar problema PAP

import sys
from os import path
import numpy as np
import re

def instanceGen(f_name):
    f = open(f_name, "r")
    content = f.readlines()
    f.close()

    P = content[0][2:]
    D = content[1][2:]
    T = content[2][2:]
    S = content[3][2:]
    H = content[4][2:]

    param = {"P" : P, "D" : D, "T" : T, "S" : S, "H" : H}
    for input in param:
        v = param.get(input).replace('\n','')
        print("{} = {}\n".format(input, v))

    hd = np.zeros(shape = (int(D), 1), dtype = "int")
    for disc in range (len(hd)):
        hd[disc] = content[disc + 6].replace('\n', '')
    print("hd length = {}".format(len(hd)))
    print("hd:\n{}\n".format(hd))

    apd = np.zeros(shape = (int(P), int(D)), dtype = "int")
    for prof in range (len(apd[0])):
        line = content[7 + len(hd) + prof]
        #print("Linha {} = \n{}\n".format(prof, line))
        v = ""
        disc = 0
        for ch in line:
            #print("ch = {}".format(ch))
            #print("code = {}".format(ord(ch)))
            if ord(ch) != 9 and ord(ch) != 10:
                v = v + ch
                print("v = {}".format(v))
            else:
                apd[prof, disc] = int(v)
                v = ""
                disc += 1

    print("apd = \n{}\n".format(apd))

    rpt = np.zeros(shape = (int(P), int(T)), dtype = "int")
    for disp in range (len(rpt)):
        line = content[8 + len(hd) + len(apd) + disp]
        #print("Linha {} = \n{}\n".format(disp, line))
        t = 0
        for ch in line:
            #print("ch = {}".format(ch))
            #print("code = {}".format(ord(ch)))
            if ord(ch) != 9 and ord(ch) != 10:
                v = ch
            else:
                rpt[disp, t] = int(v)
                t += 1

    print("rpt = \n{}\n".format(rpt))

if __name__ == "__main__":

    f_name = sys.argv[1]
    if path.exists(f_name):
        print("Arquivo de entrada: {}\n".format(f_name[10:]))
        instanceGen(f_name)
    else:
        sys.exit("Erro: O arquivo {} nao existe\n".format(f_name))