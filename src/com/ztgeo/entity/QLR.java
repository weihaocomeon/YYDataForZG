package com.ztgeo.entity;

import java.sql.Date;

public class QLR {
	private String ID;
	private String QLR;
	private String ZJLX;
	private String ZJH;
	private String GYQK;
	private String GYFE;
	private String DYCS;
	private String SLBH;
	private String FCSLBH;
	private Integer ZLZT;
	private Date UPDATETIME;
	
	public QLR() {
	}

	

	public QLR(String iD, String qLR, String zJLX, String zJH, String gYQK, String gYFE, String dYCS, String sLBH,
			String fCSLBH, Integer zLZT, Date uPDATETIME) {
		super();
		ID = iD;
		QLR = qLR;
		ZJLX = zJLX;
		ZJH = zJH;
		GYQK = gYQK;
		GYFE = gYFE;
		DYCS = dYCS;
		SLBH = sLBH;
		FCSLBH = fCSLBH;
		ZLZT = zLZT;
		UPDATETIME = uPDATETIME;
	}



	public String getZJH() {
		return ZJH;
	}



	public void setZJH(String zJH) {
		ZJH = zJH;
	}



	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getQLR() {
		return QLR;
	}

	public void setQLR(String qLR) {
		QLR = qLR;
	}

	public String getZJLX() {
		return ZJLX;
	}

	public void setZJLX(String zJLX) {
		ZJLX = zJLX;
	}

	public String getGYQK() {
		return GYQK;
	}

	public void setGYQK(String gYQK) {
		GYQK = gYQK;
	}

	public String getGYFE() {
		return GYFE;
	}

	public void setGYFE(String gYFE) {
		GYFE = gYFE;
	}

	public String getDYCS() {
		return DYCS;
	}

	public void setDYCS(String dYCS) {
		DYCS = dYCS;
	}

	public String getSLBH() {
		return SLBH;
	}

	public void setSLBH(String sLBH) {
		SLBH = sLBH;
	}

	public String getFCSLBH() {
		return FCSLBH;
	}

	public void setFCSLBH(String fCSLBH) {
		FCSLBH = fCSLBH;
	}

	public Integer getZLZT() {
		return ZLZT;
	}

	public void setZLZT(Integer zLZT) {
		ZLZT = zLZT;
	}

	public Date getUPDATETIME() {
		return UPDATETIME;
	}

	public void setUPDATETIME(Date uPDATETIME) {
		UPDATETIME = uPDATETIME;
	}

	@Override
	public String toString() {
		return "QLR [ID=" + ID + ", QLR=" + QLR + ", ZJLX=" + ZJLX + ", GYQK=" + GYQK + ", GYFE=" + GYFE + ", DYCS="
				+ DYCS + ", SLBH=" + SLBH + ", FCSLBH=" + FCSLBH + ", ZLZT=" + ZLZT + ", UPDATETIME=" + UPDATETIME
				+ "]";
	}

		
	
}
