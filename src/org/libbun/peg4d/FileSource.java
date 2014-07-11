package org.libbun.peg4d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

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
		this.readBuffer();
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
			this.readBuffer();
			buffer_pos = (n - this.buffer_offset);
		}
		return (char)this.buffer[(int)buffer_pos];
	}
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				this.file.seek(startIndex);
				byte[] b = new byte[(int)(endIndex - startIndex)];
				this.file.read(b);
				return new String(b, "UTF8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	private void readBuffer() {
		try {
			System.out.println("read buffer: " + this.buffer_offset);
			this.file.seek(this.buffer_offset);
			this.file.read(this.buffer, 0, 4096);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
