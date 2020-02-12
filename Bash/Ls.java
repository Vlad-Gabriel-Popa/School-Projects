//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Ls implements ICommand {

    private static Ls instance;
    private CurrentDirectory currentDirectory;
    Path path;
    private boolean isRecursive;
    private PrintWriter outputFile;
    private PrintWriter errorFile;

    private Ls(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory, String path, boolean isRecursive) {
        this.currentDirectory = currentDirectory;
        this.path = new Path(path);
        this.isRecursive = isRecursive;
        this.outputFile = outputFile;
        this. errorFile = errorFile;
    }

    public static Ls getInstance(PrintWriter outputFile, PrintWriter errorFile, CurrentDirectory currentDirectory, String path, boolean isRecursive) {
        if (instance == null) {
            instance = new Ls(outputFile, errorFile, currentDirectory, path, isRecursive);
            return instance;
        }

        instance.setCurrentDirectory(currentDirectory);
        instance.setPath(new Path(path));
        instance.setRecursive(isRecursive);
        return instance;
    }

    private void setCurrentDirectory(CurrentDirectory currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    private void setPath(Path path) {
        this.path = path;
    }

    private void setRecursive(boolean recursive) {
        isRecursive = recursive;
    }

    @Override
    public void execute() {
        if(currentDirectory.get().ls(outputFile, path, isRecursive) == 1){
            errorFile.println("ls: " + path.getFullPath() +": No such directory");
        }
    }
}
