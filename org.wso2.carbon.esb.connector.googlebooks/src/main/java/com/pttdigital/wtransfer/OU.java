package com.pttdigital.wtransfer;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;

public class OU {
	
	public static Timestamp now;
	
	public static ArrayList<Field> getFields(Class<?> c) {
		ArrayList<Field> afs = new ArrayList<Field>();
		Field[] fs = c.getDeclaredFields();
		for (int i = 0; i < fs.length; i++) {
			if (fs[i].getModifiers() == 1) {
				afs.add(fs[i]);
			}
		}
		return afs;
	}
}