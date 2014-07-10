package org.libbun.drv;

import org.libbun.BunType;
import org.libbun.Main;
import org.libbun.Namespace;
import org.libbun.PegObject;
import org.libbun.SourceBuilder;

public class PegDumpper extends SourceDriver {

	@Override
	public String getDesc() {
		return "PegDumpper 1.0 by Kimio Kuramitsu (YNU)";
	}

	// Template Engine
	@Override
	public void pushNode(PegObject node) {
		Main._PrintLine(node);
	}
	
	@Override
	public void pushUnknownNode(PegObject node) {
		this.pushNode(node);
	}

	@Override
	public void pushErrorNode(PegObject node) {
		SourceBuilder sb = new SourceBuilder(null);
		this.stringify(node, sb);
		this.pushCode(sb.toString());
	}

	private SourceBuilder stringify(PegObject node, SourceBuilder sb) {
		if(node.isFailure()) {
			sb.append("//syntax error");
		}
		else if(node.size() == 0) {
			sb.appendNewLine(node.tag, ": ", node.getText());
		}
		else {
			sb.appendNewLine(node.tag);
			sb.openIndent(": {");
			for(int i = 0; i < node.size(); i++) {
				if(node.get(i) != null) {
					stringify(node.get(i), sb);
				}
				else {
					sb.appendNewLine("@missing subnode at " + i);
				}
			}
			sb.closeIndent("}");
		}
		return sb;
	}

	@Override
	public void initTable(Namespace gamma) {
	}

	@Override
	public void startTopLevel() {
		System.out.println();
	}

	@Override
	public void endTopLevel() {
	}

	@Override
	public void pushName(PegObject node, String name) {
	}

	@Override
	public boolean hasCommand(String cmd) {
		return false;
	}

	@Override
	public void pushApplyNode(String name, PegObject args) {
	}

	@Override
	public void pushType(BunType type) {
	}



}




