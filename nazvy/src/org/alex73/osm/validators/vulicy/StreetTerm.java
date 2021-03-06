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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Сьпіс родавых тэрмінаў з пазначэньнем іх роду ў беларускай і расейскай мовах.
 */
public enum StreetTerm {

    вуліца("улица", StreetTermRod.ZAN, "вуліца", StreetTermRod.ZAN, "ул.", "вул.", "ул"),

    няма("", StreetTermRod.NI, "", StreetTermRod.NI),

    бульвар("бульвар", StreetTermRod.MUZ, "бульвар", StreetTermRod.MUZ, "бул."),

    завулак("переулок", StreetTermRod.MUZ, "завулак", StreetTermRod.MUZ, "пер.", "зав.", "завул.", "пер"),

    тупік("тупик", StreetTermRod.MUZ, "тупік", StreetTermRod.MUZ),

    праспект("проспект", StreetTermRod.MUZ, "праспект", StreetTermRod.MUZ, "просп."),

    праезд("проезд", StreetTermRod.MUZ, "праезд", StreetTermRod.MUZ, "пр."),

    тракт("тракт", StreetTermRod.MUZ, "тракт", StreetTermRod.MUZ),

    мост("мост", StreetTermRod.MUZ, "мост", StreetTermRod.MUZ),

    спуск("спуск", StreetTermRod.MUZ, "спуск", StreetTermRod.MUZ),

    уезд("въезд", StreetTermRod.MUZ, "уезд", StreetTermRod.MUZ, "ўезд"),

    алея("аллея", StreetTermRod.ZAN, "алея", StreetTermRod.ZAN),

    плошча("площадь", StreetTermRod.ZAN, "плошча", StreetTermRod.ZAN),

    набярэжная("набережная", StreetTermRod.ZAN, "набярэжная", StreetTermRod.ZAN),

    пляцоўка("площадка", StreetTermRod.ZAN, "пляцоўка", StreetTermRod.ZAN),

    пуцеправод("путепровод", StreetTermRod.MUZ, "пуцеправод", StreetTermRod.MUZ, "пуцепровад"),
    
    шаша("шоссе", StreetTermRod.NI, "шаша", StreetTermRod.ZAN);

    private final String nameRu;
    private final StreetTermRod rodRu;
    private final String nameBe;
    private final StreetTermRod rodBe;
    private final Set<String> variants;

    StreetTerm(String nameRu, StreetTermRod rodRu, String nameBe, StreetTermRod rodBe, String... variants) {
        this.nameRu = nameRu;
        this.rodRu = rodRu;
        this.nameBe = nameBe;
        this.rodBe = rodBe;
        this.variants = new TreeSet<>(Arrays.asList(variants));
        this.variants.add(nameBe);
        this.variants.add(nameRu);
    }

    public String getNameRu() {
        return nameRu;
    }

    public StreetTermRod getRodRu() {
        return rodRu;
    }

    public String getNameBe() {
        return nameBe;
    }

    public StreetTermRod getRodBe() {
        return rodBe;
    }

    public Set<String> getVariants() {
        return variants;
    }
}
