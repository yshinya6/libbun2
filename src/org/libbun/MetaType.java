package org.libbun;

public abstract class MetaType  {
	protected int         typeId    = -1;
	protected Object      typeInfo;  // used in Class<?> in JVM (for example)

	MetaType(Object typeInfo) {
		this.typeInfo = typeInfo;
		if(!this.hasVarType()) {
			this.typeId = MetaType._NewTypeId(this);
		}
	}

	MetaType(boolean pooled) {
		if(pooled) {
			this.typeId = MetaType._NewTypeId(this);
		}
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

	public abstract boolean hasVarType();
	public abstract MetaType getRealType();
	public abstract boolean hasGreekType();
	public abstract MetaType getRealType(MetaType[] greekContext);

	public abstract void stringfy(UniStringBuilder sb);
	public abstract boolean is(MetaType valueType, MetaType[] greekContext);
	public abstract PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext);
	
//	public int getGenericSize() {
//		return 0;
//	}
//
//	public MetaType getGenericTypeAt(int index) {
//		return MetaType.VoidType;
//	}

	//
	//	public static BType _LookupMutableType(LibBunTypeChecker Gamma, BType Type, @Nullable BToken MutableToken) {
	//		if(Gamma.IsSupportMutable) {
	//			String MangleName = "M:"+Type.TypeId;
	//			BType MutableType = BTypePool._ClassNameMap.GetOrNull(MangleName);
	//			if(MutableType == null) {
	//				MutableType = new BMutableType(Type);
	//				BTypePool._ClassNameMap.put(MangleName, MutableType);
	//			}
	//			return MutableType;
	//		}
	//		return Type;
	//	}
	//
	//	public static BType _LookupNullableType(LibBunTypeChecker Gamma, BType Type, @Nullable BToken MaybeToken) {
	//		if(Gamma.IsSupportNullable) {
	//			String MangleName = "N:"+Type.TypeId;
	//			BType NullableType = BTypePool._ClassNameMap.GetOrNull(MangleName);
	//			if(NullableType == null) {
	//				NullableType = new BMutableType(Type);
	//				BTypePool._ClassNameMap.put(MangleName, NullableType);
	//			}
	//			return NullableType;
	//		}
	//		return Type;
	//	}

//
//	public final static MetaType _GetGenericType1(MetaType BaseType, MetaType ParamType) {
//		String MangleName = _MangleType2(BaseType, ParamType);
//		MetaType GenericType = _TypeNameMap.GetOrNull(MangleName);
//		if(GenericType == null) {
//			GenericType = new GenericType(MetaType.UniqueTypeFlag, BaseType, ParamType);
//			_TypeNameMap.put(MangleName, GenericType);
//		}
//		return GenericType;
//	}
//
//	final static MetaType[] _UniqueTypes(UniArray<MetaType> TypeList) {
//		String MangleName = "[]" + _MangleTypes(TypeList);
//		MetaType[] Types = _UniqueTypeSetMap.GetOrNull(MangleName);
//		if(Types == null) {
//			Types = TypeList.compactArray();
//			_UniqueTypeSetMap.put(MangleName, Types);
//		}
//		return Types;
//	}
//
//
//	final static String _MangleType2(MetaType Type1, MetaType Type2) {
//		return ":" + Type1.typeId + ":" + Type2.typeId;
//	}



	
//	
//
//	public MetaType GetRealType() {
//		return this;
//	}
//
//	public MetaType GetSuperType() {
//		return this.refType;
//	}
//
//	public MetaType GetBaseType() {
//		return this;
//	}
//
//
//
//	public final boolean Equals(MetaType Type) {
//		return (this.GetRealType() == Type.GetRealType());
//	}
//
//	public final boolean Accept(MetaType Type) {
//		MetaType ThisType = this.GetRealType();
//		if(ThisType == Type.GetRealType()) {
//			return true;
//		}
//		if(Type.getGenericSize() == 0) {
//			MetaType SuperClass = Type.GetSuperType();
//			while(SuperClass != null) {
//				if(SuperClass == ThisType) {
//					return true;
//				}
//				SuperClass = SuperClass.GetSuperType();
//			}
//		}
//		return false;
//	}
//
//	public boolean IsGreekType() {
//		return false;
//	}
//
//	public MetaType GetGreekRealType(MetaType[] Greek) {
//		return this.GetRealType();
//	}
//
//
//	public final boolean IsVoidType() {
//		return (this.GetRealType() == MetaType.VoidType);
//	}
//
//	public boolean IsVarType() {
//		return (this.GetRealType() == MetaType.VarType);
//	}
//
//	public final boolean IsInferrableType() {
//		return (!this.IsVarType() && !this.IsVoidType());
//	}
//
//	public final boolean IsTypeType() {
//		return (this.GetRealType() == MetaType.TypeType);
//	}
//
//	public final boolean IsBooleanType() {
//		return (this.GetRealType() == MetaType.BooleanType);
//	}
//
//	public final boolean IsIntType() {
//		return (this.GetRealType() == MetaType.IntType);
//	}
//
//	public final boolean IsFloatType() {
//		return (this.GetRealType() == MetaType.FloatType);
//	}
//
//	public final boolean IsNumberType() {
//		return (this.IsIntType() || this.IsFloatType());
//	}
//
//	public final boolean IsStringType() {
//		return (this.GetRealType() == MetaType.StringType);
//	}
//
//	public final boolean IsArrayType() {
//		return (this.GetBaseType() == GenericType._ArrayType);
//	}
//
//	public final boolean IsMapType() {
//		return (this.GetBaseType() == GenericType._MapType);
//	}
//
//	public final boolean IsOpenType() {
//		return Main._IsFlag(this.typeFlag, MetaType.OpenTypeFlag);
//	}
//
//	public final String StringfyClassMember(String Name) {
//		return Name + " of " + this.shortName;
//	}
//
//	public final String GetUniqueName() {
//		return this.getName()+":"+this.typeId;
//		//return LibBunSystem._Stringfy(this.TypeId);
//	}
//
//	//	public final boolean AcceptValue(Object Value) {
//	//		return (Value != null) ? this.Accept(ZSystem.GuessType(Value)) : true;
//	//	}
//
//	public boolean IsFuncType() {
//		return (this.GetRealType() instanceof FuncType);
//	}
//
//	public String StringfySignature(String FuncName) {
//		return FuncName;
//	}
//
//	public void Maybe(MetaType T, SourceToken SourceToken) {
//	}
//
//	public void bunfy(UniStringBuilder builder) {
//		builder.Append(this.getName());
//	}
	
	final static UniArray<MetaType>   _TypeList;
	final static UniMap<MetaType>     _TypeNameMap;

	public static MetaType UntypedType;
	public static MetaType VoidType        = null;
	public static MetaType VarType         = null;
	public static MetaType BooleanType     = null;
//	public static IntType IntType          = null;
	public static MetaType FloatType       = null;
//	public static StringType StringType    = null;
//	public static MetaType TypeType        = null;
		
	static {
		_TypeList = new UniArray<MetaType>(new MetaType[128]);
		_TypeNameMap = new UniMap<MetaType>();
		UntypedType = new UntypedType();
		setGenerics("Func:*", new FuncType("Func<", ",", ">"));
	}
//
//	public final static MetaType _AlphaType  = new GreekType(0);
//
//	public final static GenericType _ArrayType = new GenericType("Array");
//	public final static GenericType _MapType = new GenericType("Map");


	public final static int _NewTypeId(MetaType T) {
		int TypeId = _TypeList.size();
		_TypeList.add(T);
		return TypeId;
	}

	public final static MetaType TypeOf(int TypeId) {
		if(TypeId < _TypeList.size()) {
			return _TypeList.ArrayValues[TypeId];
		}
		return MetaType.UntypedType;
	}

	public final static void setGenerics(String key, GenericType t) {
		_TypeNameMap.put(key, t);
	}

	public final static MetaType newGenericType(String baseName, UniArray<MetaType> typeList) {
		UniStringBuilder sb = new UniStringBuilder();
		sb.append(baseName);
		if(!mangleTypes(sb, typeList)) {
			return cloneGenericType(baseName, typeList, false);
		}
		String key = sb.toString();
		MetaType pType = _TypeNameMap.get(key, null);
		if(pType == null) {
			pType = cloneGenericType(baseName, typeList, true);
			_TypeNameMap.put(key, pType);
		}
		//System.out.println("pType: " + pType);
		return pType;
	}

	private final static boolean mangleTypes(UniStringBuilder sb, UniArray<MetaType> TypeList) {
		for(int i = 0; i < TypeList.size(); i++) {
			MetaType Type = TypeList.ArrayValues[i];
			if(Type.hasVarType()) {
				return false;
			}
			sb.append("+" + Type.typeId);
		}
		return true;
	}

	private static GenericType cloneGenericType(String name, UniArray<MetaType> TypeList, boolean uniquefy) {
		String key = name + ":" + TypeList.size();
		MetaType t = _TypeNameMap.get(key, null);
		if(t == null) {
			key = name + ":*";
			t = _TypeNameMap.get(key, null);
		}
		if(t instanceof GenericType) {
			GenericType skeltonType = (GenericType)t;
			if(uniquefy) {
				return skeltonType.newCloneType(uniqueTypes(TypeList));
			}
			else {
				return skeltonType.newCloneType(TypeList.compactArray());
			}
		}
		Main._PrintDebug("undefined generics: " + name + ":" + TypeList.size());
		return null;
	}
	
	private final static MetaType[] uniqueTypes(UniArray<MetaType> TypeList) {
		UniStringBuilder sb = new UniStringBuilder();
		sb.append("Func");
		mangleTypes(sb, TypeList);
		String key = sb.toString();
		MetaType type = _TypeNameMap.get(key, null);
		if(type instanceof FuncType) {
			return ((FuncType)type).typeParams;
		}
		GenericType funcType = cloneGenericType("Func", TypeList, false);
		_TypeNameMap.put(key, funcType);
		return funcType.typeParams;
	}

	public final static FuncType _LookupFuncType2(MetaType P1, MetaType P2, MetaType R) {
		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[3]);
		TypeList.add(P1);
		TypeList.add(P2);
		TypeList.add(R);
		return newFuncType(TypeList);
	}

	public final static FuncType _LookupFuncType2(MetaType P1, MetaType R) {
		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[2]);
		TypeList.add(P1);
		TypeList.add(R);
		return newFuncType(TypeList);
	}

	public final static FuncType _LookupFuncType2(MetaType R) {
		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[2]);
		TypeList.add(R);
		return newFuncType(TypeList);
	}

	public final static FuncType newFuncType(UniArray<MetaType> TypeList) {
		MetaType funcType = newGenericType("Func", TypeList);
		if(funcType instanceof FuncType) {
			return (FuncType)funcType;
		}
		return null;
	}

	public final static FuncType _LookupFuncType2(MetaType P1, MetaType P2, MetaType P3, MetaType R) {
		UniArray<MetaType> TypeList = new UniArray<MetaType>(new MetaType[3]);
		TypeList.add(P1);
		TypeList.add(P2);
		TypeList.add(P3);
		TypeList.add(R);
		return newFuncType(TypeList);
	}

	public static boolean isSubType(MetaType nodeType, MetaType metaType) {
		return false;
	}

	// =======================================================================
	
	public static MetaType newVoidType(String name, Object typeInfo) {
		return new VoidType(name, typeInfo);
	}

	public static MetaType newBooleanType(String name, Object typeInfo) {
		return new BooleanType(name, typeInfo);
	}

	public static MetaType newIntType(String name, int size, Object typeInfo) {
		return new IntType(name, size, typeInfo);
	}

	public static MetaType newFloatType(String name, int size, Object typeInfo) {
		return new FloatType(name, size, typeInfo);
	}

	public static MetaType newStringType(String name, Object typeInfo) {
		return new StringType(name, typeInfo);
	}

	public static MetaType newAnyType(String name, Object typeInfo) {
		return new AnyType(name, typeInfo);
	}

	public static MetaType newGreekType(String name, int greekId, Object typeInfo) {
		return new GreekType(name, greekId, typeInfo);
	}


}

class VarType extends MetaType {
	private int varId = 0;
	private MetaType realType;

	public VarType(int varId) {
		super(null);
		this.varId = varId;
		this.realType = null;
	}

	@Override
	public MetaType getRealType() {
		if(this.realType != null) {
			return this.realType.getRealType();
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
		return this.hasGreekType();
	}

	@Override
	public void stringfy(UniStringBuilder sb) {
		sb.append("var");
	}

	public boolean is(MetaType valueType, MetaType[] greekContext) {
		if(this.realType == null) {
			this.realType = valueType;
			return true;
		}
		return this.realType.is(valueType, greekContext);
	}

	public final PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		if(this.realType == null) {
			MetaType nodeType = node.getType(MetaType.UntypedType);
			this.realType = nodeType;
			return node;
		}
		return this.realType.match(node, allowSubType, greekContext);
	}

}

class ValueType extends MetaType {
	
	protected String name;
	public ValueType(String name, Object typeInfo) {
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
	
	public PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		MetaType nodeType = node.getType(MetaType.UntypedType);
		if(nodeType == this) {
			return node;
		}
		if(allowSubType && MetaType.isSubType(nodeType, this)) {
			return node;  // FIXME
		}
		return null;
	}

}

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

	public final PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		MetaType nodeType = node.getType(MetaType.UntypedType);
		if(greekContext[this.GreekId] == null) {
			greekContext[this.GreekId] = nodeType;
			return node;
		}
		return greekContext[this.GreekId].match(node, allowSubType, greekContext);
	}
}

abstract class GenericType extends MetaType {
	private final static MetaType[] nullParams = new MetaType[0];
	protected int    genericId;
	protected String prefix;
	protected String suffix;
	protected String comma;
	protected MetaType[] typeParams;

	public GenericType(String prefix, String comma, String suffix) {
		super(true);
		this.genericId = this.typeId;
		this.prefix = prefix;
		this.comma = comma;
		this.suffix = suffix;
		typeParams = nullParams;
	}
	
	public abstract GenericType newCloneType(MetaType[] typeParams);

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
		sb.append(this.prefix);
		for(int i = 0; i < typeParams.length; i++) {
			if(i > 0) {
				sb.append(this.comma);
			}
			typeParams[i].stringfy(sb);
		}
		sb.append(this.suffix);
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

	public final PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		MetaType nodeType = node.getType(MetaType.UntypedType);
		if(this.is(nodeType, greekContext)) {
			return node;
		}
		return null;
	}
}

class ArrayType extends GenericType {
	public ArrayType(String prefix, String suffix) {
		super(prefix, ",", suffix);
	}

	@Override
	public GenericType newCloneType(MetaType[] typeParams) {
		ArrayType t = new ArrayType(this.prefix, this.suffix);
		t.typeParams = typeParams;
		t.genericId = this.genericId;
		return t;
	}

}

class FuncType extends GenericType {

	public FuncType(String prefix, String comma, String suffix) {
		super("Func<", ",", ">");
	}

	@Override
	public GenericType newCloneType(MetaType[] typeParams) {
		FuncType t = new FuncType(this.prefix, this.comma, this.suffix);
		t.typeParams = typeParams;
		t.genericId = this.genericId;
		return t;
	}

//	@Override public final MetaType GetBaseType() {
//		return FuncType._FuncType;
//	}
//
//	@Override public final int getGenericSize() {
//		return this.typeParams.length;
//	}
//
//	@Override public final MetaType getGenericTypeAt(int Index) {
//		return this.typeParams[Index];
//	}
//
//
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
//
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


class UntypedType extends ValueType {
	public UntypedType() {
		super("untyped", null);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
	
	public PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		return node;
	}
}

class VoidType extends ValueType {
	public VoidType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
	
	public PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		return node;
	}
}

class AnyType extends ValueType {
	public AnyType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
	public boolean is(MetaType valueType, MetaType[] greekContext) {
		return true;
	}
	
	public PegObject match(PegObject node, boolean allowSubType, MetaType[] greekContext) {
		return node;
	}
}


class BooleanType extends ValueType {
	public BooleanType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
}

class NumberType extends ValueType {
	public NumberType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
}

class IntType extends NumberType {
	int size;
	public IntType(String name, int size, Object typeInfo) {
		super(name, typeInfo);
	}
}

class FloatType extends NumberType {
	int size;
	public FloatType(String name, int size, Object typeInfo) {
		super(name, typeInfo);
	}
}

class StringType extends ValueType {
	public StringType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
}

class ObjectType extends ValueType {
	public ObjectType(String name, Object typeInfo) {
		super(name, typeInfo);
	}
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

