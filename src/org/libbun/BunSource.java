package org.libbun;

public class BunSource {
	public final SourceLogger logger;
	String fileName;
	int    lineNumber;
	public String  sourceText;

	public BunSource(String fileName, int lineNumber, String sourceText, SourceLogger logger) {
		this.fileName = fileName;
		this.lineNumber = lineNumber;
		this.sourceText = sourceText;
		this.logger = logger;
	}

	public SourceToken newToken(Peg createdPeg, int startIndex, int endIndex) {
		return new SourceToken(createdPeg, this, startIndex, endIndex);
	}

	public SourceToken newToken(Peg createdPeg, int startIndex, int endIndex, String token) {
		return new SourceToken(createdPeg, this, startIndex, endIndex, token);
	}

	public final String substring(int startIndex, int endIndex) {
		return this.sourceText.substring(startIndex, endIndex);
	}

	public final char charAt(int n) {
		if(0 <= n && n < this.sourceText.length()) {
			return Main._GetChar(this.sourceText, n);
		}
		return '\0';
	}

	public final int getLineNumber(int Position) {
		int LineNumber = this.lineNumber;
		int i = 0;
		while(i < Position) {
			char ch = Main._GetChar(this.sourceText, i);
			if(ch == '\n') {
				LineNumber = LineNumber + 1;
			}
			i = i + 1;
		}
		return LineNumber;
	}

	public final int getIndentSize(int fromPosition) {
		int startPosition = this.getLineStartPosition(fromPosition);
		int length = 0;
		String s = this.sourceText;
		for(;startPosition < s.length();startPosition=startPosition+1) {
			char ch = Main._GetChar(s, startPosition);
			if(ch == '\t') {
				length = length + 8;
			}
			else if(ch == ' ') {
				length = length + 1;
			}
			else {
				break;
			}
		}
		return length;

	}

	public final int getLineStartPosition(int fromPostion) {
		String s = this.sourceText;
		int startIndex = fromPostion;
		if(!(startIndex < s.length())) {
			startIndex = s.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			char ch = Main._GetChar(s, startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getLineTextAt(int pos) {
		String s = this.sourceText;
		int startIndex = this.getLineStartPosition(pos);
		int endIndex = startIndex;
		while(endIndex < s.length()) {
			char ch = Main._GetChar(s, endIndex);
			if(ch == '\n') {
				break;
			}
			endIndex = endIndex + 1;
		}
		return s.substring(startIndex, endIndex);
	}

	public final String getMakerLine(int pos) {
		String s = this.sourceText;
		int startIndex = this.getLineStartPosition(pos);
		String markerLine = "";
		int i = startIndex;
		while(i < pos) {
			char ch = Main._GetChar(s, i);
			if(ch == '\n') {
				break;
			}
			if(ch == '\t') {
				markerLine = markerLine + "\t";
			}
			else {
				markerLine = markerLine + " ";
			}
			i = i + 1;
		}
		return markerLine + "^";
	}

	public final String formatErrorHeader(String error, int pos, String message) {
		return "(" + this.fileName + ":" + this.getLineNumber(pos) + ") [" + error +"] " + message;
	}

	public final String formatErrorMessage(String errorType, int pos, String msg) {
		String Line = this.getLineTextAt(pos);
		String Delim = "\n\t";
		if(Line.startsWith("\t") || Line.startsWith(" ")) {
			Delim = "\n";
		}
		String Header = this.formatErrorHeader(errorType, pos, msg);
		String Marker = this.getMakerLine(pos);
		msg = Header + Delim + Line + Delim + Marker;
		return msg;
	}

	public String checkFileName(String fileName) {
		int loc = this.fileName.lastIndexOf("/");
		if(loc > 0) {
			return this.fileName.substring(0, loc+1) + fileName; 
		}
		return fileName;
	}

//	public final int GetLineHeadPosition(int Position) {
//		String s = this.sourceText;
//		int StartIndex = 0;
//		int i = Position;
//		if(!(i < s.length())) {
//			i = s.length() - 1;
//		}
//		while(i >= 0) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\n') {
//				StartIndex = i + 1;
//				break;
//			}
//			i = i - 1;
//		}
//		return StartIndex;
//	}
//
//	public final int CountIndentSize(int Position) {
//		String s = this.sourceText;
//		int length = 0;
//		int i = Position;
//		while(i < s.length()) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\t') {
//				length = length + 8;
//			}
//			else if(ch == ' ') {
//				length = length + 1;
//			}
//			else {
//				break;
//			}
//			i = i + 1;
//		}
//		return length;
//	}
//
//	public final String GetLineText(int Position) {
//		String s = this.sourceText;
//		int StartIndex = 0;
//		int EndIndex = s.length();
//		int i = Position;
//		if(!(i < s.length())) {
//			i = s.length() - 1;
//		}
//		while(i >= 0) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\n') {
//				StartIndex = i + 1;
//				break;
//			}
//			i = i - 1;
//		}
//		i = Position;
//		while(i < s.length()) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\n') {
//				EndIndex = i;
//				break;
//			}
//			i = i + 1;
//		}
//		return s.substring(StartIndex, EndIndex);
//	}
//
//	public final String GetLineMarker(int Position) {
//		String s = this.sourceText;
//		int StartIndex = 0;
//		int i = Position;
//		if(!(i < s.length())) {
//			i = s.length() - 1;
//		}
//		while(i >= 0) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\n') {
//				StartIndex = i + 1;
//				break;
//			}
//			i = i - 1;
//		}
//		String Line = "";
//		i = StartIndex;
//		while(i < Position) {
//			char ch = Main._GetChar(s, i);
//			if(ch == '\n') {
//				break;
//			}
//			if(ch == '\t') {
//				Line = Line + "\t";
//			}
//			else {
//				Line = Line + " ";
//			}
//			i = i + 1;
//		}
//		return Line + "^";
//	}
//
//	public final String FormatErrorHeader(String Error, int Position, String Message) {
//		return "(" + this.fileName + ":" + this.getLineNumber(Position) + ") [" + Error +"] " + Message;
//	}
//
//	public final String FormatErrorMarker(String Error, int Position, String Message) {
//		String Line = this.GetLineText(Position);
//		String Delim = "\n\t";
//		if(Line.startsWith("\t") || Line.startsWith(" ")) {
//			Delim = "\n";
//		}
//		String Header = this.FormatErrorHeader(Error, Position, Message);
//		String Marker = this.GetLineMarker(Position);
//		Message = Header + Delim + Line + Delim + Marker;
//		return Message;
//	}


}
