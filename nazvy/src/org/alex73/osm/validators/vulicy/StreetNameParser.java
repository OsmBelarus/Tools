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
        name = name.replace("МОПРовс", "Мопровс");
        name = name.replaceAll("([ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮИЩ])", " $1");
        name = name.replaceAll("([ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮИЩ])", " $1");
        name = name.replaceAll("([ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮИЩ])", " $1");
        name = name.replaceAll("([ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮИЩ])", " $1");
        name = name.replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ").trim();
        name = name.replace("- ", "-").replaceAll("\" ([ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮИЩ])", "\"$1");
        name = name.replace("Б С С Р", "БССР");
        name = name.replace("М К А Д", "МКАД");
        name = name.replace("К П С С", "КПСС");
        name = name.replace("С С С Р", "СССР");
        name = name.replace("В Л К С М", "ВЛКСМ");
        name = name.replace("Л К С М Б", "ЛКСМБ");
        name = name.replace("4 пер. Транзитный", "пер. 4-й Транзитный");
        name = name.replace("1905 г. ул.", "ул. 1905 года");
        name = name.replace("8-го Марта", "8 Марта");
        name = name.replace("Восточный 3 пер.", "Восточный 3-й пер.");
        name = name.replace("К. Маркса", "Карла Маркса");
        name = name.replace("Корначенка", "Корначёнка");
        name = name.replace("Корначенок", "Корначёнка");
        name = name.replace("Косачева", "Косачёва");
        name = name.replace("Вишневая", "Вишнёвая");
        name = name.replace("Петренкова Жени", "Жени Петренкова");
        name = name.replace("Я. Коласа", "Якуба Коласа");
        name = name.replace("Я. Купалы", "Янки Купалы");
        name = name.replace("Ф. Скорины", "Франциска Скорины");
        name = name.replace("вуліца Траецкая набярэжная", "Траецкая набярэжная");
        name = name.replace("ул. Даргомыжского ул.", "ул. Даргомыжского");
        name = name.replace("ул. Коммунальный проезд", "Коммунальный проезд");
        name = name.replace("1-й переулок Якуба Коласа пер.", "1-й переулок Якуба Коласа");
        name = name.replace("Латышский проезд улица", "Латышский проезд");
        name = name.replace("Ціраспальская набярэжная вул.", "Ціраспальская набярэжная");
        name = name.replace("ул. Бобруйское Шоссе", "Бобруйское Шоссе");
        name = name.replace("Староостровенское шоссе ул.", "Староостровенское шоссе");
        name = name.replace("ул. набережная Эссы", "набережная Эссы");
        name = name.replace("ул. Набережная Эссы", "Набережная Эссы");
        name = name.replace("ул. Великолукский тракт", "Великолукский тракт");
        name = name.replace("аллея Карла Маркса ул.", "аллея Карла Маркса");
        name = name.replace("Новый просп. ул.", "Новый просп.");
        name = name.replace("ул. Липовая аллея", "Липовая аллея");
        name = name.replace("ул. Витебское шоссе", "Витебское шоссе");
        name = name.replace("Молодежная", "Молодёжная");
        name = name.replace("1 Мозырский пер.", "1-й Мозырский пер.");
        name = name.replace("2 Мозырский пер.", "2-й Мозырский пер.");
        name = name.replace("8 марта", "8 Марта");
        name = name.replace("9 мая", "9 Мая");
        name = name.replace("9 января", "9 Января");
        name = name.replace("Березовская", "Берёзовская");
        name = name.replace("Вишневый", "Вишнёвый");
        name = name.replace("генерала", "Генерала");
        name = name.replace("Озерная", "Озёрная");
        name = name.replace("Рогачевская", "Рогачёвская");
        name = name.replace("Червоный перулок", "Червоный переулок");
        name = name.replace("50 Летия БССР", "50-летия БССР");
        name = name.replaceAll("^Королика$", "Королика Кочесу");
        name = name.replace("Урожаыная", "Урожайная");
        name = name.replace("2 пер. Тургенева", "2-й пер. Тургенева");
        name = name.replace("Демина", "Дёмина");
        name = name.replace("завулак Зялёны", "Зелёный переулок");
        name = name.replace("Краснознаменная", "Краснознамённая");
        name = name.replace("Нормандия-Неман", "Нормандия-Нёман");
        name = name.replace("Пугачева", "Пугачёва");
        name = name.replace("ул. 9-е Января", "ул. 9 Января");
        name = name.replace("беговая улица", "Беговая улица");
        name = name.replace("В. Хоружей", "Веры Хоружей");
        name = name.replace("Житняя улица", "Житная улица");
        name = name.replace("1-й Зап. тупик", "1-й Западный тупик");
        name = name.replace("Францыска Скарыны", "Франциска Скорыны");
        name = name.replace("Рогачевский", "Рогачёвский");
        name = name.replace("ПЕР. 1-Й МАНЕВИЧА", "пер. 1-й Маневича");
        name = name.replace("Пинский проезд улица", "Пинский проезд");
        name = name.replace("Титенский тупик улица", "Титенский тупик");
        name = name.replace("Комунальный переулок улица", "Коммунальный переулок");
        name = name.replace("Коммунальный переулок улица", "Коммунальный переулок");
        name = name.replace("пер Линейный улица", "пер. Линейный");
        name = name.replace("улица Гуляма Якубова", "улица Якубова");
        name = name.replace("а Гуляма Якубова", "Гуляма Якубова");
        name = name.replace("Гуляма Якубова", "Якубова");
        name = name.replace("улица Заборского", "улица Архитектора Заборского");
        name = name.replace("улица Горецкого", "улица Максима Горецкого");
        name = name.replace("3-го Сентября", "3 Сентября");
        name = name.replace("улица Вышелесского", "улица Академика Вышелесского");
        name = name.replace("ул. Вышелесского", "улица Академика Вышелесского");
        name = name.replace(" 120-й Дивизии", " 120 Дивизии");
        name = name.replace("Зубачева", "Зубачёва");
        name = name.replace("Игнатия Домейко", "Игната Домейко");
        name = name.replace("имени газеты \"Звязда\"", "Газеты Звязда");
        name = name.replace("имени газеты \"Правда\"", "Газеты Правда");
        name = name.replace("Иосифа Гашкевича", "Иосифа Гошкевича");
        name = name.replace("Ковалева", "Ковалёва");
        name = name.replace("Комаровское Кольцо", "Комаровское кольцо");
        name = name.replace("Краснозвезд", "Краснозвёзд");
        name = name.replace("Неманская", "Нёманская");
        name = name.replace("Огарева", "Огарёва");
        name = name.replace("Пономарева", "Пономарёва");
        name = name.replace("Пугачевская", "Пугачёвская");
        name = name.replace("Саперов", "Сапёров");
        name = name.replace("Стебенева", "Стебенёва");
        name = name.replace("судмалиса", "Судмалиса");
        name = name.replace("Таежная", "Таёжная");
        name = name.replace("Твердый", "Твёрдый");
        name = name.replace("Чюрлениса", "Чюрлёниса");
        name = name.replace("пр-д Котовского", "проезд Котовского");
        name = name.replace("улица Ширмы", "улица Григория Ширмы");
        name = name.replace("улица Чайкиной", "улица Лизы Чайкиной");
        name = name.replace("улица Чарота", "улица Михася Чарота");
        name = name.replace("улица Сердича", "улица Данилы Сердича");
        name = name.replace("улица Семеняко", "улица Юрия Семеняко");
        name = name.replace("ул .", "ул. ");
        name = name.replace("2-й. ", "2-й ");
        name = name.replace("1йпер. Гоголя", "1-й пер. Гоголя");
        name = name.replace("Пекреулок", "переулок");
        name = name.replace("п-д.", "пр-д.");
        name = name.replace("пе. 8 Марта", "пер. 8 Марта");
        name = name.replace("пере.", "пер.");
        name = name.replace("пр-д.", "пр-д ");
        name = name.replace("Ш ирокая Улица", "Широкая улица");
        name = name.replace("ул,", "ул. ");

        return name;
    }
}
