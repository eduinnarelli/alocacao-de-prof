# Modelo de programação linear inteira para solucionar problema PAP

import sys
from os import path
import numpy as np
import gurobipy as gp
from gurobipy import GRB
from gurobipy import quicksum

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

def solve(P, D, T, S, H, hd, apd, rpt, f_name):
    try:
        # Criacao do modelo
        model = gp.Model("PAP instancia {}\n".format(f_name))

        # Variaveis de decisao
        # Variavel binaria x_{p}_{d} indica se o professor p é alocado a disciplina d
        x_vars = {(p, d) : model.addVar(vtype = GRB.BINARY, name = "x_{0}_{1}".format(p, d))
                  for p in range(P) for d in range(D)}

        # Variavel binaria y_{d}_{t} indica se a disciplina d é alocada no periodo t
        y_vars = {(d, t) : model.addVar(vtype = GRB.BINARY, name = "y_{0}_{1}".format(d, t))
                  for d in range(D) for t in range(T)}

        # Variavel binaria z_{p}_{t} indica se o professor p é alocado no periodo t
        z_vars = {(p, t) : model.addVar(vtype = GRB.BINARY, name = "x_{0}_{1}".format(p, t))
                  for p in range(P) for t in range(T)}

        # Restricoes

        # Garante que uma disciplina pode estar alocada a no maximo um professor
        r0 = {(d) : 
        model.addConstr(
                lhs = gp.quicksum(x_vars[p, d] for p in range(P)),
                sense = GRB.LESS_EQUAL,
                rhs = 1, 
                name = "r0_{0}".format(d))
            for d in range(D)}

        # Cada disciplina d exige hd periodos em uma semana
        r1 = {(d) : 
        model.addConstr(
                lhs = gp.quicksum(y_vars[d, t] for t in range(T)),
                sense = GRB.EQUAL,
                rhs = hd[d], 
                name = "r1_{0}".format(d))
            for d in range(D)}

        # O instituto possui S salas, portanto no maximo S disciplinas podem ser ministradas no mesmo periodo
        r2 = {(t) : 
        model.addConstr(
                lhs = gp.quicksum(y_vars[d, t] for d in range(D)),
                sense = GRB.LESS_EQUAL,
                rhs = S, 
                name = "r2_{0}".format(t))
            for t in range(T)}

        # Cada professor p possui restricoes de quais periodos nao pode lecionar, se r_{p}_{t} = 1 entao o 
        # professor p pode lecionar no periodo t. Caso contrario, r_{p}_{t} = 0
        r3 = {(p, t) : 
        model.addConstr(
                lhs = z_vars[p, t],
                sense = GRB.LESS_EQUAL,
                rhs = rpt[p, t], 
                name = "r3_{0}_{1}".format(p, t))
            for p in range(P) for t in range(T)}

        # Um professor pode estar alocado a mais de uma disciplina, no entanto a carga de disciplinas do professor deve somar no maximo H períodos
        r4 = {(p) : 
        model.addConstr(
                lhs = gp.quicksum(z_vars[p, t] for t in range(T)),
                sense = GRB.LESS_EQUAL,
                rhs = H, 
                name = "r4_{0}".format(p))
            for p in range(P)}

        # Se um professor e alocado a uma disciplina e esta e alocada num período, o professor e alocado no mesmo período
        r5 = {(p, d, t) : 
        model.addConstr(
                lhs = x_vars[p, d] + y_vars[d, t] - 1,
                sense = GRB.LESS_EQUAL,
                rhs = z_vars[p, t], 
                name = "r5_{0}_{1}_{2}".format(p, d, t))
            for p in range(P) for d in range(D) for t in range(T)}

        # Funcao objetivo
        # Maximiza os professores com a maior avaliacao possivel alocados para cada disciplina
        exp1 = gp.quicksum(apd[p, d] * x_vars[p, d] 
                         for d in range(D) 
                         for p in range(P))

        exp2 = 100 * gp.quicksum(1 - quicksum(x_vars[p, d] for p in range(P)) 
                         for d in range(D))

        exp = exp1 - exp2
        model.setObjective(exp, GRB.MAXIMIZE)

        # Otimizar
        model.setParam(GRB.Param.TimeLimit, 1800.0)
        model.optimize()
        
        print("Nome do modelo: ", model.ModelName)
        print("\nValor da funcao objetivo: ", model.ObjVal)
        print("\nNumero de variaveis de decisao: ", model.NumVars)
        print("\nNumero de restricoes: ", model.NumConstrs)
        print("\nNumero de objetivos: ", model.NumObj)
        print("\nNumero de iteracoes: ", model.IterCount)
        print("\nTempo de execucao: ", model.Runtime)

    except gp.GurobiError as e:
        print('Error code ' + str(e.errno) + ': ' + str(e))

    except AttributeError:
        print('Encountered an attribute error')


if __name__ == "__main__":

    # Verifica se arquivo de entrada foi fornecido corretamente
    if len(sys.argv) != 2:
        sys.exit("Uso: python pap_pli.py instances\nome_do_arquivo.pap")
    else:
        f_path = sys.argv[1]
        f_name = f_path[10:]
        if path.exists(f_path):
            print("Arquivo de entrada: {}\n".format(f_name))

            # Gera instancias a partir do arquivo de entrada
            P, D, T, S, H, hd, apd, rpt = instanceGen(f_path)

            # Imprime todos os parâmetros gerados
            param = {"P" : P, "D" : D, "T" : T, "S" : S, "H" : H}
            for input in param:
                v = param.get(input).replace('\n','')
                print("{} = {}\n".format(input, v))

            print("hd = \n{}\n".format(hd))
            print("apd = \n{}\n".format(apd))
            print("rpt = \n{}\n".format(rpt))

            # Resolve a instancia via Gurobi
            solve(int(P), int(D), int(T), int(S), int(H), hd, apd, rpt, f_name)

        else:
            sys.exit("Erro: O arquivo {} nao existe\n".format(f_path))