package org.libbun;

public class SourceToken {
	Peg createdPeg;
	final BunSource source;
	int  startIndex;
	int  endIndex;
	String token;

	SourceToken(Peg createdPeg, BunSource source, int startIndex, int endIndex) {
		this.createdPeg = createdPeg;
		this.source = source;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.token = null;
	}

	public SourceToken(Peg createdPeg, BunSource source, int startIndex, int endIndex, String token) {
		this.createdPeg = createdPeg;
		this.source = source;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.token = token;
	}

	public final int size() {
		return this.endIndex - this.startIndex;
	}

	public final boolean isNull() {
		return (this.startIndex == this.endIndex);
	}

	public final String getText() {
		if(this.token == null) {
			if(this.size() > 0) {
				this.token = this.source.substring(this.startIndex, this.endIndex);
			}
			else {
				this.token = "";
			}
		}
		return this.token;
	}

	public final String getFileName() {
		return this.source.fileName;
	}

	public final int getLineNumber() {
		return this.source.getLineNumber(this.startIndex);
	}



	public final int indexOf(String s) {
		int loc = this.source.sourceText.indexOf(s, this.startIndex);
		if(loc != -1 && loc < this.endIndex) {
			return loc - this.startIndex;
		}
		return -1;
	}

	public final String substring(int startIndex) {
		return this.source.sourceText.substring(startIndex + this.startIndex, this.endIndex);
	}

	public final String substring(int startIndex, int endIndex) {
		startIndex = startIndex + this.startIndex;
		endIndex = endIndex + this.startIndex;
		if(endIndex <= this.endIndex) {
			return this.source.sourceText.substring(startIndex, endIndex);
		}
		return null;
	}

	public final String getIndentText() {
		int startPosition = this.source.getLineStartPosition(this.startIndex);
		int i = startPosition;
		for(; i < this.startIndex; i++) {
			char ch = this.source.charAt(i);
			if(ch != ' ' && ch == '\t') {
				break;
			}
		}
		return this.source.substring(startPosition, i);
	}
	
	public String formatErrorMessage(String errorType, String msg) {
		return this.source.formatErrorMessage(errorType, this.startIndex, msg);
	}



}
