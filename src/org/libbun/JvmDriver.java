package org.libbun;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class JvmDriver extends BunDriver {
	private final JvmByteCodeLoader loader;
	private final Stack<BunType> typeStack;
	private final UniMap<Class<?>> classMap;
	private ClassBuilder classBuilder;
	private GeneratorAdapter adapter;

	public JvmDriver() {
		this.loader = new JvmByteCodeLoader();
		this.typeStack = new Stack<BunType>();
		this.classMap = new UniMap<Class<?>>();
		this.addCommand("PUSH_INT", new PushInt());
		this.addCommand("PUSH_FLOAT", new PushFloat());
		this.addCommand("PUSH_BOOL", new PushBool());
		this.addCommand("PUSH_STRING", new PushString());
		this.addCommand("OP", new CallOperator());
	}

	@Override
	public void initTable(Namespace gamma) {
		gamma.setType(BunType.newValueType("int", long.class));
		this.classMap.put("int", long.class);
		gamma.setType(BunType.newValueType("float", double.class));
		this.classMap.put("float", double.class);
		gamma.setType(BunType.newValueType("bool", boolean.class));
		this.classMap.put("bool", boolean.class);
		gamma.setType(BunType.newValueType("String", String.class));
		this.classMap.put("String", String.class);
		gamma.setType(BunType.newVoidType("void", Void.class));
		this.classMap.put("void", Void.class);
		gamma.setType(BunType.newAnyType("any", Object.class));
		this.classMap.put("any", Object.class);
		gamma.setType(BunType.newGreekType("alpha", 0, null));
		gamma.setType(BunType.newGreekType("beta", 0, null));
		KonohaTypeChecker.initDriver(this);
		gamma.loadBunModel("lib/driver/jvm/konoha.bun", this);
	}

	@Override
	public void startTransaction(String fileName) {	// create top level wrapper
		this.classBuilder = new ClassBuilder();
		this.adapter = this.classBuilder.createAdapter();
	}

	@Override
	public void endTransaction() {
		this.insertPrintIns();
		this.adapter.returnValue();
		this.adapter.endMethod();
		this.classBuilder.visitEnd();
		String className = this.classBuilder.getClassName();
		byte[] byteCode = this.classBuilder.toByteArray();
		Class<?> generatedClass = this.loader.createChildLoader().generaeteClassFromByteCode(className, byteCode);
		try {
			generatedClass.getMethod("invoke").invoke(null);
		}
		catch(Throwable t) {
			if(t instanceof InvocationTargetException) {
				t = t.getCause();
			}
			t.printStackTrace();
		}
	}

	private void pushTypeToTypeStack(BunType type) {
		Class<?> javaClass = this.classMap.get(type.getName());
		if(javaClass == null) {
			throw new RuntimeException("illegal type: " + type);
		}
		if(!javaClass.equals(Void.class)) {
			this.typeStack.push(type);
		}
	}

	/**
	 * pop and get type from stack top
	 * @return
	 * - return null, if typeStack is empty.
	 */
	private BunType popTypeFromTypeStack() {
		if(this.typeStack.isEmpty()) {
			return null;
		}
		return this.typeStack.pop();
	}

	/**
	 * interactive mode only.
	 */
	private void insertPrintIns() {
		BunType type = this.popTypeFromTypeStack();
		if(type == null) {
			return;
		}
		Class<?> javaClass = this.classMap.get(type.getName());
		try {
			java.lang.reflect.Method method = JvmOperator.class.getMethod("printValue", javaClass, String.class);
			this.adapter.push(type.getName());
			this.adapter.invokeStatic(Type.getType(JvmOperator.class), Method.getMethod(method));
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * BunType to Java class
	 * @param type
	 * @return
	 * - throw exception if has no java class.
	 */
	private Class<?> toJavaClass(BunType type) {
		String typeName = type.getName();
		Class<?> javaClass = this.classMap.get(typeName);
		if(javaClass == null) {
			throw new RuntimeException("has no java class: " + typeName);
		}
		return javaClass;
	}

	@Override
	public void pushGlobalName(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushLocalName(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushUndefinedName(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushApplyNode(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushType(BunType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushNewLine() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushCode(String text) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * push value as java long.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class PushInt extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			adapter.push(Long.parseLong(node.getText()));
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * push value as java double.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class PushFloat extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			adapter.push(Double.parseDouble(node.getText()));
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * push value as java boolean.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class PushBool extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			if(param[0].equals("true")) {
				adapter.push(true);
			}
			else {
				adapter.push(false);
			}
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * push value as java string.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class PushString extends DriverCommand {

		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			adapter.push(this.parseTokenText(node.getText()));
			pushTypeToTypeStack(node.getType(null));
		}

		private String parseTokenText(String text) {
			StringBuilder sBuilder = new StringBuilder();
			int size = text.length();
			for(int i = 0; i < size; i++) {
				char ch = text.charAt(i);
				if(ch == '\\') {
					char nextCh = text.charAt(++i);
					switch(nextCh) {
					case 't' : ch = '\t'; break;
					case 'b' : ch = '\b'; break;
					case 'n' : ch = '\n'; break;
					case 'r' : ch = '\r'; break;
					case 'f' : ch = '\f'; break;
					case '\'': ch = '\''; break;
					case '"' : ch = '"';  break;
					case '\\': ch = '\\'; break;
					}
				}
				sBuilder.append(ch);
			}
			return sBuilder.toString();
		}
	}

	private class CallOperator extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String opName = node.name.substring(1);
			int size = node.size();
			Class<?>[] paramClasses = new Class<?>[size];
			for(int i = 0; i < size; i++) {
				paramClasses[i] = toJavaClass(node.get(i).getType(null));
				popTypeFromTypeStack();
			}
			callOperator(opName, paramClasses);
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * look up and generate invokestatic instruction.
	 * @param opName
	 * - operator name.
	 * @param paramClasses
	 * - operator parameter classes.
	 */
	private void callOperator(String opName, Class<?>[] paramClasses) {
		try {
			java.lang.reflect.Method method = JvmOperator.class.getMethod(opName, paramClasses);
			Method methodDesc = Method.getMethod(method);
			adapter.invokeStatic(Type.getType(JvmOperator.class), methodDesc);
		}
		catch(SecurityException e) {
			e.printStackTrace();
		}
		catch(NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * definition of builtin function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class JvmOperator {
		// binary op definition
		// ADD
		public static long   add(long x, long y)     { return x + y; }
		public static double add(long x, double y)   { return x + y; }
		public static double add(double x, long y)   { return x + y; }
		public static double add(double x, double y) { return x + y; }
		// string concat
		public static String add(String left, long right)    { return left + right; }
		public static String add(String left, double right)  { return left + right; }
		public static String add(String left, boolean right) { return left + right; }
		public static String add(String left, Object right)  { return left + right; }
		public static String add(long left, String right)    { return left + right; }
		public static String add(double left, String right)  { return left + right; }
		public static String add(boolean left, String right) { return left + right; }
		public static String add(Object left, String right)  { return left + right; }

		// SUB
		public static long   sub(long left, long right)     { return left - right; }
		public static double sub(long left, double right)   { return left - right; }
		public static double sub(double left, long right)   { return left - right; }
		public static double sub(double left, double right) { return left - right; }

		// MUL
		public static long   mul(long left, long right)     { return left * right; }
		public static double mul(long left, double right)   { return left * right; }
		public static double mul(double left, long right)   { return left * right; }
		public static double mul(double left, double right) { return left * right; }

		// DIV
		public static long   div(long left, long right)     { return left / right; }
		public static double div(long left, double right)   { return left / right; }
		public static double div(double left, long right)   { return left / right; }
		public static double div(double left, double right) { return left / right; }

		// MOD
		public static long   mod(long left, long right)     { return left % right; }
		public static double mod(long left, double right)   { return left % right; }
		public static double mod(double left, long right)   { return left % right; }
		public static double mod(double left, double right) { return left % right; }

		// EQ
		public static boolean eq(long left, long right)       { return left == right; }
		public static boolean eq(long left, double right)     { return left == right; }
		public static boolean eq(double left, long right)     { return left == right; }
		public static boolean eq(double left, double right)   { return left == right; }

		public static boolean eq(boolean left, boolean right) { return left == right; }
		public static boolean eq(String left, String right)   { return left.equals(right); }
		public static boolean eq(Object left, Object right)   { return left.equals(right); }

		// NOTEQ
		public static boolean noteq(long left, long right)       { return left != right; }
		public static boolean noteq(long left, double right)     { return left != right; }
		public static boolean noteq(double left, long right)     { return left != right; }
		public static boolean noteq(double left, double right)   { return left != right; }

		public static boolean noteq(boolean left, boolean right) { return left != right; }
		public static boolean noteq(String left, String right)   { return !left.equals(right); }
		public static boolean noteq(Object left, Object right)   { return !left.equals(right); }

		// LT
		public static boolean lt(long left, long right)     { return left < right; }
		public static boolean lt(long left, double right)   { return left < right; }
		public static boolean lt(double left, long right)   { return left < right; }
		public static boolean lt(double left, double right) { return left < right; }

		// LTE
		public static boolean lte(long left, long right)     { return left <= right; }
		public static boolean lte(long left, double right)   { return left <= right; }
		public static boolean lte(double left, long right)   { return left <= right; }
		public static boolean lte(double left, double right) { return left <= right; }

		// GT
		public static boolean gt(long left, long right)     { return left > right; }
		public static boolean gt(long left, double right)   { return left > right; }
		public static boolean gt(double left, long right)   { return left > right; }
		public static boolean gt(double left, double right) { return left > right; }

		// GTE
		public static boolean gte(long left, long right)     { return left >= right; }
		public static boolean gte(long left, double right)   { return left >= right; }
		public static boolean gte(double left, long right)   { return left >= right; }
		public static boolean gte(double left, double right) { return left >= right; }

		// unary op definition
		// NOT
		public static boolean not(boolean right) { return !right; }

		// PLUS
		public static long   plus(long right)   { return +right; }
		public static double plus(double right) { return +right; }

		// MINUS
		public static long   minus(long right)   { return -right; }
		public static double minus(double right) { return -right; }

		// COMPL
		public static long compl(long right) { return ~right; }

		// specific op
		public static void printValue(long value, String typeName)    { System.out.println("(" + typeName + ") " + value); }
		public static void printValue(double value, String typeName)  { System.out.println("(" + typeName + ") " + value); }
		public static void printValue(boolean value, String typeName) { System.out.println("(" + typeName + ") " + value); }
		public static void printValue(String value, String typeName)  { System.out.println("(" + typeName + ") " + value); }
		public static void printValue(Object value, String typeName)  { System.out.println("(" + typeName + ") " + value); }
	}

	public static class DebugableJvmDriver extends JvmDriver {
		public DebugableJvmDriver() {
			super();
			JvmByteCodeLoader.setDebugMode(true);
		}
	}
}

/**
 * ClasWriter wrapper
 * @author skgchxngsxyz-osx
 *
 */
class ClassBuilder extends ClassWriter {
	/**
	 * name suffix for top level wrapper class
	 */
	private static int classNameSiffix = -1;

	/**
	 * internal class name. must not contain path separator (/).
	 */
	private final String fullyQualifiedName;

	/**
	 * create new ClassBuilder for top level wrapper class
	 */
	public ClassBuilder() {
		super(ClassWriter.COMPUTE_FRAMES);
		this.fullyQualifiedName = "TopLevel" + ++classNameSiffix;
		this.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, this.fullyQualifiedName, null, "java/lang/Object", null);
	}

	/**
	 * create new GeneratorAdapter for top level wrapper method.
	 * @return
	 */
	public GeneratorAdapter createAdapter() {
		Method methodDesc = Method.getMethod("void invoke()");
		return new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, methodDesc, null, null, this);
	}

	public String getClassName() {
		return this.fullyQualifiedName;
	}
}

/**
 * used for defined class loading.
 * do not call ClassLoader#loadClass().
 * @author skgchxngsxyz-osx
 *
 */
class JvmByteCodeLoader extends ClassLoader {
	private String fullyQualifiedName;
	private static boolean isDebugMode = false;

	private byte[] byteCode;
	private boolean initialized = false;

	public JvmByteCodeLoader() {
		super();
	}

	/**
	 * used for child class loader creation.
	 * @param loader
	 */
	private JvmByteCodeLoader(JvmByteCodeLoader loader) {
		super(loader);
	}

	public static void setDebugMode(boolean debugMode) {
		isDebugMode = debugMode;
	}

	@Override
	protected Class<?> findClass(String name) {
		if(!this.initialized) {
			return null;
		}
		this.initialized = false;
		Class<?>  generetedClass = this.defineClass(this.fullyQualifiedName, this.byteCode, 0, this.byteCode.length);
		this.byteCode = null;
		return generetedClass;
	}

	/**
	 * set byte code and class name.
	 * before class loading, must call it.
	 * @param className
	 * - fully qualified name(do not contain / ).
	 * @param byteCode
	 */
	private void setByteCode(String className, byte[] byteCode) {
		this.fullyQualifiedName = className;
		this.byteCode = byteCode;
		this.initialized = true;
	}

	public Class<?> generaeteClassFromByteCode(String className, byte[] byteCode) {
		this.setByteCode(className, byteCode);
		this.dump();
		try {
			return this.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public JvmByteCodeLoader createChildLoader() {
		return new JvmByteCodeLoader(this);
	}

	/**
	 * for debug purpose.
	 */
	private void dump() {
		if(!isDebugMode) {
			return;
		}
		int index = this.fullyQualifiedName.lastIndexOf('.');
		String fileName = this.fullyQualifiedName.substring(index + 1) + ".class";
		System.err.println("@@@@ Dump ByteCode: " + fileName + " @@@@");
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(new File(fileName));
			fileOutputStream.write(this.byteCode);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
