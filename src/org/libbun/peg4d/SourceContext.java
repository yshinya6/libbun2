//package org.libbun.peg4d;
//
//import org.libbun.Main;
//import org.libbun.UCharset;
//
//public class SourceContext {
//	public final          ParserSource source;
//	protected long        sourcePosition = 0;
//	public    long        endPosition;
//
//	long backtrackCount = 0;
//	long backtrackSize = 0;
//
//	public SourceContext(ParserSource source, long startIndex, long endIndex) {
//		this.source = source;
//		this.sourcePosition = startIndex;
//		this.endPosition = endIndex;
//	}
//
//	public SourceContext subContext(long startIndex, long endIndex) {
//		return new SourceContext(this.source, startIndex, endIndex);
//	}
//
//	public final long getPosition() {
//		return this.sourcePosition;
//	}
//	
//	public void setPosition(long pos) {
//		this.sourcePosition = pos;
//	}
//
//	public final void rollback(long pos) {
//		if(this.sourcePosition > pos) {
//			this.backtrackCount = this.backtrackCount + 1;
//			this.backtrackSize = this.backtrackSize + (this.sourcePosition - pos);
//		}
//		this.sourcePosition = pos;
//	}
//	
//	public String substring(long startIndex, long endIndex) {
//		return this.source.substring(startIndex, endIndex);
//	}
//
//	@Override
//	public final String toString() {
//		if(this.endPosition > this.sourcePosition) {
//			return this.source.substring(this.sourcePosition, this.endPosition);
//		}
//		return "";
//	}
//
//	public final boolean hasChar() {
//		return this.sourcePosition < this.endPosition;
//	}
//
//	public final char charAt(long n) {
//		return this.source.charAt(n);
//	}
//
//	public final char getChar() {
//		if(this.hasChar()) {
//			return this.charAt(this.sourcePosition);
//		}
//		return '\0';
//	}
//
//	public final char getChar(long n) {
//		long pos = this.sourcePosition + n;
//		if(pos >= 0 && pos < this.endPosition) {
//			return this.charAt(pos);
//		}
//		return '\0';
//	}
//
//	public final void consume(long plus) {
//		this.sourcePosition = this.sourcePosition + plus;
//	}
//
//	public final boolean match(char ch) {
//		if(ch == this.getChar()) {
//			this.consume(1);
//			return true;
//		}
//		return false;
//	}
//
//	public final boolean match(String text) {
//		if(this.endPosition - this.sourcePosition >= text.length()) {
//			for(int i = 0; i < text.length(); i++) {
//				if(text.charAt(i) != this.charAt(this.sourcePosition + i)) {
//					return false;
//				}
//			}
//			this.consume(text.length());
//			return true;
//		}
//		return false;
//	}
//
//	public final boolean match(UCharset charset) {
//		if(charset.match(this.getChar())) {
//			this.consume(1);
//			return true;
//		}
//		return false;
//	}
//	
//	public final long matchZeroMore(UCharset charset) {
//		for(;this.hasChar(); this.consume(1)) {
//			char ch = this.charAt(this.sourcePosition);
//			if(!charset.match(ch)) {
//				break;
//			}
//		}
//		return this.sourcePosition;
//	}
//
////	public final void skipComment(UCharset skipChars) {
////		while(this.hasChar()) {
////			this.matchZeroMore(skipChars);
////			int pos = this.getPosition();
////			if(this.match('/') && this.match('/')) {
////				while(this.hasChar()) {
////					char ch = this.getChar();
////					this.consume(1);
////					if(ch == '\n') {
////						break;
////					}
////				}
////			}
////			else {
////				this.rollback(pos);
////				return;
////			}
////		}
////	}
//	
//
////	public final boolean matchIndentSize(String text) {
////		int indentSize = 0;
////		if(this.endPosition - this.sourcePosition >= text.length()) {
////			for(int i = this.sourcePosition; i < this.endPosition; i++) {
////				char ch = this.charAt(i);
////				if(ch != ' ' && ch != '\t') {
////					break;
////				}
////				indentSize++;
////			}
////			if(indentSize != text.length()) {
////				return false;
////			}
////			this.consume(indentSize);
////			return true;
////		}
////		return false;
////	}
//
//	public final String formatErrorMessage(String msg1, String msg2) {
//		return this.source.formatErrorMessage(msg1, this.sourcePosition, msg2);
//	}
//
//	public final void showPosition(String msg) {
//		showPosition(msg, this.getPosition());
//	}
//
//	public final void showPosition(String msg, long pos) {
//		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
//	}
//
//	public final void showErrorMessage(String msg) {
//		System.out.println(this.source.formatErrorMessage("error", this.sourcePosition, msg));
//		Main._Exit(1, msg);
//	}
//
//
//}
//
