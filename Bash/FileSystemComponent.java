//Popa Vlad-Gabriel 323CB Tema3
import java.io.PrintWriter;

//clasa abstracta ce sta la baza sistemului de fisiere(composite pattern)
public abstract class FileSystemComponent implements Comparable<FileSystemComponent> {

    private String componentName;
    private FileSystemComponent parentDirectory;
    //retinem folderul de lucru in mod static
    protected static CurrentDirectory currentDirectory;

    public FileSystemComponent(String componentName, FileSystemComponent parentDirectory) {
        this.componentName = componentName;
        this.parentDirectory = parentDirectory;
    }

    public static void setCurrentDirectory(CurrentDirectory currentDirectory) {
        FileSystemComponent.currentDirectory = currentDirectory;
    }

    public String getName() {
        return componentName;
    }

    public FileSystemComponent getParentDirectory() {
        return parentDirectory;
    }

    //schimba folderul parinte al unei componente
    protected void setParentDirectory(FileSystemComponent parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    //functia care v-a ajuta la sortarea lexicografica a componentelor
    @Override
    public int compareTo(FileSystemComponent fileSystemComponent) {

        return componentName.compareTo(fileSystemComponent.getName());
    }

    //obitine calea absoluta a unei componente de la o cale ce poate fi relativa
    public abstract String getPathOf(Path path);

    //creaza un folder
    public abstract int mkdir(Path path);


    //intoarce calea absoluta a componentei care o apeleaza
    public String pwd() {
        if (parentDirectory == null) {
            //daca se apeleaza pwd din root trebuie sa adaugam caracterul / deoarece
            if (currentDirectory.get() != this) {
                return "";
            } else {
                return "/";
            }
        }
        return parentDirectory.pwd() + "/" + componentName;
    }

    //asemanator pwd dar e folosita in interiorul sistemului de fisiere
    //pentru a evita eroarea in care current
    public String getComponentPath() {
        if (parentDirectory == null) {
            return "";
        }
        return parentDirectory.getComponentPath() + "/" + componentName;
    }

    //spune daca component se afla in subarborele componentei curente
    public boolean hasSubComponent(FileSystemComponent component) {

        if (component == null) {
            return false;
        }
        if (component == this) {
            return true;
        }
        return hasSubComponent(component.getParentDirectory());

    }

    //returneaza componenta aflata la adresa formata de toti tokenii din path cu exceptia ultimilor untreated components tokeni
    public abstract FileSystemComponent getComponentAtPath(Path path, int untreatedComponents);

    public abstract int cd(Path path);

    public abstract int ls(PrintWriter outputFile, Path path, boolean isRecursive);

    public abstract int touch(Path path);

    public abstract int rm(Path path);

    public abstract void copy(Directory destination);

    public abstract int cp(Path source, Path destination);

    public abstract int mv(Path source, Path destination);
}
