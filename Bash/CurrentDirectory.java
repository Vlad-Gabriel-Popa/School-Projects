//Popa Vlad-Gabriel 323CB Tema3
public class CurrentDirectory {
    private static CurrentDirectory instance;
    private FileSystemComponent currentDirectory;


    private CurrentDirectory(FileSystemComponent currentDirectory){
        this.currentDirectory = currentDirectory;
    }

    public static CurrentDirectory getInstance(FileSystemComponent currentDirectory){
        if(instance == null){
            instance = new CurrentDirectory(currentDirectory);
            return instance;
        }
        instance.setCurrentDirectory(currentDirectory);
        return instance;
    }

    public void setCurrentDirectory(FileSystemComponent currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public FileSystemComponent get(){
        return currentDirectory;
    }
}
