//package combustionEnthalpyDataScript;
//
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//
//
//public class JSoupScriptMapCASToCombustEnthalpy {
//    public static Double getCombustionEnthalpyNIST(String chemicalCASNumber) {
//        String baseUrl = "https://webbook.nist.gov/cgi/cbook.cgi/";
//        String queryParams = String.format("?ID=%s&Units=SI&cTG=on&cTC=on&cTR=on", chemicalCASNumber.replace(" ", "+"));
//
//        try {
//            // Connect to the NIST Chemistry Webbook website.
//            // Turn on the Condensed Phase Thermochemistry datatables
//            // Turn on the Gas Phase Thermochemistry datatables
//            Document document = Jsoup.connect(baseUrl + queryParams).get();
//
//            // Get any elements that are associated with combustion enthalpy
//            Elements elements = document.getElementsContainingText("ΔcH");
//            Elements labelCells = new Elements();
//            Elements targetCells = new Elements();
//
//            // Find all the cells (tag=td) associated with combustion enthalpy
//            // ( NOTE: exclude any that are just notes explaining an acronym at
//            // the bottom of the page (filter: table class=data) ).
//            for (Element element : elements) {
//                if (element.tag().getName().equals("td") && element.parents().is(".data")){
//                    labelCells.add(element);
//                }
//            }
//
//            // labelCells are all the cells that are immediately to the LEFT of the target cells that
//            // have the combustion enthalpy data. The target cells are the nextSibling() of the label cells.
//            // Use a regex to extract the combustion enthalpy value.
//            Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
//            for (Element labelCell : labelCells){
//                String text = labelCell.nextSibling().toString();
//                Matcher matcher = pattern.matcher(text);
//                if (matcher.find()) {
//                    return Double.parseDouble(matcher.group());
//                }
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static Double getCombustionEnthalpyCHEMEO(String chemicalCASNumber) {
//        String baseUrl = "https://www.chemeo.com/cid/";
//        String queryParams = String.format("?ID=%s&Units=SI&cTG=on&cTC=on&cTR=on", chemicalCASNumber.replace(" ", "+"));
//
//        try {
//            // Connect to the NIST Chemistry Webbook website.
//            // Turn on the Condensed Phase Thermochemistry datatables
//            // Turn on the Gas Phase Thermochemistry datatables
//            Document document = Jsoup.connect(baseUrl + queryParams).get();
//
//            // Get any elements that are associated with combustion enthalpy
//            Elements elements = document.getElementsContainingText("ΔcH");
//            Elements labelCells = new Elements();
//            Elements targetCells = new Elements();
//
//            // Find all the cells (tag=td) associated with combustion enthalpy
//            // ( NOTE: exclude any that are just notes explaining an acronym at
//            // the bottom of the page (filter: table class=data) ).
//            for (Element element : elements) {
//                if (element.tag().getName().equals("td") && element.parents().is(".data")){
//                    labelCells.add(element);
//                }
//            }
//
//            // labelCells are all the cells that are immediately to the LEFT of the target cells that
//            // have the combustion enthalpy data. The target cells are the nextSibling() of the label cells.
//            // Use a regex to extract the combustion enthalpy value.
//            Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
//            for (Element labelCell : labelCells){
//                String text = labelCell.nextSibling().toString();
//                Matcher matcher = pattern.matcher(text);
//                if (matcher.find()) {
//                    return Double.parseDouble(matcher.group());
//                }
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static <E> Set<E> findExclusiveElements(List<E> list1, List<E> list2) {
//        Set<E> set1 = new HashSet<>(list1);
//        Set<E> set2 = new HashSet<>(list2);
//
//        Set<E> exclusiveElements = new HashSet<>(set1);
//        exclusiveElements.addAll(set2);
//
//        Set<E> commonElements = new HashSet<>(set1);
//        commonElements.retainAll(set2);
//
//        exclusiveElements.removeAll(commonElements);
//
//        return exclusiveElements;
//    }
//
//
//    public static void main(String[] args) {
//
//        Map<String, Double> combustionEnthalpyMap = new LinkedHashMap<>(300);
//        List<String> notFoundOnNIST = new ArrayList<>();
//
//        for (Chemical chemical : Chemical.values()) {
//            String chemicalCAS = chemical.getCAS();
//            Double combustionEnthalpy = getCombustionEnthalpyNIST(chemicalCAS);
//            if (combustionEnthalpy != null) {
//                combustionEnthalpyMap.put(chemicalCAS, combustionEnthalpy);
//            }else {
//                notFoundOnNIST.add(chemicalCAS);
//            }
//        }
//
//        System.out.println("notFoundOnNIST = " + notFoundOnNIST);
//
////        for (Chemical chemical : notFoundOnNIST){
////            String chemicalCAS = chemical.getCAS();
////            Double combustionEnthalpy = getCombustionEnthalpyCHEMEO(chemicalCAS);
////            if (combustionEnthalpy != null) {
////                combustionEnthalpyMap.put(chemicalCAS, combustionEnthalpy);
////            }else {
////                System.out.println("CAS NOT FOUND: " + chemicalCAS);
////            }
////        }
//
//
//        // Copy and paste this to a .combustionEnthalpyData.txt
////        for (Map.Entry<String, Double> entry : combustionEnthalpyMap.entrySet()){
////            System.out.println(entry);
////            System.out.println(entry.getKey());
////            System.out.println(entry.getValue());
////        }
////
////        System.out.println("combustionEnthalpyMap.size() = " + combustionEnthalpyMap.size());
//    }
//}
//
