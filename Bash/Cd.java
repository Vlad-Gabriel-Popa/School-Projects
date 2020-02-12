//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

public class Cd implements ICommand {
    //asiguram existenta unei singure instante a comenzii cd pe tot parcursul programului
    private static Cd instance;
    //trebuie sa stim care este directorul curent pentru a putea aplica functia pe el
    private CurrentDirectory currentDirectory;
    //calea catre folderul pe care se face cd
    private Path path;
    private PrintWriter errorFile;

    private Cd(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        this.currentDirectory = currentDirectory;
        this.path = new Path(path);
        this.errorFile = errorFile;
    }

    //daca Cd a mai fost instantiat, schimbam parametrii cu cei noi si intoarcem instanta
    public static Cd getInstance(PrintWriter errorFile, CurrentDirectory currentDirectory, String path) {
        if (instance == null) {
            instance = new Cd(errorFile, currentDirectory, path);
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
        //functia se ocupa atat de executarea comenzii cat si de afisarea erorilor
        if (currentDirectory.get().cd(path) == 1) {
            errorFile.println("cd: " + path.getFullPath() + ": No such directory");
        }
    }
}
