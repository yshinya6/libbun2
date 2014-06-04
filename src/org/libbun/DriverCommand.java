package org.libbun;

public abstract class DriverCommand {
	public abstract void invoke(PegDriver driver, PegObject node, String[] param);
}
