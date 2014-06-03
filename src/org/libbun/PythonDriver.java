package org.libbun;

public class PythonDriver extends SourceDriver {

	@Override
	public void initTable(Namespace gamma) {
		gamma.setType(MetaType.newVoidType("void", null));
		gamma.setType(MetaType.newBooleanType("bool", null));
		gamma.setType(MetaType.newIntType("int",  0, null));
		gamma.setType(MetaType.newIntType("long", 0, null));
		gamma.setType(MetaType.newFloatType("float", 64, null));
		gamma.setType(MetaType.newStringType("unicode", null));
		gamma.setType(MetaType.newStringType("str", null));
		gamma.setType(MetaType.newAnyType("any", null));
		gamma.setType(MetaType.newGreekType("alpha", 0, null));
		gamma.setType(MetaType.newGreekType("beta", 0, null));
		
		gamma.addNullFunctor("#null",   "alpha");
		gamma.addTrueFunctor("#true",   "bool");
		gamma.addFalseFunctor("#false", "bool");
		gamma.addIntegerFunctor("#integer", "int");
		gamma.addFloatFunctor("#float", "float");
		gamma.addCharacterFunctor("#character", "unicode", true);
		gamma.addStringFunctor("#string", "unicode", true);
		
		gamma.loadBunModel("lib/driver/python/common.bun", this);
	}

	@Override
	public void pushNull(PegObject node) {
		this.pushCode("None");
	}

	@Override
	public void pushTrue(PegObject node) {
		this.pushCode("True");
	}

	@Override
	public void pushFalse(PegObject node) {
		this.pushCode("False");
	}

	@Override
	public void pushInteger(PegObject node, long num) {
		this.pushCode("" + num);
	}

	@Override
	public void pushFloat(PegObject node, double num) {
		this.pushCode("" + num);
	}

	@Override
	public void pushCharacter(PegObject node, char ch) {
		this.pushCode(UniCharset._QuoteString("u'", ""+ch, "'"));
	}

	@Override
	public void pushString(PegObject node, String text) {
		this.pushCode(UniCharset._QuoteString("u'", text, "'"));
	}

	@Override
	public void pushRawLiteral(PegObject node, String text, MetaType type) {
		this.pushCode(text);
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
