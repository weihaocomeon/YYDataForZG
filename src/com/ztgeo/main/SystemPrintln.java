package com.ztgeo.main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class SystemPrintln {

	public SystemPrintln() {
		OutputStream textAreaStream = new OutputStream() {
			public void write(int b) throws IOException {
				StringStr.jta.append(String.valueOf((char) b));
			}
			public void write(byte b[]) throws IOException {
				StringStr.jta.append(new String(b));
			}
			public void write(byte b[], int off, int len) throws IOException {
				StringStr.jta.append(new String(b, off, len));
			}
		};

		PrintStream myOut = new PrintStream(textAreaStream);
		System.setOut(myOut);
		System.setErr(myOut);
	}
	
	
	
}
