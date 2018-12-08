# -*- coding:utf-8 -*-
#!/usr/bin/env python3

import os
import shutil
import pandas as pd
import subprocess
import re
from concurrent import futures
import pdb
import pprint

def runall(path):
    algorithmList=[]
    result = pd.DataFrame()
    for root, folder, files in os.walk(path):
        for file in files:
            algorithmList.append(root+"/"+file)
    if "log" in os.listdir('.'):
        shutil.rmtree("log")
    os.mkdir("log")
    pprint.pprint(algorithmList)
    with futures.ProcessPoolExecutor() as pool:
        r = pool.map(runAlgorithm, algorithmList)
    print("flag 1")
    for res in r:
        res = res.splitlines()
        name = res[0]
        for l in res:
            if "Evaluator value:" in l:
                eva = l.split("Evaluator value:")[-1]
                result.loc[name,eva.split(" ")[0]] = eva.split(" ")[-1]
    result.to_excel("result.xls")


def runAlgorithm(dir):
    res = subprocess.run(["librec.cmd", "rec", "-exec", "-conf", dir],
                   stderr=subprocess.PIPE, stdout=subprocess.PIPE)
    name = re.split("/|\.", dir)[-2]
    with open("./log/"+name,"w") as f:
        f.write(res.stderr.decode("gbk"))
    print("{0} is complete".format(name))
    return name + "\n" + res.stderr.decode("gbk")


if __name__ == '__main__':
    runall('../core/src/main/resources/rec/')
    pass
