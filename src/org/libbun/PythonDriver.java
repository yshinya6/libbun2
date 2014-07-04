package org.libbun;

public class PythonDriver extends SourceDriver {

	@Override
	public void initTable(Namespace gamma) {
		gamma.loadBunModel("lib/driver/python/common.bun", this);
		gamma.loadBunModel("lib/driver/python/pytypes.bun", this);
	}

	@Override
	public void pushType(BunType type) {
		this.pushCode(type.getName());
	}

	public void pushApplyNode(String name, PegObject args) {
		this.pushCode(name);
		this.pushNodeList("(", args, ", ", ")");
	}

}
