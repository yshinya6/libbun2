package org.libbun.drv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class JvmRuntime {
	private final static MethodHandle fbUnaryHande = initFallBackHandle("UnaryOp");
	private final static MethodHandle testUnaryHandle = initTestHandle("UnaryOp", 1);

	private final static MethodHandle fbBinaryHandle = initFallBackHandle("BinaryOp");
	private final static MethodHandle testBinaryHandle = initTestHandle("BinaryOp", 2);

	private final static MethodHandle fbCompHandle = initFallBackHandle("CompOp", Boolean.class);

	private final static MethodHandle fbMethodHandle = initFallBackHandle("Method");
	private final static MethodHandle testMethodHandle = initTestHandleForVarg("Method");

	private final static MethodHandle fbFuncHandle = initFallBackHandle("Func");
	private final static MethodHandle testFuncHandle = initTestHandle("Func", 1);

	protected static MethodHandle initFallBackHandle(String name) {
		return initFallBackHandle(name, Object.class);
	}

	protected static MethodHandle initFallBackHandle(String name, Class<?> returnClass) {
		MethodType type = MethodType.methodType(returnClass, new Class<?>[]{CachedCallSite.class, Object[].class});
		try {
			return MethodHandles.lookup().findStatic(JvmRuntime.class, "fallBackFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected static MethodHandle initTestHandle(String name, int paramSize) {
		Class<?>[] paramClasses = new Class<?>[paramSize * 2];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = Class.class;
			paramClasses[i + paramSize] = Object.class;
		}
		MethodType type = MethodType.methodType(boolean.class, paramClasses);
		try {
			return MethodHandles.lookup().findStatic(JvmRuntime.class, "testFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected static MethodHandle initTestHandleForVarg(String name) {
		Class<?>[] paramClasses = new Class<?>[] {Class[].class, Object[].class};
		MethodType type = MethodType.methodType(boolean.class, paramClasses);
		try {
			return MethodHandles.lookup().findStatic(JvmRuntime.class, "testFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	public static class CachedCallSite extends MutableCallSite {
		private final String methodName;
		private final MethodHandles.Lookup lookup;

		private CachedCallSite(String methodName, MethodHandles.Lookup lookup, MethodType type) {
			super(type);
			this.methodName = methodName;
			this.lookup = lookup;
		}
	}

	// unary op
	/**
	 * bootstrap method for unary op. used for indy
	 * 
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 */
	public static CallSite bsmUnaryOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbUnaryHande.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for unary op. used for indy
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForUnaryOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> rightValueClass = args[0].getClass();
		// target method handle
		MethodHandle targetHandle = 
				callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, 
						type.changeParameterType(0, rightValueClass).changeReturnType(rightValueClass).unwrap());
		targetHandle = targetHandle.asType(type);

		// test method handle
		MethodHandle testHandle = testUnaryHandle.bindTo(rightValueClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for unary op. used for indy
	 * @param clazz
	 * @param value
	 * @return
	 */
	public static boolean testForUnaryOp(Class<?> clazz, Object value) {
		return value.getClass().equals(clazz);
	}

	//binary op
	/**
	 * boot strap method for binary op, 
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmBinaryOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbBinaryHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for binary op, used for indy
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForBinaryOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> leftClass = args[0].getClass();
		Class<?> rightClass = args[1].getClass();
		// target handle
		MethodType methodType = type.changeParameterType(0, leftClass).changeParameterType(1, rightClass);
		if(leftClass == String.class || rightClass == String.class) {
			methodType = methodType.changeReturnType(String.class).unwrap();
		}
		else if(leftClass == Double.class || rightClass == Double.class) {
			methodType = methodType.changeReturnType(Double.class).unwrap();
		}
		else {
			methodType = methodType.changeReturnType(methodType.parameterType(0)).unwrap();
		}
		MethodHandle targetHandle = callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, methodType);
		targetHandle = targetHandle.asType(type);
		// test  handle
		MethodHandle testHandle = testBinaryHandle.bindTo(leftClass).bindTo(rightClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for binary op
	 * @param leftClass
	 * @param rightClass
	 * @param leftValue
	 * @param rightValue
	 * @return
	 */
	public static boolean testForBinaryOp(Class<?> leftClass, Class<?> rightClass, Object leftValue, Object rightValue) {
		return leftValue.getClass().equals(leftClass) && rightValue.getClass().equals(rightClass);
	}

	// compare op
	/**
	 * boot strap method for comparator op. used for indy.
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmCompOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbCompHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for comparator op. used for indy.
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Boolean fallBackForCompOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> leftClass = args[0].getClass();
		Class<?> rightClass = args[1].getClass();
		// target handle
		MethodType methodType = 
				type.changeParameterType(0, leftClass).
				changeParameterType(1, rightClass).changeReturnType(boolean.class).unwrap();
		MethodHandle targetHandle = callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, methodType);
		targetHandle = targetHandle.asType(type);
		// test handle
		MethodHandle testHandle = testBinaryHandle.bindTo(leftClass).bindTo(rightClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return (Boolean) targetHandle.invokeWithArguments(args);
	}

	// method call
	/**
	 * boot strap method for method call. used for indy.
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmMethod(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbMethodHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for method call. used for indy.
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForMethod(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		int paramSize = args.length;
		Class<?>[] paramClasses = new Class<?>[paramSize];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = args[i].getClass();
		}
		// target handle
		MethodHandle targetHandle = lookupMethodHandle(paramClasses[0], callSite.methodName);
		targetHandle = targetHandle.asType(type);

		// test handle
		MethodHandle testHandle = testMethodHandle.bindTo(paramClasses).asCollector(Object[].class, paramSize);
		// guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for method call. used for indy.
	 * @param paramClasses
	 * - include recv class
	 * @param paramas
	 * - include recv object
	 * @return
	 */
	public static boolean testForMethod(Class<?>[] paramClasses, Object[] paramas) {
		int size = paramClasses.length;
		for(int i = 0; i < size; i++) {
			if(!paramClasses[i].equals(paramas[i].getClass())) {
				return false;
			}
		}
		return true;
	}

	private final static Map<Class<?>, vTable> vTableMap = new HashMap<Class<?>, vTable>();

	private static MethodHandle lookupMethodHandle(Class<?> recvClass, String methodName) throws Throwable {
		if(recvClass == null || recvClass.equals(Object.class)) {
			throw new NoSuchMethodException();
		}
		vTable table = vTableMap.get(recvClass);
		if(table == null) {
			table = new vTable(recvClass);
			vTableMap.put(recvClass, table);
		}
		return table.lookup(methodName);
	}

	/**
	 * contains method handle of public instance method.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class vTable {
		private final Map<String, MethodHandle> mhMap;

		/**
		 * lookup and set all public methods of recv class.
		 * if found overloaded method, set the last method.
		 * @param recvClass
		 * @throws Throwable 
		 */
		private vTable(Class<?> recvClass) throws Throwable {
			this.mhMap = new HashMap<String, MethodHandle>();
			Lookup lookup = MethodHandles.lookup();
			java.lang.reflect.Method[] methods = recvClass.getMethods();
			for(java.lang.reflect.Method method : methods) {
				MethodHandle handle = lookup.unreflect(method);
				String methodName = method.getName();
				this.mhMap.put(methodName, handle);
			}
		}

		/**
		 * look up method handle.
		 * @param methodName
		 * - method name, do not support method overload.
		 * @return
		 * - if has no method handle, throw exception
		 */
		private MethodHandle lookup(String methodName) throws NoSuchMethodException {
			MethodHandle handle = this.mhMap.get(methodName);
			if(handle == null) {
				throw new NoSuchMethodException(methodName);
			}
			return handle;
		}
	}

	private static MethodHandle lookupFunc(Class<?> funcClass) throws Throwable {
		if(!funcClass.getSuperclass().equals(FuncHolder.class)) {
			throw new RuntimeException("require func class: " + funcClass.getSimpleName());
		}
		MethodHandle handle = funcMap.get(funcClass);
		if(handle != null) {
			return handle;
		}
		java.lang.reflect.Method[] methods = funcClass.getMethods();
		for(java.lang.reflect.Method method : methods) {
			if(method.getName().equals(JvmDriver.staticFuncMethodName)) {
				handle = MethodHandles.lookup().unreflect(method);
				funcMap.put(funcClass, handle);
				return handle;
			}
		}
		throw new NoSuchMethodException();
	}

	private final static Map<Class<?>, MethodHandle> funcMap = new HashMap<Class<?>, MethodHandle>();

	public static CallSite bsmFunc(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbFuncHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	public static Object fallBackForFunc(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		int paramSize = args.length;
		Class<?>[] paramClasses = new Class<?>[paramSize];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = args[i].getClass();
		}
		// target handle
		MethodHandle targetHandle = lookupFunc(paramClasses[0]);
		targetHandle = targetHandle.asType(type);
		// test handle
		MethodHandle testHandle = testFuncHandle.bindTo(paramClasses[0]);
		// guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	public static boolean testForFunc(Class<?> recvClass, Object recvObject) {
		return recvObject.getClass().equals(recvClass);
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

		// for interactive mode
		public static void printValue(long value)    { System.out.println("(" + long.class.getSimpleName() + ") " + value); }
		public static void printValue(double value)  { System.out.println("(" + double.class.getSimpleName() + ") " + value); }
		public static void printValue(boolean value) { System.out.println("(" + boolean.class.getSimpleName() + ") " + value); }
		public static void printValue(Object value)  { 
			if(value != null) System.out.println("(" + value.getClass().getSimpleName() + ") " + value); 
		}

		// println
		public static void println(Object value) { System.out.println(value); }

		// assert
		public static void assertImpl(boolean result) {
			if(!result) {
				new AssertionError().printStackTrace();
				System.exit(1);
			}
		}
	}

	protected static void appendStringifiedValue(StringBuilder sBuilder, Object value) {
		if(value == null) {
			sBuilder.append("$null$");
		}
		else if(value instanceof String) {
			sBuilder.append("\"");
			sBuilder.append(value);
			sBuilder.append("\"");
		}
		else {
			sBuilder.append(value);
		}
	}

	public static class ArrayImpl {
		private final ArrayList<Object> valueList;
		public ArrayImpl(Object[] values) {
			this.valueList = new ArrayList<Object>();
			for(Object value : values) {
				this.valueList.add(value);
			}
		}

		public void add(Object value) {
			this.valueList.add(value);
		}

		public Object get(Long index) {
			int actualIndex = index.intValue();
			return this.valueList.get(actualIndex);
		}

		public Object set(Long index, Object value) {
			int actualIndex = index.intValue();
			this.valueList.add(actualIndex, value);
			return value;
		}

		public Long size() {
			long size = this.valueList.size();
			return size;
		}

		@Override
		public String toString() {
			StringBuilder sBuilder = new StringBuilder();
			int size = this.valueList.size();
			sBuilder.append("[");
			for(int i = 0; i < size; i++) {
				if(i > 0) {
					sBuilder.append(", ");
				}
				appendStringifiedValue(sBuilder, this.valueList.get(i));
			}
			sBuilder.append("]");
			return sBuilder.toString();
		}
	}

	public static class MapImpl {
		private final Map<String, Object> valueMap;
		public MapImpl(String[] keys, Object[] values) {
			this.valueMap = new LinkedHashMap<String, Object>();
			int size = keys.length;
			for(int i = 0; i < size; i++) {
				this.valueMap.put(keys[i], values[i]);
			}
		}

		public Object get(String key) {
			return this.valueMap.get(key);
		}

		public Object set(String key, Object value) {
			this.valueMap.put(key, value);
			return value;
		}

		@Override
		public String toString() {
			StringBuilder sBuilder = new StringBuilder();
			Set<Entry<String, Object>> entrySet = this.valueMap.entrySet();
			int count = 0;
			sBuilder.append("{");
			for(Entry<String, Object> entry : entrySet) {
				if(count++ > 0) {
					sBuilder.append(", ");
				}
				appendStringifiedValue(sBuilder, entry.getKey());
				sBuilder.append(":");
				appendStringifiedValue(sBuilder, entry.getValue());
			}
			sBuilder.append("}");
			return sBuilder.toString();
		}

		public ArrayImpl keys() {
			Set<String> keySet = this.valueMap.keySet();
			Object[] keys = new Object[keySet.size()];
			int index = 0;
			for(String key : keySet) {
				keys[index++] = key;
			}
			return new ArrayImpl(keys);
		}

		public ArrayImpl values() {
			Collection<Object> valueSet = this.valueMap.values();
			Object[] values = new Object[valueSet.size()];
			int index = 0;
			for(Object value : valueSet) {
				values[index++] = value;
			}
			return new ArrayImpl(values);
		}

		public boolean hasKey(String key) {
			return this.valueMap.containsKey(key);
		}
	}

	public static class ExceptionImpl extends Exception {
		private static final long serialVersionUID = 7138934397471427929L;

		public static ExceptionImpl wrap(Throwable t) {
			if(t instanceof ExceptionImpl) {
				return (ExceptionImpl) t;
			}
			return new ExceptionImpl(t);
		}

		private ExceptionImpl(Throwable t) {
			super(t);
		}

		public ExceptionImpl(Object cause) {
			super(cause != null ? cause.toString() : "");
		}
	}

	public static class FuncHolder {
		protected FuncHolder() {}	// do nothing
	}
}

/**
 * ClasWriter wrapper
 * @author skgchxngsxyz-osx
 *
 */
class ClassBuilder extends ClassWriter implements Opcodes {
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
		this.visit(JvmDriver.JAVA_VERSION, ACC_PUBLIC | ACC_FINAL, this.fullyQualifiedName, null, "java/lang/Object", null);
	}

	/**
	 * create new ClassBuilder for class.
	 * @param className
	 * - fully qualified name. contains (/)
	 */
	public ClassBuilder(String className) {
		this(className, Object.class);
	}

	public ClassBuilder(String className, Class<?> superClass) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.fullyQualifiedName = className;
		String superClassInternalName = Type.getInternalName(superClass);
		this.visit(JvmDriver.JAVA_VERSION, ACC_PUBLIC | ACC_FINAL, this.fullyQualifiedName, null, superClassInternalName, null);
	}

	/**
	 * create new GeneratorAdapter for top level wrapper method.
	 * @return
	 */
	public MethodBuilder createMethodBuilder() {
		Method methodDesc = Method.getMethod("void invoke()");
		return new MethodBuilder(ACC_PUBLIC | ACC_FINAL |ACC_STATIC, methodDesc, null, null, this);
	}

	public String getClassName() {
		return this.fullyQualifiedName;
	}
}

class MethodBuilder extends GeneratorAdapter implements Opcodes {
	private final VarScopes scopes;
	private final Map<String, Label> labelMap;
	private final Stack<Label> breakLabels;
	private final Stack<Label> continueLabels;

	public MethodBuilder(int access, Method method, String signature, Type[] exceptions, ClassVisitor cv) {
		super(access, method, signature, exceptions, cv);
		int startIndex = 0;
		if((access & ACC_STATIC) != ACC_STATIC) {
			startIndex = 1;
		}
		this.scopes = new VarScopes(startIndex);
		this.labelMap = new HashMap<String, Label>();
		this.breakLabels = new Stack<Label>();
		this.continueLabels = new Stack<Label>();
	}

	public VarScopes getScopes() {
		return this.scopes;
	}

	public Stack<Label> getBreackLabels() {
		return this.breakLabels;
	}

	public Stack<Label> getContinueLabels() {
		return this.continueLabels;
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

	/**
	 * unbox stacktop
	 * @param stacktopClass
	 * @param unboxedClass
	 * - must not be stack top class
	 */
	public void unbox(Class<?> stacktopClass, Class<?> unboxedClass) {
		if(!stacktopClass.isPrimitive()) {
			this.unbox(Type.getType(unboxedClass));
		}
	}

	public Map<String, Label> getLabelMap() {
		return this.labelMap;
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

	/**
	 * 
	 * @param varName
	 * @param varClass
	 * @param isReadOnly
	 * @return
	 * - throw exception, if has already defined.
	 */
	public VarEntry addEntry(String varName, Class<?> varClass, boolean isReadOnly) {
		return this.scopeStack.peek().addEntry(varName, varClass, isReadOnly);
	}

	public FuncEntry addFuncEntry(String funcName, boolean isReadOnly) {
		return this.scopeStack.peek().addFuncEntry(funcName, isReadOnly);
	}

	/**
	 * 
	 * @param varName
	 * @return
	 * - throw exception, if had no entry.
	 */
	public VarEntry getEntry(String varName) {
		VarEntry entry = this.scopeStack.peek().getEntry(varName, true);
		if(entry == null) {
			throw new RuntimeException("undefined variable: " + varName);
		}
		return entry;
	}

	/**
	 * lookup entry from current and parent scope.
	 * @param varName
	 * @return
	 * return true, if has entry.
	 */
	public boolean hasEntry(String varName) {
		return this.scopeStack.peek().getEntry(varName, true) != null;
	}

	public boolean hasEntryInCurrentScope(String varName) {
		return this.scopeStack.peek().getEntry(varName, false) != null;
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
			throw new RuntimeException("already defined variable: " + varName);
		}
		int valueSize = Type.getType(varClass).getSize();
		int varIndex = this.currentIndex;
		VarEntry entry = new VarEntry(varIndex, varClass, false, isReadOnly);
		this.entryMap.put(varName, entry);
		this.currentIndex += valueSize;
		return entry;
	}

	public FuncEntry addFuncEntry(String funcName, boolean isReadOnly) {
		if(this.entryMap.containsKey(funcName)) {
			throw new RuntimeException("already defined function: " + funcName);
		}
		FuncEntry entry = new FuncEntry(funcName, false, isReadOnly);
		this.entryMap.put(funcName, entry);
		return entry;
	}

	/**
	 * get VarEntry.
	 * @param varName
	 * - variable name.
	 * @return
	 * - return null, if has no entry.
	 */
	public VarEntry getEntry(String varName, boolean lookupParent) {
		VarEntry entry = this.entryMap.get(varName);
		if(entry != null) {
			return  entry;
		}
		if(lookupParent) {
			return this.parentScope.getEntry(varName, lookupParent);
		}
		return null;
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
			throw new RuntimeException("already defined variable: " + varName);
		}
		VarEntry entry = new VarEntry(-1, varClass, true, isReadOnly);
		this.entryMap.put(varName, entry);
		return entry;
	}

	@Override
	public FuncEntry addFuncEntry(String funcName, boolean isReadOnly) {
		if(this.entryMap.containsKey(funcName)) {
			throw new RuntimeException("already defined function: " + funcName);
		}
		FuncEntry entry = new FuncEntry(funcName, true, isReadOnly);
		this.entryMap.put(funcName, entry);
		return entry;
	}

	@Override
	public VarEntry getEntry(String varName, boolean lookupParent/*do not use*/) {
		VarEntry entry = this.entryMap.get(varName);
		if(entry != null) {
			return entry;
		}
		return null;
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

	/**
	 * represents variable class.
	 */
	private final Class<?> variableClass;

	public VarEntry(int varIndex, Class<?> varClass, boolean isGlobal, boolean isReadOnly) {
		this.varIndex = varIndex;
		this.isGlobal = isGlobal;
		this.isReadOnly = isReadOnly;
		this.variableClass = varClass;
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

	public Class<?> getVarClass() {
		return this.variableClass;
	}
}

class FuncEntry extends VarEntry {
	private final static String funcNamePrefix = "FuncHolder_";
	private static int funcNameSuffix = -1;

	private final String internalFuncName;

	public FuncEntry(String funcName, boolean isGlobal, boolean isReadOnly) {
		super(-1, null, isGlobal, isReadOnly);
		this.internalFuncName = funcNamePrefix + funcName + ++funcNameSuffix;
	}

	public String getInternalName() {
		return this.internalFuncName;
	}
}
