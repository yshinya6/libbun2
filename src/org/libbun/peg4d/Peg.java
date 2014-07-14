package org.libbun.peg4d;

import java.util.HashSet;

import org.libbun.Main;
import org.libbun.UCharset;
import org.libbun.UList;
import org.libbun.UMap;
import org.libbun.UStringBuilder;

public abstract class Peg {
	public final static int Debug = 1;
	public final static int TextMatchOnly = 1 << 1;
	
	int       flag     = 0;
	String    ruleName = null;

	ParserSource source = null;
	int       sourcePosition = 0;
	
	protected abstract Peg clone(PegTransformer tr);
	protected abstract void stringfy(UStringBuilder sb, boolean debugMode);
	protected abstract void makeList(PegRuleSet parser, UList<String> list, UMap<String> set);
	protected abstract PegObject simpleMatch(PegObject left, ParserContext context);
	protected abstract void verify(String ruleName, PegRuleSet rules);
	public abstract void accept(PegVisitor visitor);

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}
	
	public final void set(int uflag) {
		this.flag = this.flag | uflag;
	}

	public final void unset(int uflag) {
		this.flag = (this.flag & (~uflag));
	}
	
	
	public int size() {
		return 0;
	}
	public Peg get(int index) {
		return this;  // to avoid NullPointerException
	}

	@Override public String toString() {
		UStringBuilder sb = new UStringBuilder();
		this.stringfy(sb, false);
		if(this.ruleName != null) {
			sb.append(" defined in ");
			sb.append(this.ruleName);
		}
		return sb.toString();
	}
	
//	public boolean hasLeftRecursion() {
//		return this.hasLeftRecursion;
//	}
//	
//	public void setLeftRecursion(boolean lrExistense) {
//		this.hasLeftRecursion = lrExistense;
//	}

	public final PegChoice appendAsChoice(Peg e) {
		if(this instanceof PegChoice) {
			((PegChoice)this).extend(e);
			return ((PegChoice)this);
		}
		else {
			PegChoice choice = new PegChoice();
			choice.add(this);
			choice.extend(e);
			return choice;
		}
	}
	protected PegObject performMatch(PegObject left, ParserContext context) {
		if(this.is(Peg.Debug)) {
			PegObject node2 = this.simpleMatch(left, context);
			String msg = "matched";
			if(node2.isFailure()) {
				msg = "failed";
			}
			String line = context.formatErrorMessage(msg, this.toString());
			System.out.println(line + "\n\tnode #" + left + "# => #" + node2 + "#");
			return node2;
		}
		return this.simpleMatch(left, context);
	}

	public final String toPrintableString(String name, String Setter, String Choice, String SemiColon, boolean debugMode) {
		UStringBuilder sb = new UStringBuilder();
		sb.append(name);
		sb.append(Setter);
		if(this instanceof PegChoice) {
			for(int i = 0; i < this.size(); i++) {
				if(i > 0) {
					sb.append(Choice);
				}
				this.get(i).stringfy(sb, debugMode);
			}
		}
		else {
			this.stringfy(sb, debugMode);
		}
		sb.append(SemiColon);
		return sb.toString();
	}
	
	protected void dump(String msg) {
		if(this.source != null) {
			System.out.println(this.source.formatErrorMessage("*", this.sourcePosition, msg));
		}
		else {
			System.out.println("unknown source: " + msg);
		}
	}
	protected void warning(String msg) {
		if(Main.VerbosePegMode) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}
}

abstract class PegTransformer {
	public abstract Peg transform(Peg e);
}

class PegNoTransformer extends PegTransformer {
	@Override
	public Peg transform(Peg e) {
		return e;
	}
}

abstract class PegAtom extends Peg {
	String symbol;
	public PegAtom (String symbol) {
		super();
		this.symbol = symbol;
	}
	@Override
	protected void stringfy(UStringBuilder sb, boolean debugMode) {
		sb.append(this.symbol);
	}	
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Peg get(int index) {
		return this;  // just avoid NullPointerException
	}
	@Override
	protected void makeList(PegRuleSet parser, UList<String> list, UMap<String> set) {
	}

}

class PegString extends PegAtom {
	public PegString(String symbol) {
		super(symbol);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegString(this.symbol);
		}
		return ne;
	}
	public final static String quoteString(String s) {
		char quote = '\'';
		if(s.indexOf("'") != -1) {
			quote = '"';
		}
		return UCharset._QuoteString(quote, s, quote);
	}
	@Override
	protected void stringfy(UStringBuilder sb, boolean debugMode) {
		sb.append(PegString.quoteString(this.symbol));
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegString(this);
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchString(left, this);
	}
}

class PegAny extends PegAtom {
	public PegAny() {
		super(".");
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegAny();
		}
		return ne;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAny(this);
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchAny(left, this);
	}
}

class PegCharacter extends PegAtom {
	UCharset charset;
	public PegCharacter(String token) {
		super(token);
		this.charset = new UCharset(token);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegCharacter(this.symbol);
		}
		return ne;
	}
	@Override
	protected void stringfy(UStringBuilder sb, boolean debugMode) {
		sb.append("[" + this.symbol, "]");
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegCharacter(this);
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchCharacter(left, this);
	}
}

class PegLabel extends PegAtom {
	public PegLabel(String token) {
		super(token);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegLabel(this.symbol);
		}
		return ne;
	}
	@Override protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchLabel(left, this);
	}
	@Override
	protected void makeList(PegRuleSet parser, UList<String> list, UMap<String> set) {
		if(!set.hasKey(this.symbol)) {
			Peg next = parser.getRule(this.symbol);
			list.add(this.symbol);
			set.put(this.symbol, this.symbol);
			next.makeList(parser, list, set);
		}
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		if(!rules.hasRule(this.symbol)) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			rules.foundError = true;
		}
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegLabel(this);
	}
}

abstract class PegUnary extends Peg {
	Peg innerExpr;
	boolean prefix;
	public PegUnary(Peg e, boolean prefix) {
		super();
		this.innerExpr = e;
		this.prefix = prefix;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}
	protected abstract String getOperator();
	@Override
	protected final void stringfy(UStringBuilder sb, boolean debugMode) {
		if(this.prefix) {
			sb.append(this.getOperator());
		}
		if(this.innerExpr instanceof PegAtom || this.innerExpr instanceof PegNewObject) {
			this.innerExpr.stringfy(sb, debugMode);
		}
		else {
			sb.append("(");
			this.innerExpr.stringfy(sb, debugMode);
			sb.append(")");
		}
		if(!this.prefix) {
			sb.append(this.getOperator());
		}
	}
	@Override
	protected void makeList(PegRuleSet parser, UList<String> list, UMap<String> set) {
		this.innerExpr.makeList(parser, list, set);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		this.innerExpr.verify(ruleName, rules);
	}
}

class PegOptional extends PegUnary {
	public PegOptional(Peg e) {
		super(e, false);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegOptional(this.innerExpr.clone(tr));
		}
		return ne;
	}
	@Override
	protected String getOperator() {
		return "?";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegOptional(this);
	}
	@Override protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchOptional(left, this);
	}
}

class PegRepeat extends PegUnary {
	public int atleast = 0; 
	protected PegRepeat(Peg e, int atLeast) {
		super(e, false);
		this.atleast = atLeast;
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegRepeat(this.innerExpr.clone(tr), this.atleast);
		}
		return ne;
	}
	@Override
	protected String getOperator() {
		return (this.atleast > 0) ? "+" : "*";
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchRepeat(left, this);
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitOneMore(this);
	}
}

//class PegZeroMore extends PegRepeat {
//	public PegZeroMore(Peg e) {
//		super(e, 0);
//	}
//	@Override
//	protected Peg clone(PegTransformer tr) {
//		Peg e = this.innerExpr.clone(tr);
//		if(e != this) {
//			return new PegZeroMore(e);
//		}
//		return this;
//	}
//	@Override
//	protected String getOperator() {
//		return "*";
//	}
//	@Override
//	public void accept(PegVisitor visitor) {
//		visitor.visitZeroMore(this);
//	}
//}

class PegAnd extends PegUnary {
	PegAnd(Peg e) {
		super(e, true);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegAnd(this.innerExpr.clone(tr));
		}
		return ne;
	}
	@Override
	protected String getOperator() {
		return "&";
	}
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchAnd(left, this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAnd(this);
	}
}

class PegNot extends PegUnary {
	PegNot(Peg e) {
		super(e, true);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegNot(this.innerExpr.clone(tr));
		}
		return ne;
	}
	@Override
	protected String getOperator() {
		return "!";
	}
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchNot(left, this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegNot(this);
	}
}

abstract class PegList extends Peg {
	protected UList<Peg> list;
	PegList() {
		super();
		this.list = new UList<Peg>(new Peg[2]);
	}
	public final int size() {
		return this.list.size();
	}
	public final Peg get(int index) {
		return this.list.ArrayValues[index];
	}
	public void add(Peg e) {
		this.list.add(e);
	}
	@Override protected void stringfy(UStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = this.get(i);
			if(e instanceof PegChoice || e instanceof PegSequence) {
				sb.append("(");
				e.stringfy(sb, debugMode);
				sb.append(")");
			}
			else {
				e.stringfy(sb, debugMode);
			}
		}
	}
	@Override
	protected void makeList(PegRuleSet parser, UList<String> list, UMap<String> set) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).makeList(parser, list, set);
		}
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			e.verify(ruleName, rules);
		}
	}
}

class PegSequence extends PegList {
	PegSequence() {
		super();
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			PegList l = new PegSequence();
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(tr));
			}
			return l;
		}
		return ne;
	}
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchSequence(left, this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSequence(this);
	}
	public Peg cdr() {
		PegSequence seq = new PegSequence(); 
		for(int i = 1; i < this.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			seq.list.add(e);
		}
		return seq;
	}
}

class PegChoice extends PegList {
	boolean hasCatch = false;
	PegChoice() {
		super();
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			PegList l = new PegChoice();
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(tr));
			}
			return l;
		}
		return ne;
	}
	public void extend(Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.add(e.get(i));
			}
		}
		else if(e != null) {
			this.list.add(e);
		}
	}
	@Override protected void stringfy(UStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			Peg e = this.list.ArrayValues[i];
			e.stringfy(sb, debugMode);
		}
	}
//	
//	@Override
//	protected PegObject fastMatch(PegObject left, ParserContext context) {
//		int stackPosition = context.getStackPosition(this);
//		PegObject node = left;
//		Peg errorPeg = context.storeFailurePeg();
//		long errorPosition = context.storeFailurePosition();
//		for(int i = 0; i < this.size(); i++) {
//			Peg e  = this.get(i);
//			if(e.memoizationMode) {
//				left.memoizationMode = true;
//			}
//			clearMemoCounter++;
//			if(e instanceof PegCatch) {
//				if(!context.isVerifyMode()) {
//					node = context.newErrorObject();
//					if(Main.VerbosePegMode) {
//						Main._PrintLine(node.formatSourceMessage("error: " + this.ruleName, " by " + node.createdPeg));
//					}
//				}
//				context.restoreFailure(errorPeg, errorPosition);
//				return e.performMatch(node, context);
//			}
//			node = e.performMatch(left, context);
//			left.memoizationMode = false;
//			if(!node.isFailure()) {
//				break;
//			}
//			context.popBack(stackPosition, Peg._BackTrack);
//		}
//		clearMemoCounter--;
//		if(clearMemoCounter == 0) {
//			context.clearMemo();
//		}
//		return node;
//	}
	
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchChoice(left, this);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			e.verify(ruleName, rules);
			if(e instanceof PegCatch) {
				this.hasCatch = true;
			}
		}
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitChoice(this);
	}
}

class PegSetter extends PegUnary {
	public int index;
	public PegSetter(Peg e, int index) {
		super(e, false);
		this.innerExpr = e;
		this.index = index;
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegSetter(this.innerExpr.clone(tr), this.index);
		}
		return ne;
	}
	@Override
	protected String getOperator() {
		if(this.index != -1) {
			return "^" + this.index;
		}
		return "^";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSetter(this);
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchSetter(left, this);
	}
}

class PegTag extends PegAtom {
	public PegTag(String tag) {
		super(tag);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegTag(this.symbol);
		}
		return ne;
	}
	@Override
	protected final void stringfy(UStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append(this.symbol);
		}
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchTag(left, this);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		rules.addObjectLabel(this.symbol);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitObjectLabel(this);
	}
}

class PegMessage extends PegAtom {
	public PegMessage(String message) {
		super(message);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegMessage(this.symbol);
		}
		return ne;
	}
	@Override
	protected final void stringfy(UStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append("`");			
			sb.append(this.symbol);
			sb.append("`");			
		}
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchMessage(left, this);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
	}
	@Override
	public void accept(PegVisitor visitor) {
		//visitor.visitDirect(this);
	}
}

class PegPipe extends PegAtom {
	public PegPipe(String ruleName) {
		super(ruleName);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegPipe(this.symbol);
		}
		return ne;
	}
	@Override
	protected final void stringfy(UStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append("|> ");
			sb.append(this.symbol);
		}
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchPipe(left, this);
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
		this.ruleName = ruleName;
		if(!rules.hasRule(this.symbol)) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			rules.foundError = true;
		}
	}
	@Override
	public void accept(PegVisitor visitor) {
		//visitor.visitDirect(this);
	}
}

class PegNewObject extends PegList {
	boolean leftJoin = false;
	String nodeName = "noname";
	public PegNewObject(boolean leftJoin) {
		super();
		this.leftJoin = leftJoin;
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			PegList l = new PegNewObject(this.leftJoin);
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(tr));
			}
			return l;
		}
		return ne;
	}

	public void add(Peg e) {
		if(e instanceof PegSequence) {
			for(int i =0; i < e.size(); i++) {
				this.list.add(e.get(i));
			}
		}
		else {
			this.list.add(e);
		}
	}

	@Override
	protected final void stringfy(UStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			if(this.leftJoin) {
				sb.append("8<^ ");
			}
			else {
				sb.append("8< ");
			}
		}
		else {
			sb.append("( ");
		}
		super.stringfy(sb, debugMode);
		if(debugMode) {
			sb.append(" >8");
		}
		else {
			sb.append(" )");
		}
	}
	@Override
	public PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchNewObject(left, this);
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitNewObject(this);
	}
}

class PegIndent extends PegAtom {
	PegIndent() {
		super("indent");
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg ne = tr.transform(this);
		if(ne == null) {
			ne = new PegIndent();
		}
		return ne;
	}
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchIndent(left, this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitIndent(this);
	}
}

// (e / catch e)

class PegCatch extends PegUnary {
	PegCatch (String ruleName, Peg first) {
		super(first, true);
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		Peg e = this.innerExpr.clone(tr);
		if(e != this) {
			return new PegCatch(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "catch ";
	}
	@Override
	protected PegObject simpleMatch(PegObject left, ParserContext context) {
		return context.matchCatch(left, this);
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitCatch(this);
	}
}

abstract class PegOptimized extends Peg {
	Peg orig;
	PegOptimized (Peg orig) {
		super();
		this.orig = orig;
		this.ruleName = orig.ruleName;
	}
	@Override
	protected Peg clone(PegTransformer tr) {
		return this;
	}
	@Override
	protected void stringfy(UStringBuilder sb, boolean debugMode) {
		this.orig.stringfy(sb, debugMode);
	}
	@Override
	protected void makeList(PegRuleSet parser, UList<String> list, UMap<String> set) {
	}
	@Override
	protected void verify(String ruleName, PegRuleSet rules) {
	}
	@Override
	public void accept(PegVisitor visitor) {
	}

}

//class PegCType extends PegAtom {
//	static public HashSet<String> typedefs = new HashSet<String>();
//	boolean AddType = false;
//	PegCType(String leftLabel, boolean AddType) {
//		super(AddType ? "addtype" : "ctype");
//		this.AddType = AddType;
//	}
//
//	@Override
//	protected Peg clone(String ns) {
//		return this;
//	}
//	
//	
//	@Override
//	protected PegObject simpleMatch(PegObject left, ParserContext context) {
//		if(left.source != null) {
//			if(AddType) {
//				if(left.tag.equals("#DeclarationNoAttribute") && left.AST.length >= 2) {
//					// left.AST = [typedef struct A, StructA]
//					PegObject first = left.AST[0];
//					if(first.AST.length >= 2) {
//						String firstText = first.AST[0].getText().trim();
//						// first is "typedef"
//						if(first.AST[0].tag.equals("#storageclassspecifier") && firstText.equals("typedef")) {
//							PegObject second = left.AST[1];
//							for (int i = 0; i < second.AST.length; i++) {
//								PegObject decl = second.get(i);
//								if(decl.tag.equals("#declarator")) {
//									// "typedef struct A StructA;"
//									// add new typename StructA
//									System.out.println(decl.get(decl.AST.length - 1).getText());
//									typedefs.add(decl.get(decl.AST.length - 1).getText());
//								}
//							}
//							return left;
//						}
//					}
//				}
//			}
//			else {
//				String name = left.getText().trim();
//				if (!typedefs.contains(name)) {
//					return new PegObject(null); //not match
//				}
//			}
//		}
//		return left;
//	}
//
//	@Override
//	public void accept(PegVisitor visitor) {
//		// TODO Auto-generated method stub
//	}
//
//}
