package org.libbun;

public abstract class DriverCommand {
	public abstract void invoke(BunDriver driver, PegObject node, String[] param);
}
