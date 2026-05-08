package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtils {
	
	public static Date strToDate(String format, String date) throws ParseException {
		return new SimpleDateFormat(format).parse(date);
	}
	
	public static Date toDate(String date) throws ParseException {
		return strToDate("yyyy-MM-dd", date);
	}
	
	public static String toFormattedDateNow(String format) {
		return new SimpleDateFormat(format).format(new Date());
	}
	
	public static String toFormattedDateNow() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()
		        + TimeUnit.MINUTES.toMillis(2)));
	}
	
	public static void main(String[] args) throws ParseException {
		System.out.println(toFormattedDateNow("yyyy-MM-dd HH:mm:ss"));
		System.out.println(strToDate("yyyy", "1992"));
		System.out.println(strToDate("yyyy-MM", "1992-12"));
		System.out.println(strToDate("yyyy-MM-dd", "1992-12-01"));
	}
}
