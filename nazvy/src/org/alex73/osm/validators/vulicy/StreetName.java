/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.alex73.osm.validators.vulicy;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreetName {

    public Integer index;
    public StreetTerm term;
    public String name;
    public Boolean prym;

    static final Pattern RE_INDEX = Pattern.compile("([0-9]{1,})\\-?(я|й|ый|ий|шы|гі|і|ці|ты|ы|ой|ая)");

    public void parseAny(String nameOrig) throws ParseException {
        List<String> words = new ArrayList<>();
        for (String w : nameOrig.split("\\s+")) {
            words.add(w.trim());
        }

        // шукаем родавы тэрмін
        rt: for (StreetTerm st : StreetTerm.values()) {
            for (int i = 0; i < words.size(); i++) {
                String w = words.get(i);
                if (st.getVariants().contains(w.toLowerCase())) {
                    term = st;
                    words.remove(i);
                    break rt;
                }
            }
        }

        if (!nameOrig.contains("Линия") && !nameOrig.contains("Тупик") && !nameOrig.contains("Переезд")) {
            // для "Ліній" індэксы толькі ў назьве, акрамя "Другая Шостая Линия" :)
            for (int i = 0; i < words.size(); i++) {
                Matcher m = RE_INDEX.matcher(words.get(i));
                if (m.matches()) {
                    if (index != null) {
                        throw new ParseException("Зашмат індэксаў: " + nameOrig, 0);
                    }
                    index = Integer.parseInt(m.group(1));
                    if (index > 20) {
                        index = null;
                    } else {
                        words.remove(i);
                        i--;
                    }
                }
            }
        }

        if (words.isEmpty()) {
            return;
        }

        name = merge(words);
    }

    public String getRightName() {
        StringBuilder o = new StringBuilder(100);
        if (prym != null && prym) {
            if (index != null) {
                o.append(getIndexText()).append(' ');
            }
            o.append(name).append(' ');
            o.append(getRodavyTermin(o.toString()));
        } else {
            if (index != null) {
                o.append(getIndexText()).append(' ');
            }
            o.append(getRodavyTermin(o.toString())).append(' ');
            o.append(name);
        }
        return o.toString().trim();
    }

    protected String getRodavyTermin(String prevText) {
        return term.getNameRu();
    }

    @Override
    public String toString() {
        return (index != null ? index + "_" : "") + name + "_" + term;
    }

    public String getIndexText() {
        if (term == null) {
            System.err.println("Родавы тэрмин ня вызначаны: " + this);
            return index + "-???";
        }
        switch (term.getRodRu()) {
        case MUZ:
            return index + "-й";
        case ZAN:
            return index + "-я";
        case NI:
            return index + "-е";
        default:
            throw new RuntimeException();
        }
    }

    String merge(List<String> words) {
        StringBuilder o = new StringBuilder(200);
        for (String w : words) {
            o.append(' ').append(w);
        }
        return o.toString().trim();
    }
}
