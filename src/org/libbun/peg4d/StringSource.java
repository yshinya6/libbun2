package org.libbun.peg4d;

import org.libbun.Main;

public class StringSource extends ParserSource {
	public String  sourceText;
	public StringSource(String fileName, long linenum, String sourceText) {
		super(fileName, linenum);
		this.sourceText = sourceText;
	}
	public final long length() {
		return this.sourceText.length();
	}
	public final char charAt(long n) {
		if(0 <= n && n < this.length()) {
			return Main._GetChar(this.sourceText, (int)n);
		}
		return '\0';
	}
	public final String substring(long startIndex, long endIndex) {
		return this.sourceText.substring((int)startIndex, (int)endIndex);
	}
}

