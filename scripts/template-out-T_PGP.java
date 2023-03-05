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