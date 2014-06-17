package org.libbun;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class JvmIndyDriver extends JvmDriver {
	protected final Map<String, Handle> handleMap;

	private final static MethodHandle fbUnaryHande = initFallBackHandle("UnaryOp");
	private final static MethodHandle testUnaryHandle = initTestHandle("UnaryOp", 1);

	private final static MethodHandle fbBinaryHandle = initFallBackHandle("BinaryOp");
	private final static MethodHandle testBinaryHandle = initTestHandle("BinaryOp", 2);

	private final static MethodHandle fbCompHandle = initFallBackHandle("CompOp", Boolean.class);

	public JvmIndyDriver() {
		super("lib/driver/jvm/jvm4python.bun");
		JvmDriver.JAVA_VERSION = V1_7;
		this.addCommand("PUSH_AS_LONG_WRAP", new PushAsLongWrap());
		this.addCommand("PUSH_AS_DOUBLE_WRAP", new PushAsDoubleWrap());
		this.addCommand("PUSH_AS_BOOLEAN_WRAP", new PushAsBooleanWrap());
		this.addCommand("INDY", new DynamicInvokeCommand());

		this.handleMap = new HashMap<String, Handle>();
		this.initBsmHandle("UnaryOp");
		this.initBsmHandle("BinaryOp");
		this.initBsmHandle("CompOp");
	}

	public static class DebuggableJvmIndyDriver extends JvmIndyDriver {
		public DebuggableJvmIndyDriver() {
			JvmByteCodeLoader.setDebugMode(true);
		}
	}

	@Override
	public void initTable(Namespace gamma) {
		super.initTable(gamma);
		this.classMap.put("Integer", Integer.class);
		this.classMap.put("Float", Float.class);
		this.classMap.put("Long", Long.class);
		this.classMap.put("Double", Double.class);
		this.classMap.put("Boolean", Boolean.class);
	}

	protected void initBsmHandle(String name) {
		String bsmName = "bsm" + name;
		Type[] paramTypes = {Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class)};
		Method methodDesc = new Method(bsmName, Type.getType(CallSite.class), paramTypes);
		Handle handle = new Handle(H_INVOKESTATIC, Type.getType(this.getClass()).getInternalName(), bsmName, methodDesc.getDescriptor());
		this.handleMap.put(bsmName, handle);
	}

	protected static MethodHandle initFallBackHandle(String name) {
		return initFallBackHandle(name, Object.class);
	}

	protected static MethodHandle initFallBackHandle(String name, Class<?> returnClass) {
		MethodType type = MethodType.methodType(returnClass, new Class<?>[]{CachedCallSite.class, Object[].class});
		try {
			return MethodHandles.lookup().findStatic(JvmIndyDriver.class, "fallBackFor" + name, type);
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
			return MethodHandles.lookup().findStatic(JvmIndyDriver.class, "testFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected class PushAsLongWrap extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(Long.parseLong(node.getText()));
			mBuilders.peek().box(Type.LONG_TYPE);
		}
	}

	/**
	 * push value as java double.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsDoubleWrap extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(Double.parseDouble(node.getText()));
			mBuilders.peek().box(Type.DOUBLE_TYPE);
		}
	}

	/**
	 * push value as java boolean.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsBooleanWrap extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			if(param[0].equals("true")) {
				mBuilders.peek().push(true);
			}
			else {
				mBuilders.peek().push(false);
			}
			mBuilders.peek().box(Type.BOOLEAN_TYPE);
		}
	}

	/**
	 * INDY [method name] [bootstrap method name] [return class] [param classes...]
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class DynamicInvokeCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			String methodName = param[0];
			int size = param.length;
			int startIndex = 3;
			Type returnType = Type.getType(classMap.get(param[startIndex - 1]));
			Type[] paramTypes = new Type[size - startIndex];
			for(int i = 0; i < paramTypes.length; i++) {
				paramTypes[i] = Type.getType(classMap.get(param[startIndex + i]));
			}
			Type typeDesc = Type.getMethodType(returnType, paramTypes);
			mBuilder.invokeDynamic(methodName, typeDesc.getDescriptor(), handleMap.get(param[1]));
		}
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
		testHandle = targetHandle.asType(testHandle.type());
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
		testHandle = targetHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return (Boolean) targetHandle.invokeWithArguments(args);
	}
}
