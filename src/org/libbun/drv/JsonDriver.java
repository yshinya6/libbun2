package org.libbun.drv;

import org.libbun.BunType;
import org.libbun.Functor;
import org.libbun.Main;
import org.libbun.Namespace;
import org.libbun.UMap;
import org.libbun.peg4d.PegObject;

public class JsonDriver extends SourceDriver {

	@Override
	public String getDesc() {
		return "JSON generator 1.0 by Kimio Kuramitsu (YNU)";
	}

	@Override
	public void initTable(Namespace gamma) {
		gamma.loadBunModel("lib/driver/json/common.bun", this);
		
	}

	@Override
	public void pushType(BunType type) {
		this.pushCode(type.getName());
	}

	public void pushApplyNode(String name, PegObject args) {
		this.pushCode(name);
		this.pushNodeList("(", args, ", ", ")");
	}

	public void generateMain() {  // Override
		if(this.hasMainFunction()) {
			this.pushCode("\nif __name__ == '__main__':\n   main()\n");
		}
	}

}
