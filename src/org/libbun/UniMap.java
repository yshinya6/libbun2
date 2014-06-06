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

import java.util.HashMap;

public final class UniMap <T> {
	final HashMap<String, T>	m;

	public UniMap() {
		this.m = new HashMap<String, T>();
	}

//	public UniMap(int TypeId, T[] Literal) {
//		this.m = new HashMap<String, T>();
//		int i = 0;
//		while(i < Literal.length) {
//			this.m.put(Literal[i].toString(), Literal[i+1]);
//			i = i + 2;
//		}
//	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int i = 0;
		for(String Key : this.m.keySet()) {
			if(i > 0) {
				sb.append(", ");
			}
			sb.append(this.stringify(Key));
			sb.append(" : ");
			sb.append(this.stringify(this.m.get(Key)));
			i++;
		}
		sb.append("}");
		return sb.toString();
	}

	protected String stringify(Object Value) {
		if(Value instanceof String) {
			return UniCharset._QuoteString((String) Value);
		}
		return Value.toString();
	}

	public final void put(String key, T value) {
		this.m.put(key, value);
	}

	public final T get(String key) {
		return this.m.get(key);
	}

	public final T get(String key, T defaultValue) {
		T Value = this.m.get(key);
		if(Value == null) {
			return defaultValue;
		}
		return Value;
	}

	public final void remove(String Key) {
		this.m.remove(Key);
	}

	public final boolean hasKey(String Key) {
		return this.m.containsKey(Key);
	}

	public final UniArray<String> keys() {
		UniArray<String> a = new UniArray<String>(new String[this.m.size()]);
		for(String k : this.m.keySet()) {
			a.add(k);
		}
		return a;
	}

	public final int size() {
		return this.m.size();
	}
}
