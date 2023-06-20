package com.pttdigital.wtransfer;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pttdigital.wtransfer.ImportV2.OL;

public class Schedule {
	public String siteSource;
	public boolean useDynamicDirSource;
	public String fnDynamicDirSource;
	public String staticDirSource;
	public String siteTarget;
	public boolean useDynamicDirTarget;
	public String fnDynamicDirTarget;
	public String staticDirTarget;
	public int retryCount;
	public int retryIntervalMs;
	public String fnIsFileToMove;
	public String fnRenameTo;
	public String archiveFolder;
	public String fnArchiveRenameTo;
	public int workerThreads;
	public String workspace;
	public String name;
	public String description;
	public String pgpDirection;
	public String pgpKeyPath;
	public String pgpKeyPassword;
	public String fnPgpRenameTo;
	public String plan;
	public boolean enabled;
	public Timestamp previousCheckpoint;
	public Timestamp validFrom;
	public Timestamp validUntil;
	public Timestamp created;
	public Timestamp modified;
	public boolean isPendingAdHoc;
	
	public static final String wordPlan = "plan";
	
	private static HashMap<String, Integer> _mapMonth = null;
	private static HashMap<String, Integer> mapMonth() {
		if (_mapMonth == null) {
			_mapMonth = new HashMap<String, Integer>();
			_mapMonth.put("january", 1);
			_mapMonth.put("february", 2);
			_mapMonth.put("march", 3);
			_mapMonth.put("april", 4);
			_mapMonth.put("may", 5);
			_mapMonth.put("june", 6);
			_mapMonth.put("july", 7);
			_mapMonth.put("august", 8);
			_mapMonth.put("september", 9);
			_mapMonth.put("october", 10);
			_mapMonth.put("november", 11);
			_mapMonth.put("december", 12);
		}
		return _mapMonth;
	}
	
	private static HashMap<String, Integer> _mapDay = null;
	private static HashMap<String, Integer> mapDay() {
		if (_mapDay == null) {
			_mapDay = new HashMap<String, Integer>();
			_mapDay.put("sunday", 1);
			_mapDay.put("monday", 2);
			_mapDay.put("tuesday", 3);
			_mapDay.put("wednesday", 4);
			_mapDay.put("thursday", 5);
			_mapDay.put("friday", 6);
			_mapDay.put("saturday", 7);
		}
		return _mapDay;
	}
	
	public static boolean isTriggered(String planningString, Timestamp time_now, Timestamp time_prev) throws Exception {
		
		if (planningString.equals("*") || planningString.equals("*-*-* *:*:*")) {
			return true;
		}
		
		String[] sp1 = planningString.split(" ");
		
		if (sp1.length != 2) { throw new Exception("Unacceptable planning string \"" + planningString + "\", required in format \"yyyy-MM-dd HH:mm:ss\"."); }
		
		String[] sp2 = sp1[0].split("-");
		
		if (sp2.length != 3) { throw new Exception("Unacceptable planning string \"" + planningString + "\", required in format \"yyyy-MM-dd HH:mm:ss\"."); }

		String[] sp3 = sp1[1].split(":");
		
		if (sp3.length != 3) { throw new Exception("Unacceptable planning string \"" + planningString + "\", required in format \"yyyy-MM-dd HH:mm:ss\"."); }

		Calendar calendarPrev = Calendar.getInstance();
		Calendar calendarNow = Calendar.getInstance();
		
		calendarPrev.setTime(time_prev);
		calendarNow.setTime(time_now);
		
		long millisNow = calendarNow.getTimeInMillis();
		long millisPrev = calendarPrev.getTimeInMillis();
		long delta = millisNow - millisPrev;
		if (delta > 120 * 1000) {
			return false;
		} else {
			int hPrev = calendarPrev.get(Calendar.HOUR_OF_DAY);
			int mPrev = calendarPrev.get(Calendar.MINUTE);
			int sPrev = calendarPrev.get(Calendar.SECOND);
			int sDayPrev = 3600 * hPrev + 60 * mPrev + sPrev;

			int hNow = calendarNow.get(Calendar.HOUR_OF_DAY);
			int mNow = calendarNow.get(Calendar.MINUTE);
			int sNow = calendarNow.get(Calendar.SECOND);
			int sDayNow = 3600 * hNow + 60 * mNow + sNow;
			
			// day roll-over
			sDayPrev = sDayPrev > sDayNow ? sDayPrev - 3600 * 24 : sDayPrev;

			int now_year = calendarNow.get(Calendar.YEAR);
			int now_month = calendarNow.get(Calendar.MONTH) + 1;
			int dayOfWeek = calendarNow.get(Calendar.DAY_OF_WEEK);
			int currentDayOfMonth = calendarNow.get(Calendar.DAY_OF_MONTH);
			
			String planned_text_year = sp2[0];
			String planned_text_month = sp2[1];
			String planned_text_day = sp2[2];

			String planned_text_hour = sp3[0];
			String planned_text_minute = sp3[1];
			String planned_text_second = sp3[2];
			
			// -------------------------------------------------------------------------------- > year verification
			boolean ok_year = false;
			if (planned_text_year.equals("*")) {
				ok_year = true;
			} else {
				try {
					ok_year = Integer.parseInt(planned_text_year) == now_year;
				} catch (Exception ex) { throw new Exception("Error while verifying year plan: \"" + planned_text_year + "\" is not an integer."); }
			}
			// -------------------------------------------------------------------------------- > month verification
			boolean ok_month = false;
			if (planned_text_month.equals("*")) {
				ok_month = true;
			} else {
				int planned_month_number = -1; try { planned_month_number = Integer.parseInt(planned_text_month); } catch (Exception ex) { }
				if (planned_month_number < 0) {
					// jan, feb, mar, ...
					if (mapMonth().containsKey(planned_text_month.toLowerCase())) {
						planned_month_number = mapMonth().get(planned_text_month.toLowerCase());
						ok_month = planned_month_number == now_month;
					} else {
						throw new Exception("Error while verifying month plan \"" + planned_text_month + "\" is not valid.");
					}
				} else {
					// 1, 2, 3, ..., 12
					ok_month = planned_month_number == now_month;
				}
			}
			// -------------------------------------------------------------------------------- > day verification
			boolean ok_day = false;
			if (planned_text_day.equals("*")) {
				ok_day = true;
			} else {
				int planned_day_number = -1; try { planned_day_number = Integer.parseInt(planned_text_day); } catch (Exception ex) { }
				if (planned_day_number < 0) {
					// sunday, monday, tuesday, wednesday, thursday, friday, saturday
					if (mapDay().containsKey(planned_text_day.toLowerCase())) {
						planned_day_number = mapDay().get(planned_text_day.toLowerCase());
						ok_day = planned_day_number == dayOfWeek;
					} else {
						throw new Exception("Error while verifying day plan \"" + planned_text_day + "\" is not valid.");
					}
				} else {
					// 1, 2, 3, ..., 31
					ok_day = planned_day_number == currentDayOfMonth;
				}
			}
			// -------------------------------------------------------------------------------- > HH:mm:ss verification
			int planned_hour = -1;
			if (planned_text_hour.equals("*")) {
				planned_hour = hNow;
			} else {
				try { planned_hour = Integer.parseInt(planned_text_hour); } catch (Exception ex) { throw new Exception("Error while verifying hour plan \"" + planned_text_hour + "\" is not an integer, " + ex); }
			}
			int planned_minute = -1;
			if (planned_text_minute.equals("*")) {
				planned_minute = mNow;
			} else {
				try { planned_minute = Integer.parseInt(planned_text_minute); } catch (Exception ex) { throw new Exception("Error while verifying minute plan \"" + planned_text_minute + "\" is not an integer, " + ex); }
			}
			int planned_second = -1;
			try {
				planned_second = Integer.parseInt(planned_text_second);
			} catch (Exception ex) {
				throw new Exception("Error while verifying second plan \"" + planned_text_second + "\" is not an integer, " + ex);
			}
			
			int planned_sDay = 3600 * planned_hour + 60 * planned_minute + planned_second;
			
			return sDayPrev <= planned_sDay && planned_sDay < sDayNow && ok_year && ok_month && ok_day;
			
			// -------------------------------------------------------------------------------- > end
		}
	}
	
	public static void validatePlanningArray(String planningArray) throws Exception {
		// String planningArray = "*-april-* 00:00,*-*-* *:00:00,*-march-sunday 00:00:00,*-march-19 00:00:00";
		if (planningArray == null) {
			throw new Exception("Error validate planning array, \"planning\" cannot be null.");
		}
		
		long lnow = Calendar.getInstance().getTimeInMillis();
		Timestamp time_now = new Timestamp(lnow);
		Timestamp time_prev = new Timestamp(lnow - 10 * 1000);
		
		String[] sep = planningArray.split(",");
		for (String s : sep) {
			
			boolean b = isTriggered(s, time_now, time_prev);
			if ("".length() == 8756) { System.out.println(b); }
		}
	}

	public static String getPlanningArray(byte[] bodyRaw) throws Exception {
		JsonObject json = new Gson().fromJson(new String(bodyRaw), JsonObject.class);
		String plan = null; try { plan = json.get(wordPlan).getAsString(); } catch (Exception ex) { }
		
		
		return plan;
	}
}