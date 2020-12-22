# Modelo de programação linear inteira para solucionar problema PAP

import sys
import csv
import os
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
    P = int(content[0][2:].replace('\n', ''))
    D = int(content[1][2:].replace('\n', ''))
    T = int(content[2][2:].replace('\n', ''))
    S = int(content[3][2:].replace('\n', ''))
    H = int(content[4][2:].replace('\n', ''))

    # Armazena valores de hd em um vetor de dimensao D x 1, obtem um valor por
    # linha e remove os caracteres de quebra de linha
    hd = np.zeros(shape=(int(D), 1), dtype="int")
    for disc in range(len(hd)):
        hd[disc] = int(content[disc + 6].replace('\n', ''))
    #print("hd length = {}".format(len(hd)))
    # print("hd:\n{}\n".format(hd))

    # Armazena valores de apd em uma matriz de dimensao P x D, obtem uma linha e
    # cada celula da matriz recebe o valor delimitado entre os espacos vazios de uma linha
    apd = np.zeros(shape=(int(P), int(D)), dtype="int")
    for prof in range(len(apd)):
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
    rpt = np.zeros(shape=(int(P), int(T)), dtype="int")
    for disp in range(len(rpt)):
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
    param = [P, D, T, S, H, hd, apd, rpt]

    return param


def solve(P, D, T, S, H, hd, apd, rpt, f_name):
    try:
        # Criacao do modelo
        model = gp.Model(f_name)

        # Variaveis de decisao
        # Variavel binaria x_{p}_{d} indica se o professor p é alocado a disciplina d
        x_vars = {(p, d): model.addVar(vtype=GRB.BINARY, name="x_{0}_{1}".format(p, d))
                  for p in range(P) for d in range(D)}

        # Variavel binaria y_{d}_{t} indica se a disciplina d é alocada no periodo t
        y_vars = {(d, t): model.addVar(vtype=GRB.BINARY, name="y_{0}_{1}".format(d, t))
                  for d in range(D) for t in range(T)}

        # Variavel binaria z_{p}_{t} indica se o professor p é alocado no periodo t
        z_vars = {(p, t): model.addVar(vtype=GRB.BINARY, name="x_{0}_{1}".format(p, t))
                  for p in range(P) for t in range(T)}

        # Restricoes

        # Garante que uma disciplina pode estar alocada a no maximo um professor
        r0 = {(d):
              model.addConstr(
            lhs=gp.quicksum(x_vars[p, d] for p in range(P)),
            sense=GRB.LESS_EQUAL,
            rhs=1,
            name="r0_{0}".format(d))
            for d in range(D)}

        # Cada disciplina d exige hd periodos em uma semana
        r1 = {(d):
              model.addConstr(
            lhs=gp.quicksum(y_vars[d, t] for t in range(T)),
            sense=GRB.EQUAL,
            rhs=gp.quicksum(np.squeeze(hd)[d] * x_vars[p, d]
                            for p in range(P)),
            name="r1_{0}".format(d))
            for d in range(D)}

        # O instituto possui S salas, portanto no maximo S disciplinas podem ser ministradas no mesmo periodo
        r2 = {(t):
              model.addConstr(
            lhs=gp.quicksum(y_vars[d, t] for d in range(D)),
            sense=GRB.LESS_EQUAL,
            rhs=S,
            name="r2_{0}".format(t))
            for t in range(T)}

        # Cada professor p possui restricoes de quais periodos nao pode lecionar, se r_{p}_{t} = 1 entao o
        # professor p pode lecionar no periodo t. Caso contrario, r_{p}_{t} = 0
        r3 = {(p, t):
              model.addConstr(
            lhs=z_vars[p, t],
            sense=GRB.LESS_EQUAL,
            rhs=rpt[p, t],
            name="r3_{0}_{1}".format(p, t))
            for p in range(P) for t in range(T)}

        # Um professor pode estar alocado a mais de uma disciplina, no entanto a carga de disciplinas do professor deve somar no maximo H períodos
        r4 = {(p):
              model.addConstr(
            lhs=gp.quicksum(z_vars[p, t] for t in range(T)),
            sense=GRB.LESS_EQUAL,
            rhs=H,
            name="r4_{0}".format(p))
            for p in range(P)}

        # Se um professor e alocado a uma disciplina e esta e alocada num período, o professor e alocado no mesmo período
        r5 = {(p, d, t):
              model.addConstr(
            lhs=x_vars[p, d] + y_vars[d, t] - 1,
            sense=GRB.LESS_EQUAL,
            rhs=z_vars[p, t],
            name="r5_{0}_{1}_{2}".format(p, d, t))
            for p in range(P) for d in range(D) for t in range(T)}

        # Funcao objetivo
        # Maximiza os professores com a maior avaliacao possivel alocados para cada disciplina e subtrai
        # uma penalidade de valor 100 caso nao exista solucao
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

        # Analisa resultados da otimizacao
        if model.status == GRB.OPTIMAL or model.status == GRB.TIME_LIMIT:
            if model.SolCount > 0:
                print("Nome do modelo: ", model.ModelName)
                print("Valor da funcao objetivo: ", model.ObjVal)
                print("Tempo de execucao: ", model.Runtime)
                print("Numero de iteracoes: ", model.IterCount)
                print("Numero de variaveis de decisao: ", model.NumVars)
                print("Numero de restricoes: ", model.NumConstrs)
                print("Numero de objetivos: ", model.NumObj)

                with open("pap_pli_resultados.csv", 'a', newline='') as file:
                    writer = csv.writer(file)
                    writer.writerow([model.ModelName, model.ObjVal, model.Runtime,
                                     model.IterCount, model.NumVars, model.NumConstrs, model.NumObj])
            else:
                print('Modelo nao gerou solucao viavel dentro do tempo limite de {}\n'.format(model.Params.timeLimit))

        elif model.status == GRB.INF_OR_UNBD:
            print('Modelo inviavel ou ilimitado\n')

        elif model.status == GRB.INFEASIBLE:
            print('Modelo inviavel\n')

        elif model.status == GRB.UNBOUNDED:
            print('Modelo ilimitado\n')

        else:
            print('Otimizacao terminou com status {}\n'.format(model.status))
            # Consultar https://www.gurobi.com/documentation/9.1/refman/optimization_status_codes.html

    except gp.GurobiError as e:
        print('Codigo de erro {} : {}\n'.format(str(e.errno), str(e)))

    except AttributeError:
        print('Encontrado erro de atributo\n')


if __name__ == "__main__":

    # Verifica se arquivo de entrada foi fornecido corretamente
    if len(sys.argv) != 2:
        sys.exit("Uso: python pap_pli.py <diretorio com instacias .pap>")
    else:
        # Gera instancias a partir dos arquivos de entrada .pap existentes
        # no diretorio fornecido
        dir = sys.argv[1]

        if path.exists(dir):
            obj = os.scandir(dir)
            f_dict = {}

            # Escaneia o diretorio e armazena o nome do arquivo e seus parametros
            # como um par chave e valor de um dicionario
            print("Arquivos no diretorio {}:\n".format(dir))
            for entry in obj:
                if entry.is_dir() or entry.is_file():
                    if entry.name[-4:] == ".pap":
                        # print(entry.name)
                        param = instanceGen(entry)
                        # print(param)
                        f_dict[entry.name] = param

                    if len(f_dict) == 0:
                        sys.exit(
                            "Erro: O diretorio {} nao possui arquivos .pap\n".format(dir))
            # print(f_dict.items())

            # Cria arquivo .csv para armazenar os resultados
            with open("pap_pli_resultados.csv", 'a', newline='') as file:
                writer = csv.writer(file)
                writer.writerow(["Instancia", "Valor Obj", "Tempo(s)",
                                 "# Iteracoes", "# Variaveis", " # Restricoes", "# Obj"])

            # Resolve todas as instancia via Gurobi
            for inst in f_dict:
                print("Instância {}\n".format(inst[:-4]))
                param = f_dict.get(inst)

                print("P = {}".format(param[0]))
                print("D = {}".format(param[1]))
                print("T = {}".format(param[2]))
                print("S = {}".format(param[3]))
                print("H = {}".format(param[4]))
                print("hd shape = {}".format(param[5].shape))
                #print("hd = \n{}\n".format(param[5]))
                print("apd shape = {}".format(param[6].shape))
                #print("apd = \n{}\n".format(param[6]))
                print("rpt shape = {}\n".format(param[7].shape))
                #print("rpt = \n{}\n".format(param[7]))

                solve(param[0], param[1], param[2], param[3],
                      param[4], param[5], param[6], param[7], inst[:-4])
        else:
            sys.exit("Erro: O diretorio {} nao existe\n".format(dir))
