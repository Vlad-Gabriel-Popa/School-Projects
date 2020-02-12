//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Touch implements ICommand {

    private static Touch instance;
    private CurrentDirectory currentDirectory;
    private Path path;
    private PrintWriter errorFile;

    private Touch(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        this.currentDirectory = currentDirectory;
        this.path = new Path(path);
        this.errorFile = errorFile;
    }

    public static Touch getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        if (instance == null) {
            instance = new Touch(errorFile, currentDirectory, path);
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
        switch(currentDirectory.get().touch(path)){
            case 1:
                String fullPath = path.getFullPath();
                int lastFolderIndex = fullPath.lastIndexOf("/");
                String parentPath = fullPath.substring(0, lastFolderIndex);
                errorFile.println("touch: " + parentPath + ": No such directory");
                break;

            case 2:
                path.refresh();
                fullPath = currentDirectory.get().getPathOf(path);
                errorFile.println("touch: cannot create file " + fullPath + ": Node exists");
                break;
        }
    }
}
