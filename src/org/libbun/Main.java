package org.libbun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.libbun.drv.JvmDriver;
import org.libbun.drv.PegDumpper;
import org.libbun.peg4d.FileSource;
import org.libbun.peg4d.JsonPegGenerator;
import org.libbun.peg4d.PackratParser;
import org.libbun.peg4d.Peg4DParser;
import org.libbun.peg4d.ParserContext;
import org.libbun.peg4d.PegObject;
import org.libbun.peg4d.PegRuleSet;
import org.libbun.peg4d.ParserSource;
import org.libbun.peg4d.RecursiveDecentParser;
import org.libbun.peg4d.StringSource;
import org.libbun.peg4d.ValidParserContext;
import org.libbun.peg4d.XmlPegGenerator;

public class Main {
	public final static String  ProgName  = "Peg4d";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 2;
	public final static int     MinerVersion = 0;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "2.0";
	public final static String  Copyright = "Copyright (c) 2013-2014, Konoha project authors";
	public final static String  License = "BSD-Style Open Source";

	// -l konoha.peg
	private static String LanguagePeg = "lib/peg/konoha.peg"; // default

	// -d driver
	private static String DriverName = "peg";  // default

	// -i
	private static boolean ShellMode = false;

	//
	private static String InputFileName = null;

	// --bigdata
	private static boolean BigDataOption = false;

	// -o
	private static String OutputFileName = null;

	// --parse-only -p
	public static boolean ParseOnlyMode = false;

	// --verbose
	public static boolean VerboseMode    = false;

	// --verbose:peg
	public static boolean VerbosePeg = false;

	// --verbose:optimized
	public static boolean VerboseOptimized = false;

	// --verbose:bun
	public static boolean VerboseBunMode = false;

	// --verbose:stat
	public static boolean VerboseStat = false;

	// --parser
	public static String ParserType = "--parser";

	// --disable-memo
	public static boolean NonMemoPegMode = false;

	// --valid
	public static boolean ValidateMode = false;

	public static boolean ValidateJsonMode = false;

	public static boolean ValidateXMLMode = false;

	public static String InputDataFile = "";

	// --parser
	public static int OptimizedLevel = 2;


	private static void parseCommandArguments(String[] args) {
		int index = 0;
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				LanguagePeg = args[index];
				ParseOnlyMode  = true;
				index = index + 1;
			}
			else if ((argument.equals("-l") || argument.equals("--lang"))  && (index < args.length)) {
				LanguagePeg = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-d") || argument.equals("--driver")) && (index < args.length)) {
				DriverName = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-o") || argument.equals("--out")) && (index < args.length)) {
				OutputFileName = args[index];
				index = index + 1;
			}
			else if (argument.startsWith("-O")) {
				if(argument.equals("-O0")) {
					OptimizedLevel = 0;
				}
				if(argument.equals("-O1")) {
					OptimizedLevel = 1;  // Peephole
				}
				if(argument.equals("-O2")) {
					OptimizedLevel = 2;  // inlining
				}
				if(argument.equals("-O3")) {
					OptimizedLevel = 3;  // prediction
				}
				if(argument.equals("-O4")) {
					OptimizedLevel = 4;  // experimental
				}
			}
			else if (argument.equals("-i")) {
				ShellMode = true;
			}
			else if (argument.equals("--bigdata")) {
				BigDataOption = true;
			}
			else if (argument.equals("--parse-only")) {
				ParseOnlyMode = true;
			}
			else if(argument.startsWith("--verbose")) {
				if(argument.equals("--verbose:bun")) {
					VerboseBunMode = true;
				}
				else if(argument.equals("--verbose:stat")) {
					VerboseStat = true;
				}
				else if(argument.equals("--verbose:peg")) {
					VerbosePeg = true;
				}
				else if(argument.equals("--verbose:optimized")) {
					VerbosePeg = true;
				}
				else {
					VerboseMode = true;
				}
			}
			else if (argument.equals("--valid")) {
				LanguagePeg = "sample/jsonObject.peg";
				ParseOnlyMode  = true;
				ValidateJsonMode = true;
				ValidateMode = true;
			}
			else if (argument.equals("--valid:xml")) {
				LanguagePeg = "sample/xml_dtd.peg";
				ParseOnlyMode  = true;
				ValidateXMLMode = true;
				ValidateMode = true;
			}
			else if(argument.startsWith("--parser:")) {
				ParserType = argument;
			}
			else {
				ShowUsage("unknown option: " + argument);
			}
		}
		if (index < args.length) {
			InputFileName = args[index];
			index++;
			if(ValidateMode && index < args.length) {
				InputDataFile = args[index];
			}
		}
		else {
			ShellMode = true;
		}
	}

	public final static void ShowUsage(String Message) {
		System.out.println(ProgName + " :");
		System.out.println("  --peg|-p  FILE          Parser Only Mode");
		System.out.println("  --lang|-l  FILE         Language file");
		System.out.println("  --driver|-d  NAME       Driver");
		System.out.println("  --out|-o  FILE          Output filename");
		System.out.println("  --bigdata               Expecting BigData Processing");
		System.out.println("  --parser:NAME           (Option) Alternative parser");
		System.out.println("  --verbose               Printing Debug infomation");
		System.out.println("  --verbose:peg           Printing Peg/Debug infomation");
		System.out.println("  --verbose:bun           Printing Peg/Bun infomation");
		Main._Exit(0, Message);
	}

	private final static UMap<Class<?>> driverMap = new UMap<Class<?>>();
	static {
		driverMap.put("py", org.libbun.drv.PythonDriver.class);
		driverMap.put("python", org.libbun.drv.PythonDriver.class);
		driverMap.put("ll", org.libbun.drv.LLVMDriver.class);
		driverMap.put("llvm", org.libbun.drv.LLVMDriver.class);
		driverMap.put("jvm", JvmDriver.class);
		driverMap.put("jvm-debug", JvmDriver.DebuggableJvmDriver.class);
		driverMap.put("peg", org.libbun.drv.PegDumpper.class);
		driverMap.put("json", org.libbun.drv.JsonDriver.class);
	}

	private static BunDriver loadDriverImpl(String driverName) {
		try {
			return (BunDriver) driverMap.get(driverName).newInstance();
		}
		catch(Exception e) {
		}
		return null;
	}

	private static BunDriver loadDriver(String driverName) {
		BunDriver d = loadDriverImpl(driverName);
		if(d == null) {
			System.out.println("Supported driver list:");
			UList<String> driverList = driverMap.keys();
			for(int i = 0; i < driverList.size(); i++) {
				String k = driverList.ArrayValues[i];
				d = loadDriverImpl(k);
				if(d != null) {
					System.out.println("\t" + k + " - " + d.getDesc());
				}
			}
			Main._Exit(1, "undefined driver: " + driverName);
		}
		return d;
	}

	public final static ParserContext newParserContext(ParserSource source) {
		if(inParseAndValidateJson) {
			return new ValidParserContext(source);
		}
		if(ParserType.equalsIgnoreCase("--parser:packrat")) {
			return new PackratParser(source);
		}
		if(ParserType.equalsIgnoreCase("--parser:simple")) {
			return new RecursiveDecentParser(source);
		}
		return new Peg4DParser(source);  // best parser
	}

	public final static void main(String[] args) {
		parseCommandArguments(args);
		BunDriver driver = loadDriver(DriverName);
		Namespace gamma = new Namespace(driver);
		gamma.loadPegFile("main", LanguagePeg);
		driver.initTable(gamma);
		if(InputFileName != null) {
			loadScript(gamma, driver, InputFileName);
		}
		else {
			ShellMode = true;
		}
		if(ShellMode) {
			performShell(gamma, driver);
		}
	}

	private static void loadScript(Namespace gamma, BunDriver driver, String fileName) {
		String startPoint = "TopLevel";
		ParserSource source = Main.loadSource(fileName);
		parseLine(gamma, driver, startPoint, source);
	}

	private static void parseLine(Namespace gamma, BunDriver driver, String startPoint, ParserSource source) {
		try {
			ParserContext context = Main.newParserContext(source);
			gamma.initParserRuleSet(context, "main");
			driver.startTransaction(OutputFileName);
			while(context.hasNode()) {
				context.beginStatInfo();
				PegObject node = context.parseNode(startPoint);
				System.out.println(node.toString());
				context.endStatInfo(node);
				if(ValidateMode) {
					parseAndValidate(node, gamma, driver, startPoint);
				}
				gamma.setNode(node);
				if(!ParseOnlyMode && driver != null) {
					if(!(driver instanceof PegDumpper)) {
						node = gamma.tryMatch(node, true);
					}
					else {
						node.matched = Functor.ErrorFunctor;
					}
					if(VerboseMode) {
						System.out.println("Typed node: \n" + node + "\n:untyped: " + node.countUnmatched(0));
					}
					driver.startTopLevel();
					node.matched.build(node, driver);
					driver.endTopLevel();
				}
				if(node.is("#error")) {
					if(OutputFileName != null) {
						Main._Exit(1, "toplevel error was found");
					}
					break;
				}
			}
			driver.generateMain();
			driver.endTransaction();
		}
		catch (Exception e) {
			PrintStackTrace(e, source.getLineNumber(0));
		}
	}

	private static boolean inParseAndValidateJson = false;

	private final static PegObject parseAndValidate(PegObject node, Namespace gamma, BunDriver driver, String startPoint) {
		String language = null;
		if (ValidateXMLMode) {
		XmlPegGenerator generator = new XmlPegGenerator();
		language = generator.generateXmlPegFile(node);
		}
		else if (ValidateJsonMode) {
		JsonPegGenerator generator = new JsonPegGenerator();
		language = generator.generateJsonPegFile(node);
		}
		gamma.loadPegFile("main", language);
		driver.initTable(gamma);
		if(InputDataFile != null) {
			inParseAndValidateJson = true;
			ParserSource source = Main.loadSource(InputDataFile);
			ParserContext context = Main.newParserContext(source);
			gamma.initParserRuleSet(context, "main");
			driver.startTransaction(OutputFileName);
			while(context.hasNode()) {
				node = context.parsePegObject(new PegObject("#toplevel"), startPoint);
					if(context.hasChar()) {
						System.out.println(ValidParserContext.InvalidLine);
						//System.out.println("** uncosumed: '" + context + "' **");
						break;
					}
					else {
						System.out.println("parsed:\n" + node.toString());
						System.out.println("\n\nVALID");
					}
			}
		}
		return node;
	}

	public final static void performShell(Namespace gamma, BunDriver driver) {
		Main._PrintLine(ProgName + Version + " (" + CodeName + ") on " + Main._GetPlatform());
		Main._PrintLine(Copyright);
		Main._PrintLine("Driver: " + driver.getDesc());
		int linenum = 1;
		String line = null;
		while ((line = readMultiLine(">>> ", "    ")) != null) {
			String startPoint = "TopLevel";
			if(VerbosePeg) {
				if(line.startsWith("?")) {
					int loc = line.indexOf(" ");
					if(loc > 0) {
						startPoint = line.substring(1, loc);
						line = line.substring(loc+1);
					}
					else {
						PegRuleSet p = gamma.root.getRuleSet("main");
						p.show(line.substring(1));
						startPoint = null;
					}
				}
			}
			if(startPoint != null) {
				ParserSource source = new StringSource("(stdin)", linenum, line);
				parseLine(gamma, driver, startPoint, source);
			}
			linenum = linenum + 1;
		}
		Main._PrintLine("");
	}

	private static jline.ConsoleReader ConsoleReader = null;

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		String line = readSingleLine(prompt);
		if(line == null) {
			System.exit(0);
		}
		if(prompt2 != null) {
			int level = 0;
			while((level = CheckBraceLevel(line)) > 0) {
				String line2 = readSingleLine(prompt2);
				line += "\n" + line2;
			}
			if(level < 0) {
				line = "";
				Main._PrintLine(" .. canceled");
			}
		}
		ConsoleReader.getHistory().addToHistory(line);
		return line;
	}

	private static void PrintStackTrace(Exception e, long linenum) {
		StackTraceElement[] elements = e.getStackTrace();
		int size = elements.length + 1;
		StackTraceElement[] newElements = new StackTraceElement[size];
		int i = 0;
		for(; i < size; i++) {
			if(i == size - 1) {
				newElements[i] = new StackTraceElement("<TopLevel>", "TopLevelEval", "stdin", (int)linenum);
				break;
			}
			newElements[i] = elements[i];
		}
		e.setStackTrace(newElements);
		e.printStackTrace();
	}

	private final static int CheckBraceLevel(String Text) {
		int level = 0;
		for(int i = 0; i < Text.length(); i++) {
			char ch = Text.charAt(i);
			if(ch == '{') {
				level++;
			}
			if(ch == '}') {
				level--;
			}
		}
		return level;
	}

	// file

	public final static ParserSource loadSource(String fileName) {
		//ZLogger.VerboseLog(ZLogger.VerboseFile, "loading " + FileName);
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				if(BigDataOption) {
					return new FileSource(fileName);
				}
				Stream = new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				Main._Exit(1, "file not found: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while(line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return new StringSource(fileName, 1, builder.toString());
		}
		catch(IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

	public final static String _GetPlatform() {
		return "Java JVM-" + System.getProperty("java.version");
	}

//	public final static String _GetEnv(String Name) {
//		return System.getenv(Name);
//	}
//
//	public final static void _Print(Object msg) {
//		System.err.print(msg);
//	}

	public final static void _PrintLine(Object message) {
		System.err.println(message);
	}

	public final static void _Exit(int status, String message) {
		if(Main.VerboseMode) {
			System.err.println("EXIT " + Main._GetStackInfo(3) + " " + message);
		}
		else {
			System.err.println("EXIT " + message);
		}
		System.exit(status);
	}

	public static boolean DebugMode = false;

	public final static void _PrintDebug(String msg) {
		if(Main.DebugMode) {
			_PrintLine("DEBUG " + Main._GetStackInfo(3) + ": " + msg);
		}
	}

	private final static String _GetStackInfo(int depth) {
		String LineNumber = " ";
		Exception e =  new Exception();
		StackTraceElement[] Elements = e.getStackTrace();
		if(depth < Elements.length) {
			StackTraceElement elem = Elements[depth];
			LineNumber += elem;
		}
		return LineNumber;
	}

	public final static boolean _IsFlag(int flag, int flag2) {
		return ((flag & flag2) == flag2);
	}

	public final static int _UnsetFlag(int flag, int flag2) {
		return (flag & (~flag2));
	}

	public final static char _GetChar(String Text, int Pos) {
		return Text.charAt(Pos);
	}

	public final static String _CharToString(char ch) {
			return ""+ch;
	}

	public static String _SourceBuilderToString(UStringBuilder sb) {
		return Main._SourceBuilderToString(sb, 0, sb.slist.size());
	}

	public static String _SourceBuilderToString(UStringBuilder sb, int beginIndex, int endIndex) {
		StringBuilder jsb = new StringBuilder();
		for(int i = beginIndex; i < endIndex; i = i + 1) {
			jsb.append(sb.slist.ArrayValues[i]);
		}
		return jsb.toString();
	}

	public final static void _WriteFile(String fileName, UList<UStringBuilder> list) {
		if(fileName == null) {
			for(int i = 0; i < list.size(); i++) {
				UStringBuilder sb = list.ArrayValues[i];
				System.out.println(sb.toString());
				sb.clear();
			}
		}
		else {
			try {
				BufferedWriter w = new BufferedWriter(new FileWriter(fileName));
				for(int i = 0; i < list.size(); i++) {
					UStringBuilder sb = list.ArrayValues[i];
					w.write(sb.toString());
					w.write("\n\n");
					sb.clear();
				}
				w.close();
			}
			catch(IOException e) {
				_Exit(1, "cannot to write: " + e);
			}
		}
	}

//	public final static String[] _GreekNames = {
//		/*"Alpha"*/ "\u03B1", "\u03B2", "\u03B3"
//	};

	public final static BunType[] _NewTypeArray(int size) {
		return new BunType[size];
	}

	public final static PegObject[] _NewPegObjectArray(int size) {
		return new PegObject[size];
	}

	public final static void _ArrayCopy(Object src, int sIndex, Object dst, int dIndex, int length) {
		System.arraycopy(src, sIndex, dst, dIndex, length);
	}

}
