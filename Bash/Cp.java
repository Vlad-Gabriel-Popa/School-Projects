//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Cp implements ICommand {

    private static Cp instance;
    private CurrentDirectory currentDirectory;
    private Path source;
    private Path destination;
    private PrintWriter errorFile;

    private Cp(PrintWriter errorFile, CurrentDirectory currentDirectory, String source, String destination) {
        this.currentDirectory = currentDirectory;
        this.source = new Path(source);
        this.destination = new Path(destination);
        this.errorFile = errorFile;
    }


    public static Cp getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String source, String destination) {
        if (instance == null) {
            instance = new Cp(errorFile, currentDirectory, source, destination);
            return instance;
        }
        instance.setCurrentDirectory(currentDirectory);
        instance.setSource(new Path(source));
        instance.setDestination(new Path(destination));
        return instance;
    }

    private void setCurrentDirectory(CurrentDirectory currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    private void setSource(Path source) {
        this.source = source;
    }

    private void setDestination(Path destination) {
        this.destination = destination;
    }

    //functia se ocupa atat de executarea comenzii cat si de afisarea erorilor
    @Override
    public void execute() {
        switch (currentDirectory.get().cp(source, destination)) {
            case 1:
                errorFile.println("cp: cannot copy " + source.getFullPath() + ": No such file or directory");
                break;
            case 2:
                errorFile.println("cp: cannot copy into " + destination.getFullPath() + ": No such directory");
                break;
            case 3:
                errorFile.println("cp: cannot copy " + source.getFullPath() + ": Node exists at destination");
                break;

        }
    }
}
