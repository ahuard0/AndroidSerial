package com.huard.androidserial;

import com.github.mikephil.charting.data.Entry;

import java.util.Comparator;

public class EntryXComparator implements Comparator<Entry> {
    @Override
    public int compare(Entry entry1, Entry entry2) {
        return Float.compare(entry1.getX(), entry2.getX());
    }
}
