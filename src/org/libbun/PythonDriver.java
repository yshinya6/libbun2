package org.libbun;

public class PythonDriver extends PegDriver {

	private String fileName;
	private UniStringBuilder builder;
	
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
		
		gamma.loadBunModel();
		gamma.addNullFunctor("#null",   "alpha");
		gamma.addTrueFunctor("#true",   "bool");
		gamma.addFalseFunctor("#false", "bool");
		gamma.addIntegerFunctor("#integer", "int");
		gamma.addFloatFunctor("#float", "float");
		gamma.addCharacterFunctor("#character", "unicode", true);
		gamma.addStringFunctor("#string", "unicode", true);
		gamma.addFunctor(new PyBlockFunctor("#block"));
		gamma.load("lib/driver/python/common.bun", this);
	}

	@Override
	public void startTransaction(String fileName) {
		this.fileName = fileName;
		this.builder = new UniStringBuilder();
	}

	@Override
	public void endTransaction() {
		this.builder.show();
		this.builder = null;
	}

	@Override
	public void pushNull(PegObject node) {
		this.builder.append("Null");
	}

	@Override
	public void pushTrue(PegObject node) {
		this.builder.append("True");
	}

	@Override
	public void pushFalse(PegObject node) {
		this.builder.append("False");
	}

	@Override
	public void pushInteger(PegObject node, long num) {
		this.builder.append("" + num);
	}

	@Override
	public void pushFloat(PegObject node, double num) {
		this.builder.append("" + num);
	}

	@Override
	public void pushCharacter(PegObject node, char ch) {
		this.builder.append(UniCharset._QuoteString("u'", ""+ch, "'"));
	}

	@Override
	public void pushString(PegObject node, String text) {
		this.builder.append(UniCharset._QuoteString("u'", text, "'"));
	}

	@Override
	public void pushRawLiteral(PegObject node, String text, MetaType type) {
		this.builder.append(text);
	}

	@Override
	public void pushGlobalName(PegObject node, String name) {
		this.builder.append(name);
	}

	@Override
	public void pushLocalName(PegObject node, String name) {
		this.builder.append(name);
	}

	@Override
	public void pushUndefinedName(PegObject node, String name) {
		this.builder.append(name);
	}

	@Override
	public void pushNewLine() {
		this.builder.appendNewLine();
	}

	@Override
	public void pushCode(String text) {
		this.builder.append(text);
	}

	@Override
	public void pushType(MetaType type) {
		this.builder.append(type.getName());
	}

	@Override
	public void pushCommand(String name, PegObject node) {
		// TODO Auto-generated method stub
		System.out.println("debug");
		
	}
	
	@Override
	public void openIndent() {
		builder.openIndent();
	}

	@Override
	public void closeIndent() {
		builder.closeIndent();
	}

	
	class PyBlockFunctor extends Functor {
		public PyBlockFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.openIndent();// 
			for(int i = 0; i < node.size(); i++) {
				driver.pushNewLine();
				driver.pushNode(node.get(i));
				node.get(i);
			}
			driver.closeIndent();
		}
	}




}
