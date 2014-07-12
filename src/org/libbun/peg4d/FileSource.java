package org.libbun.peg4d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import org.libbun.Main;

public class FileSource extends ParserSource {
	private final static int PageSize = 4096;
	
	private RandomAccessFile file;
	private long buffer_offset;
	private byte[] buffer;
	public FileSource(String fileName) throws FileNotFoundException {
		super(fileName, 1);
		if(Main.VerbosePegMode) {
			System.out.println("random access file: " + fileName);
		}
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

	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	public final char charAt(long n) {
		long buffer_pos = (n - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(n);
			this.readBuffer(this.buffer_offset, this.buffer);
			buffer_pos = (n - this.buffer_offset);
		}
		return (char)this.buffer[(int)buffer_pos];
	}
	
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				long off_s = buffer_alignment(startIndex);
				long off_e = buffer_alignment(endIndex);
				if(off_s == off_e) {
					if(this.buffer_offset != off_s) {
						this.buffer_offset = off_s;
						this.readBuffer(this.buffer_offset, this.buffer);
					}
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
//			if(Main.VerbosePegMode) {
//				System.out.println("read buffer: " + pos + ", size = " + b.length);
//			}
			this.file.seek(pos);
			this.file.read(b);
			this.statIOCount += 1;
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
