package org.libbun;

public abstract class PegDriver {

	public abstract void initTable(Namespace gamma);

	public abstract void startTransaction(String fileName);
	public abstract void endTransaction();
	
	public void pushNode(PegObject node) {
		node.build(this);
	}
	
	public void pushUnknownNode(PegObject node) {
		System.err.println("undefined functor node: " + node.name + "\n" + node + "\n");
	}

	public void pushSubNodes(String openCode, String interCode, PegObject node, String closeCode) {
		this.pushCode(openCode);
		for(int i = 0; i < node.size(); i++) {
			if(i > 0) {
				this.pushCode(interCode);
			}
			pushNode(node.get(i));
		}
		this.pushCode(closeCode);
	}

	// Functor
	public abstract void pushNull(PegObject node);
	public abstract void pushTrue(PegObject node);
	public abstract void pushFalse(PegObject node);
	public abstract void pushInteger(PegObject node, long num);
	public abstract void pushFloat(PegObject node, double num);
	public abstract void pushCharacter(PegObject node, char ch);
	public abstract void pushString(PegObject node, String text);
	public abstract void pushRawLiteral(PegObject node, String text, MetaType type);
	public abstract void pushGlobalName(PegObject node, String name);
	public abstract void pushLocalName(PegObject node, String name);

	// Template Engine
	public abstract void pushNewLine();
	public abstract void pushCode(String text);
	public abstract void pushType(MetaType type);
	public abstract void pushCommand(String name, PegObject node);

	public void report(PegObject node, String errorType, String msg) {
		System.err.println(node.source.formatErrorMessage(errorType, msg));
	}

}
