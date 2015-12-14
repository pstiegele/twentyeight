package de.paulsapp.twentyeight;

import java.text.DecimalFormat;

import com.github.mikephil.charting.utils.ValueFormatter;

public class MyValueFormatter implements ValueFormatter {

    private DecimalFormat mFormat;

    public MyValueFormatter() {
        mFormat = new DecimalFormat("###,###,##"); // use one decimal
    }

    @Override
    public String getFormattedValue(float value) {
        return mFormat.format(value) + "°"; // append a dollar-sign
    }
}	