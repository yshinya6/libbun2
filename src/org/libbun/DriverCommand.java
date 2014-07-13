package org.libbun;

import org.libbun.peg4d.PegObject;

public abstract class DriverCommand {
	public abstract void invoke(BunDriver driver, PegObject node, String[] param);
}
