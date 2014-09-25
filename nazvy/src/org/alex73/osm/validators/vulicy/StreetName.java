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

        for (int i = 0; i < words.size(); i++) {
            StreetTerm rt = parseRodavyTermin(words.get(i));
            if (rt != null) {
                if (term != null) {
                    throw new ParseException("Зашмат родавых тэрмінаў: " + nameOrig, 0);
                }
                term = rt;
                words.remove(i);
                i--;
            }
        }

        if (!nameOrig.contains("Линия") && !nameOrig.contains("Тупик")) {
            // для "Ліній" індэксы толькі ў назве, акрамя "Другая Шостая Линия" :)
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
        return "idx:" + index + "_" + name + "_" + term;
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

    StreetTerm parseRodavyTermin(String w) {
        switch (w.toLowerCase()) {
        case "бульвар":
        case "бул.":
            return StreetTerm.бульвар;
        case "улица":
        case "вуліца":
        case "ул.":
        case "вул.":
        case "ул":
            return StreetTerm.вуліца;
        case "аллея":
        case "алея":
            return StreetTerm.алея;
        case "переулок":
        case "завулак":
        case "пер.":
        case "зав.":
        case "завул.":
        case "пер":
            return StreetTerm.завулак;
        case "тупик":
            return StreetTerm.тупік;
        case "проспект":
        case "праспект":
        case "просп.":
            return StreetTerm.праспект;
        case "проезд":
        case "пр.":
        case "праезд":
            return StreetTerm.праезд;
        case "тракт":
            return StreetTerm.тракт;
        case "мост":
            return StreetTerm.мост;
        case "площадь":
        case "плошча":
            return StreetTerm.плошча;
        case "шоссе":
            return StreetTerm.шаша;
        case "набережная":
        case "набярэжная":
            return StreetTerm.набярэжная;
        case "площадка":
        case "пляцоўка":
            return StreetTerm.пляцоўка;
        case "путепровод":
        case "пуцепровад":
        case "пуцеправод":
            return StreetTerm.пуцеправод;
        case "спуск":
            return StreetTerm.спуск;
        case "въезд":
        case "уезд":
        case "ўезд":
            return StreetTerm.уезд;
        default:
            return null;
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
