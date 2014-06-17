#!python
#coding:utf-8
import sys, os, json, re, fnmatch, subprocess
from os.path import isfile, join

def log(level, message):
    print "[" + level + "] " + message

def collect_files(path, pattern):
    files = [join(path, f) for f in os.listdir(path) if isfile(join(path, f)) ]
    return fnmatch.filter(files, "*." + pattern)

class TestRunner:
    def __init__(self, verbose):
       self.testcases = None
       self.conf      = None
       self.verbose   = verbose

    def load_config(self, root, json_path):
        if os.path.exists(json_path) == False:
            log("warning", json_path + " is not exists")
            return False
        f = open(json_path, "r")
        conf = json.load(f)
        ipattern = conf["input.ext"]
        opattern = conf["output.ext"]
        if conf["peg"] == None:
            conf["peg"] = ""
        if ipattern == None:
            log("warning", "input.ext is not defined")
            return False
        if opattern == None:
            log("info", "output.ext is not defined. use " + ipattern + ".out")
            conf["output.ext"] = ipattern + ".out"

        conf["peg"] = conf["peg"].replace("#{SOURCE_ROOT}", root)
        self.conf = conf
        return True

    def prepare(self, root, path):
        json_path = path + "/run.json"
        if self.load_config(root, json_path) == False:
            return False

        inputs  = collect_files(path, self.conf["input.ext"])
        outputs = collect_files(path, self.conf["output.ext"])
        inputs.sort(); outputs.sort();
        if len(inputs) != len(outputs):
            log("warning",
                    "some test result or test file is missing. (" + path + ")")
            return False
        m = {}
        i = 0
        while i < len(inputs):
            m[inputs[i]] = outputs[i]
            i += 1
        self.testcases = m
        return True

    def run_once(self, root, peg, input_file):
        cmd = ['java', '-ea', '-jar', root + '/libbun2.jar',
                '--parse-only', '--verbose:ast',
                '-l', peg, input_file]
        print "# " + " ".join(cmd)
        p = subprocess.Popen(cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                shell=False)
        exit_status = p.wait()
        output = "".join(p.stdout.readlines())
        errors = "".join(p.stderr.readlines())
        return (exit_status, output, errors)

    def run(self):
        executed = 0
        passed   = 0
        if len(self.testcases) == 0:
            return

        for (k,v) in self.testcases.items():
            (status, output, erros) = self.run_once(root, self.conf["peg"], k)
            #if self.verbose:
            #    print 'return: %d' % (status)
            #    print 'stdout: %s' % (output, )
            #    print 'stderr: %s' % (errors, )
            executed += 1
            expected = open(v, "r").read()
            if status == 0 and expected == output:
                passed += 1
            if self.verbose:
                print '-----------------------'
                print 'result:\n%s' % (output)
                print '-----------------------'
                print 'expected\n%s' % (expected)
                print '-----------------------'
        print "# of testcases: %d, # of OK: %d, # of FAILED: %d" % (
                executed, passed, executed - passed)
        return

if __name__ == '__main__':
    root = os.path.abspath(os.path.dirname(__file__) + "/../../")
    dirs = filter(lambda x : x != "tool", os.listdir(root + "/test-peg/"))
    target = ""
    if len(sys.argv) > 1:
        target = sys.argv[1]
        dirs = filter(lambda x : x == target, dirs)

    for path in dirs:
        runner = TestRunner(True)
        if runner.prepare(root, root + "/test-peg/" + path):
            runner.run()
