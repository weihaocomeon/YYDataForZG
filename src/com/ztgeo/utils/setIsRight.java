package com.ztgeo.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class setIsRight {
	public static int setIsRight(ResultSet set){
		int count = 0;
		try {
			while(set.next()){
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}
}
