//Popa Vlad-Gabriel 323CB Tema3
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, FileNotFoundException {

        File fileIn = new File(args[0]);
        Scanner sc = new Scanner(fileIn);
        PrintWriter output = new PrintWriter(new FileWriter(args[1]));
        PrintWriter error = new PrintWriter(new FileWriter(args[2]));

        //cream folderul root si il setam ca director de lucru
        FileSystemComponent root = new Directory("", null);
        CurrentDirectory currentDirectory = CurrentDirectory.getInstance(root);
        FileSystemComponent.setCurrentDirectory(currentDirectory);

        //cream un invocator de comanda
        //comenzile vor fi create cu un factory in interiorul acestui obiect
        CommandInvoker commandInvoker = CommandInvoker.getInstance();

        int commandNumber = 1;
        while(sc.hasNextLine()){
            //citim o comanda
            String line = sc.nextLine();
            List<String> parameters = Arrays.asList(line.split(" "));
            output.println(commandNumber);
            error.println(commandNumber);
            commandNumber++;
            //setam comanda cu parametrii primiti si o executam
            commandInvoker.setCommand(output, error, currentDirectory, parameters);
            commandInvoker.executeCommand();
        }

        output.close();
        error.close();
        sc.close();
    }
}