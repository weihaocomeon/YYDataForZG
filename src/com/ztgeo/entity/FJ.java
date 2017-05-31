package com.ztgeo.entity;

import java.sql.Date;

public class FJ {
	private String DJSLBH;
	private String FJID;
	private String SLBH;
	private String FJXH;
	private String FJRQ;
	private String FJXX;
	private String JBR;
	private String SHR;
	private String FJLX;
	
	public FJ() {
	}

	

	public FJ(String dJSLBH, String fJID, String sLBH, String fJXH, String fJRQ, String fJXX, String jBR, String sHR,
			String fJLX) {
		super();
		DJSLBH = dJSLBH;
		FJID = fJID;
		SLBH = sLBH;
		FJXH = fJXH;
		FJRQ = fJRQ;
		FJXX = fJXX;
		JBR = jBR;
		SHR = sHR;
		FJLX = fJLX;
	}



	public String getDJSLBH() {
		return DJSLBH;
	}

	public void setDJSLBH(String dJSLBH) {
		DJSLBH = dJSLBH;
	}

	public String getFJID() {
		return FJID;
	}

	public void setFJID(String fJID) {
		FJID = fJID;
	}

	public String getSLBH() {
		return SLBH;
	}

	public void setSLBH(String sLBH) {
		SLBH = sLBH;
	}

	

	public String getFJXH() {
		return FJXH;
	}



	public void setFJXH(String fJXH) {
		FJXH = fJXH;
	}



	public String getFJRQ() {
		return FJRQ;
	}

	public void setFJRQ(String fJRQ) {
		FJRQ = fJRQ;
	}

	public String getFJXX() {
		return FJXX;
	}

	public void setFJXX(String fJXX) {
		FJXX = fJXX;
	}

	public String getJBR() {
		return JBR;
	}

	public void setJBR(String jBR) {
		JBR = jBR;
	}

	public String getSHR() {
		return SHR;
	}

	public void setSHR(String sHR) {
		SHR = sHR;
	}

	public String getFJLX() {
		return FJLX;
	}

	public void setFJLX(String fJLX) {
		FJLX = fJLX;
	}

	@Override
	public String toString() {
		return "FJ [DJSLBH=" + DJSLBH + ", FJID=" + FJID + ", SLBH=" + SLBH + ", FJXH=" + FJXH + ", FJRQ=" + FJRQ
				+ ", FJXX=" + FJXX + ", JBR=" + JBR + ", SHR=" + SHR + ", FJLX=" + FJLX + "]";
	}
	
	
}
