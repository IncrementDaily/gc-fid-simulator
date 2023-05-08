package Scripts;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DataPairingScript {
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
        String orderedCasNumbersData = "src/main/java/combustionEnthalpyDataScript/OrderedCasNumbers.txt";
        BufferedReader casNumReader = getDataFileReader(orderedCasNumbersData);

        String orderedChemicalsData = "src/main/java/combustionEnthalpyDataScript/OrderedChemicals.txt";
        BufferedReader chemReader = getDataFileReader(orderedChemicalsData);

        String chemDataLine;
        String casDataLine;
        while ((casDataLine = readDataLine(casNumReader)) != null) {
            chemDataLine = readDataLine(chemReader);
            String cas = "CASNUMBER";
            String output = chemDataLine.replace(cas,casDataLine);
            System.out.println(output);
        }



    }
}