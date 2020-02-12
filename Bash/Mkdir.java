//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Mkdir implements ICommand {

    private static Mkdir instance;
    private CurrentDirectory currentDirectory;
    private Path path;
    private PrintWriter errorFile;

    private Mkdir(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        this.currentDirectory = currentDirectory;
        this.path = new Path(path);
        this.errorFile = errorFile;
    }

    public static Mkdir getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        if (instance == null) {
            instance = new Mkdir(errorFile, currentDirectory, path);
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

        switch (currentDirectory.get().mkdir(path)) {
            case 1:
                String fullPath = path.getFullPath();
                int lastFolderIndex = fullPath.lastIndexOf("/");
                String parentPath = fullPath.substring(0, lastFolderIndex);
                errorFile.println("mkdir: " + parentPath + ": No such directory");
                break;

            case 2:
		path.refresh();
                fullPath = currentDirectory.get().getPathOf(path);
                errorFile.println("mkdir: cannot create directory " + fullPath + ": Node exists");
                break;
        }
    }
}
