package com.demo.ui;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ContextLoaderListener;

import com.demo.dto.ColumnDefinition;
import com.demo.service.DynamicTableService;
import com.demo.ui.validation.DecimalValidator;
import com.vaadin.addon.pagination.Pagination;
import com.vaadin.addon.pagination.PaginationChangeListener;
import com.vaadin.addon.pagination.PaginationResource;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Binder;
import com.vaadin.data.converter.DateToSqlDateConverter;
import com.vaadin.data.converter.LocalDateToDateConverter;
import com.vaadin.data.converter.StringToBigDecimalConverter;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.EnableVaadin;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.server.SpringVaadinServlet;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.HeaderRow;

@SpringUI
@Title("GUI Vaadin Demo")
@SuppressWarnings("serial")
@Theme("mytheme")
public class DemoUI extends UI {

	private static final int FIRST_PAGE = 1;
	private static final int LIMIT_PAGE = 5;
	private VerticalLayout mainLayout;
	private ComboBox<String> selectTableName;

	@Autowired
	private DynamicTableService dynamicTableService;

	@WebListener
	public static class DemoContextLoaderListener extends ContextLoaderListener {
	}

	@Configuration
	@EnableVaadin
	public static class DemoConfiguration {

	}

	@Override
	protected void init(VaadinRequest request) {
		mainLayout = new VerticalLayout();

		final ProgressBar bar = new ProgressBar();
		bar.setIndeterminate(true);
		mainLayout.addComponent(bar);
		bar.setVisible(false);

		try {
			List<String> tableNamesList = dynamicTableService.listTables();
			String defaultSelection = tableNamesList.get(0);
			buildComboCombox(defaultSelection, tableNamesList, bar);
			buildGrid(defaultSelection);
			setContent(mainLayout);
		} catch (Exception exp) {
			// @TODO
			// For Sure this will be handle not like this at all
			exp.printStackTrace();
		}

	}

	private void buildComboCombox(String defaultSelection, List<String> tableNamesList, final ProgressBar bar) throws SQLException {
		selectTableName = new ComboBox<>("Select table name");
		selectTableName.setItems(tableNamesList);
		selectTableName.setItemCaptionGenerator(name -> name);
		selectTableName.setEmptySelectionAllowed(false);

		selectTableName.setEmptySelectionCaption(defaultSelection);
		selectTableName.addSelectionListener(event -> {
			bar.setVisible(true);
			String tableName = selectTableName.getValue();

			Iterator<Component> iter = mainLayout.iterator();
			while (iter.hasNext()) {
				Component currentComponent = iter.next();
				if ("grid-layout-unique-id".equals(currentComponent.getId())) {
					mainLayout.removeComponent(currentComponent);
				}
			}
			buildGrid(tableName);
			bar.setVisible(false);
		});

		mainLayout.addComponent(selectTableName);
	}

	@WebServlet(urlPatterns = "/*", name = "DemoUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = DemoUI.class, productionMode = true)
	public static class DemoUIServlet extends SpringVaadinServlet {
	}

	private Grid<Map<ColumnDefinition, Object>> createGrid(List<Map<ColumnDefinition, Object>> data) {
		final Grid<Map<ColumnDefinition, Object>> grid = new Grid<>();
		buildFields(data, grid);
		grid.setWidth("100%");
		grid.setColumnReorderingAllowed(true);
		grid.setFrozenColumnCount(1);
		grid.getEditor().setEnabled(true);
		return grid;
	}

	private void buildFields(List<Map<ColumnDefinition, Object>> data, final Grid<Map<ColumnDefinition, Object>> grid) {

		ListDataProvider<Map<ColumnDefinition, Object>> dataProvider = new ListDataProvider<>(data);
		grid.setDataProvider(dataProvider);

		final HeaderRow headerRow = grid.appendHeaderRow();
		Binder<Map<ColumnDefinition, Object>> binder = grid.getEditor().getBinder();
		binder.setBean(data.get(0));
		for (Map.Entry<ColumnDefinition, Object> entry : data.get(0).entrySet()) {
			Column<Map<ColumnDefinition, Object>, ?> c = grid.addColumn(v -> v.get(entry.getKey()))
					.setCaption(entry.getKey().getColumnName()).setId(entry.getKey().getColumnName());

			switch (entry.getKey().getColumnDataType()) {
			case NUMBER:
				TextField tfAsNumber = new TextField();
				// tfAsNumber.setMaxLength(entry.getKey().getColumnSize());
				c.setEditorBinding(binder.forField(tfAsNumber).asRequired("Required")
						.withConverter(new StringToIntegerConverter("Must be Integer"))
						.bind(v -> Integer.parseInt(v.get(entry.getKey()) + ""), (k, v) -> k.put(entry.getKey(), v)));

				TextField filterTextField = new TextField();
				headerRow.getCell(c).setComponent(filterTextField);

				filterTextField.addValueChangeListener(event -> {
					dataProvider.setFilter(v -> filterTextField.getValue().isEmpty() ? true
							: String.valueOf(v.get(entry.getKey())).equalsIgnoreCase(filterTextField.getValue()));
					dataProvider.refreshAll();
				});

				break;
			case DATE:
				DateField df = new DateField();
				df.setDateFormat("yyyy-MM-dd");
				c.setEditorBinding(binder.forField(df).withConverter(new LocalDateToDateConverter())
						.withConverter(new DateToSqlDateConverter()).bind(
								v -> entry.getKey() == null || v.get(entry.getKey()) == null ? null
										: java.sql.Date.valueOf(LocalDate.parse(v.get(entry.getKey()) + "")
												.format(DateTimeFormatter.ISO_DATE)),
								(k, v) -> k.put(entry.getKey(), v)));

				DateField dfFilter = new DateField();
				dfFilter.setDateFormat("yyyy-MM-dd");
				headerRow.getCell(c).setComponent(dfFilter);

				dfFilter.addValueChangeListener(event -> {

					dataProvider.setFilter(
							v -> dfFilter.getValue() != null && entry.getKey() != null && v.get(entry.getKey()) != null
									? LocalDate.parse(v.get(entry.getKey()) + "", DateTimeFormatter.ISO_DATE).isEqual(
											dfFilter.getValue())
									: true);
					dataProvider.refreshAll();
				});

				break;
			case VARCHAR:
				TextField tfAsString = new TextField();
				tfAsString.setMaxLength(entry.getKey().getColumnSize());
				c.setEditorBinding(binder.forField(tfAsString).asRequired("Required")
						.bind(v -> v.get(entry.getKey()) + "", (k, v) -> k.put(entry.getKey(), v)));

				TextField tfFilter = new TextField();
				headerRow.getCell(c).setComponent(tfFilter);

				tfFilter.addValueChangeListener(event -> {
					dataProvider.setFilter(v -> tfFilter.getValue().isEmpty() ? true
							: String.valueOf(v.get(entry.getKey())).equalsIgnoreCase(tfFilter.getValue()));
					dataProvider.refreshAll();
				});

				break;
			case DECIMAL:
				c.setEditorBinding(binder.forField(new TextField()).asRequired("Required")
						.withValidator(new DecimalValidator(entry.getKey()))
						.withConverter(new StringToBigDecimalConverter("Only decimal number allowed here"))
						.bind(v -> new BigDecimal(v.get(entry.getKey()) + ""), (k, v) -> k.put(entry.getKey(), v)));
				break;

			default:
				System.out.println("unknow " + entry.getKey().getColumnDataType());
				break;
			}
		}
	}

	public void buildGrid(String tableName) {
		try {
			List<ColumnDefinition> tableColumns = dynamicTableService.getColumnDefinitions(tableName);
			List<Map<ColumnDefinition, Object>> dataMap = dynamicTableService.getData(tableName, tableColumns);

			final long TOTAL_PAGE = Long.valueOf(dataMap.size());
			final List<Map<ColumnDefinition, Object>> TABLE_DATA_LIMIT = dataMap.subList(0,
					dataMap.size() > LIMIT_PAGE ? LIMIT_PAGE : dataMap.size());
			final Grid<Map<ColumnDefinition, Object>> grid = createGrid(TABLE_DATA_LIMIT);
			final Pagination pagination = createPagination(TOTAL_PAGE, FIRST_PAGE, LIMIT_PAGE);

			pagination.addPageChangeListener(new PaginationChangeListener() {
				@Override
				public void changed(PaginationResource event) {

					ListDataProvider<Map<ColumnDefinition, Object>> dataProvider = new ListDataProvider<>(
							dataMap.subList(event.fromIndex(), event.toIndex()));
					grid.setDataProvider(dataProvider);
					grid.scrollToStart();
				}
			});

			GridLayout gridLayout = new GridLayout();
			gridLayout.setWidth("100%");
			gridLayout.setId("grid-layout-unique-id");

			gridLayout.addComponent(grid);
			gridLayout.addComponent(pagination);
			mainLayout.addComponent(gridLayout);
		} catch (Exception exp) {
			// @TODO
			// For Sure this will be handle not like this at all
			exp.printStackTrace();
		}
	}

	private Pagination createPagination(long total, int page, int limit) {
		final PaginationResource paginationResource = PaginationResource.newBuilder().setTotal(total).setPage(page)
				.setLimit(limit).build();
		final Pagination pagination = new Pagination(paginationResource);
		pagination.setItemsPerPage(10, 20, 50, 100);
		return pagination;
	}

}
