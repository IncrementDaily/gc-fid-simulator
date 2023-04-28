package combustionEnthalpyDataScript;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CSVWriter {
    public void writeCSV(String fileName, List<String[]> data) {
        File file = new File(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String[] row : data) {
                writer.write(String.join(",", row));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing the CSV file: " + e);
        }
    }



    private static List<String[]> csv = new ArrayList<>();

    public static BufferedReader getDataFileReader(String filepath){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return reader;
    }
    public static int getDataFileLineCount(String filePath){
        int lineCount = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            while (reader.readLine() != null){
                lineCount++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineCount;
    }
    public static String readDataLine(BufferedReader reader){
        String dataLine = null;
        try {
            dataLine = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataLine;
    }
    public static void closeDataFile(BufferedReader reader){
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        csv.add(new String[] {"CAStoMF", "CAS_ID", "numberC", "numberH", "numberO", "numberN", "numberS", "numberFl", "numberCl", "numberBr", "numberI"});














//        HashMap<String,String> casToSourceCodeString = new HashMap<>();
//        HashMap<String,String> casToMolecularFormula = new HashMap<>();
//
//        String sourceCodeStringFilePath = "src/main/java/combustionEnthalpyDataScript/enum";
//        BufferedReader sourceCodeStringFilereader = getDataFileReader(sourceCodeStringFilePath);

//        String line;
//        while ((line = readDataLine(sourceCodeStringFilereader)) != null){
//            String casNumber = "";
//            casToSourceCodeString.put(casNumber, line);
//
//
//
//
//        }




    }
}
