//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Pwd implements ICommand {

    private static Pwd instance;
    private CurrentDirectory currentDirectory;
    private PrintWriter outputFile;
    private PrintWriter errorFile;

    private Pwd(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory) {
        this.currentDirectory = currentDirectory;
        this.outputFile = outputFile;
        this.errorFile = errorFile;
    }

    public static Pwd getInstance(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory) {
        if (instance == null) {
            instance = new Pwd(outputFile, errorFile, currentDirectory);
            return instance;
        }
        instance.setCurrentDirectory(currentDirectory);
        return instance;
    }

    private void setCurrentDirectory(CurrentDirectory currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void execute() {
        outputFile.println(currentDirectory.get().pwd());
    }
}
