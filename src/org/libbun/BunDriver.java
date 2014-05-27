package org.libbun;

public abstract class BunDriver {

	public abstract void initTable(Namespace gamma);

	public abstract void pushCode(String text);
	public abstract void pushNode(PegObject node);
	public abstract void pushTypeOf(PegObject node);
	public abstract void pushCommand(String name, PegObject node);

	public abstract void pushNewLine();
	public abstract void pushName(String name, int nameIndex);
	public abstract void pushLiteral(PegObject node, String number, MetaType type);

	public abstract void pushErrorMessage(SourceToken source, String msg);

	public void report(PegObject node, String errorType, String msg) {
		System.err.println(node.source.formatErrorMessage(errorType, msg));
	}

}
