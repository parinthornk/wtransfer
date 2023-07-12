package org.wso2.carbon.esb.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;

public class ZConnector {
	
	public static class Constant {
		/*public static final String HOST = "localhost";
		public static final int PORT = 2022;
		public static final String DATABASE = "postgres";
		public static final String USERNAME = "postgres";
		public static final String PASSWORD = "postgres";
		public static final String SCHEMA = "public";*/
		
		/*public static final String Q_HOST = "localhost";
		public static final int Q_PORT = 5672;
		public static final String Q_USERNAME = "guest";
		public static final String Q_PASSWORD = "guest*/
		
		public static final String HOST = "10.224.146.29";
		public static final int PORT = 5551;
		public static final String DATABASE = "sharedev";
		public static final String USERNAME = "apimadmin";
		public static final String PASSWORD = "u7sHyI1$";
		public static final String SCHEMA = "wtransfer";
		
		/*public static final String Q_HOST = "10.224.183.19";
		public static final int Q_PORT = 5672;
		public static final String Q_USERNAME = "zparinthornk";
		public static final String Q_PASSWORD = "es4HBYTR";*/
		
		public static final String Q_HOST = "localhost";
		public static final int Q_PORT = 5672;
		public static final String Q_USERNAME = "guest";
		public static final String Q_PASSWORD = "guest";
		
		public static final String WTRANSFER_API_ENDPOINT = "http://10.224.143.44:8290/wtransfer";
		
		public static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
		
		public static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024;
		public static final String CUSTOM_FILE_SERVER_ENDPOINT = "http://127.0.0.1:62580/custom-file-server/site-operations";
	}
	
	public static class ZResult {
		public int statusCode = 204;
		public String content = null;
		public HashMap<String, String> headers = new HashMap<String, String>(); 
		public static ZResult ERROR_501(String method, String path) {
			ZResult ret = new ZResult();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("error", "The requested operation ["+method.toUpperCase()+", "+path+"] is not recognized.");
			ret.content = ZConnector.ConvertToJsonString(m1);
			ret.statusCode = 501;
			return ret;
		}
		public static ZResult ERROR_500(Exception ex) {
			ZResult ret = new ZResult();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("error", ex.getMessage());
			ret.content = ZConnector.ConvertToJsonString(m1);
			ret.statusCode = 500;
			return ret;
		}
		public static ZResult ERROR_400(Exception ex) {
			ZResult ret = new ZResult();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("error", ex.getMessage());
			ret.content = ZConnector.ConvertToJsonString(m1);
			ret.statusCode = 400;
			return ret;
		}
		public static ZResult ERROR_501(Exception ex) {
			ZResult ret = new ZResult();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("error", ex.getMessage());
			ret.content = ZConnector.ConvertToJsonString(m1);
			ret.statusCode = 501;
			return ret;
		}
		public static ZResult OK_200(String json) {
			ZResult ret = new ZResult();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put("message", "" + json);
			ret.content = json;
			ret.statusCode = 200;
			return ret;
		}
		public static ZResult OK_204() {
			ZResult ret = new ZResult();
			ret.statusCode = 204;
			return ret;
		}
	}
	
	public static class ZObject {
		public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException { return null; }
		public static ZResult execute(ZObject z) {
			Connection c = null;
			Statement stmt = null;
			ResultSet rs = null;
			ZResult ret = new ZResult();
			try {
/*
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<version>42.5.1</version>
</dependency>
*/
				Class.forName("org.postgresql.Driver");
				c = DriverManager.getConnection("jdbc:postgresql://"+Constant.HOST+":"+Constant.PORT+"/"+Constant.DATABASE+"", ""+Constant.USERNAME+"", ""+Constant.PASSWORD+"");
				stmt = c.createStatement();
				if ("".length() != 0) { rs = stmt.executeQuery(""); }
				ret = z.run(c, stmt, rs);
			} catch (Exception e) {
				Map<String, Object> m1 = new HashMap<String, Object>();
				m1.put("error", e + "");
				ret.content = ConvertToJsonString(m1);
				ret.statusCode = 500;
			}
			try { rs.close(); } catch (Exception ex) { }
			try { stmt.close(); } catch (Exception ex) { }
			try { c.close(); } catch (Exception ex) { }
			return ret;
		}
	}
	
	/*private static String specialChars = "$*&+,/:;=?@ <>#{}|\\^~[]`'\"";
	public static void validateSpecialcharacters(String text) throws SQLException {
		int len = specialChars.length();
		for (int i=0;i<len;i++) {
			char c = specialChars.charAt(i);
			if (text.contains(c + "")) {
				throw new SQLException("Invalid character \""+c+"\" in \""+text+"\" ."); 
			}
		}
	}*/
	
	public static String ConvertToJsonString(Object object) {
	    //return new GsonBuilder().setPrettyPrinting().create().toJson(object);
		
		return new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(object);
	}
	
	public static class T_WKS {
		public static ZResult list() {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_WKS";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_WKS where name = '" + name + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_WKS \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String name, final String description) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (name != null) { mh.put("name", name); }
					if (description != null) { mh.put("description", description); }

					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_WKS ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						rName += name;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("name", rName);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String name, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("name") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_WKS SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_WKS \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_WKS where name = '" + name + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_WKS \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_SIT {
		public static ZResult list(final String workspace) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_SIT where workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("host", resultSet.getString("host"));
						m1.put("port", resultSet.getInt("port"));
						m1.put("protocol", resultSet.getString("protocol"));
						m1.put("rootfolder", resultSet.getString("rootfolder"));
						m1.put("username", resultSet.getString("username"));
						m1.put("password", resultSet.getString("password"));
						m1.put("keypath", resultSet.getString("keypath"));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_SIT where name = '" + name + "' and workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("host", resultSet.getString("host"));
						m1.put("port", resultSet.getInt("port"));
						m1.put("protocol", resultSet.getString("protocol"));
						m1.put("rootfolder", resultSet.getString("rootfolder"));
						m1.put("username", resultSet.getString("username"));
						m1.put("password", resultSet.getString("password"));
						m1.put("keypath", resultSet.getString("keypath"));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SIT \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final String name, final String description, final String host, final int port, final String protocol, final String rootfolder, final String username, final String password, final String keypath) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (name != null) { mh.put("name", name); }
					if (description != null) { mh.put("description", description); }
					if (host != null) { mh.put("host", host); }
					if (protocol != null) { mh.put("protocol", protocol); }
					if (rootfolder != null) { mh.put("rootfolder", rootfolder); }
					if (username != null) { mh.put("username", username); }
					if (password != null) { mh.put("password", password); }
					if (keypath != null) { mh.put("keypath", keypath); }
					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_SIT ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						rName += name;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("name", rName);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final String name, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("name") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_SIT SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "' and workspace = '" + workspace + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SIT \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_SIT where name = '" + name + "' and workspace = '" + workspace + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SIT \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_PGP {
		public static ZResult list(final String workspace) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_PGP where workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("direction", resultSet.getString("direction"));
						m1.put("keypath", resultSet.getString("keypath"));
						m1.put("keypassword", resultSet.getString("keypassword"));
						m1.put("fnrenameto", resultSet.getString("fnrenameto"));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_PGP where name = '" + name + "' and workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("direction", resultSet.getString("direction"));
						m1.put("keypath", resultSet.getString("keypath"));
						m1.put("keypassword", resultSet.getString("keypassword"));
						m1.put("fnrenameto", resultSet.getString("fnrenameto"));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_PGP \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final String name, final String description, final String direction, final String keypath, final String keypassword, final String fnrenameto) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (name != null) { mh.put("name", name); }
					if (description != null) { mh.put("description", description); }
					if (direction != null) { mh.put("direction", direction); }
					if (keypath != null) { mh.put("keypath", keypath); }
					if (keypassword != null) { mh.put("keypassword", keypassword); }
					if (fnrenameto != null) { mh.put("fnrenameto", fnrenameto); }
					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_PGP ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						rName += name;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("name", rName);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final String name, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("name") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_PGP SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "' and workspace = '" + workspace + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_PGP \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_PGP where name = '" + name + "' and workspace = '" + workspace + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_PGP \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_CFG {
		public static ZResult list(final String workspace) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_CFG where workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("replace", resultSet.getInt("replace"));
						m1.put("deleteaftersuccess", resultSet.getInt("deleteaftersuccess"));
						m1.put("retrycount", resultSet.getInt("retrycount"));
						m1.put("retryintervalms", resultSet.getInt("retryintervalms"));
						m1.put("fnisfilenametomove", resultSet.getString("fnisfilenametomove"));
						m1.put("fnrenameto", resultSet.getString("fnrenameto"));
						m1.put("archivefolder", resultSet.getString("archivefolder"));
						m1.put("fnarcrenameto", resultSet.getString("fnarcrenameto"));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_CFG where name = '" + name + "' and workspace = '" + workspace + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("replace", resultSet.getInt("replace"));
						m1.put("deleteaftersuccess", resultSet.getInt("deleteaftersuccess"));
						m1.put("retrycount", resultSet.getInt("retrycount"));
						m1.put("retryintervalms", resultSet.getInt("retryintervalms"));
						m1.put("fnisfilenametomove", resultSet.getString("fnisfilenametomove"));
						m1.put("fnrenameto", resultSet.getString("fnrenameto"));
						m1.put("archivefolder", resultSet.getString("archivefolder"));
						m1.put("fnarcrenameto", resultSet.getString("fnarcrenameto"));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_CFG \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final String name, final String description, final int replace, final int deleteaftersuccess, final int retrycount, final int retryintervalms, final String fnisfilenametomove, final String fnrenameto, final String archivefolder, final String fnarcrenameto) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (name != null) { mh.put("name", name); }
					if (description != null) { mh.put("description", description); }
					if (fnisfilenametomove != null) { mh.put("fnisfilenametomove", fnisfilenametomove); }
					if (fnrenameto != null) { mh.put("fnrenameto", fnrenameto); }
					if (archivefolder != null) { mh.put("archivefolder", archivefolder); }
					if (fnarcrenameto != null) { mh.put("fnarcrenameto", fnarcrenameto); }
					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_CFG ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						rName += name;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("name", rName);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final String name, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("name") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_CFG SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "' and workspace = '" + workspace + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_CFG \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_CFG where name = '" + name + "' and workspace = '" + workspace + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_CFG \"" +name + "\" does not exist for [workspace = \"" + workspace + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_TSK {
		public static ZResult list(final String workspace, final String source, final String target, final String pgp, final String config) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_TSK where workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("source", resultSet.getString("source"));
						m1.put("target", resultSet.getString("target"));
						m1.put("pgp", resultSet.getString("pgp"));
						m1.put("config", resultSet.getString("config"));
						m1.put("id", resultSet.getInt("id"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("status", resultSet.getString("status"));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final String source, final String target, final String pgp, final String config, final int id) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_TSK where id = '" + id + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("source", resultSet.getString("source"));
						m1.put("target", resultSet.getString("target"));
						m1.put("pgp", resultSet.getString("pgp"));
						m1.put("config", resultSet.getString("config"));
						m1.put("id", resultSet.getInt("id"));
						m1.put("description", resultSet.getString("description"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("status", resultSet.getString("status"));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_TSK \"" +id + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final String source, final String target, final String pgp, final String config, final String description, final String status) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (source != null) { mh.put("source", source); }
					if (target != null) { mh.put("target", target); }
					if (pgp != null) { mh.put("pgp", pgp); }
					if (config != null) { mh.put("config", config); }
					if (description != null) { mh.put("description", description); }
					if (status != null) { mh.put("status", status); }
					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_TSK ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning id";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String id = resultSet.getString("id");
						rName += id;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("id", Long.parseLong(rName));
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final String source, final String target, final String pgp, final String config, final int id, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("id") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_TSK SET " + l + dsffdfds + "modified = '" +now + "' WHERE id = '" + id + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_TSK \"" +id + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final String source, final String target, final String pgp, final String config, final int id) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_TSK where id = '" + id + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_TSK \"" +id + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_LOG {
		public static ZResult list(final String workspace, final int task) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_LOG where workspace = '" + workspace + "' and task = '" + task + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("task", resultSet.getInt("task"));
						m1.put("id", resultSet.getInt("id"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("logtype", resultSet.getString("logtype"));
						m1.put("title", resultSet.getString("title"));
						m1.put("body", resultSet.getString("body"));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final int task, final int id) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_LOG where id = '" + id + "' and workspace = '" + workspace + "' and task = '" + task + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("task", resultSet.getInt("task"));
						m1.put("id", resultSet.getInt("id"));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						m1.put("logtype", resultSet.getString("logtype"));
						m1.put("title", resultSet.getString("title"));
						m1.put("body", resultSet.getString("body"));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_LOG \"" +id + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final int task, final String logtype, final String title, final String body) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (logtype != null) { mh.put("logtype", logtype); }
					if (title != null) { mh.put("title", title); }
					if (body != null) { mh.put("body", body); }
					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_LOG ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning id";
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String id = resultSet.getString("id");
						rName += id;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("id", Long.parseLong(rName));
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final int task, final int id, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("id") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_LOG SET " + l + dsffdfds + "modified = '" +now + "' WHERE id = '" + id + "' and workspace = '" + workspace + "' and task = '" + task + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_LOG \"" +id + "\" does not exist for [workspace = \"" + workspace + "\" and task = \"" + task + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final int task, final int id) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_LOG where id = '" + id + "' and workspace = '" + workspace + "' and task = '" + task + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_LOG \"" +id + "\" does not exist for [workspace = \"" + workspace + "\" and task = \"" + task + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}

	public static class T_SCH {
		public static ZResult list(final String workspace, final String source, final String target, final String pgp, final String config) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_SCH where workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("source", resultSet.getString("source"));
						m1.put("target", resultSet.getString("target"));
						m1.put("pgp", resultSet.getString("pgp"));
						m1.put("config", resultSet.getString("config"));
						m1.put("plan", resultSet.getString("plan"));
						m1.put("validfrom", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validfrom")));
						m1.put("validuntil", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validuntil")));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						ls1.add(m1);
					}
					Map<String, Object> m2 = new HashMap<String, Object>();
					m2.put("list", ls1);
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(m2);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult get(final String workspace, final String source, final String target, final String pgp, final String config, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "select * from " + Constant.SCHEMA + ".T_SCH where name = '" + name + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					resultSet = statement.executeQuery(sql);
					ArrayList<Object> ls1 = new ArrayList<Object>();
					while (resultSet.next()) {
						Map<String, Object> m1 = new HashMap<String, Object>();
						m1.put("workspace", resultSet.getString("workspace"));
						m1.put("name", resultSet.getString("name"));
						m1.put("description", resultSet.getString("description"));
						m1.put("source", resultSet.getString("source"));
						m1.put("target", resultSet.getString("target"));
						m1.put("pgp", resultSet.getString("pgp"));
						m1.put("config", resultSet.getString("config"));
						m1.put("plan", resultSet.getString("plan"));
						m1.put("validfrom", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validfrom")));
						m1.put("validuntil", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("validuntil")));
						m1.put("created", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("created")));
						m1.put("modified", new SimpleDateFormat(Constant.DATEFORMAT).format(resultSet.getTimestamp("modified")));
						ls1.add(m1);
					}
					
					if (ls1.size() == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SCH \"" +name + "\" does not exist.");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					
					ZResult ret = new ZResult();
					ret.content = ConvertToJsonString(ls1.get(0));
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult create(final String workspace, final String name, final String description, final String source, final String target, final String pgp, final String config, final String plan) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					Map<String, Object> mh = new HashMap<String, Object>();
					if (workspace != null) { mh.put("workspace", workspace); }
					if (name != null) { mh.put("name", name); }
					if (description != null) { mh.put("description", description); }
					if (source != null) { mh.put("source", source); }
					if (target != null) { mh.put("target", target); }
					if (pgp != null) { mh.put("pgp", pgp); }
					if (config != null) { mh.put("config", config); }
					if (plan != null) { mh.put("plan", plan); }

					Set<String> keys = mh.keySet();
					String skeys = "";
					String svalues = "";
					for (String key : keys) {
						skeys += key;
						svalues += "'" + mh.get(key) + "'";
						skeys += ", ";
						svalues += ", ";
					}
					String sql = "insert into " + Constant.SCHEMA + ".T_SCH ("+skeys+"created, modified) values ("+svalues+"'"+now+"', '"+now+"') returning name";
					System.out.println(sql);
					resultSet = statement.executeQuery(sql);
					String rName = "";
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						rName += name;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("name", rName);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 201;
					return ret;
				}
			});
		}
		
		public static ZResult update(final String workspace, final String source, final String target, final String pgp, final String config, final String name, final HashMap<String, Object> params) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis());
					String l = "";
					Set<String> keys = params.keySet();
					int count = keys.size();
					int i = 0;
					for (String key : keys) {
						String fieldName = key.toLowerCase();
						if (fieldName.compareToIgnoreCase("name") == 0) {
							System.out.println("Primary field cannot be modified. The requested field: \""+fieldName+"\" was ignored.");
							i++;
							continue;
						}
						Object fieldValue = params.get(key);
						if (fieldValue == null) {
							i++;
							continue;
						}
						l += fieldName + " = " + "'" +fieldValue + "'";
						if (i == count - 1) {
						} else {
							l += ", ";
						}
						i++;
					}
					String dsffdfds = count > 0 ? ", " : "";
					String sql = "UPDATE " + Constant.SCHEMA + ".T_SCH SET " + l + dsffdfds + "modified = '" +now + "' WHERE name = '" + name + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					int updated = statement.executeUpdate(sql);
					if (updated == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SCH \"" +name + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("updated", updated);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
		
		public static ZResult delete(final String workspace, final String source, final String target, final String pgp, final String config, final String name) {
			return ZObject.execute(new ZObject() {
				@Override
				public ZResult run(Connection connection, Statement statement, ResultSet resultSet) throws SQLException {
					String sql = "delete from " + Constant.SCHEMA + ".T_SCH where name = '" + name + "' and workspace = '" + workspace + "' and source = '" + source + "' and target = '" + target + "' and pgp = '" + pgp + "' and config = '" + config + "'";
					int deleted = statement.executeUpdate(sql);
					if (deleted == 0) {
						ZResult ret = new ZResult();
						HashMap<String, Object> m1 = new HashMap<String, Object>();
						m1.put("error", "T_SCH \"" +name + "\" does not exist for [workspace = \"" + workspace + "\" and source = \"" + source + "\" and target = \"" + target + "\" and pgp = \"" + pgp + "\" and config = \"" + config + "\"].");
						ret.content = ConvertToJsonString(m1);
						ret.statusCode = 404;
						return ret;
					}
					ZResult ret = new ZResult();
					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put("deleted", deleted);
					ret.content = ConvertToJsonString(m1);
					ret.statusCode = 200;
					return ret;
				}
			});
		}
	}
}