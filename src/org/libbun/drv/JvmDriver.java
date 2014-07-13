package org.libbun.drv;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.libbun.BunDriver;
import org.libbun.BunType;
import org.libbun.DriverCommand;
import org.libbun.Namespace;
import org.libbun.UMap;
import org.libbun.drv.JvmRuntime.ArrayImpl;
import org.libbun.drv.JvmRuntime.FuncHolder;
import org.libbun.drv.JvmRuntime.JvmOperator;
import org.libbun.drv.JvmRuntime.MapImpl;
import org.libbun.peg4d.PegObject;

import org.objectweb.asm.Handle;
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
public class JvmDriver extends BunDriver implements Opcodes {
	public final static String globalVarHolderSuffix = "_LetVarHolder";
	public final static String globalVarHolderFieldName = "letVarValue";
	public final static String staticFuncMethodName = "callFuncDirectly";
	public final static String funcMethodName = "callFunc";
	public final static String funcFieldName = "funcField";

	public static int JAVA_VERSION = V1_7;
	protected final String bunModel;

	/**
	 * used for byte code loading.
	 */
	protected final JvmByteCodeLoader loader;

	/**
	 * used for bun type to java class translation.
	 */
	protected final UMap<Class<?>> classMap;

	/**
	 * used for java class generation.
	 */
	protected ClassBuilder classBuilder;

	/**
	 * used for java method (includes constructor, static initializer) generation.
	 */
	protected final Stack<MethodBuilder> mBuilders;
	protected Namespace gamma;

	/**
	 * represents current command name,
	 */
	protected String currentCommand;

	protected final Map<String, Handle> handleMap;

	/**
	 * contains type descriptor and method descriptor of static method.
	 */
	protected final Map<String, Pair<Type, Method>> staticFuncMap;
	
	public JvmDriver() {
		this("lib/driver/jvm/common.bun");
	}

	protected JvmDriver(String bunModel) {
		this.bunModel = bunModel;
		this.loader = new JvmByteCodeLoader();
		this.classMap = new UMap<Class<?>>();
		this.mBuilders = new Stack<MethodBuilder>();

		this.staticFuncMap = new HashMap<>();

		this.handleMap = new HashMap<String, Handle>();
		this.initBsmHandle("UnaryOp");
		this.initBsmHandle("BinaryOp");
		this.initBsmHandle("CompOp");
		this.initBsmHandle("Method");
		this.initBsmHandle("Func");

		// init driver command.
		this.addCommand("PushAsLong", new PushAsLong());
		this.addCommand("PushAsDouble", new PushAsDouble());
		this.addCommand("PushAsBoolean", new PushAsBoolean());
		this.addCommand("PushAsString", new PushAsString());
		this.addCommand("CallOp", new CallOperator());

		this.addCommand("And", new CondAnd());
		this.addCommand("Or", new CondOr());
		this.addCommand("VarDecl", new VarDecl());
		this.addCommand("If", new IfStatement());
		this.addCommand("While", new WhileStatement());
		this.addCommand("Block", new Block());
		this.addCommand("Print", new PrintCommand());
		this.addCommand("IsStmtEnd", new IsStmtEndCommand());

		this.addCommand("Label", new LabelCommand());
		this.addCommand("Jump", new JumpCommand());
		this.addCommand("Box", new BoxCommand());
		this.addCommand("Unbox", new UnBoxCommand());

		this.addCommand("NewArray",  new NewArrayCommand());
		this.addCommand("NewMap", new NewMapCommand());

		this.addCommand("Assign", new AssignCommand());
		this.addCommand("PyAssign", new PythonAssign());
		this.addCommand("Trinary", new TrinaryCommand());
		this.addCommand("Defun", new DefineFunction());
		this.addCommand("Return", new ReturnStatement());

		this.addCommand("CallDynamic", new DynamicInvokeCommand());
		this.addCommand("Apply", new ApplyCommand());

		/**
		 * add jvm opcode command
		 */
		new ZeroOperandInsCommand().addToDriver(this);
		new SingleIntOperandInsCommand().addToDriver(this);
		new VarInsCommand().addToDriver(this);
		new TypeInsCommand().addToDriver(this);
		new FieldInsCommand().addToDriver(this);
		new MethodInsCommand().addToDriver(this);
		new JumpInsCommand().addToDriver(this);
	}

	@Override
	public String getDesc() {
		return "Java bytecode generator by Nagisa Sekiguchi (YNU)";
	}

	@Override
	public void initTable(Namespace gamma) {
		this.classMap.put("long", long.class);
		this.classMap.put("int", int.class);
		this.classMap.put("float", float.class);
		this.classMap.put("double", double.class);
		this.classMap.put("boolean", boolean.class);
		this.classMap.put("String", String.class);
		this.classMap.put("void", Void.class);
		this.classMap.put("Object", Object.class);
		this.classMap.put("untyped", Object.class);

		this.classMap.put("Integer", Integer.class);
		this.classMap.put("Float", Float.class);
		this.classMap.put("Long", Long.class);
		this.classMap.put("Double", Double.class);
		this.classMap.put("Boolean", Boolean.class);
		gamma.loadBunModel(this.bunModel, this);
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
		if(this.mBuilders.empty()) {
			return;
		}
		this.mBuilders.peek().returnValue();
		this.mBuilders.pop().endMethod();
		this.classBuilder.visitEnd();
		String className = this.classBuilder.getClassName();
		byte[] byteCode = this.classBuilder.toByteArray();
		Class<?> generatedClass = this.loader.createChildLoader().generateClassFromByteCode(className, byteCode);
		try {
			generatedClass.getMethod("invoke").invoke(null);
		}
		catch(InvocationTargetException e) {
			e.getCause().printStackTrace();
		}
		catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public void startTopLevel() {
	}

	@Override
	public void endTopLevel() {
	}


	protected void initBsmHandle(String name) {
		String bsmName = "bsm" + name;
		Type[] paramTypes = {Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class)};
		Method methodDesc = new Method(bsmName, Type.getType(CallSite.class), paramTypes);
		Handle handle = new Handle(H_INVOKESTATIC, Type.getType(JvmRuntime.class).getInternalName(), bsmName, methodDesc.getDescriptor());
		this.handleMap.put(bsmName, handle);
	}

	/**
	 * insert print instruction after top level expression.
	 * it is interactive mode only.
	 */
	protected void insertPrintIns(Class<?> stackTopClass, boolean allowPrinting) {
		if(stackTopClass.equals(Void.class)) {
			return;
		}
		if(!allowPrinting) {
			this.mBuilders.peek().pop(stackTopClass);
			return;
		}
		if(!stackTopClass.isPrimitive()) {
			stackTopClass = Object.class;
		}
		try {
			java.lang.reflect.Method method = JvmOperator.class.getMethod("printValue", stackTopClass);
			this.mBuilders.peek().invokeStatic(Type.getType(JvmOperator.class), Method.getMethod(method));
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * 
	 * BunType to Java class
	 * @param type
	 * @return
	 * - if has no class, return Object.class
	 */
	protected Class<?> toJavaClass(BunType type) {
		if(type == null) {
			return Object.class;
		}
		String typeName = type.getName();
		Class<?> javaClass = this.classMap.get(typeName);
		if(javaClass == null) {
			//throw new RuntimeException("has no java class: " + typeName);
			return Object.class;
		}
		return javaClass;
	}

	protected BunType toType(PegObject pObject) {
		String name = pObject.tag;
		String typeName = name.substring(2);
		BunType type = this.gamma.getType(typeName, null);
		if(type == null) {
			throw new RuntimeException("undefined type: " + typeName);
		}
		return type;
	}

	public String getCommandSymbol() {
		return this.currentCommand;
	}

	@Override
	public void pushCommand(String cmd, PegObject node, String[] params) {
		DriverCommand command = this.commandMap.get(cmd);
		this.currentCommand = cmd;
		command.invoke(this, node, params);
	}

	@Override
	public void pushName(PegObject node, String name) {
		MethodBuilder mBuilder = mBuilders.peek();
		String varName = node.getText();
		VarEntry entry = mBuilder.getScopes().getEntry(varName);
		if(!(entry instanceof FuncEntry)) {	// load variable
			Type varTypeDesc = Type.getType(entry.getVarClass());
			if(!entry.isGlobal()) {	// get local variable
				mBuilder.visitVarInsn(varTypeDesc.getOpcode(ILOAD), entry.getVarIndex());
			}
			else {	// get global variable
				Type varHolderDesc = Type.getType(varName + globalVarHolderSuffix);
				mBuilder.getStatic(varHolderDesc, globalVarHolderFieldName, varTypeDesc);
			}
		}
		else {	// load func object
			Type funcHolderDesc = Type.getType("L" + ((FuncEntry)entry).getInternalName() + ";");
			mBuilder.getStatic(funcHolderDesc, funcFieldName, funcHolderDesc);
		}
		if(node.getParent() == null) {
			insertPrintIns(toJavaClass(node.getType(null).getReturnType()), node.source.fileName.equals("(stdin)"));
		}
	}

	@Override
	public void pushApplyNode(String name, PegObject args) {	// invoke static
		this.pushNode(args.getParent());
	}

	@Override
	public void pushType(BunType type) {
	}

	@Override
	public void pushCode(String text) {
	}

	@Override
	public void pushErrorNode(PegObject errorNode) {
		super.pushErrorNode(errorNode);
		mBuilders.clear();
	}

	protected void generateBlockWithNewScope(BunDriver driver, PegObject node) {
		VarScopes scopes = this.mBuilders.peek().getScopes();
		scopes.createNewScope();
		this.generateBlockWithCurrentScope(driver, node);
		scopes.removeCurrentScope();
	}

	protected void generateBlockWithCurrentScope(BunDriver driver, PegObject node) {
		if(!node.is("#block")) {
			throw new RuntimeException("require block");
		}
		driver.pushNode(node);
	}

	protected class IsStmtEndCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			if(node.getParent() == null) {
				BunType type = node.getType(null);
				if(node.is("#apply")) {
					type = node.get(1).getType(null);
				}
				insertPrintIns(toJavaClass(type), node.source.fileName.equals("(stdin)"));
				return;
			}
			if(param.length == 1 && param[0].equals("method")) {
				PegObject applyNode = node.getParent();
				this.invoke(driver, applyNode, new String[]{});
			}
		}
	}

	/**
	 * push value as java long.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsLong extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(Long.parseLong(node.getText()));
		}
	}

	/**
	 * push value as java double.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsDouble extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(Double.parseDouble(node.getText()));
		}
	}

	/**
	 * push value as java boolean.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsBoolean extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(param[0].equals("true"));
		}
	}

	/**
	 * push value as java string.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class PushAsString extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().push(this.parseTokenText(node.getText()));
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
	protected class CallOperator extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String opName = node.tag.substring(1);
			int size = node.size();
			Class<?>[] paramClasses = new Class<?>[size];
			for(int i = 0; i < size; i++) {
				paramClasses[i] = toJavaClass(node.get(i).getType(null));
			}
			this.callOperator(opName, paramClasses);
		}

		/**
		 * look up and generate invokestatic instruction.
		 * @param opName
		 * - operator name.
		 * @param paramClasses
		 * - operator parameter classes.
		 */
		protected void callOperator(String opName, Class<?>[] paramClasses) {
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
	}

	/**
	 * generate conditional and. after evaluation, push boolean.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class CondAnd extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			Label rightLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();
			// and left
			driver.pushNode(node.get(0));
			mBuilder.unbox(toJavaClass(node.get(0).getType(null)), boolean.class);
			mBuilder.push(true);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, rightLabel);
			mBuilder.push(false);
			mBuilder.goTo(mergeLabel);
			// and right
			mBuilder.mark(rightLabel);
			driver.pushNode(node.get(1));
			mBuilder.unbox(toJavaClass(node.get(1).getType(null)), boolean.class);
			mBuilder.mark(mergeLabel);
		}
	}

	/**
	 * generate condiotional or. after evaluation, push boolean.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class CondOr extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			Label rightLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();
			// or left
			driver.pushNode(node.get(0));
			mBuilder.unbox(toJavaClass(node.get(0).getType(null)), boolean.class);
			mBuilder.push(true);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, rightLabel);
			mBuilder.push(true);
			mBuilder.goTo(mergeLabel);
			// or right
			mBuilder.mark(rightLabel);
			driver.pushNode(node.get(1));
			mBuilder.unbox(toJavaClass(node.get(1).getType(null)), boolean.class);
			mBuilder.mark(mergeLabel);
		}
	}

	protected class TrinaryCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			Label elseLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();
			// cond
			driver.pushNode(node.get(0));
			mBuilder.unbox(toJavaClass(node.get(0).getType(null)), boolean.class);
			mBuilder.push(true);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);
			// then
			driver.pushNode(node.get(1));
			mBuilder.goTo(mergeLabel);
			// else
			mBuilder.mark(elseLabel);
			driver.pushNode(node.get(2));
			// merge
			mBuilder.mark(mergeLabel);
		}
	}

	/**
	 * generate let.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class VarDecl extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String varName = node.get(0).getText();
			boolean isReadOnly = node.is("#let");
			defineVariable(driver, varName, node.get(2), isReadOnly);
		}
	}

	protected void defineVariable(BunDriver driver, String varName, PegObject initValueNode, boolean isReadOnly) {
		MethodBuilder currentBuilder = mBuilders.peek();
		// add var entry to scope
		Class<?> varClass = toJavaClass(initValueNode.getType(null));
		VarEntry entry = currentBuilder.getScopes().addEntry(varName, varClass, isReadOnly);

		int scopeDepth = currentBuilder.getScopes().depth();
		if(scopeDepth > 1) {	// define as local variable.
			driver.pushNode(initValueNode);
			currentBuilder.visitVarInsn(Type.getType(varClass).getOpcode(ISTORE), entry.getVarIndex());
		}
		else {	// define as global variable.
			ClassBuilder cBuilder = new ClassBuilder(varName + JvmDriver.globalVarHolderSuffix);
			// create static initializer
			Method methodDesc = Method.getMethod("void <clinit> ()");
			MethodBuilder mBuilder = new MethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, methodDesc, null, null, cBuilder);
			mBuilders.push(mBuilder);
			driver.pushNode(initValueNode);
			Type ownerTypeDesc = Type.getType(cBuilder.getClassName());
			Type fieldTypeDesc = Type.getType(varClass);
			mBuilder.putStatic(ownerTypeDesc, globalVarHolderFieldName, fieldTypeDesc);
			mBuilder.returnValue();
			mBuilder.endMethod();
			mBuilders.pop();

			// create var field
			cBuilder.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, globalVarHolderFieldName, fieldTypeDesc.getDescriptor(), null, null);

			// finalize
			cBuilder.visitEnd();
			loader.generateClassFromByteCode(cBuilder.getClassName(), cBuilder.toByteArray());

			// initialized let var.
			currentBuilder.getStatic(ownerTypeDesc, globalVarHolderFieldName, fieldTypeDesc);
			currentBuilder.pop(varClass);
		}
	}

	protected class AssignCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			assignToLeft(driver, node, param);
		}
	}

	protected void assignToLeft(BunDriver driver, PegObject node, String[] param) {	//TODO: field
		MethodBuilder mBuilder = mBuilders.peek();
		PegObject leftNode = node.get(0);
		PegObject rightNode = node.get(1);
		if(leftNode.is("#name")) {
			String varName = leftNode.getText();
			VarEntry entry = mBuilder.getScopes().getEntry(varName);
			if(entry.isReadOnly()) {
				throw new RuntimeException("read only variable: " + varName);
			}
			Type varTypeDesc = Type.getType(entry.getVarClass());
			if(!entry.isGlobal()) {	// local variable
				int varIndex = entry.getVarIndex();
				driver.pushNode(rightNode);
				mBuilder.visitVarInsn(varTypeDesc.getOpcode(ISTORE), varIndex);
			}
			else { // global variable
				driver.pushNode(rightNode);
				Type varHolderDesc = Type.getType(varName + globalVarHolderSuffix);
				mBuilder.putStatic(varHolderDesc, globalVarHolderFieldName, varTypeDesc);
			}
		}
		else if(leftNode.is("#index")) {
			PegObject recvNode = leftNode.get(0);
			driver.pushNode(recvNode);
			
		}
		else if(leftNode.is("#field")) {	//TODO:
			
		}
		else {
			throw new RuntimeException("unsuppored assing: " + leftNode.tag);
		}
	}

	protected class PythonAssign extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			PegObject leftNode = node.get(0);
			if(leftNode.is("#name")) {
				if(!mBuilders.peek().getScopes().hasEntry(leftNode.getText())) {
					defineVariable(driver, leftNode.getText(), node.get(1), false);
					return;
				}
			}
			assignToLeft(driver, node, param);
		}
	}

	protected class SymbolCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			String varName = node.getText();
			VarEntry entry = mBuilder.getScopes().getEntry(varName);
			if(!(entry instanceof FuncEntry)) {	// load variable
				Type varTypeDesc = Type.getType(entry.getVarClass());
				if(!entry.isGlobal()) {	// get local variable
					mBuilder.visitVarInsn(varTypeDesc.getOpcode(ILOAD), entry.getVarIndex());
				}
				else {	// get global variable
					Type varHolderDesc = Type.getType(varName + globalVarHolderSuffix);
					mBuilder.getStatic(varHolderDesc, globalVarHolderFieldName, varTypeDesc);
				}
			}
			else {	// load func object
				Type funcHolderDesc = Type.getType("L" + ((FuncEntry)entry).getInternalName() + ";");
				mBuilder.getStatic(funcHolderDesc, funcFieldName, funcHolderDesc);
			}
		}
	}

	protected class Block extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int size = node.size();
			for(int i = 0; i < size; i++) {
				PegObject targetNode = node.get(i);
				driver.pushNode(targetNode);
				Class<?> stacktopClass = toJavaClass(targetNode.getType(null));
				if(!stacktopClass.equals(Void.class)) {
					mBuilders.peek().pop(stacktopClass);
				}
			}
		}
	}

	protected class IfStatement extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int nodeSize = node.size();
			MethodBuilder mBuilder = mBuilders.peek();
			Label elseLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();
			// if cond
			driver.pushNode(node.get(0));
			mBuilder.unbox(toJavaClass(node.get(0).getType(null)), boolean.class);
			mBuilder.push(true);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);
			// then block
			generateBlockWithNewScope(driver, node.get(1));
			mBuilder.goTo(mergeLabel);
			// else block
			mBuilder.mark(elseLabel);
			if(nodeSize == 3) {
				generateBlockWithNewScope(driver, node.get(2));
			}
			mBuilder.mark(mergeLabel);
		}
	}

	protected class WhileStatement extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			Label breakLabel = mBuilder.newLabel();
			Label continueLabel = mBuilder.newLabel();
			mBuilder.getBreackLabels().push(breakLabel);
			mBuilder.getContinueLabels().push(continueLabel);

			mBuilder.mark(continueLabel);
			mBuilder.push(true);
			driver.pushNode(node.get(0));
			mBuilder.unbox(toJavaClass(node.get(0).getType(null)), boolean.class);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, breakLabel);
			generateBlockWithNewScope(driver, node.get(1));
			mBuilder.goTo(continueLabel);
			mBuilder.mark(breakLabel);

			mBuilder.getBreackLabels().pop();
			mBuilder.getContinueLabels().pop();
		}
	}

	protected class BoxCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String typeName = param[0];
			Class<?> stacktopClass = classMap.get(typeName);
			mBuilders.peek().box(Type.getType(stacktopClass));
		}
	}

	protected class UnBoxCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String typeName = param[0];
			Class<?> stacktopClass = classMap.get(typeName);
			mBuilders.peek().unbox(Type.getType(stacktopClass));
		}
	}

	/**
	 * generate array. acutauly call constructor of ArrayImpl.ArrayImpl(Object[])
	 * not support primitive value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class NewArrayCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int size = node.size();
			Type elementTypeDesc = Type.getType(Object.class);
			Type arrayClassDesc = Type.getType(ArrayImpl.class);
			Method arrayInitDesc = this.createArrayInitDesc(elementTypeDesc);

			GeneratorAdapter adapter = mBuilders.peek();
			adapter.newInstance(arrayClassDesc);
			adapter.dup();
			adapter.push(size);
			adapter.newArray(elementTypeDesc);
			for(int i = 0; i < size; i++) {
				adapter.dup();
				adapter.push(i);
				driver.pushNode(node.get(i));
				adapter.arrayStore(elementTypeDesc);
			}
			adapter.invokeConstructor(arrayClassDesc, arrayInitDesc);
		}

		private Method createArrayInitDesc(Type elementTypeDesc) {
			Type paramTypeDesc = Type.getType("[" + elementTypeDesc.getDescriptor());
			Type returnTypeDesc = Type.VOID_TYPE;
			return new Method("<init>", returnTypeDesc, new Type[]{paramTypeDesc});
		}
	}

	protected class NewMapCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int size = node.size();
			Type keyTypeDesc = Type.getType(String.class);
			Type valueTypeDesc = Type.getType(Object.class);
			Type mapClassDesc = Type.getType(MapImpl.class);
			Method mapInitDesc = this.createMapInitDesc(valueTypeDesc);

			GeneratorAdapter adapter = mBuilders.peek();
			adapter.newInstance(mapClassDesc);
			adapter.dup();
			// create key array
			adapter.push(size);
			adapter.newArray(keyTypeDesc);
			for(int i = 0; i < size; i++) {
				adapter.dup();
				adapter.push(i);
				driver.pushNode(node.get(i).get(0));
				adapter.checkCast(keyTypeDesc);
				adapter.arrayStore(keyTypeDesc);
			}
			// create value array
			adapter.push(size);
			adapter.newArray(valueTypeDesc);
			for(int i = 0; i < size; i++) {
				adapter.dup();
				adapter.push(i);
				driver.pushNode(node.get(i).get(1));
				adapter.arrayStore(valueTypeDesc);
			}
			adapter.invokeConstructor(mapClassDesc, mapInitDesc);
		}

		private Method createMapInitDesc(Type valueTypeDesc) {
			Type paramTypeDesc1 = Type.getType("[" + Type.getType(String.class).getDescriptor());
			Type paramTypeDesc2 = Type.getType("[" + valueTypeDesc.getDescriptor());
			Type returnTypeDesc = Type.VOID_TYPE;
			return new Method("<init>", returnTypeDesc, new Type[]{paramTypeDesc1, paramTypeDesc2});
		}
	}

	protected class PrintCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			Class<?> valueClass = toJavaClass(node.get(0).getType(null));
			if(!valueClass.isPrimitive()) {
				valueClass = Object.class;
			}
			try {
				java.lang.reflect.Method method = JvmOperator.class.getMethod("printValue", valueClass);
				mBuilders.peek().invokeStatic(Type.getType(JvmOperator.class), Method.getMethod(method));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	protected class DefineFunction extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String funcName = node.get(0).getText();
			String internalName = mBuilders.peek().getScopes().addFuncEntry(funcName, false).getInternalName();
			ClassBuilder cBuilder = new ClassBuilder(internalName, FuncHolder.class);

			// static field
			Type fieldTypeDesc = Type.getType("L" + internalName + ";");
			cBuilder.visitField(ACC_PUBLIC | ACC_STATIC, funcFieldName, fieldTypeDesc.getDescriptor(), null, null);

			// static initializer
			Method initDesc = Method.getMethod("void <init> ()");
			Method clinitDesc = Method.getMethod("void <clinit> ()");
			MethodBuilder mBuilder = new MethodBuilder(ACC_PUBLIC | ACC_STATIC, clinitDesc, null, null, cBuilder);
			mBuilder.newInstance(fieldTypeDesc);
			mBuilder.dup();
			mBuilder.invokeConstructor(fieldTypeDesc, initDesc);
			mBuilder.putStatic(fieldTypeDesc, funcFieldName, fieldTypeDesc);
			mBuilder.returnValue();
			mBuilder.endMethod();

			// constructor
			mBuilder = new MethodBuilder(ACC_PUBLIC, initDesc, null, null, cBuilder);
			mBuilder.loadThis();
			mBuilder.invokeConstructor(Type.getType(FuncHolder.class), initDesc);
			mBuilder.returnValue();
			mBuilder.endMethod();

			// static method
			BunType returnType = node.getType(null).getReturnType();
			PegObject paramsNode = node.get(1);
			Method methodDesc = this.toMethodDesc(returnType, staticFuncMethodName, paramsNode);//TODO:
			staticFuncMap.put(internalName, new Pair<Type, Method>(fieldTypeDesc, methodDesc));
			mBuilder = new MethodBuilder(ACC_PUBLIC | ACC_STATIC, methodDesc, null, null, cBuilder);
			mBuilders.push(mBuilder);
			// set argument
			mBuilder.getScopes().createNewScope();
			int paramSize = paramsNode.size();
			for(int i = 0; i < paramSize; i++) {
				PegObject paramNode = paramsNode.get(i).get(0);
				mBuilder.getScopes().addEntry(paramNode.getText(), toJavaClass(paramNode.getType(null)), false);
			}
			// generate func body
			generateBlockWithCurrentScope(driver, node.get(3));
			mBuilder.getScopes().removeCurrentScope();
			mBuilders.pop().endMethod();

			// instance method
			Method indirectMethidDesc = this.toMethodDesc(returnType, funcMethodName, paramsNode);
			mBuilder = new MethodBuilder(ACC_PUBLIC, indirectMethidDesc, null, null, cBuilder);
			mBuilder.loadArgs();
			mBuilder.invokeStatic(fieldTypeDesc, methodDesc);
			mBuilder.returnValue();
			mBuilder.endMethod();
			
			loader.generateClassFromByteCode(internalName, cBuilder.toByteArray());
		}

		// TODO: return type
		private Method toMethodDesc(BunType returnType, String methodName, PegObject paramsNode) {
			Type returnTypeDesc = Type.getType(toJavaClass(returnType));
			int paramSize = paramsNode.size();
			Type[] paramTypeDescs = new Type[paramSize];
			for(int i = 0; i < paramSize; i++) {
				paramTypeDescs[i] = Type.getType(toJavaClass(paramsNode.get(i).get(0).getType(null)));
			}
			return new Method(methodName, returnTypeDesc, paramTypeDescs);
		}
	}

	protected class ReturnStatement extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			mBuilders.peek().returnValue();
		}
	}

	protected class JumpCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			String target = param[0];
			if(target.equals("break")) {
				Label label = mBuilder.getBreackLabels().peek();
				mBuilder.goTo(label);
			} else if(target.equals("continue")) {
				Label label = mBuilder.getContinueLabels().peek();
				mBuilder.goTo(label);
			} else {
				throw new RuntimeException("unsupported target: " + target);
			}
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

	protected class ApplyCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			PegObject targetNode = node.get(0);
			PegObject argsNode = node.get(1);
			if(targetNode.is("#field")) {	// method call
				PegObject recvNode = targetNode.get(0);
				String methodName = targetNode.get(1).getText();
				int paramSize = argsNode.size();
				driver.pushNode(recvNode);
				for(int i = 0 ; i < paramSize; i++) {
					driver.pushNode(argsNode.get(i));
				}
				String typeDesc = this.createDescriptor(paramSize + 1).getDescriptor();
				mBuilders.peek().invokeDynamic(methodName, typeDesc, handleMap.get("bsmMethod"));
			}
			else if(targetNode.is("#name")) {	// func call
				MethodBuilder mBuilder = mBuilders.peek();
				Pair<Type, Method> pair = this.lookupFunc(driver, targetNode);
				int paramSize = argsNode.size();
				for(int i = 0 ; i < paramSize; i++) {
					driver.pushNode(argsNode.get(i));
				}
				mBuilder.invokeStatic(pair.getLeft(), pair.getRight());
			}
			else {
				throw new RuntimeException("unsupported apply: " + targetNode.tag);
			}
		}

		private Type createDescriptor(int paramSize) {
			Type[] paramTypeDescs = new Type[paramSize];
			Type returnTypeDesc = Type.getType(Object.class);
			for(int i = 0; i < paramSize; i++) {
				paramTypeDescs[i] = Type.getType(Object.class);
			}
			return Type.getMethodType(returnTypeDesc, paramTypeDescs);
		}

		private Pair<Type, Method> lookupFunc(BunDriver driver, PegObject funcNameNode) {
			VarScopes scopes = mBuilders.peek().getScopes();
			String funcName = funcNameNode.getText();
			String key = funcName;
			if(scopes.hasEntry(funcName)) {
				//
				FuncEntry entry = (FuncEntry) scopes.getEntry(funcName);
				key = entry.getInternalName();
			}
			return staticFuncMap.get(key);
		}
	}

	protected abstract class JvmOpcodeCommand extends DriverCommand {
		public abstract void addToDriver(JvmDriver driver);

		/**
		 * replace '.' to '/'
		 * add class name prefix (org/libbun/drv/JvmRuntime/)
		 * @param name
		 * - class name or method descriptor.
		 * @return
		 */
		protected String format(String name) {
			return this.format(name, false);
		}

		protected String format(String name, boolean usedForMethodDesc) {
			switch(name) {
			case "int":
				return Type.INT_TYPE.getDescriptor();
			case "long":
				return Type.LONG_TYPE.getDescriptor();
			case "short":
				return Type.SHORT_TYPE.getDescriptor();
			case "byte":
				return Type.BYTE_TYPE.getDescriptor();
			case "float":
				return Type.FLOAT_TYPE.getDescriptor();
			case "double":
				return Type.DOUBLE_TYPE.getDescriptor();
			case "boolean":
				return Type.BOOLEAN_TYPE.getDescriptor();
			case "void":
				return Type.VOID_TYPE.getDescriptor();
			}
			String replacedName = name.replace('.', '/');
			if(replacedName.indexOf('/') == -1) {
				replacedName = "org/libbun/drv/JvmRuntime$" + replacedName;
			}
			return usedForMethodDesc? "L" + replacedName + ";" : replacedName;
		}

		/**
		 * create method descriptor
		 * @param returnClassName
		 * @param paramClassName
		 * @param startIndex
		 * @return
		 */
		protected String format(String returnClassName, String[] paramClassName, int startIndex) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append('(');
			for(int i = startIndex; i < paramClassName.length; i++) {
				sBuilder.append(this.format(paramClassName[i], true));
			}
			sBuilder.append(')');
			sBuilder.append(this.format(returnClassName, true));
			return sBuilder.toString();
		}
	}

	protected class ZeroOperandInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = ZeroOperandIns.toCode(ins);
			mBuilders.peek().visitInsn(code.getOpCode());
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			ZeroOperandIns[] codes = ZeroOperandIns.values();
			for(ZeroOperandIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class SingleIntOperandInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = SingleIntOperandIns.toCode(ins);
			mBuilders.peek().visitIntInsn(code.getOpCode(), Integer.parseInt(param[0]));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			SingleIntOperandIns[] codes = SingleIntOperandIns.values();
			for(SingleIntOperandIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class VarInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = VarIns.toCode(ins);
			mBuilders.peek().visitVarInsn(code.getOpCode(), Integer.parseInt(param[0]));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			VarIns[] codes = VarIns.values();
			for(VarIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class TypeInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = TypeIns.toCode(ins);
			mBuilders.peek().visitTypeInsn(code.getOpCode(), format(param[0]));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			TypeIns[] codes = TypeIns.values();
			for(TypeIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class FieldInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = FieldIns.toCode(ins);
			mBuilders.peek().visitFieldInsn(code.getOpCode(), format(param[0]), format(param[1]), format(param[2]));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			FieldIns[] codes = FieldIns.values();
			for(FieldIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class MethodInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = MethodIns.toCode(ins);
			mBuilders.peek().visitMethodInsn(code.getOpCode(), format(param[0]), param[2], format(param[1], param, 3));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			MethodIns[] codes = MethodIns.values();
			for(MethodIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class JumpInsCommand extends JvmOpcodeCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			String ins = ((JvmDriver) driver).getCommandSymbol();
			JvmOpCode code = JumpIns.toCode(ins);
			MethodBuilder mBuilder = mBuilders.peek();
			mBuilder.visitJumpInsn(code.getOpCode(), mBuilder.getLabelMap().get(param[0]));
		}

		@Override
		public void addToDriver(JvmDriver driver) {
			JumpIns[] codes = JumpIns.values();
			for(JumpIns code : codes) {
				driver.addCommand(code.name(), this);
			}
		}
	}

	protected class LabelCommand extends DriverCommand {	//FIXME:
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int nameSuffix = node.hashCode();
			Map<String, Label> labelMap = mBuilders.peek().getLabelMap();
			for(String labelName : param) {
				String actualName = labelName + nameSuffix;
				if(labelMap.containsKey(actualName)) {
					throw new RuntimeException("areadly defined label: " + actualName);
				}
				labelMap.put(actualName, mBuilders.peek().newLabel());
			}
		}
	}

	protected class UnLabelCommand extends DriverCommand {	//FIXME
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			int nameSuffix = node.hashCode();
			Map<String, Label> labelMap = mBuilders.peek().getLabelMap();
			for(String labelName : param) {
				labelMap.remove(labelName + nameSuffix);
			}
		}
	}

	public static class DebuggableJvmDriver extends JvmDriver {
		public DebuggableJvmDriver() {
			JvmByteCodeLoader.setDebugMode(true);
		}
	}
}

/**
 * represent jvm opcode
 * @author skgchxngsxyz-osx
 *
 */
interface JvmOpCode {
	/**
	 * get opcode used by asm.
	 * @return
	 * - opcode
	 */
	public int getOpCode();
}

/**
 * OP 
 * @author skgchxngsxyz-osx
 *
 */
enum ZeroOperandIns implements JvmOpCode {
	NOP          (Opcodes.NOP), 
	ACONST_NULL  (Opcodes.ACONST_NULL), 
	ICONST_M1    (Opcodes.ICONST_M1), 
	ICONST_0     (Opcodes.ICONST_0), 
	ICONST_1     (Opcodes.ICONST_1), 
	ICONST_2     (Opcodes.ICONST_2), 
	ICONST_3     (Opcodes.ICONST_3), 
	ICONST_4     (Opcodes.ICONST_4), 
	ICONST_5     (Opcodes.ICONST_5), 
	LCONST_0     (Opcodes.LCONST_0), 
	LCONST_1     (Opcodes.LCONST_1), 
	FCONST_0     (Opcodes.FCONST_0), 
	FCONST_1     (Opcodes.FCONST_1), 
	FCONST_2     (Opcodes.FCONST_2), 
	DCONST_0     (Opcodes.DCONST_0), 
	DCONST_1     (Opcodes.DCONST_1), 
	IALOAD       (Opcodes.IALOAD), 
	LALOAD       (Opcodes.LALOAD), 
	FALOAD       (Opcodes.FALOAD), 
	DALOAD       (Opcodes.DALOAD), 
	AALOAD       (Opcodes.AALOAD), 
	BALOAD       (Opcodes.BALOAD), 
	CALOAD       (Opcodes.CALOAD), 
	SALOAD       (Opcodes.SALOAD), 
	IASTORE      (Opcodes.IASTORE), 
	LASTORE      (Opcodes.LASTORE), 
	FASTORE      (Opcodes.FASTORE), 
	DASTORE      (Opcodes.DASTORE), 
	AASTORE      (Opcodes.AASTORE), 
	BASTORE      (Opcodes.BASTORE), 
	CASTORE      (Opcodes.CASTORE), 
	SASTORE      (Opcodes.SASTORE), 
	POP          (Opcodes.POP), 
	POP2         (Opcodes.POP2), 
	DUP          (Opcodes.DUP), 
	DUP_X1       (Opcodes.DUP_X1), 
	DUP_X2       (Opcodes.DUP_X2), 
	DUP2         (Opcodes.DUP2), 
	DUP2_X1      (Opcodes.DUP2_X1), 
	DUP2_X2      (Opcodes.DUP2_X2), 
	SWAP         (Opcodes.SWAP), 
	IADD         (Opcodes.IADD), 
	LADD         (Opcodes.LADD), 
	FADD         (Opcodes.FADD), 
	DADD         (Opcodes.DADD), 
	ISUB         (Opcodes.ISUB), 
	LSUB         (Opcodes.LSUB), 
	FSUB         (Opcodes.FSUB), 
	DSUB         (Opcodes.DSUB), 
	IMUL         (Opcodes.IMUL), 
	LMUL         (Opcodes.LMUL), 
	FMUL         (Opcodes.FMUL), 
	DMUL         (Opcodes.DMUL), 
	IDIV         (Opcodes.IDIV), 
	LDIV         (Opcodes.LDIV), 
	FDIV         (Opcodes.FDIV), 
	DDIV         (Opcodes.DDIV), 
	IREM         (Opcodes.IREM), 
	LREM         (Opcodes.LREM), 
	FREM         (Opcodes.FREM), 
	DREM         (Opcodes.DREM), 
	INEG         (Opcodes.INEG), 
	LNEG         (Opcodes.LNEG), 
	FNEG         (Opcodes.FNEG), 
	DNEG         (Opcodes.DNEG), 
	ISHL         (Opcodes.ISHL), 
	LSHL         (Opcodes.LSHL), 
	ISHR         (Opcodes.ISHR), 
	LSHR         (Opcodes.LSHR), 
	IUSHR        (Opcodes.IUSHR), 
	LUSHR        (Opcodes.LUSHR), 
	IAND         (Opcodes.IAND), 
	LAND         (Opcodes.LAND), 
	IOR          (Opcodes.IOR), 
	LOR          (Opcodes.LOR), 
	IXOR         (Opcodes.IXOR), 
	LXOR         (Opcodes.LXOR), 
	I2L          (Opcodes.I2L), 
	I2F          (Opcodes.I2F), 
	I2D          (Opcodes.I2D), 
	L2I          (Opcodes.L2I), 
	L2F          (Opcodes.L2F), 
	L2D          (Opcodes.L2D), 
	F2I          (Opcodes.F2I), 
	F2L          (Opcodes.F2L), 
	F2D          (Opcodes.F2D), 
	D2I          (Opcodes.D2I), 
	D2L          (Opcodes.D2L), 
	D2F          (Opcodes.D2F), 
	I2B          (Opcodes.I2B), 
	I2C          (Opcodes.I2C), 
	I2S          (Opcodes.I2S), 
	LCMP         (Opcodes.LCMP), 
	FCMPL        (Opcodes.FCMPL), 
	FCMPG        (Opcodes.FCMPG), 
	DCMPL        (Opcodes.DCMPL), 
	DCMPG        (Opcodes.DCMPG), 
	IRETURN      (Opcodes.IRETURN), 
	LRETURN      (Opcodes.LRETURN), 
	FRETURN      (Opcodes.FRETURN), 
	DRETURN      (Opcodes.DRETURN), 
	ARETURN      (Opcodes.ARETURN), 
	RETURN       (Opcodes.RETURN), 
	ARRAYLENGTH  (Opcodes.ARRAYLENGTH), 
	ATHROW       (Opcodes.ATHROW), 
	MONITORENTER (Opcodes.MONITORENTER), 
	MONITOREXIT  (Opcodes.MONITOREXIT);

	private final int opcode;
	private ZeroOperandIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [byte]
 * @author skgchxngsxyz-osx
 *
 */
enum SingleIntOperandIns implements JvmOpCode {
	BIPUSH   (Opcodes.BIPUSH), 
	SIPUSH   (Opcodes.SIPUSH), 
	NEWARRAY (Opcodes.NEWARRAY);

	private final int opcode;
	private SingleIntOperandIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [var index]
 * @author skgchxngsxyz-osx
 *
 */
enum VarIns implements JvmOpCode {
	ILOAD  (Opcodes.ILOAD), 
	LLOAD  (Opcodes.LLOAD), 
	FLOAD  (Opcodes.FLOAD), 
	DLOAD  (Opcodes.DLOAD), 
	ALOAD  (Opcodes.ALOAD), 
	ISTORE (Opcodes.ISTORE), 
	LSTORE (Opcodes.LSTORE), 
	FSTORE (Opcodes.FSTORE), 
	DSTORE (Opcodes.DSTORE), 
	ASTORE (Opcodes.ASTORE), 
	RET    (Opcodes.RET);

	private final int opcode;
	private VarIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [internal class name]
 * @author skgchxngsxyz-osx
 *
 */
enum TypeIns implements JvmOpCode {
	NEW        (Opcodes.NEW), 
	ANEWARRAY  (Opcodes.ANEWARRAY), 
	CHECKCAST  (Opcodes.CHECKCAST), 
	INSTANCEOF (Opcodes.INSTANCEOF);

	private final int opcode;
	private TypeIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [internal class name of owner] [field name] [field's type descriptor]
 * @author skgchxngsxyz-osx
 *
 */
enum FieldIns implements JvmOpCode {
	GETSTATIC (Opcodes.GETSTATIC), 
	PUTSTATIC (Opcodes.PUTSTATIC), 
	GETFIELD  (Opcodes.GETFIELD), 
	PUTFIELD  (Opcodes.PUTFIELD);

	private final int opcode;
	private FieldIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [internal class name of owner] [return class name] [method name] [param class names]...
 * @author skgchxngsxyz-osx
 *
 */
enum MethodIns implements JvmOpCode {
	INVOKEVIRTUAL   (Opcodes.INVOKEVIRTUAL), 
	INVOKESPECIAL   (Opcodes.INVOKESPECIAL), 
	INVOKESTATIC    (Opcodes.INVOKESTATIC), 
	INVOKEINTERFACE (Opcodes.INVOKEINTERFACE);

	private final int opcode;
	private MethodIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [label name]
 * @author skgchxngsxyz-osx
 *
 */
enum JumpIns implements JvmOpCode {
	IFEQ      (Opcodes.IFEQ), 
	IFNE      (Opcodes.IFNE), 
	IFLT      (Opcodes.IFLT), 
	IFGE      (Opcodes.IFGE), 
	IFGT      (Opcodes.IFGT), 
	IFLE      (Opcodes.IFLE), 
	IF_ICMPEQ (Opcodes.IF_ICMPEQ), 
	IF_ICMPNE (Opcodes.IF_ICMPNE), 
	IF_ICMPLT (Opcodes.IF_ICMPLT), 
	IF_ICMPGE (Opcodes.IF_ICMPGE), 
	IF_ICMPGT (Opcodes.IF_ICMPGT), 
	IF_ICMPLE (Opcodes.IF_ICMPLE), 
	IF_ACMPEQ (Opcodes.IF_ACMPEQ), 
	IF_ACMPNE (Opcodes.IF_ACMPNE), 
	GOTO      (Opcodes.GOTO), 
	JSR       (Opcodes.JSR), 
	IFNULL    (Opcodes.IFNULL), 
	IFNONNULL (Opcodes.IFNONNULL);

	private final int opcode;
	private JumpIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

/**
 * OP [non null value(Integer, Float, Long, Double, String...)]
 * @author skgchxngsxyz-osx
 *
 */
enum LdcIns implements JvmOpCode {	//TODO:
	LDC (Opcodes.LDC);

	private final int opcode;
	private LdcIns(int opcode) {
		this.opcode = opcode;
	}

	@Override
	public int getOpCode() {
		return this.opcode;
	}

	public static JvmOpCode toCode(String codeString) {
		try {
			return valueOf(codeString);
		}
		catch(Exception e) {
		}
		return null;
	}
}

class Pair<L,R> {
	private L left;
	private R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return this.left;
	}

	public R getRight() {
		return this.right;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public void setRight(R right) {
		this.right = right;
	}

	@Override
	public String toString() {
		return "(" + this.left.toString() + ", " + this.right.toString() + ")";
	}
}
