package com.awaitquality.api.spigot.chat.formatter;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.awaitquality.api.spigot.chat.formatter.Formattable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Formatter {
    public static <T extends Formattable> String format(T t, String string) {
        for (Map.Entry<String, String> entry : t.getPlaceHolders().entrySet()) {
            string = string.replace(entry.getKey(), entry.getValue());
        }
        return ChatUtil.translate(string);
    }

    public static <T extends Formattable> List<String> format(T t, List<String> list) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string : list) {
            arrayList.add(Formatter.format(t, string));
        }
        return arrayList;
    }
}

