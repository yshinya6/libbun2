//package org.libbun.peg4d;
//
//import org.libbun.BunTag;
//import org.libbun.BunType;
//import org.libbun.Functor;
//import org.libbun.Main;
//import org.libbun.SourceBuilder;
//import org.libbun.SymbolTable;
//import org.libbun.UCharset;
//import org.libbun.UMap;
//
//class PegoTag {
//	public  int    tagId;
//	public  String tagName;
//	Functor matched = null;
//	BunType typed   = null;
//
//	private PegoTag(int tagId, String tagName) {
//		this.tagId = tagId;
//		this.tagName = tagName;
//	}
//	
//	private static UMap<PegoTag> pooled = new UMap<PegoTag>();
//	
//	static {
//		newPegoTag("!");       // tagId = 0;   Failure
//		newPegoTag("|");       // tagId = 1;   Pipe
//	}
//	
//	public final static int id(String tagName) {
//		PegoTag t = pooled.get(tagName);
//		if(t == null) {
//			return -1;
//		}
//		return t.tagId;
//	}
//	
//	public final static PegoTag newPegoTag(String tagName) {
//		PegoTag t = pooled.get(tagName);
//		if(t == null) {
//			t = new PegoTag(pooled.size(), tagName);
//			pooled.put(tagName, t);
//		}
//		return new PegoTag(t.tagId, tagName);
//	}
//	
////	// method 
////
////	public final boolean isFailure() {
////		return this.tagId == 0;
////	}
////
////	public final boolean isPipe() {
////		return this.tagId == 1;
////	}
////	
////	public final String getName() {
////		return this.tagName;
////	}
////	
////	public final boolean isMatched() {
////		return this.matched != null;
////	}
////	
////	public final boolean isUntyped() {
////		return this.typed == null || this.typed == BunType.UntypedType;
////	}
////
////	public final BunType getType() {
////		if(this.typed == null) {
////			if(this.matched != null) {
////				this.typed = this.matched.getReturnType(BunType.UntypedType);
////			}
////			if(this.typed == null) {
////				return BunType.UntypedType;
////			}
////		}
////		return this.typed;
////	}
//	
//}
//
//public abstract class Pego {
//	public Pego       parent = null;
//	public PegoTag    tag = null;
//	int               sourcePosition;
//	
//	public Pego(long pos, PegoTag tag) {
//		this.tag = tag;
//		this.sourcePosition = (int)pos;
//	}
//	
//	public final Pego getParent() {
//		return this.parent;
//	}
//	
//	public final void setParent(Pego node) {
//		this.parent = node;
//	}
//
//	public final boolean isFailure() {
//		return (this.tag == null);
//	}
//	
//	public final boolean is(String functor) {
//		return this.tag.equals(functor);
//	}
//
//	public ParserSource getSource() {
//		return this.parent.getSource();
//	}
//	
//	public final long getSourcePosition() {
//		return this.sourcePosition;
//	}
//	
//	@Override
//	public String toString() {
//		SourceBuilder sb = new SourceBuilder(null);
//		this.stringfy(sb);
//		return sb.toString();
//	}
//	
//	protected abstract void stringfy(SourceBuilder sb);
//
//	public abstract String getText();
//	
//	public int size() {
//		return 0;
//	}
//	public Pego get(int index) {
//		return null;
//	}
//	public Pego get(int index, Pego defaultValue) {
//		return defaultValue;
//	}
//	public void set(int index, Pego node) {
//	}
//	public void append(Pego node) {
//	}
//	public void insert(int index, Pego childNode) {
//	}
//	public void removeAt(int index) {
//	}
//}
//
//class PegoToken extends Pego {
//	String text;
//	public PegoToken(long pos, PegoTag tag, String text) {
//		super(pos, tag);
//		this.text = text;
//	}
//	public final String getText() {
//		return this.text;
//	}
//	protected void stringfy(SourceBuilder sb) {
//		sb.append("{");
//		sb.append(this.tag.tagName);
//		sb.append(UCharset._QuoteString("'", this.getText(), "'"));
//		sb.append("}");
//	}
//}
//
//class PegoAST extends Pego {
//	Pego[] AST = null;
//	PegoAST(long pos, PegoTag tag, int size) {
//		super(pos, tag);
//		this.AST = new Pego[size];
//	}
//	protected final void stringfy(SourceBuilder sb) {
//		sb.openIndent("{" + this.tag.tagName);
//		for(int i = 0; i < this.size(); i++) {
//			sb.appendNewLine();
//			this.AST[i].stringfy(sb);
//		}
//		sb.closeIndent("}");
//	}
//	public final String getText() {
//		return "";
//	}
//	public final int size() {
//		return this.AST.length;
//	}
//	public final Pego get(int index) {
//		return this.AST[index];
//	}
//	private final void resizeAst(int size) {
//		if(this.AST == null && size > 0) {
//			this.AST = new Pego[size];
//		}
//		else if(this.AST.length != size) {
//			Pego[] newast = new Pego[size];
//			if(size > this.AST.length) {
//				Main._ArrayCopy(this.AST, 0, newast, 0, this.AST.length);
//			}
//			else {
//				Main._ArrayCopy(this.AST, 0, newast, 0, size);
//			}
//			this.AST = newast;
//		}
//	}
//	private final void expandAstToSize(int newSize) {
//		if(newSize > this.size()) {
//			this.resizeAst(newSize);
//		}
//	}
//	public final Pego get(int index, Pego defaultValue) {
//		if(index < this.size()) {
//			return this.AST[index];
//		}
//		return defaultValue;
//	}
//	public final void set(int index, Pego node) {
//		if(!(index < this.size())){
//			this.expandAstToSize(index+1);
//		}
//		this.AST[index] = node;
//		node.parent = this;
//	}
//	public final void append(Pego node) {
//		int size = this.size();
//		this.expandAstToSize(size+1);
//		this.AST[size] = node;
//		node.parent = this;
//	}
//	public final void insert(int index, Pego childNode) {
//		int oldsize = this.size();
//		if(index < oldsize) {
//			Pego[] newast = new Pego[oldsize+1];
//			if(index > 0) {
//				Main._ArrayCopy(this.AST, 0, newast, 0, index);
//			}
//			newast[index] = childNode;
//			childNode.parent = this;
//			if(oldsize > index) {
//				Main._ArrayCopy(this.AST, index, newast, index+1, oldsize - index);
//			}
//			this.AST = newast;
//		}
//		else {
//			this.append(childNode);
//		}
//	}
//	public final void removeAt(int index) {
//		int oldsize = this.size();
//		if(oldsize > 1) {
//			Pego[] newast = new Pego[oldsize-1];
//			if(index > 0) {
//				Main._ArrayCopy(this.AST, 0, newast, 0, index);
//			}
//			if(oldsize - index > 1) {
//				Main._ArrayCopy(this.AST, index+1, newast, index, oldsize - index - 1);
//			}
//			this.AST = newast;
//		}
//		else {
//			this.AST = null;
//		}
//	}
//}
//
//class PegoGamma extends Pego {
//	Pego inner;
//	public PegoGamma(long pos, String tag, Pego inner) {
//		super(pos, PegoTag.newPegoTag(tag));
//		this.inner = inner;
//	}
//	
//}
//
//class PegoSource extends Pego {
//	Peg    createdPeg = null;
//	public ParserSource    source = null;
//	public String          optionalToken = null;
//	public int length = 0;
//
//	public PegoSource(ParserSource source, long pos, String tag, Peg createdPeg) {
//		super(pos, PegoTag.newPegoTag(tag));
//		this.source = source;
//		this.createdPeg = createdPeg;
//	}
//
//	public final void setLength(long length) {
//		this.length = (int)length;
//	}
//	
//	public final String getText() {
//		if(this.optionalToken != null) {
//			return this.optionalToken;
//		}
//		if(this.source != null) {
//			return this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.length);
//		}
//		return "";
//	}
//
//	protected final void stringfy(SourceBuilder sb) {
//		if(this.isFailure()) {
//			sb.append("{");
//			sb.append(this.tag.tagName);
//			sb.append(UCharset._QuoteString("'", this.getText(), "'"));
//			sb.append("}");
//			//sb.append(this.formatSourceMessage("syntax error", this.info()));
//		}
//		else {
//			sb.append("{");
//			sb.append(this.tag.tagName);
//			if(this.optionalToken != null) {
//				sb.append(UCharset._QuoteString("`", this.optionalToken, "`"));
//			}
//			else {
//				sb.append("pos="+this.getSourcePosition());
//				sb.append(",len="+this.length);
//			}
//			sb.append("}");
//		}
//	}
//
//	
////	public final String formatSourceMessage(String type, String msg) {
////		return this.source.formatErrorMessage(type, this.getSourcePosition(), msg);
////	}
////
////	public final boolean isEmptyToken() {
////		return this.length == 0;
////	}
////
////
////	public final int count() {
////		int count = 1;
////		for(int i = 0; i < this.size(); i++) {
////			count = count + this.get(i).count();
////		}
////		return count;
////	}
////
////	public final void checkNullEntry() {
////		for(int i = 0; i < this.size(); i++) {
////			if(this.AST[i] == null) {
////				this.AST[i] = new PegObject("#empty", this.source, null, this.startIndex);
////				this.AST[i].parent = this;
////			}
////		}
////	}
////
////	public final String textAt(int index, String defaultValue) {
////		if(index < this.size()) {
////			return this.AST[index].getText();
////		}
////		return defaultValue;
////	}
////
////	public BunType typeAt(SymbolTable gamma, int index, BunType defaultType) {
////		if(index < this.size()) {
////			PegObject node = this.AST[index];
////			if(node.typed != null) {
////				return node.typed;
////			}
////			if(node.matched == null && gamma != null) {
////				node = gamma.tryMatch(node, true);
////			}
////			if(node.matched != null) {
////				return node.matched.getReturnType(defaultType);
////			}
////		}
////		return defaultType;
////	}
////
////
////
////	private String info() {
////		if(this.matched == null) {
////			if(this.source != null && Main.VerbosePegMode) {
////				return "         ## by peg : " + this.createdPeg;
////			}
////			return "";
////		}
////		else {
////			return "      :: " + this.getType(null) + " by " + this.matched;
////		}
////	}
////
////	public final PegObject findParentNode(String name) {
////		PegObject node = this;
////		while(node != null) {
////			if(node.is(name)) {
////				return node;
////			}
////			node = node.parent;
////		}
////		return null;
////	}
////
////	public final SymbolTable getSymbolTable() {
////		PegObject node = this;
////		while(node.gamma == null) {
////			node = node.parent;
////		}
////		return node.gamma;
////	}
////
////	public final SymbolTable getLocalSymbolTable() {
////		if(this.gamma == null) {
////			SymbolTable gamma = this.getSymbolTable();
////			gamma = new SymbolTable(gamma.getNamespace(), this);
////			// NOTE: this.gamma was set in SymbolTable constructor
////			assert(this.gamma != null);
////		}
////		return this.gamma;
////	}
////
////	public final BunType getType(BunType defaultType) {
////		if(this.typed == null) {
////			if(this.matched != null) {
////				this.typed = this.matched.getReturnType(defaultType);
////			}
////			if(this.typed == null) {
////				return defaultType;
////			}
////		}
////		return this.typed;
////	}
////
////	public boolean isVariable() {
////		// TODO Auto-generated method stub
////		return true;
////	}
////
////	public void setVariable(boolean flag) {
////	}
////
////	public final int countUnmatched(int c) {
////		for(int i = 0; i < this.size(); i++) {
////			PegObject o = this.get(i);
////			c = o.countUnmatched(c);
////		}
////		if(this.matched == null) {
////			return c+1;
////		}
////		return c;
////	}
////
////	public final boolean isUntyped() {
////		return this.typed == null || this.typed == BunType.UntypedType;
////	}
//}
