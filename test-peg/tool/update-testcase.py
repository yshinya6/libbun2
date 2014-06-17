#!python
#coding:utf-8
import sys, os, json, re, fnmatch, subprocess
from os.path import isfile, join
import testrunner

# usage: python update-testcase.py config.json testcase
runner = testrunner.TestRunner(False)
conf   = os.path.abspath(sys.argv[1])
test   = os.path.abspath(sys.argv[2])
if not(os.path.exists(conf) and conf.endswith("run.json")):
    print conf
    print os.path.exists(conf)
    print fnmatch.fnmatch(conf, "run.json")
    testrunner.log("error",
            conf + " is not exisit or file extention is not .json")
else:
    root = os.path.abspath(os.path.dirname(__file__) + "/../../")
    runner.load_config(root, conf)
    (status, output, erros) = runner.run_once(root, runner.conf["peg"], test)
    base, ext = os.path.splitext(test)
    outfile = base + "." + runner.conf["output.ext"]
    f = open(outfile, 'w')
    f.write(output)
    print "# generating " + outfile
