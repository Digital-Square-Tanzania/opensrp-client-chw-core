package org.smartregister.chw.core.domain;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class MedicalHistory {

    private String title;
    private List<String> text;
    private List<android.text.SpannableStringBuilder> SpannableStringBuilders;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getText() {
        return text;
    }

    public void setText(List<String> text) {
        this.text = text;
    }

    public void setText(String text) {
        if (this.text == null)
            this.text = new ArrayList<>();

        this.text.add(text);
    }

    //This implementation allows passing of SpannableStringBuilders to allow formatting of the
    //text set into the Medical Histories
    public List<SpannableStringBuilder> getSpannableStringBuilders() {
        return SpannableStringBuilders;
    }

    public void setSpannableStringBuilders(List<SpannableStringBuilder> spannableStringBuilders) {
        SpannableStringBuilders = spannableStringBuilders;
    }
}
