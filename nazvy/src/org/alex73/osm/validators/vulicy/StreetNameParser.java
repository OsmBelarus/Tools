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

public class StreetNameParser {
    public static StreetName parse(String name) throws ParseException {
        StreetName orig;
        if (name.equals("Набережная улица") || name.equals("ул. Набережная") || name.equals("улица Набережная")
                || name.equals("Набярэжная вул.") || name.equals("Набережная ул.") || name.equals("Набережная Улица")
                || name.equals("Набережная")) {
            orig = new StreetName();
            orig.name = "Набережная";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("улица Троицкая набережная")) {
            orig = new StreetName();
            orig.name = "Троицкая";
            orig.term = StreetTerm.набярэжная;
        } else if (name.equals("1-я Набережная улица")) {
            orig = new StreetName();
            orig.name = "Набережная";
            orig.index = 1;
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("2-я Набережная улица")) {
            orig = new StreetName();
            orig.name = "Набережная";
            orig.index = 2;
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("Верхняя Набережная улица")) {
            orig = new StreetName();
            orig.name = "Верхняя Набережная";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("Нижняя Набережная улица")) {
            orig = new StreetName();
            orig.name = "Нижняя Набережная";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("20-й дивизии")) {
            orig = new StreetName();
            orig.name = "20-й дивизии";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("улица 6-й Гвардейской Армии")) {
            orig = new StreetName();
            orig.name = "6-й Гвардейской Армии";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("МКАД")) {
            orig = new StreetName();
            orig.name = "МКАД";
            orig.term = StreetTerm.няма;
        } else if (name.equals("улица 5-й Форт")) {
            orig = new StreetName();
            orig.name = "5-й Форт";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("улица Красная Площадь")) {
            orig = new StreetName();
            orig.name = "Красная Площадь";
            orig.term = StreetTerm.вуліца;
        } else if (name.equals("улица Тупик")) {
            orig = new StreetName();
            orig.name = "Тупик";
            orig.term = StreetTerm.вуліца;
        } else {
            orig = new StreetName();
            orig.parseAny(name);
        }
        return orig;
    }

    public static String fix(String name) {
        name = name.replace(".", ". ").replace("-й", "-й ").replace("-ый", "-ый ").replace("-я", "-я ");
        name = name.replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ").trim();

        return name;
    }
}
