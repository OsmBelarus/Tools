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

public class StreetNameBe extends StreetName {

    @Override
    protected String getRodavyTermin(String prevText) {
        String t = term.getNameBe();

        if (t.startsWith("у")) {// ў?
            prevText = prevText.trim();
            if (prevText.length() > 0) {
                switch (prevText.charAt(prevText.length() - 1)) {
                case 'у':
                case 'е':
                case 'ы':
                case 'а':
                case 'о':
                case 'э':
                case 'я':
                case 'і':
                case 'ю':
                    t = "ў" + t.substring(1);
                }
            }
        }
        return t;
    }

    @Override
    public String getIndexText() {
        switch (term.getRodBe()) {
        case MUZ:
            switch (index) {
            case 2:
            case 3:
                return index + "-і";
            }
            return index + "-ы";
        case ZAN:
            return index + "-я";
        case NI:
            return index + "-е";
        default:
            throw new RuntimeException();
        }
    }
}
