package combustionEnthalpyDataScript;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JSoupScriptGetCasNumbers {
    public static String getCasNumber(String chemicalName) {
        Document backupDocument = null;
        boolean isBackupDocument = false;
        String baseUrl = "https://webbook.nist.gov/cgi/cbook.cgi/";
        String queryParams = String.format("?Name=%s", chemicalName.replace(" ", "+"));

        try {
            // Connect to the NIST Chemistry Webbook website.
            Document document = Jsoup.connect(baseUrl + queryParams).get();

            // Make sure you actually selected a compound.
            // Just arbitrarily trust that one of the options is the right option.
            if (document.getElementsContainingText("Search Results").size()>0){
                // Find a link for one of the "One of these choices?"
                Elements main = document.select("main");
                Elements orderedLists = main.select("ol");
                Element firstLink = orderedLists.select("a[href]").first();
                // Navigate to that link
                String backupURL = firstLink.absUrl("href");
                backupDocument = Jsoup.connect(backupURL).get();
                isBackupDocument = true;
            }

            if (!isBackupDocument){
                // Get any elements that are associated with CAS Number
                Elements elements = document.getElementsContainingText("Cas Registry Number:");

                // Discard any that aren't list items (<li>)
                String rawCASString;
                String patternString = "CAS Registry Number:</strong>\\s*([0-9]+-[0-9]+-[0-9]+)";
                for (Element element : elements){
                    if (element.tag().equals(Tag.valueOf("li"))){
                        rawCASString = element.toString();
                        Pattern pattern = Pattern.compile(patternString);
                        Matcher matcher = pattern.matcher(rawCASString);

                        if (matcher.find()) {
                            String casNumber = matcher.group(1);
                            return casNumber;
                        } else {
                            return "No CAS Number found.";
                        }
                    }
                }
            }

            if (isBackupDocument){
                // Get any elements that are associated with CAS Number
                Elements elements = backupDocument.getElementsContainingText("Cas Registry Number:");

                // Discard any that aren't list items (<li>)
                String rawCASString;
                String patternString = "CAS Registry Number:</strong>\\s*([0-9]+-[0-9]+-[0-9]+)";
                for (Element element : elements){
                    if (element.tag().equals(Tag.valueOf("li"))){
                        rawCASString = element.toString();
                        Pattern pattern = Pattern.compile(patternString);
                        Matcher matcher = pattern.matcher(rawCASString);

                        if (matcher.find()) {
                            String casNumber = matcher.group(1);
                            return casNumber;
                        } else {
                            return "No CAS Number found.";
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

        int counter = 0;

        Map<String, String> chemicalToCasNumber = new LinkedHashMap<>();

//        for (Chemical chemical : Chemical.values()) {
//            String chemicalName = chemical.getName();
//            String casNumber = getCasNumber(chemicalName);
//            if (casNumber != null) {
//                chemicalToCasNumber.put(chemicalName, casNumber);
//            }else{
//                System.out.println("NO CAS FOUND: " + chemicalName);
//
//            }
//        }
//
//        for (Map.Entry<String,String> entry : chemicalToCasNumber.entrySet()){
//            System.out.println(entry);
//        }
//
//        System.out.println(Chemical.values().length);

        System.out.println("combustionEnthalpyMap.size() = " + chemicalToCasNumber.size());

        // JUST GET THESE CAS NUMBERS MANUALLY THERES ONLY 5 of THEM NO NEED TO REWRITE THE SCRIPT TO CATCH THESE
        // EDGE CASES

    }
}

