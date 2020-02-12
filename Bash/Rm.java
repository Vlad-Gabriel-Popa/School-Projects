//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Rm implements ICommand {

    private static Rm instance;
    private CurrentDirectory currentDirectory;
    private Path path;
    private PrintWriter errorFile;

    private Rm(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        this.currentDirectory = currentDirectory;
        this.path = new Path(path);
        this.errorFile = errorFile;
    }

    public static Rm getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        if (instance == null) {
            instance = new Rm(errorFile, currentDirectory, path);
            return instance;
        }
        instance.setCurrentDirectory(currentDirectory);
        instance.setPath(new Path(path));
        return instance;
    }

    private void setCurrentDirectory(CurrentDirectory currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    private void setPath(Path path) {
        this.path = path;
    }

    @Override
    public void execute() {
        if(currentDirectory.get().rm(path) == 1){
            errorFile.println("rm: cannot remove " +  path.getFullPath()  + ": No such file or directory");
        }
    }
}
