package org.libbun;

public class MatchFunction {
	public void invoke(Functor functor, PegObject node, boolean hasNextChoice) {
		node.matched = functor;
	}
}
