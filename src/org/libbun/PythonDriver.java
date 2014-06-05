package org.libbun;

public class PythonDriver extends SourceDriver {

	@Override
	public void initTable(Namespace gamma) {
		gamma.setType(MetaType.newVoidType("void", null));
		gamma.setType(MetaType.newAnyType("any", null));
		gamma.setType(MetaType.newGreekType("alpha", 0, null));
		gamma.setType(MetaType.newGreekType("beta", 0, null));
		KonohaTypeChecker.initDriver(this);
		gamma.loadBunModel("lib/driver/python/konoha.bun", this);
		gamma.loadBunModel("lib/driver/python/konoha_python.bun", this);
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

	@Override
	public void pushApplyNode(PegObject node, String name) {
		// TODO Auto-generated method stub
		
	}
}
