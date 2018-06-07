package com.demo.service;

import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.demo.dto.ColumnDataType;
import com.demo.dto.ColumnDefinition;

@Service
public class DynamicTableService {

	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	@Value("${schemaName}")
	private String schemaName;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");

	@PostConstruct
	public void init() {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public List<String> listTables() throws SQLException {
		List<String> tableNames = new ArrayList<>();
		DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
		try (ResultSet rs = metaData.getTables(null, "DEMO", "%", new String[] { "TABLE" });) {
			while (rs.next()) {
				tableNames.add(rs.getString("TABLE_NAME"));
			}
		}
		return tableNames;
	}

	/**
	 * 
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public List<ColumnDefinition> getColumnDefinitions(String tableName) throws Exception {
		List<ColumnDefinition> columnDefinitions = new ArrayList<ColumnDefinition>();
		DatabaseMetaData meta = jdbcTemplate.getDataSource().getConnection().getMetaData();
		try (ResultSet rsColumns = meta.getColumns(null, null, tableName, null)) {
			while (rsColumns.next()) {
				ColumnDefinition columnDefinition = new ColumnDefinition();
				columnDefinition.setColumnName(rsColumns.getString("COLUMN_NAME"));
				int columnSize = rsColumns.getInt("COLUMN_SIZE");
				columnDefinition.setColumnSize(columnSize);
				String columnType = rsColumns.getString("TYPE_NAME");
				setColumnDataType(columnDefinition, columnSize, columnType);
				columnDefinitions.add(columnDefinition);
			}
		} catch (Exception exp) {
			throw exp;
		}
		return columnDefinitions;
	}

	private void setColumnDataType(ColumnDefinition columnDefinition, int columnSize, String columnType) {
		if (columnType.contains("NUMBER") && columnSize == 0) {
			columnDefinition.setColumnDataType(ColumnDataType.NUMBER);
		} else if (columnType.contains("NUMBER") && columnSize > 0) {
			columnDefinition.setColumnDataType(ColumnDataType.DECIMAL);
		} else if (columnType.equals("DATE")) {
			columnDefinition.setColumnDataType(ColumnDataType.DATE);
		} else if (columnType.contains("TIMESTAMP")) {
			columnDefinition.setColumnDataType(ColumnDataType.DATETIME);
		} else if (columnType.contains("VARCHAR")) {
			columnDefinition.setColumnDataType(ColumnDataType.VARCHAR);
		}
	}

	/**
	 * 
	 * @param tableName
	 * @param columnDefinitions
	 * @return
	 * @throws Exception
	 */
	public List<Map<ColumnDefinition, Object>> getData(String tableName, List<ColumnDefinition> columnDefinitions)
			throws Exception {

		List<Map<ColumnDefinition, Object>> dataMap = new ArrayList<>();

		String seperatedColumns = getSeperatedColumns(columnDefinitions);
		String orderBy = columnDefinitions.get(0).getColumnName();

		try (final PreparedStatement stmt = jdbcTemplate.getDataSource().getConnection()
				.prepareStatement(String.format("select %s from %s order by %s", seperatedColumns, tableName, orderBy));
				final ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				Map<ColumnDefinition, Object> map = new LinkedHashMap<>();
				for (ColumnDefinition columnDefinition : columnDefinitions) {
					Object dataValue = getDataValue(rs, columnDefinition);
					map.put(columnDefinition, dataValue);
				}
				dataMap.add(map);
			}
		}
		return dataMap;
	}

	private Object getDataValue(final ResultSet rs, ColumnDefinition columnDefinition) throws SQLException {
		Object dataValue = rs.getObject(columnDefinition.getColumnName());
		if (columnDefinition.getColumnDataType().equals(ColumnDataType.DATE)) {
			return DATE_FORMAT.format(rs.getDate(columnDefinition.getColumnName()));
		} else if (columnDefinition.getColumnDataType().equals(ColumnDataType.DATETIME)) {
			return DATE_TIME_FORMAT.format(new Date(rs.getTimestamp(columnDefinition.getColumnName()).getTime()));
		}
		return dataValue;
	}

	/**
	 * 
	 * @param columnDefinitions
	 * @return
	 */
	private String getSeperatedColumns(List<ColumnDefinition> columnDefinitions) {
		StringBuilder seperatedColumnNames = new StringBuilder();
		for (ColumnDefinition columnDefinition : columnDefinitions) {
			seperatedColumnNames.append(columnDefinition.getColumnName());
			seperatedColumnNames.append(",");
		}
		return seperatedColumnNames.substring(0, seperatedColumnNames.length() - 1).toString();
	}

}
