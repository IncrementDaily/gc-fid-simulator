package Scripts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JSoupScriptCAStoFormula {
    public static Double getFormulaFromCAS(String chemicalCASNumber, List<String> notFound, Object[][] data, int rowToWriteTo) {
        String baseUrl = "https://webbook.nist.gov/cgi/cbook.cgi/";
        String queryParams = String.format("?ID=%s", chemicalCASNumber.replace(" ", "+"));

        try {
            // Connect to the NIST Chemistry Webbook website
            Document document;

            try (InputStream writer = new FileInputStream(new File("src\\main\\java\\com\\karimbouchareb\\chromatographysimulator\\pages-for-cas\\" + chemicalCASNumber))) {
                document = Jsoup.parse(writer, "UTF-8", baseUrl);
            }
//                    connect(baseUrl + queryParams).get();

            // Select the element that has the chemical formula string or mark the cas as not found
            if (document.select("#main > h1").toString().contains("Registry Number Not Found")) {
                data[rowToWriteTo][0] = chemicalCASNumber;
                notFound.add(chemicalCASNumber);
            }else {
                System.out.println(chemicalCASNumber);
                Elements targetElement = document.select("#main > ul:nth-child(2) > li:nth-child(1)");

//                System.out.println(targetElement);

                // Initialize Data Object[][]
                Object[] dataRow = new Object[11];
                // Use RegEx / Pattern Matching to parse number of carbons, number of C, H, O, N, S, F, Cl, Br, I, Si
                String regex = ".*?</strong>";
                String output = targetElement.toString().replaceAll(regex, "");
//                System.out.println(output);
                String regex2 = "<sub>|</sub>|</li>";
                String output2 = output.replaceAll(regex2, "");
//                System.out.println(output2);
                // Search for the letters that correspond to atoms that are relevant for combustionEnthalpy calculation
                // as group1 (atomCount column) and search for 0 or more digits as group2 (value to place in atomCount
                // column).
                Pattern regex3 = Pattern.compile("([CHONSFCBIS][rl]*)([0-9]*)");
                Matcher matcher = regex3.matcher(output2);
                System.out.println(output2);
                data[rowToWriteTo][0] = chemicalCASNumber;
                data[rowToWriteTo][1] = 0;
                data[rowToWriteTo][2] = 0;
                data[rowToWriteTo][3] = 0;
                data[rowToWriteTo][4] = 0;
                data[rowToWriteTo][5] = 0;
                data[rowToWriteTo][6] = 0;
                data[rowToWriteTo][7] = 0;
                data[rowToWriteTo][8] = 0;
                data[rowToWriteTo][9] = 0;
                data[rowToWriteTo][10] = 0;
                int dataColumnIndex = -1;
                while (matcher.find()){
                    String atomCountColumn = matcher.group(1);
                    System.out.print(atomCountColumn);
                    switch (atomCountColumn){
                        case "C": dataColumnIndex = 1;
                        break;
                        case "H": dataColumnIndex = 2;
                        break;
                        case "O": dataColumnIndex = 3;
                        break;
                        case "N": dataColumnIndex = 4;
                        break;
                        case "S": dataColumnIndex = 5;
                        break;
                        case "F": dataColumnIndex = 6;
                        break;
                        case "Cl": dataColumnIndex = 7;
                        break;
                        case "Br": dataColumnIndex = 8;
                        break;
                        case "I": dataColumnIndex = 9;
                        break;
                    }
                    String countValue = matcher.group(2);
                    if (countValue.equals("")) countValue = "1";
                    System.out.print(" " + countValue);
                    System.out.println();
                    data[rowToWriteTo][dataColumnIndex] = countValue;
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static CSVParser getDataParser(){
        try {
            FileReader fileReader = new FileReader("src/main/java/Scripts/LSERdata5-4-2023.csv");
            return CSVFormat.DEFAULT.parse(fileReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeCsvFile(String fileName, String[] header, Object[][] data) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(header).build();
        // Create a BufferedWriter and CSVPrinter
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            // Write the data to the CSV file
            for (Object[] row : data) {
                csvPrinter.printRecord(row);
            }

            // Flush the stream and close the CSVPrinter
            csvPrinter.flush();
        }
    }


    public static void main(String[] args) {
        Object[][] data = new Object[5000][11];
        ArrayList<String> notFound = new ArrayList<>();
        CSVParser parser = getDataParser();

        int rowToWriteTo = 0;
        for (CSVRecord record : parser){
            if (record.get(0).equals("CAS")) continue;
            String cas = record.get(0);
            getFormulaFromCAS(cas, notFound, data, rowToWriteTo);
            data[rowToWriteTo][0] = cas;
            rowToWriteTo++;
        }

        System.out.println(notFound);
        System.out.println("notFound.size() = " + notFound.size());


        // Write the CSV file
        String fileName = "src/main/java/Scripts/example.csv";

        // Define the header for the CSV file
        String[] header = {"CAS", "C", "H", "O", "N","S","F","Cl","Br","I","Si"};

        // Write the file
        try {
            writeCsvFile(fileName, header, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

