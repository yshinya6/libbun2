package org.libbun;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * generate java byte code and invoke.
 * @author skgchxngsxyz-osx
 *
 */
public class JvmDriver extends BunDriver {
	public final static String letHolderSuffix = "_LetVarHolder";
	public final static String letHolderFieldName = "letValue";

	/**
	 * used for byte code loading.
	 */
	private final JvmByteCodeLoader loader;

	/**
	 * represent jvm operand stack map.
	 */
	private final Stack<BunType> typeStack;

	/**
	 * used for bun type to java class translation.
	 */
	private final UniMap<Class<?>> classMap;

	/**
	 * used for java class generation.
	 */
	private ClassBuilder classBuilder;

	/**
	 * used for java method (includes constructor, static initializer) generation.
	 */
	private final Stack<MethodBuilder> mBuilders;
	private Namespace gamma;

	public JvmDriver() {
		this.loader = new JvmByteCodeLoader();
		this.typeStack = new Stack<BunType>();
		this.classMap = new UniMap<Class<?>>();
		this.mBuilders = new Stack<MethodBuilder>();
		this.addCommand("PUSH_INT", new PushInt());
		this.addCommand("PUSH_FLOAT", new PushFloat());
		this.addCommand("PUSH_BOOL", new PushBool());
		this.addCommand("PUSH_STRING", new PushString());
		this.addCommand("OP", new CallOperator());
		this.addCommand("AND", new CondAnd());
		this.addCommand("OR", new CondOr());
		this.addCommand("LET", new LetDecl());
		this.addCommand("IF", new IfStatement());
		this.addCommand("BLOCK", new Block());
	}

	@Override
	public void initTable(Namespace gamma) {
		this.gamma = gamma;
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
		this.mBuilders.push(this.classBuilder.createMethodBuilder());
	}

	/**
	 * finalize class builder and invoke generated class.
	 */
	@Override
	public void endTransaction() {
		this.insertPrintIns();
		this.mBuilders.peek().returnValue();
		this.mBuilders.pop().endMethod();
		this.classBuilder.visitEnd();
		String className = this.classBuilder.getClassName();
		byte[] byteCode = this.classBuilder.toByteArray();
		Class<?> generatedClass = this.loader.createChildLoader().generateClassFromByteCode(className, byteCode);
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

	/**
	 * push bun type to type stack
	 * @param type
	 * - if not void type, push it to stack.
	 * throw exception, if illeagal type.
	 */
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
	 * insert print instruction after top level expression.
	 * it is interactive mode only.
	 */
	private void insertPrintIns() {
		BunType type = this.popTypeFromTypeStack();
		if(type == null) {
			return;
		}
		Class<?> javaClass = this.classMap.get(type.getName());
		try {
			java.lang.reflect.Method method = JvmOperator.class.getMethod("printValue", javaClass, String.class);
			this.mBuilders.peek().push(type.getName());
			this.mBuilders.peek().invokeStatic(Type.getType(JvmOperator.class), Method.getMethod(method));
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

	private BunType toType(PegObject pObject) {
		String name = pObject.name;
		String typeName = name.substring(2);
		BunType type = this.gamma.getType(typeName, null);
		if(type == null) {
			throw new RuntimeException("undefined type: " + typeName);
		}
		return type;
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
			mBuilders.peek().push(Long.parseLong(node.getText()));
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
			mBuilders.peek().push(Double.parseDouble(node.getText()));
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
				mBuilders.peek().push(true);
			}
			else {
				mBuilders.peek().push(false);
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
			mBuilders.peek().push(this.parseTokenText(node.getText()));
			pushTypeToTypeStack(node.getType(null));
		}

		/**
		 * decode escape sequence. 
		 * @param text
		 * - may be include encoded escape sequence.
		 * @return
		 * - decoded string value.
		 */
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

	/**
	 * generate binary and unary operator call instruction.
	 * @author skgchxngsxyz-osx
	 *
	 */
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
			mBuilders.peek().invokeStatic(Type.getType(JvmOperator.class), methodDesc);
		}
		catch(SecurityException e) {
			e.printStackTrace();
		}
		catch(NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	/**
	 * generate conditional and.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class CondAnd extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			GeneratorAdapter adapter = mBuilders.peek();
			Label rightLabel = adapter.newLabel();
			Label mergeLabel = adapter.newLabel();
			// and left
			driver.pushNode(node.get(0));
			adapter.push(true);
			adapter.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, rightLabel);
			adapter.push(false);
			adapter.goTo(mergeLabel);
			// and right
			adapter.mark(rightLabel);
			driver.pushNode(node.get(1));
			adapter.mark(mergeLabel);

			popTypeFromTypeStack();
			popTypeFromTypeStack();
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * generate condiotional or.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class CondOr extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			GeneratorAdapter adapter = mBuilders.peek();
			Label rightLabel = adapter.newLabel();
			Label mergeLabel = adapter.newLabel();
			// or left
			driver.pushNode(node.get(0));
			adapter.push(true);
			adapter.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.NE, rightLabel);
			adapter.push(true);
			adapter.goTo(mergeLabel);
			// or right
			adapter.mark(rightLabel);
			driver.pushNode(node.get(1));
			adapter.mark(mergeLabel);

			popTypeFromTypeStack();
			popTypeFromTypeStack();
			pushTypeToTypeStack(node.getType(null));
		}
	}

	/**
	 * generate let.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private class LetDecl extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder currentBuilder = mBuilders.peek();
			String varName = node.get(0).getText();
			int scopeDepth = currentBuilder.getScopes().depth();
			if(scopeDepth > 1) {	// define as local variable.
				driver.pushNode(node.get(2));
				Class<?> varClass = toJavaClass(popTypeFromTypeStack());
				VarEntry entry = currentBuilder.getScopes().addEntry(varName, varClass, true);
				currentBuilder.storeLocal(entry.getVarIndex(), Type.getType(varClass));
			}
			else {	// define as global variable.
				ClassBuilder cBuilder = new ClassBuilder(varName + JvmDriver.letHolderSuffix);
				// create static initializer
				Method methodDesc = Method.getMethod("void <clinit> ()");
				MethodBuilder mBuilder = new MethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, methodDesc, null, null, cBuilder);
				mBuilders.push(mBuilder);
				driver.pushNode(node.get(2));
				Class<?> varClass = toJavaClass(popTypeFromTypeStack());
				Type ownerTypeDesc = Type.getType(cBuilder.getClassName());
				Type fieldTypeDesc = Type.getType(varClass);
				mBuilder.putStatic(ownerTypeDesc, letHolderFieldName, fieldTypeDesc);
				mBuilder.returnValue();
				mBuilder.endMethod();
				mBuilders.pop();

				// create var field
				cBuilder.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, letHolderFieldName, fieldTypeDesc.getDescriptor(), null, null);

				// finalize
				cBuilder.visitEnd();
				loader.generateClassFromByteCode(cBuilder.getClassName(), cBuilder.toByteArray());

				// initialized let var.
				currentBuilder.getStatic(ownerTypeDesc, letHolderFieldName, fieldTypeDesc);
				currentBuilder.pop(varClass);
			}
		}
	}

	private class IfStatement extends DriverCommand {

		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			// TODO Auto-generated method stub
			
		}
		
	}

	private class Block extends DriverCommand {

		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			// TODO Auto-generated method stub
			
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
	 * create new ClassBuilder for class.
	 * @param className
	 * - fully qualified name. contains (/)
	 */
	public ClassBuilder(String className) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.fullyQualifiedName = className;
		this.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, this.fullyQualifiedName, null, "java/lang/Object", null);
	}

	/**
	 * create new GeneratorAdapter for top level wrapper method.
	 * @return
	 */
	public MethodBuilder createMethodBuilder() {
		Method methodDesc = Method.getMethod("void invoke()");
		return new MethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, methodDesc, null, null, this);
	}

	public String getClassName() {
		return this.fullyQualifiedName;
	}
}

class MethodBuilder extends GeneratorAdapter {
	private final VarScopes scopes;

	public MethodBuilder(int arg0, Method arg1, String arg2, Type[] arg3, ClassVisitor arg4) {
		super(arg0, arg1, arg2, arg3, arg4);
		int startIndex = 0;
		if((arg0 & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
			startIndex = 1;
		}
		this.scopes = new VarScopes(startIndex);
	}

	public VarScopes getScopes() {
		return this.scopes;
	}

	public void pop(Class<?> stackTopClass) {
		int size = Type.getType(stackTopClass).getSize();
		if(size == 1) {
			this.pop();
		}
		else if(size == 2) {
			this.pop2();
		}
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

	public Class<?> generateClassFromByteCode(String className, byte[] byteCode) {
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

class VarScopes {
	private final Stack<LocalVarScope> scopeStack;
	private final int baseIndex;

	public VarScopes(int baseIndex) {
		this.baseIndex = baseIndex;
		this.scopeStack = new Stack<LocalVarScope>();
		this.scopeStack.push(GlobalVarScope.getInstance());
	}

	public void createNewScope() {
		int index = this.baseIndex;
		if(this.scopeStack.size() > 1) {
			index = this.scopeStack.peek().getEndIndex();
		}
		this.scopeStack.push(new LocalVarScope(this.scopeStack.peek(), index));
	}

	public void removeCurrentScope() {
		if(this.scopeStack.size() > 1) {
			this.scopeStack.pop();
		}
	}

	public VarEntry addEntry(String varName, Class<?> varClass, boolean isReadOnly) {
		return this.scopeStack.peek().addEntry(varName, varClass, isReadOnly);
	}

	public VarEntry getEntry(String varName) {
		return this.scopeStack.peek().getEntry(varName);
	}

	public int depth() {
		return this.scopeStack.size();
	}
}

/**
 * represents local variable scope.
 * @author skgchxngsxyz-osx
 *
 */
class LocalVarScope {
	/**
	 * representds parent scope.
	 * if it is global scope, parent scope is null.
	 */
	protected final LocalVarScope parentScope;

	/**
	 * contains var entries. key is var name.
	 */
	protected final Map<String, VarEntry> entryMap;

	/**
	 * start index of local variable in this scope.
	 */
	private final int baseIndex;

	/**
	 * current local variable index.
	 * after adding new local variable, increment this index by value size.
	 */
	private int currentIndex;
	
	public LocalVarScope(LocalVarScope parentScope, int baseIndex) {
		this.parentScope = parentScope;
		this.baseIndex = baseIndex;
		this.currentIndex = this.baseIndex;
		this.entryMap = new HashMap<String, VarEntry>();
	}

	/**
	 * create and add new VarEntry. 
	 * @param varName
	 * - variable name.
	 * @param varClass
	 * @param isReadOnly
	 * - if true, represents constant variable.
	 * @return
	 * - throw exception if entry has already defined.
	 */
	public VarEntry addEntry(String varName, Class<?> varClass, boolean isReadOnly) {
		if(this.entryMap.containsKey(varName)) {
			throw new RuntimeException(varName + " is already defined");
		}
		int valueSize = Type.getType(varClass).getSize();
		int varIndex = this.currentIndex;
		VarEntry entry = new VarEntry(varIndex, false, isReadOnly);
		this.entryMap.put(varName, entry);
		this.currentIndex += valueSize;
		return entry;
	}

	/**
	 * get VarEntry.
	 * @param varName
	 * - variable name.
	 * @return
	 * - throw exception if has no entry.
	 */
	public VarEntry getEntry(String varName) {
		VarEntry entry = this.entryMap.get(varName);
		if(entry != null) {
			return  entry;
		}
		return this.parentScope.getEntry(varName);
	}

	/**
	 * get start index of local variable in this scope.
	 * @return
	 */
	public int getStartIndex() {
		return this.baseIndex;
	}

	/**
	 * get end index of local variable in this scope.
	 * @return
	 */
	public int getEndIndex() {
		return this.currentIndex;
	}
}

class GlobalVarScope extends LocalVarScope {
	protected GlobalVarScope() {
		super(null, -1);
	}

	@Override
	public VarEntry addEntry(String varName, Class<?> varClass, boolean isReadOnly) {
		if(this.entryMap.containsKey(varName)) {
			throw new RuntimeException(varName + " is already defined");
		}
		VarEntry entry = new VarEntry(-1, true, isReadOnly);
		this.entryMap.put(varName, entry);
		return entry;
	}

	@Override
	public VarEntry getEntry(String varName) {
		VarEntry entry = this.entryMap.get(varName);
		if(entry != null) {
			return entry;
		}
		throw new RuntimeException("undefined variabel: " + varName);
	}

	private static class Holder {
		private final static GlobalVarScope INSTANCE = new GlobalVarScope();
	}

	public static GlobalVarScope getInstance() {
		return Holder.INSTANCE;
	}
}

/**
 * variable entry.
 * @author skgchxngsxyz-osx
 *
 */
class VarEntry {
	/**
	 * if local variable, represents jvm local variable table index.
	 * if global variable, it is meaningless.
	 */
	private final int varIndex;

	/**
	 * if true, represents global variable (constant only).
	 */
	private final boolean isGlobal;

	/**
	 * if true, represent read only variable.
	 */
	private final boolean isReadOnly;

	public VarEntry(int varIndex, boolean isGlobal, boolean isReadOnly) {
		this.varIndex = varIndex;
		this.isGlobal = isGlobal;
		this.isReadOnly = isReadOnly;
	}

	public boolean isReadOnly() {
		return this.isReadOnly;
	}

	public boolean isGlobal() {
		return this.isGlobal;
	}

	public int getVarIndex() {
		return this.varIndex;
	}
}
