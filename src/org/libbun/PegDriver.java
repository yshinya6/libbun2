package org.libbun;

public abstract class PegDriver {

	public abstract void initTable(Namespace gamma);

	public abstract void startTransaction(String fileName);
	public abstract void endTransaction();
	
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
	public abstract void pushUndefinedName(PegObject node, String name);
	public abstract void pushType(MetaType type);

	// Template Engine
	public void pushNode(PegObject node) {
		node.build(this);
	}
	
	public void pushUnknownNode(PegObject node) {
		System.err.println("undefined functor node: " + node.name + "\n" + node + "\n");
	}

	protected UniMap<DriverCommand> commandMap = new UniMap<DriverCommand>();
	
	public boolean hasCommand(String cmd) {
		return this.commandMap.hasKey(cmd);
	}

	public void addCommand(String name, DriverCommand c) {
		this.commandMap.put(name, c);
	}

	public void pushCommand(String cmd, PegObject node, String[] params) {
		DriverCommand command = this.commandMap.get(cmd);
		command.invoke(this, node, params);
	}

	public abstract void pushNewLine();
	public abstract void pushCode(String text);
	
	public void report(PegObject node, String errorType, String msg) {
		System.err.println(node.source.formatErrorMessage(errorType, msg));
	}

}

abstract class SourceDriver extends PegDriver {

	private String fileName;
	private UniStringBuilder builder;
	
	protected SourceDriver() {
		this.addCommand("begin",     new OpenIndentCommand());
		this.addCommand("end",       new CloseIndentCommand());
		this.addCommand("typeof",    new TypeofCommand());
		this.addCommand("statement", new StatementCommand());
		this.addCommand("list",      new ListCommand());
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
	public void pushNewLine() {
		this.builder.appendNewLine();
	}

	@Override
	public void pushCode(String text) {
		this.builder.append(text);
	}

	class OpenIndentCommand extends DriverCommand {
		@Override
		public void invoke(PegDriver driver, PegObject node, String[] param) {
			builder.openIndent();
		}
	}

	class CloseIndentCommand extends DriverCommand {
		@Override
		public void invoke(PegDriver driver, PegObject node, String[] param) {
			builder.closeIndent();
		}
	}

	protected void pushStatementEnd(PegObject node) {
		this.pushCode(";");
	}
	
	class TypeofCommand extends DriverCommand {
		@Override
		public void invoke(PegDriver driver, PegObject node, String[] param) {
			driver.pushType(node.getType(MetaType.UntypedType));
		}
	}

	class StatementCommand extends DriverCommand {
		@Override
		public void invoke(PegDriver driver, PegObject node, String[] param) {
			if(node.is("#block")) {
				for(int i = 0; i < node.size(); i++) {
					driver.pushNewLine();
					driver.pushNode(node.get(i));
					((SourceDriver)driver).pushStatementEnd(node.get(i));
				}
			}
			else {
				driver.pushNewLine();
				driver.pushNode(node);
				((SourceDriver)driver).pushStatementEnd(node);
			}
		}
	}

	protected void pushListSeparator() {
		this.pushCode(", ");
	}

	class ListCommand extends DriverCommand {
		@Override
		public void invoke(PegDriver driver, PegObject node, String[] param) {
			for(int i = 0; i < node.size(); i++) {
				if(i > 0) {
					((SourceDriver)driver).pushListSeparator();
				}
				driver.pushNode(node.get(i));
			}
		}
	}

}