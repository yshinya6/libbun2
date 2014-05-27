package org.libbun;


public class SourceContext {
	public final         BunSource source;
	protected int        sourcePosition = 0;
	public final int     endPosition;

	int backtrackCount = 0;
	int backtrackSize = 0;

	public SourceContext(BunSource source, int startIndex, int endIndex) {
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
	}

	public SourceContext subContext(int startIndex, int endIndex) {
		return new SourceContext(this.source, startIndex, endIndex);
	}

	public final int getPosition() {
		return this.sourcePosition;
	}

	public final void rollback(int pos) {
		if(this.sourcePosition > pos) {
			//			System.out.println("backtracking");
			//			new Exception().printStackTrace();
			this.backtrackCount = this.backtrackCount + 1;
			this.backtrackSize = this.backtrackSize + (this.sourcePosition - pos);
		}
		this.sourcePosition = pos;
	}

	@Override
	public final String toString() {
		if(this.endPosition > this.sourcePosition) {
			return this.source.substring(this.sourcePosition, this.endPosition);
		}
		return "";
	}

	public final boolean hasChar() {
		return this.sourcePosition < this.endPosition;
	}

	public final char charAt(int n) {
		return Main._GetChar(this.source.sourceText, n);
	}

	public final char getChar() {
		if(this.hasChar()) {
			return this.charAt(this.sourcePosition);
		}
		return '\0';
	}

	public final char getChar(int n) {
		int pos = this.sourcePosition + n;
		if(pos >= 0 && pos < this.endPosition) {
			return this.charAt(pos);
		}
		return '\0';
	}

	public final int consume(int plus) {
		this.sourcePosition = this.sourcePosition + plus;
		return this.sourcePosition;
	}

	public final char nextChar() {
		if(this.hasChar()) {
			int pos = this.sourcePosition;
			this.consume(1);
			return this.charAt(pos);
		}
		return '\0';
	}

	public final int matchZeroMore(UniCharset charset) {
		for(;this.hasChar(); this.consume(1)) {
			char ch = this.charAt(this.sourcePosition);
			if(!charset.match(ch)) {
				break;
			}
		}
		return this.sourcePosition;
	}

	public final boolean match(char ch) {
		if(ch == this.getChar()) {
			this.consume(1);
			return true;
		}
		return false;
	}

	public final boolean match(UniCharset charset) {
		if(charset.match(this.getChar())) {
			this.consume(1);
			return true;
		}
		return false;
	}

	//	public boolean checkSymbolLetter(int plus) {
	//		char ch = this.getChar(plus);
	//		if(this.isSymbolLetter(ch)) {
	//			return true;
	//		}
	//		return false;
	//	}

	public void skipIndent(int indentSize) {
		int pos = this.sourcePosition;
		//		this.showPosition("skip characters until indent="+indentSize + ", pos=" + pos, pos);
		for(;pos < this.endPosition; pos = pos + 1) {
			char ch = this.charAt(pos);
			if(ch == '\n' && pos > this.sourcePosition) {
				int posIndent = this.source.getIndentSize(pos+1);
				if(posIndent <= indentSize) {
					this.sourcePosition = pos + 1;
					//					System.out.println("skip characters until indent="+indentSize + ", pos=" + this.sourcePosition);
					return ;
				}
			}
		}
		//		System.out.println("skip characters until indent="+indentSize + ", pos = endPosition");
		this.sourcePosition = this.endPosition;
	}

	public final boolean match(String text) {
		if(this.endPosition - this.sourcePosition >= text.length()) {
			for(int i = 0; i < text.length(); i++) {
				if(text.charAt(i) != this.charAt(this.sourcePosition + i)) {
					return false;
				}
			}
			this.consume(text.length());
			return true;
		}
		return false;
	}



	////	public final BunToken newToken() {
	////		return new BunToken(this.source, this.sourcePosition, this.sourcePosition);
	////	}
	////
	////	public final BunToken newToken(int startIndex, int endIndex) {
	////		return new BunToken(this.source, startIndex, endIndex);
	////	}
	//
	//	public final boolean sliceNumber(BunToken token) {
	//		char ch = this.nextChar();
	//		if(LibBunSystem._IsDigit(ch)) {
	//			for(;this.hasChar(); this.consume(1)) {
	//				ch = this.charAt(this.sourcePosition);
	//				if(!LibBunSystem._IsDigit(ch)) {
	//					break;
	//				}
	//			}
	//			token.endIndex = this.sourcePosition;
	//			return true;
	//		}
	//		return false;
	//	}
	//
	//	public final boolean isSymbolLetter(char ch) {
	//		return (LibBunSystem._IsLetter(ch)  || ch == '_');
	//	}
	//
	//	public final boolean sliceSymbol(BunToken token, String allowedChars) {
	//		char ch = this.nextChar();
	//		if(this.isSymbolLetter(ch) || allowedChars.indexOf(ch) != -1) {
	//			for(;this.hasChar(); this.consume(1)) {
	//				ch = this.charAt(this.sourcePosition);
	//				if(!this.isSymbolLetter(ch) && !LibBunSystem._IsDigit(ch) && allowedChars.indexOf(ch) == -1) {
	//					break;
	//				}
	//			}
	//			token.endIndex = this.sourcePosition;
	//			return true;
	//		}
	//		return false;
	//	}

	public final boolean sliceMatchedText(SourceToken token, String text) {
		if(this.endPosition - this.sourcePosition >= text.length()) {
			for(int i = 0; i < text.length(); i++) {
				//System.out.println("i="+i+", '"+text.charAt(i) + "', '"+this.charAt(this.currentPosition + i));
				if(text.charAt(i) != this.charAt(this.sourcePosition + i)) {
					return false;
				}
			}
			this.consume(text.length());
			token.endIndex = this.sourcePosition;
			return true;
		}
		return false;
	}

	public final boolean sliceQuotedTextUntil(SourceToken token, char endChar, String stopChars) {
		for(; this.hasChar(); this.consume(1)) {
			char ch = this.charAt(this.sourcePosition);
			if(ch == endChar) {
				token.endIndex = this.sourcePosition;
				return true;
			}
			if(stopChars.indexOf(ch) != -1) {
				break;
			}
			if(ch == '\\') {
				this.consume(1);  // skip next char;
			}
		}
		token.endIndex = this.sourcePosition;
		return false;
	}

	public final boolean sliceUntilWhiteSpace(SourceToken token, String stopChars) {
		for(; this.hasChar(); this.consume(1)) {
			char ch = this.charAt(this.sourcePosition);
			if(ch == '\\') {
				this.consume(1);  // skip next char;
			}
			else {
				if(ch == ' ' || ch == '\t' || ch == '\n') {
					token.endIndex = this.sourcePosition;
					return true;
				}
				if(stopChars.indexOf(ch) != 0) {
					break;
				}
			}
		}
		token.endIndex = this.sourcePosition;
		return false;
	}

	public final String formatErrorMessage(String msg1, String msg2) {
		return this.source.formatErrorMessage(msg1, this.sourcePosition, msg2);
	}

	public final void showPosition(String msg) {
		showPosition(msg, this.getPosition());
	}

	public final void showPosition(String msg, int pos) {
		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
	}

	public final void showErrorMessage(String msg) {
		System.out.println(this.source.formatErrorMessage("error", this.sourcePosition, msg));
		Main._Exit(1, msg);
	}

}

