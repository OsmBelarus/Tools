/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013-2014 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.validators.objects;

import gen.alex73.osm.validators.objects.BaseFilter;
import gen.alex73.osm.validators.objects.Filter;
import gen.alex73.osm.validators.objects.TagList;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.alex73.osm.utils.Belarus;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;

public class BaseCheck {
    protected final MemoryStorage osm;
    protected final Filter filter;

    protected Map<String, Pattern> patternsCache = new HashMap<>();
    protected TagCodeValues filterTags;
    private boolean filterNode, filterWay, filterRelation;
    private ICustomClass customCheck;

    public BaseCheck(Belarus osm, BaseFilter base) throws Exception {
        this.osm = osm;
        this.filter = base.getFilter();
        filterTags = tagsCompile(filter);

        if (filter == null || filter.getOsmTypes() == null) {
            filterNode = true;
            filterWay = true;
            filterRelation = true;
        } else {
            for (String ot : filter.getOsmTypes().split(",")) {
                switch (ot) {
                case "node":
                    filterNode = true;
                    break;
                case "way":
                    filterWay = true;
                    break;
                case "relation":
                    filterRelation = true;
                    break;
                default:
                    throw new RuntimeException("Невядомы osmTypes: " + ot);
                }
            }
        }

        if (base.getCustomClass() != null) {
            customCheck = (ICustomClass) Class.forName(base.getCustomClass()).newInstance();
            customCheck.init(osm);
        }
    }

    public void finish() throws Exception {
        if (customCheck != null) {
            customCheck.finish();
        }
    }

    TagCodeValues tagsCompile(TagList tagList) {
        short[] resultCodes = new short[Short.MAX_VALUE];
        String[] resultValues = new String[Short.MAX_VALUE];
        int count = 0;
        if (tagList != null) {
            for (int i = 0; i < tagList.getTag().size(); i++) {
                String tagName = tagList.getTag().get(i).getName();
                String tagValue = tagList.getTag().get(i).getValue();
                if (tagName.endsWith(":*")) {
                    String prefix = tagName.substring(0, tagName.length() - 1);
                    for (String tn : osm.getTagsPack().getTagNames()) {
                        if (tn.startsWith(prefix)) {
                            resultCodes[count] = osm.getTagsPack().getTagCode(tn);
                            resultValues[count] = tagValue;
                            count++;
                        }
                    }
                } else {
                    resultCodes[count] = osm.getTagsPack().getTagCode(tagName);
                    resultValues[count] = tagValue;
                    count++;
                }
            }
        }

        TagCodeValues result = new TagCodeValues();
        result.codes = Arrays.copyOf(resultCodes, count);
        result.values = Arrays.copyOf(resultValues, count);
        return result;
    }

    public boolean matches(IOsmObject obj) {
        switch (obj.getType()) {
        case IOsmObject.TYPE_NODE:
            if (!filterNode) {
                return false;
            }
            break;
        case IOsmObject.TYPE_WAY:
            if (!filterWay) {
                return false;
            }
            break;
        case IOsmObject.TYPE_RELATION:
            if (!filterRelation) {
                return false;
            }
            break;
        default:
            throw new RuntimeException();
        }
        for (int i = 0; i < filterTags.length(); i++) {
            if (!obj.hasTag(filterTags.codes[i])) {
                return false;
            }
        }
        for (int i = 0; i < filterTags.length(); i++) {
            if (!tagValueAllowed(filterTags.codes[i], filterTags.values[i], obj)) {
                return false;
            }
        }

        if (filter != null && filter.getCustomMethod() != null) {
            boolean r = (boolean) callCustom(filter.getCustomMethod(), obj);
            if (!r) {
                return false;
            }
        }
        return true;
    }
    

    protected Object callCustom(String method, IOsmObject obj) {
        if (customCheck == null) {
            throw new RuntimeException("Custom class not defined");
        }
        try {
            Method m = customCheck.getClass().getMethod(method, IOsmObject.class);
            return m.invoke(customCheck, obj);
        } catch (Exception ex) {
            throw new RuntimeException("Error in custom method: " + method + ": " + ex.getMessage(), ex);
        }
    }

    protected boolean tagValueAllowed(short tagCode, String tagValue, IOsmObject obj) {
        if (tagValue == null) {
            return true;
        }
        String objectValue = obj.getTag(tagCode);
        boolean r = getPatternForValue(tagValue).matcher(objectValue).matches();
        return r;
    }

    protected Pattern getPatternForValue(final String value) {
        Pattern p = patternsCache.get(value);
        if (p == null) {
            p = Pattern.compile(value);
            patternsCache.put(value, p);
        }
        return p;
    }

    protected class TagCodeValues {
        short[] codes;
        String[] values;

        public int length() {
            return codes.length;
        }
    }
}
