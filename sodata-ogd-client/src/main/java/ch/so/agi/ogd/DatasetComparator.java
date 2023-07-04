package ch.so.agi.ogd;

import static elemental2.dom.DomGlobal.console;

import java.util.Comparator;

import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import elemental2.core.JsString;

public class DatasetComparator implements Comparator<Object> {

    @Override
    public int compare(Object o1, Object o2) {
        JsPropertyMap<?> jObj1 = Js.cast(o1);
        
//        console.log(o1);
//        console.log(jObj1.get(""));
        
        String title1 = ((JsString) jObj1.get("Title")).normalize();
        JsPropertyMap<?> jObj2 = Js.cast(o2);
        String title2 = ((JsString) jObj2.get("Title")).normalize();
        
        
//        String string0 = o1.getTitle().toLowerCase();
//        String string1 = o2.getTitle().toLowerCase();
//        string0 = string0.replace("ä", "a");
//        string0 = string0.replace("ö", "o");
//        string0 = string0.replace("ü", "u");
//        string1 = string1.replace("ä", "a");
//        string1 = string1.replace("ö", "o");
//        string1 = string1.replace("ü", "u");
        return title1.compareTo(title2);
    }

}
