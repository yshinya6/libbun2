package org.libbun;

public class PythonDriver extends SourceDriver {

	@Override
	public void initTable(Namespace gamma) {
		gamma.setType(MetaType.newVoidType("void", null));
//		gamma.setType(MetaType.newBooleanType("bool", null));
//		gamma.setType(MetaType.newIntType("int",  0, null));
//		gamma.setType(MetaType.newIntType("long", 0, null));
//		gamma.setType(MetaType.newFloatType("float", 64, null));
//		gamma.setType(MetaType.newStringType("unicode", null));
//		gamma.setType(MetaType.newStringType("str", null));
		gamma.setType(MetaType.newAnyType("any", null));
		gamma.setType(MetaType.newGreekType("alpha", 0, null));
		gamma.setType(MetaType.newGreekType("beta", 0, null));
		
//		gamma.addNullFunctor("#null",   "alpha");
//		gamma.addTrueFunctor("#true",   "bool");
////		gamma.addFalseFunctor("#false", "bool");
//		gamma.addIntegerFunctor("#integer", "int");
//		gamma.addFloatFunctor("#float", "float");
//		gamma.addCharacterFunctor("#character", "unicode", true);
//		gamma.addStringFunctor("#string", "unicode", true);
		
		gamma.loadBunModel("lib/driver/python/common.bun", this);
	}

	@Override
	public void pushGlobalName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushLocalName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushUndefinedName(PegObject node, String name) {
		this.pushCode(name);
	}

	@Override
	public void pushType(MetaType type) {
		this.pushCode(type.getName());
	}
}
