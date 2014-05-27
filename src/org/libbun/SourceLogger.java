// ***************************************************************************
// Copyright (c) 2013-2014, Libbun project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

package org.libbun;


public final class SourceLogger {

	public UniArray<String>  ReportedErrorList = new UniArray<String>(new String[10]);

	public final void log(String Message) {
		this.ReportedErrorList.add(Message);
	}
//
//	public final static void _LogErrorExit(SourceToken Token, String Message) {
//		if(Token != null && Token.source != null) {
//			Message = Token.source.formatErrorMarker("error", Token.startIndex, Message);
//			Token.source.logger.log(Message);
//		}
//		else {
//			Main._Exit(1, Message);
//		}
//	}
//
//	public final static String _LogError(SourceToken Token, String Message) {
//		if(Token != null && Token.source != null) {
//			Message = Token.source.FormatErrorMarker("error", Token.startIndex, Message);
//			Token.source.logger.log(Message);
//			int loc = Message.indexOf("\n");
//			if(loc > 0) {
//				Message = Message.substring(0, loc);
//			}
//		}
//		return Message;
//	}
//
//	public final static void _LogWarning(SourceToken Token, String Message) {
//		if(Token != null) {
//			Message = Token.source.FormatErrorMarker("warning", Token.startIndex, Message);
//			Token.source.logger.log(Message);
//		}
//	}
//
//	public final static void _LogInfo(SourceToken Token, String Message) {
//		if(Token != null && Token.source != null) {
//			Message = Token.source.FormatErrorMarker("info", Token.startIndex, Message);
//			Token.source.logger.log(Message);
//		}
//	}
//
//	public final static void _LogDebug(SourceToken Token, String Message) {
//		if(Token != null && Token.source != null) {
//			Message = Token.source.FormatErrorMarker("debug", Token.startIndex, Message);
//			Token.source.logger.log(Message);
//		}
//	}

	public final String[] GetReportedErrors() {
		UniArray<String> List = this.ReportedErrorList;
		this.ReportedErrorList = new UniArray<String>(new String[10]);
		return List.compactArray();
	}

	public final void OutputErrorsToStdErr() {
		String[] Messages = this.GetReportedErrors();
		int i = 0;
		while(i < Messages.length) {
			Main._PrintLine(Messages[i]);
			i = i + 1;
		}
	}

}

