package org.libbun.peg4d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import org.libbun.Main;

public class FileSource extends ParserSource {
	private RandomAccessFile file;
	private long buffer_offset;
	private byte[] buffer;
	public FileSource(String fileName) throws FileNotFoundException {
		super(fileName, 1);
		System.out.println("random access file: " + fileName);
		this.file = new RandomAccessFile(fileName, "r");
		this.buffer = new byte[4096];
		this.buffer_offset = 0;
		this.readBuffer(this.buffer_offset, this.buffer);
	}
	public final long length() {
		try {
			return this.file.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	public final char charAt(long n) {
		long buffer_pos = (n - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < 4096)) {
			this.buffer_offset = (n / 4096) * 4096;
			this.readBuffer(this.buffer_offset, this.buffer);
			buffer_pos = (n - this.buffer_offset);
		}
		return (char)this.buffer[(int)buffer_pos];
	}
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				if(this.buffer_offset <= startIndex && endIndex < this.buffer_offset + 4096) {
						return new String(this.buffer, (int)(startIndex - this.buffer_offset), (int)(endIndex - startIndex), "UTF8");
				}
				else {
					byte[] b = new byte[(int)(endIndex - startIndex)];
					this.readBuffer(startIndex, b);
						return new String(b, "UTF8");
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	private void readBuffer(long pos, byte[] b) {
		try {
			System.out.println("read buffer: " + pos + ", size = " + b.length);
			this.file.seek(pos);
			this.file.read(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public final ParserSource trim(long startIndex, long endIndex) {
		//long pos = this.getLineStartPosition(startIndex);
		long linenum = this.getLineNumber(startIndex);
		String s = this.substring(startIndex, endIndex);
		//System.out.println("trim: " + linenum + " : " + s);
		return new StringSource(this.fileName, linenum, s);
	}

}
