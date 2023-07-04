package ch.so.agi.ogd;

import org.jboss.elemento.IsElement;

import com.google.gwt.core.client.GWT;

import elemental2.core.JsArray;
import elemental2.core.JsBoolean;
import elemental2.core.JsString;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLHeadingElement;
import elemental2.dom.HTMLTableCellElement;
import elemental2.dom.HTMLTableElement;
import elemental2.dom.HTMLTableRowElement;
import elemental2.dom.HTMLTableSectionElement;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;

import java.util.Collections;
import java.util.List;

import org.dominokit.domino.ui.badges.Badge;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;

public class MetadataElement implements IsElement<HTMLElement> {

    private final HTMLElement root;

    public MetadataElement(JsPropertyMap<?> datasetObj, JsPropertyMap<?>resource, MyMessages messages) {
        root = div().element();
        
        String description = ((JsString) datasetObj.get("Description")).normalize();
        if (description != null) {
            root.appendChild(p().css("meta-dataset-description-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString(description)).element());            
        }

        String resourceDescription;
        if (resource.get("Description")!=null) {
            resourceDescription = ((JsString)resource.get("Description")).normalize();
        } else {
            resourceDescription = "Zusätzliche Ressourcen-Info. Fehler beim Herstellen des Meta-XTF. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy.";
        }
        if (resourceDescription != null) {
            root.appendChild(p().css("meta-dataset-description-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString(resourceDescription)).element());            
        }

        HTMLHeadingElement period = h(5, "Zeitraum").element();
        root.appendChild(period);

        root.appendChild(p().css("meta-dataset-description-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString("todo")).element());

        HTMLHeadingElement contact = h(5, "Kontakt").element();
        root.appendChild(contact);

        root.appendChild(p().css("meta-dataset-description-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString("todo")).element());

        
        root.appendChild(h(5, "Attributbeschreibung").element());
        
        HTMLElement tables = div().element();

//        HTMLElement details = (HTMLElement) DomGlobal.document.createElement("details");
//        details.className = "meta-details";
//        HTMLElement summary = (HTMLElement) DomGlobal.document.createElement("summary");
//        summary.className = "meta-summary";
//        //summary.textContent = ((JsString)resource.get("Attributbeschreibung")).normalize();
//        summary.textContent = "Attributbeschreibung";
       
        HTMLTableElement rootTable = table().element();
        rootTable.appendChild(colgroup()
                .add(col().attr("span", "1").style("width: 25%"))
                .add(col().attr("span", "1").style("width: 50%"))
                .add(col().attr("span", "1").style("width: 13%"))
                .add(col().attr("span", "1").style("width: 12%"))
                .element());
        HTMLTableSectionElement tableHead = thead()
                .add(tr()
                        .add(th().style("font-weight: 400; font-style: italic;").add("Name"))
                        .add(th().style("font-weight: 400; font-style: italic;").add("Beschreibung")) 
                        .add(th().style("font-weight: 400; font-style: italic;").add("Datentyp"))
                        .add(th().style("font-weight: 400; font-style: italic;").add("Pflichtattribut")))
                .element();
        rootTable.appendChild(tableHead);
        
        JsPropertyMap<?> classDescription = Js.cast(resource.get("ClassDescription"));
        JsArray<?> attrDescs = Js.cast(classDescription.get("AttributeDescription"));
        
        for (int i=0; i<attrDescs.length; i++) {
            JsPropertyMap<?> attrObj = Js.cast(attrDescs.getAt(i));
            
            HTMLTableSectionElement tbodyParent = tbody().element();
            HTMLTableRowElement tr = tr().element();
//            HTMLTableCellElement tdSublayerIcon = null;
//            
//            JsPropertyMap<?> datasetObj = Js.cast(dataset);
//                        
//            JsArray<?> resources;
//            try {
//                resources = Js.cast(datasetObj.get("Resources"));                
//            } catch (java.lang.ClassCastException e) {                
//                JsPropertyMap<?> resourcesMap = Js.cast(datasetObj.get("Resources"));      
//                resources = JsArray.of(resourcesMap);
//            }
            
//            List<JsPropertyMap<?>> resourcesList
//            for (int i=0; i<resources.length; i++) {
//                
//            }
//            datasetObj.set("Resources", datasetObj.get("Resources"));
                        
            // Icon
//            if (resources.length > 1) {
//                //HTMLElement sublayersIcon = Icons.ALL.file_multiple_outline_mdi().style().setCursor("pointer").get().element();
//                HTMLElement sublayersIcon = Icons.ALL.plus_mdi().style().setCursor("pointer").get().element();
//                tdSublayerIcon = td().add(sublayersIcon).element();
//                tr.appendChild(tdSublayerIcon);
//            } else {
//                //HTMLElement layerIcon = Icons.ALL.file_outline_mdi().style().setCursor("pointer").get().element();
//                HTMLElement layerIcon = Icons.ALL.file_outline_mdi().element();
//                tr.appendChild(td().add(layerIcon).element());
//            }
            
            String attrName = ((JsString) attrObj.get("Name")).normalize();
            tr.appendChild(td().css("attr-cell").add(attrName).element()); 
            String attrDescription = ((JsString) attrObj.get("Description")).normalize();
            tr.appendChild(td().css("attr-cell").add(attrDescription).element()); 
            String attrDataType = ((JsString) attrObj.get("DataType")).normalize();
            tr.appendChild(td().css("attr-cell").add(attrDataType).element()); 
            Boolean attrIsMandatory = (Boolean) attrObj.get("isMandatory");
            tr.appendChild(td().css("attr-cell").add(attrIsMandatory ? "ja" : "nein").element()); 

            tbodyParent.appendChild(tr);
            rootTable.appendChild(tbodyParent);
        }


        HTMLElement p = p().css("meta-table-description-paragraph").add(rootTable).element();
        root.appendChild(p);
        
        
//        if (resourceDescription!= null) {
//            //shortDescription =  tableInfo.getShortDescription().replace("a href", "a class=\"default-link\" href");
//        } else {
//            resourceDescription = "";
//        }
        
//        HTMLElement p = p().css("meta-table-description-paragraph")
//                .add(div().add(resourceDescription))
//                        //.add(div().style("font-style: italic;")
//                                //.textContent(messages.meta_details_p_header_table() + ": "))
//                        //.add(div().textContent(tableInfo.getSqlName()))
//                       // .add(div().style("font-style: italic;")
//                           //     .textContent(messages.meta_details_p_header_description() + ": "))
//                        //.add(div().innerHtml(SafeHtmlUtils.fromTrustedString(shortDescription))))
//                .element();               

//        details.appendChild(summary);
//        details.appendChild(p);
//        tables.appendChild(details);

        root.appendChild(p().css("meta-tables-paragraph").add(tables).element()); 
        


    }
    @Override
    public HTMLElement element() {
        return root;
    }

}
