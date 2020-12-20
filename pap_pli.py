# Modelo de programação linear inteira para solucionar problema PAP

import sys
from os import path
import numpy as np
import re

# Realiza leitura do arquivo de entrada .pap e armazena os parâmetros
# nas matrizes hd, apd e rpt
def instanceGen(f_name):
    f = open(f_name, "r")
    content = f.readlines()
    f.close()

    # Obtem parametros das 5 primeiras linhas do arquivo
    P = content[0][2:]
    D = content[1][2:]
    T = content[2][2:]
    S = content[3][2:]
    H = content[4][2:]

    # Armazena valores de hd em um vetor de dimensao D x 1, obtem um valor por
    # linha e remove os caracteres de quebra de linha
    hd = np.zeros(shape = (int(D), 1), dtype = "int")
    for disc in range (len(hd)):
        hd[disc] = content[disc + 6].replace('\n', '')
    #print("hd length = {}".format(len(hd)))
    #print("hd:\n{}\n".format(hd))

    # Armazena valores de apd em uma matriz de dimensao P x D, obtem uma linha e
    # cada celula da matriz recebe o valor delimitado entre os espacos vazios de uma linha
    apd = np.zeros(shape = (int(P), int(D)), dtype = "int")
    for prof in range (len(apd)):
        line = content[7 + len(hd) + prof]
        #print("Linha {} = \n{}\n".format(prof, line))
        v = ""
        disc = 0
        for ch in line:
            #print("ch = {}".format(ch))
            #print("code = {}".format(ord(ch)))
            if ord(ch) != 9 and ord(ch) != 10 and ord(ch) != 0 and ord(ch) != 13 and ord(ch) != 32:
                v = v + ch
                #print("v = {}".format(v))
            else:
                apd[prof, disc] = int(v)
                v = ""
                disc += 1

    np.set_printoptions(threshold=np.inf)
    #print("apd = \n{}\n".format(apd))

    # Armazena valores de rpt em uma matriz de dimensao P x T, obtem uma linha e
    # cada celula da matriz recebe o valor delimitado entre os espacos vazios de uma linha
    rpt = np.zeros(shape = (int(P), int(T)), dtype = "int")
    for disp in range (len(rpt)):
        line = content[8 + len(hd) + len(apd) + disp]
        #print("Linha {} = \n{}\n".format(disp, line))
        t = 0
        for ch in line:
            #print("ch = {}".format(ch))
            #print("code = {}".format(ord(ch)))
            if ord(ch) != 9 and ord(ch) != 10 and ord(ch) != 0 and ord(ch) != 13 and ord(ch) != 32:
                v = ch
            else:
                rpt[disp, t] = int(v)
                t += 1

    #print("rpt = \n{}\n".format(rpt))

    return P, D, T, S, H, hd, apd, rpt

if __name__ == "__main__":

    # Verifica se arquivo de entrada foi fornecido corretamente
    if len(sys.argv) != 2:
        sys.exit("Uso: python pap_pli.py instances\nome_do_arquivo.pap")
    else:
        f_name = sys.argv[1]
        if path.exists(f_name):
            print("Arquivo de entrada: {}\n".format(f_name[10:]))

            # Gera instancias a partir do arquivo de entrada
            P, D, T, S, H, hd, apd, rpt = instanceGen(f_name)

            param = {"P" : P, "D" : D, "T" : T, "S" : S, "H" : H}
            for input in param:
                v = param.get(input).replace('\n','')
                print("{} = {}\n".format(input, v))

            print("hd = \n{}\n".format(hd))
            print("apd = \n{}\n".format(apd))
            print("rpt = \n{}\n".format(rpt))
        else:
            sys.exit("Erro: O arquivo {} nao existe\n".format(f_name))