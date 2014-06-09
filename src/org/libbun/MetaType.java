package org.libbun;

public abstract class MetaType  {
	protected int         typeId    = -1;
	protected Object      typeInfo;  // used in Class<?> in JVM (for example)

	MetaType(Object typeInfo) {
		this.typeInfo = typeInfo;
	}

	public final String getName() {
		UniStringBuilder sb = new UniStringBuilder();
		stringfy(sb);
		return sb.toString();
	}
	
	public final String toString() {
		UniStringBuilder sb = new UniStringBuilder();
		stringfy(sb);
		return sb.toString();
	}

	public int size() {
		return 0;
	}

	public MetaType getParamType(int index) {
		return this;
	}
	
	public int getFuncParamSize() {
		return 0;
	}

	public MetaType getFuncParamType(int index) {
		return this;
	}

	public MetaType getReturnType() {
		return this;
	}

	public Object getTypeInfo() {
		return this.typeInfo;
	}

	public abstract boolean hasVarType();
	public abstract MetaType getRealType();
	public abstract boolean hasGreekType();
	public abstract MetaType getRealType(MetaType[] greekContext);

	public abstract void stringfy(UniStringBuilder sb);
	public abstract boolean is(MetaType valueType, MetaType[] greekContext);
	
	public void build(PegObject node, BunDriver driver) {
		node.matched.build(node, driver);
	}

	// ----------------------------------------------------------------------
	
	final static UniArray<MetaType>   _TypeList;
	final static UniMap<MetaType>     _TypeNameMap;

	public static MetaType UntypedType;
		
	static {
		_TypeList = new UniArray<MetaType>(new MetaType[128]);
		_TypeNameMap = new UniMap<MetaType>();
		UntypedType = new UntypedType();
		issueType("", UntypedType);
		setGenerics(new FuncType(0));
	}

	private static void issueType(String key, MetaType t) {
		t.typeId = _TypeNameMap.size();
		_TypeNameMap.put(key, t);
	}
	
	public final static MetaType newValueType(String name, Object typeInfo) {
		MetaType t = _TypeNameMap.get(name, null);
		if(t == null) {
			t = new ValueType(name, typeInfo);
			issueType(name, t);
		}
		return t;
	}
	
	public static MetaType newVarType(PegObject node, MetaType premise) {
		return new VarType(node, premise);
	}
	
	public final static void setGenerics(GenericType t) {
		issueType(t.baseName+"<>", t);
		t.genericId = t.typeId;
	}

	public final static MetaType newGenericType(String baseName, UniArray<MetaType> typeList) {
		GenericType gt = null;
		MetaType t = _TypeNameMap.get(baseName+"<>", null);
		if(t instanceof GenericType) {
			gt = (GenericType)t;
		}
		else {
			gt = new GenericType(baseName, 0);
			setGenerics(gt);
		}
		return newGenericType(gt, typeList.ArrayValues, typeList.size());
	}

	public final static MetaType newGenericType(GenericType baseType, MetaType[] params) {
		return newGenericType(baseType, params, params.length);
	}
	
	public final static MetaType newGenericType(GenericType baseType, MetaType[] t, int size) {
		if(checkVarType(t, size)) {
			return baseType.newCloneType(compactArray(t, size));
		}
		else {
			UniStringBuilder sb = new UniStringBuilder();
			sb.append(baseType.baseName);
			mangleTypes(sb, t, size);
			String key = sb.toString();
			MetaType gt = _TypeNameMap.get(key, null);
			if(gt == null) {
				gt = baseType.newCloneType(compactArray(t, size));
				issueType(key, gt);
			}
			return gt;
		}
	}
	
	private static MetaType[] compactArray(MetaType[] t, int size) {
		if(t.length == size) {
			return t;
		}
		MetaType[] newt = new MetaType[size];
		System.arraycopy(t, 0, newt, 0, size);
		return newt;
	}

	public final static boolean checkVarType(MetaType[] t, int size) {
		for(int i = 0; i < size; i++) {
			if(t[i].hasVarType()) {
				return true;
			}
		}
		return false;
	}

	private final static void mangleTypes(UniStringBuilder sb, MetaType[] t, int size) {
		for(int i = 0; i < size; i++) {
			sb.append("+");
			sb.appendInt(t[i].typeId);
		}
	}
	
	public final static FuncType newFuncType(UniArray<MetaType> typeList) {
		MetaType funcType = newGenericType(FuncType.FuncBaseName, typeList);
		if(funcType instanceof FuncType) {
			return (FuncType)funcType;
		}
		return null;
	}

//	public final static FuncType _LookupFuncType2(MetaType P1, MetaType P2, MetaType R) {
//		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[3]);
//		TypeList.add(P1);
//		TypeList.add(P2);
//		TypeList.add(R);
//		return newFuncType(TypeList);
//	}
//
//	public final static FuncType _LookupFuncType2(MetaType P1, MetaType R) {
//		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[2]);
//		TypeList.add(P1);
//		TypeList.add(R);
//		return newFuncType(TypeList);
//	}
//
//	public final static FuncType newFuncType(MetaType R) {
//		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[2]);
//		TypeList.add(R);
//		return newFuncType(TypeList);
//	}
//
//
//	public final static FuncType _LookupFuncType2(MetaType P1, MetaType P2, MetaType P3, MetaType R) {
//		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[3]);
//		TypeList.add(P1);
//		TypeList.add(P2);
//		TypeList.add(P3);
//		TypeList.add(R);
//		return newFuncType(TypeList);
//	}

	// =======================================================================
	
	public static MetaType newVoidType(String name, Object typeInfo) {
		return new VoidType(name, typeInfo);
	}

	public static MetaType newAnyType(String name, Object typeInfo) {
		return new AnyType(name, typeInfo);
	}

	public static MetaType newGreekType(String name, int greekId, Object typeInfo) {
		return new GreekType(name, greekId, typeInfo);
	}

	public final static String keyTypeRel(String head, MetaType fromType, MetaType toType) {
		return head + "+" + fromType.typeId + "+" + toType.typeId;
	}
	
	public final static MetaType newTransType(String key, MetaType sourceType, MetaType targetType, Functor f) {
		MetaType t = _TypeNameMap.get(key, null);
		if(t == null) {
			t = new TransType(sourceType, targetType, f);
			_TypeNameMap.put(key, t);
		}
		return t;
	}
}

class ValueType extends MetaType {
	protected String name;
	ValueType(String name, Object typeInfo) {
		super(typeInfo);
		this.name = name;
	}

	@Override
	public final boolean hasVarType() {
		return false;
	}

	@Override
	public final MetaType getRealType() {
		return this;
	}
	
	@Override
	public MetaType getRealType(MetaType[] greekContext) {
		return this;
	}

	@Override
	public final boolean hasGreekType() {
		return false;
	}

	@Override
	public void stringfy(UniStringBuilder sb) {
		sb.append(this.name);
	}

	public boolean is(MetaType valueType, MetaType[] greekContext) {
		if(valueType == this) {
			return true;
		}
		return false;
	}
}

class UntypedType extends ValueType {
	UntypedType() {
		super("untyped", null);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
	
}

class VoidType extends ValueType {
	VoidType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
}

class AnyType extends ValueType {
	AnyType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
}


class VarType extends MetaType {
	public PegObject node;
	private MetaType premiseType;
	private MetaType realType;

	public VarType(PegObject node, MetaType premise) {
		super(null);
		this.node     = node;
		this.premiseType = premise;
		this.realType = null;
		node.typed = this;
	}
	
	public int size() {
		if(this.realType != null) {
			return this.realType.size();
		}
		return 0;
	}

	public MetaType getParamType(int index) {
		if(this.realType != null) {
			return this.realType.getParamType(index);
		}
		return this;
	}

	public int getFuncParamSize() {
		if(this.realType != null) {
			return this.realType.getFuncParamSize();
		}
		return 0;
	}

	public MetaType getReturnType() {
		if(this.realType != null) {
			return this.realType.getReturnType();
		}
		return this;
	}

	@Override
	public MetaType getRealType() {
		if(this.realType != null) {
			this.realType = this.realType.getRealType();
			return this.realType;
		}
		return this;
	}

	@Override
	public MetaType getRealType(MetaType[] greekContext) {
		if(this.realType != null) {
			this.realType = this.realType.getRealType(greekContext);
			return this.realType;
		}
		return this;
	}
	
	@Override
	public boolean hasVarType() {
		return true;
	}
	
	@Override
	public boolean hasGreekType() {
		MetaType realType = this.getRealType();
		return realType.hasGreekType();
	}

	@Override
	public void stringfy(UniStringBuilder sb) {
		if(this.realType != null) {
			if(Main.EnableVerbose) {
				sb.append("?");
			}
			this.realType.stringfy(sb);
			return;
		}
		sb.append("?");
	}

	public boolean is(MetaType valueType, MetaType[] greekContext) {
		if(this.realType == null) {
			valueType = valueType.getRealType();
			this.realType = valueType;
			return true;
		}
		return this.realType.is(valueType, greekContext);
	}
}

class TransType extends MetaType {
	public MetaType sourceType;
	public MetaType targetType;
	public Functor  functor;
	public TransType(MetaType sourceType, MetaType targetType, Functor functor) {
		super(functor);
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.functor = functor;
	}
	@Override
	public boolean hasVarType() {
		return false;
	}
	@Override
	public MetaType getRealType() {
		return targetType.getRealType();
	}
	@Override
	public boolean hasGreekType() {
		return this.targetType.hasGreekType();
	}
	@Override
	public MetaType getRealType(MetaType[] greekContext) {
		return this.targetType.getRealType(greekContext);
	}
	@Override
	public void stringfy(UniStringBuilder sb) {
		this.targetType.stringfy(sb);
		sb.append("(<-");
		this.sourceType.stringfy(sb);
		sb.append(")");
	}
	@Override
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return this.targetType.is(valueType, greekContext);
	}

	public void build(PegObject node, BunDriver driver) {
		PegObject o = new PegObject(this.functor.name);
		o.append(node);
		o.matched = this.functor;
		o.typed = this.targetType;
		node.typed = this.sourceType;
		driver.pushNode(o);
		//this.functor.build(o, driver);
		node.typed = this;
	}
}

/* bun */

class GreekType extends MetaType {
	public final int GreekId;
	public final String name;
	GreekType(String name, int GreekId, Object typeInfo) {
		super(typeInfo);
		this.name = name;
		this.GreekId = GreekId;
	}

	@Override
	public void stringfy(UniStringBuilder sb) {
		sb.append(name);
	}

	@Override
	public boolean hasVarType() {
		return false;
	}

	@Override
	public MetaType getRealType() {
		return this;
	}
	
	@Override
	public MetaType getRealType(MetaType[] greekContext) {
		return greekContext[this.GreekId];
	}

	@Override
	public boolean hasGreekType() {
		return true;
	}

	public final static MetaType[] _NewGreekContext(MetaType[] GreekTypes) {
		if(GreekTypes == null) {
			return Main._NewTypeArray(Main._GreekNames.length);
		}
		else {
			int i = 0;
			while(i < GreekTypes.length) {
				GreekTypes[i] = null;
				i = i + 1;
			}
			return GreekTypes;
		}
	}

	public boolean is(MetaType valueType, MetaType[] greekContext) {
		if(greekContext[this.GreekId] == null) {
			greekContext[this.GreekId] = valueType;
			return true;
		}
		return greekContext[this.GreekId].is(valueType, greekContext);
	}
}

// Mutable<T>, Assignable<T> = T
// T|null


class GenericType extends MetaType {
	private final static MetaType[] nullParams = new MetaType[0];
	protected int    genericId;
	protected String baseName;
	protected MetaType[] typeParams;

	public GenericType(String baseName, int genericId) {
		super(true);
		this.baseName = baseName;
		this.genericId = genericId;
		typeParams = nullParams;
	}
	
	public GenericType newCloneType(MetaType[] typeParams) {
		GenericType gt = new GenericType(this.baseName, this.genericId);
		gt.typeParams = typeParams;
		return gt;
	}

	@Override
	public boolean hasVarType() {
		for(int i = 0; i < typeParams.length; i++) {
			if(typeParams[i].hasVarType()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MetaType getRealType() {
		if(this.typeId == -1) {
			boolean noVarType = true;
			for(int i = 0; i < typeParams.length; i++) {
				typeParams[i] = typeParams[i].getRealType();
				if(typeParams[i].hasVarType()) {
					noVarType = false;
				}
			}
			if(noVarType) {
				return MetaType.newGenericType(this, this.typeParams);
			}
		}
		return this;
	}

	@Override
	public MetaType getRealType(MetaType[] greekContext) {
		if(this.hasGreekType()) {
			System.out.println("TODO");
			return null;
		}
		return this;
	}

	@Override
	public boolean hasGreekType() {
		for(int i = 0; i < typeParams.length; i++) {
			if(typeParams[i].hasGreekType()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void stringfy(UniStringBuilder sb) {
		sb.append(this.baseName);
		sb.append("<");
		for(int i = 0; i < typeParams.length; i++) {
			if(i > 0) {
				sb.append(",");
			}
			typeParams[i].stringfy(sb);
		}
		sb.append(">");
	}

	@Override
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		if(valueType == this) {
			return true;
		}
		if(valueType instanceof GenericType) {
			GenericType t = (GenericType)valueType;
			if(t.genericId == this.genericId && t.typeParams.length == this.typeParams.length) {
				for(int i = 0; i < this.typeParams.length; i++) {
					if(!this.typeParams[i].is(t.typeParams[i], greekContext)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
}

class FuncType extends GenericType {
	public final static String FuncBaseName    = "Func";

	public FuncType(int genericId) {
		super(FuncBaseName, genericId);
	}

	@Override
	public GenericType newCloneType(MetaType[] typeParams) {
		FuncType t = new FuncType(this.genericId);
		t.typeParams = typeParams;
		return t;
	}

//	public final MetaType GetRecvType() {
//		if(this.typeParams.length == 1) {
//			return MetaType.VoidType;
//		}
//		return this.typeParams[0];
//	}
//
//	public final MetaType getFirstType() {
//		if(this.typeParams.length < 3) {
//			return MetaType.VoidType;
//		}
//		return this.typeParams[1];
//	}

	public final int getFuncParamSize() {
		return this.typeParams.length - 1;
	}

	public final MetaType getFuncParamType(int index) {
		return this.typeParams[index];
	}

	public final MetaType getReturnType() {
		return this.typeParams[this.typeParams.length - 1];
	}


//
//	public final boolean AcceptAsFieldFunc(FuncType FuncType) {
//		if(FuncType.GetFuncParamSize() == this.GetFuncParamSize() && FuncType.GetReturnType().Equals(this.GetReturnType())) {
//			int i = 1;
//			while(i < FuncType.GetFuncParamSize()) {
//				if(!FuncType.GetFuncParamType(i).Equals(this.GetFuncParamType(i))) {
//					return false;
//				}
//				i = i + 1;
//			}
//		}
//		return true;
//	}
}



//public class ClassField {
//	public final int        FieldFlag = 0;
//	public final ClassType ClassType;
//	public final MetaType	     FieldType;
//	public final String	 FieldName;
//	public final int        FieldNativeIndex = 0;
//	public final SourceToken     SourceToken;
//
//	public ClassField(ClassType ClassType, String FieldName, MetaType FieldType, SourceToken sourceToken2) {
//		this.ClassType = ClassType;
//		this.FieldType = FieldType;
//		this.FieldName = FieldName;
//		this.SourceToken = sourceToken2;
//	}
//
//
//}

//public class ClassType extends MetaType {
//	public final static ClassType _ObjectType = new ClassType("Object");
//
//	UniArray<ClassField> FieldList = null;
//
//	private ClassType(String ShortName) {
//		super(MetaType.OpenTypeFlag|MetaType.UniqueTypeFlag, ShortName, MetaType.VarType);
//		this.typeFlag = Main._UnsetFlag(this.typeFlag, MetaType.OpenTypeFlag);
//	}
//
//	public ClassType(String ShortName, MetaType RefType) {
//		super(MetaType.OpenTypeFlag|MetaType.UniqueTypeFlag, ShortName, RefType);
//		if(RefType instanceof ClassType) {
//			this.EnforceSuperClass((ClassType)RefType);
//		}
//	}
//
//	public final void EnforceSuperClass(ClassType SuperClass) {
//		this.refType = SuperClass;
//		if(SuperClass.FieldList != null) {
//			this.FieldList = new UniArray<ClassField>(new ClassField[10]);
//			int i = 0;
//			while(i < SuperClass.FieldList.size()) {
//				ClassField Field = SuperClass.FieldList.ArrayValues[i];
//				this.FieldList.add(Field);
//				i = i + 1;
//			}
//		}
//	}
//
//	public final int GetFieldSize() {
//		if(this.FieldList != null) {
//			return this.FieldList.size();
//		}
//		return 0;
//	}
//
//	public final ClassField GetFieldAt(int Index) {
//		return this.FieldList.ArrayValues[Index];
//	}
//
//	public boolean HasField(String FieldName) {
//		if(this.FieldList != null) {
//			int i = 0;
//			while(i < this.FieldList.size()) {
//				if(FieldName.equals(this.FieldList.ArrayValues[i].FieldName)) {
//					return true;
//				}
//				i = i + 1;
//			}
//		}
//		return false;
//	}
//
//	public MetaType GetFieldType(String FieldName, MetaType DefaultType) {
//		if(this.FieldList != null) {
//			int i = 0;
//			//			System.out.println("FieldSize = " + this.FieldList.size() + " by " + FieldName);
//			while(i < this.FieldList.size()) {
//				ClassField Field = this.FieldList.ArrayValues[i];
//				//				System.out.println("Looking FieldName = " + Field.FieldName + ", " + Field.FieldType);
//				if(FieldName.equals(Field.FieldName)) {
//					return Field.FieldType;
//				}
//				i = i + 1;
//			}
//		}
//		return DefaultType;
//	}
//
//	public ClassField AppendField(MetaType FieldType, String FieldName, SourceToken sourceToken) {
//		assert(!FieldType.IsVarType());
//		if(this.FieldList == null) {
//			this.FieldList = new UniArray<ClassField>(new ClassField[4]);
//		}
//		ClassField ClassField = new ClassField(this, FieldName, FieldType, sourceToken);
//		//		System.out.println("Append FieldName = " + ClassField.FieldName + ", " + ClassField.FieldType);
//		assert(ClassField.FieldType != null);
//		this.FieldList.add(ClassField);
//		return ClassField;
//	}
//
//	//	public ZNode CheckAllFields(ZGamma Gamma) {
//	//		// TODO Auto-generated method stub
//	//
//	//		return null;  // if no error
//	//	}
//
//
//}

