package com.demo.ui.validation;

import com.demo.dto.ColumnDefinition;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

public class DecimalValidator implements Validator<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ColumnDefinition columnDefinition;

	public DecimalValidator(final ColumnDefinition columnDefinition) {
		this.columnDefinition = columnDefinition;
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		
		if(value.indexOf(".") !=-1) {
			String number = value.substring(0, value.indexOf("."));
			String fraction = value.substring(value.indexOf(".")+1);
			if(number.length() > columnDefinition.getColumnSize()) {
				return ValidationResult.error("maximum decimal "+columnDefinition.getColumnSize());
			}
			if(fraction.length() > 10) {
				return ValidationResult.error("maximum fraction 10");
			}
		}
		
		if(value.length() > columnDefinition.getColumnSize()) {
			return ValidationResult.error("maximum decimal "+columnDefinition.getColumnSize());
		}
		
		return ValidationResult.ok();
	}

}
