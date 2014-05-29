package org.libbun;

public class PegParserParser extends SourceContext {

	public PegParserParser(BunSource source, int startIndex, int endIndex) {
		super(source, startIndex, endIndex);
	}

	public PegParserParser(BunSource source) {
		super(source, 0, source.sourceText.length());
	}

	private PegParserParser subParser(int startIndex, int endIndex) {
		return new PegParserParser(this.source, startIndex, endIndex);
	}

	public boolean hasRule() {
		this.skipComment(UniCharset.SemiColon);
		return this.hasChar();
	}

	public PegRule parseRule() {
		boolean importFile = false;
		if(this.match("import")) {
			this.skipComment(UniCharset.WhiteSpaceNewLine);
			importFile = true;
		}
		int startIndex = this.getPosition();
		if(!this.match(UniCharset.Letter)) {
			this.showErrorMessage("Is forgotten ; ?");
			return null;
		}
		this.matchZeroMore(UniCharset.NameSymbol);
		String label = this.substring(startIndex, this.getPosition());
		this.skipComment(UniCharset.WhiteSpaceNewLine);
		Peg parsed = null;
		if(importFile) {
			if(!this.match("from")) {
				this.showErrorMessage("expected from");
				return null;
			}
			this.skipComment(UniCharset.WhiteSpaceNewLine);
			startIndex = this.getPosition();
			this.matchZeroMore(UniCharset.NodeLabel);
			String fileName = this.substring(startIndex, this.getPosition());
			parsed = this.importPeg(label, fileName);
		}
		else {
			if(!this.match('=') && !this.match('<', '-')) {
				this.showErrorMessage("Is forgotten ; ?");
				return null;
			}
			parsed = this.parsePegExpr(label);
		}
		if(parsed != null) {
			return new PegRule(label, parsed);
		}
		return null;
	}

	private Peg importPeg(String label, String fileName) {
		if(Main.PegDebuggerMode) {
			System.out.println("importing " + fileName);
		}
		fileName = this.source.checkFileName(fileName);
		PegParser p = new PegParser(null);
		p.loadPegFile(fileName);
		Peg e = p.getDefinedPeg(label);
		if(e == null) {
			this.showErrorMessage("undefined " + label);
		}
		return e;
	}

	private int skipQuotedString(char endChar) {
		for(; this.hasChar(); this.consume(1)) {
			char ch = this.getChar();
			if(ch == endChar) {
				int index = this.getPosition();
				this.consume(1);
				return index;
			}
			if(ch == '\\') {
				this.consume(1);  // skip next char;
			}
		}
		return -1;
	}

	private int skipGroup(int openChar, int closeChar) {
		int order = 1;
		while(this.hasChar()) {
			char ch = this.nextChar();
			if(ch == closeChar) {
				order = order - 1;
				if(order == 0) {
					return this.getPosition() - 1;
				}
			}
			if(ch == openChar) {
				order = order + 1;
			}
			if(ch == '"' || ch == '\'') {
				if(this.skipQuotedString(ch) == -1) {

					return -1;
				}
			}
			if(ch == '[') {
				int pos = this.getPosition() - 1;
				if(this.skipQuotedString(']') == -1) {
					this.rollback(pos);
					this.showErrorMessage("unclosed [");
					return -1;
				}
			}
		}
		return -1;
	}

	private Peg parsePostfix(String leftName, Peg left) {
		if(left != null) {
			if(this.match('@')) {
				left = new PegSetter(leftName, left);
			}
			if(this.match('*')) {
				return new PegZeroMore(leftName, left);
			}
			if(this.match('+')) {
				return new PegOneMore(leftName, left);
			}
			if(this.match('?')) {
				return new PegOptional(leftName, left);
			}
		}
		return left;
	}

	private String substring(int startIndex, int endIndex) {
		return this.source.substring(startIndex, endIndex);
	}

	private Peg parseSingleExpr(String leftLabel) {
		Peg right = null;
		this.skipComment(UniCharset.WhiteSpaceNewLine);
		if(this.match(';')) {
			this.consume(-1);
			return null;
		}
		if(this.match("indent")) {
			right = new PegIndent(leftLabel);
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match(UniCharset.Letter)) {
			int startIndex = this.getPosition() - 1;
			int endIndex = this.matchZeroMore(UniCharset.NameSymbol);
			right = new PegLabel(leftLabel, this.substring(startIndex, endIndex));
			right.setSource(this.source, startIndex);
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match('.')) {
			right = new PegAny(leftLabel);
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match('$')) {
			right = this.parseSingleExpr(leftLabel);
			if(right != null) {
				right = new PegSetter(leftLabel, right);
			}
			return right;
		}
		if(this.match('&')) {
			right = this.parseSingleExpr(leftLabel);
			if(right != null) {
				right = new PegAndPredicate(leftLabel, right);
			}
			return right;
		}
		if(this.match('!')) {
			right = this.parseSingleExpr(leftLabel);
			if(right != null) {
				right = new PegNotPredicate(leftLabel, right);
			}
			return right;
		}
		if(this.match('#')) {
			int startIndex = this.getPosition();
			this.matchZeroMore(UniCharset.NodeLabel);
			int endIndex = this.getPosition();
			right = new PegObjectLabel(leftLabel, this.substring(startIndex - 1, endIndex));
			return right;
		}
		if(this.match('(')) {
			int startIndex = this.getPosition();
			int endIndex = this.skipGroup('(', ')');
			if(endIndex == -1) {
				this.showErrorMessage("unclosed ')'");
				return null;
			}
			PegParserParser sub = this.subParser(startIndex, endIndex);
			right = sub.parsePegExpr(leftLabel);
			if(right != null) {
				right = this.parsePostfix(leftLabel, right);
			}
			return right;
		}
		if(this.match('[')) {
			int startIndex = this.getPosition();
			int endIndex = this.skipQuotedString(']');
			if(endIndex == -1) {
				this.showErrorMessage("unclosed ']'");
				return null;
			}
			right = new PegCharacter(leftLabel, this.source.substring(startIndex, endIndex));
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match('"')) {
			int startIndex = this.getPosition();
			int endIndex = this.skipQuotedString('"');
			if(endIndex == -1) {
				this.showErrorMessage("unclosed \"");
				return null;
			}
			String s = this.source.substring(startIndex, endIndex);
			right = new PegString(leftLabel, UniCharset._UnquoteString(s));
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match('\'')) {
			int startIndex = this.getPosition();
			int endIndex = this.skipQuotedString('\'');
			if(endIndex == -1) {
				this.showErrorMessage("unclosed '");
				return null;
			}
			String s = this.source.substring(startIndex, endIndex);
			right = new PegString(leftLabel, UniCharset._UnquoteString(s));
			return this.parsePostfix(leftLabel, right);
		}
		if(this.match('{')) {
			boolean leftJoin = false;
			if(this.match('$', ' ') || this.match('$', '\n')) {
				leftJoin = true;
			}
			int startIndex = this.getPosition();
			int endIndex = this.skipGroup('{', '}');
			if(endIndex == -1) {
				this.rollback(startIndex);
				this.showErrorMessage("unclosed '}'");
				return null;
			}
			PegParserParser sub = this.subParser(startIndex, endIndex);
			right = sub.parsePegExpr(leftLabel);
			right = new PegNewObject(leftLabel, leftJoin, right);
			right = this.parsePostfix(leftLabel, right);
			return right;
		}
		if(this.match("<<")) {
			boolean leftJoin = false;
			if(this.match('@', ' ') || this.match('@', '\n')) {
				leftJoin = true;
			}
			int startIndex = this.getPosition();
			int endIndex = this.skipGroup('<', '>');
			if(endIndex == -1 || !this.match('>')) {
				this.rollback(startIndex);
				this.showErrorMessage("unclosed '>>'");
				return null;
			}
			PegParserParser sub = this.subParser(startIndex, endIndex);
			right = sub.parsePegExpr(leftLabel);
			right = new PegNewObject(leftLabel, leftJoin, right);
			right = this.parsePostfix(leftLabel, right);
			return right;
		}
		this.showErrorMessage("unexpected character '" + this.getChar() + "'");
		return right;
	}

	private final Peg parseSequenceExpr(String leftLabel) {
		Peg left = this.parseSingleExpr(leftLabel);
		if(left == null) {
			return left;
		}
		this.skipComment(UniCharset.WhiteSpaceNewLine);
		if(this.hasChar()) {
			this.skipComment(UniCharset.WhiteSpaceNewLine);
			char ch = this.getChar();
			if(ch == '/') {
				this.consume(1);
				this.skipComment(UniCharset.WhiteSpaceNewLine);
				return left;
			}
			Peg right = this.parseSequenceExpr(leftLabel);
			if(right != null) {
				left = left.appendAsSequence(right);
			}
		}
		return left;
	}

	public final Peg parsePegExpr(String leftLabel) {
		Peg left = this.parseSequenceExpr(leftLabel);
		this.skipComment(UniCharset.WhiteSpaceNewLine);
		if(this.match(';')) {
			return left;
		}
		if(this.hasChar()) {
			if(this.match("catch ") || this.match("catch\n")) {
				this.skipComment(UniCharset.WhiteSpaceNewLine);
				Peg right = this.parsePegExpr(leftLabel);
				right = new PegCatch(leftLabel, right);
				left = left.appendAsChoice(right);
			}
			else {
				Peg right = this.parsePegExpr(leftLabel);
				if(right != null) {
					left = left.appendAsChoice(right);
				}
			}
		}
		return left;
	}

	private final boolean match(char ch, char ch2) {
		if(this.getChar(0) == ch && this.getChar(1) == ch2) {
			this.consume(2);
			return true;
		}
		return false;
	}

}

class PegRule {
	String label;
	Peg    peg;
	public PegRule(String label, Peg p) {
		this.label = label;
		this.peg   = p;
	}
}