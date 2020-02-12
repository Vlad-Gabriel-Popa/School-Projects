//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Mv implements ICommand {

    private static Mv instance;
    private CurrentDirectory currentDirectory;
    private Path source;
    private Path destination;
    private PrintWriter errorFile;

    private Mv(PrintWriter errorFile, CurrentDirectory currentDirectory, String source, String destination) {
        this.currentDirectory = currentDirectory;
        this.source = new Path(source);
        this.destination = new Path(destination);
        this.errorFile = errorFile;
    }


    public static Mv getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String source, String destination) {
        if (instance == null) {
            instance = new Mv(errorFile, currentDirectory, source, destination);
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

    @Override
    public void execute() {
        switch (currentDirectory.get().mv(source, destination)){
            case 1:
                errorFile.println("mv: cannot move " + source.getFullPath() + ": No such file or directory");
                break;
            case 2:
                errorFile.println("mv: cannot move into " + destination.getFullPath() + ": No such directory");
                break;
            case 3:
                errorFile.println("mv: cannot move " + source.getFullPath() + ": Node exists at destination");
                break;
        }
    }
}
